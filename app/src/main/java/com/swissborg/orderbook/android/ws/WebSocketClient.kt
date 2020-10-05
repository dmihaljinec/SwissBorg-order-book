package com.swissborg.orderbook.android.ws

import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.Looper
import com.github.oxo42.stateless4j.StateMachine
import com.github.oxo42.stateless4j.StateMachineConfig
import com.github.oxo42.stateless4j.delegates.Action
import com.github.oxo42.stateless4j.delegates.Trace
import com.swissborg.orderbook.android.bitfinex.waitForValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import okhttp3.*
import okio.ByteString
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.pow

@ExperimentalCoroutinesApi
class WebSocketClient @Inject constructor(
    connectivityManager: ConnectivityManager,
    private val httpClient: OkHttpClient,
    private val url: String
) : WebSocketListener() {
    private val stateMachineConfig = StateMachineConfig<State, Trigger>()
    private val stateMachine = StateMachine(State.CLOSED, stateMachineConfig)
    private var webSocket: WebSocket? = null
    private val _webSocketState = MutableStateFlow(State.CLOSED)
    val webSocketState: StateFlow<State>
        get() = _webSocketState
    private var messageChannel: BroadcastChannel<String> = createMessageChannel()
    @FlowPreview
    val messages: Flow<String>
        get() = messageChannel.asFlow()
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            isNetworkAvailable = true
            stateMachine.fire(Trigger.NETWORK_AVAILABLE)
            Timber.d("onAvailable")
        }

        override fun onLost(network: Network) {
            isNetworkAvailable = false
            stateMachine.fire(Trigger.NETWORK_LOST)
            Timber.d("onLost")
        }
    }
    private var isNetworkAvailable: Boolean = true
    private var reopenCount = 0
    private val reopenRunnable = Runnable {
        reopenCount++
        stateMachine.fire(Trigger.OPEN)
    }
    private val handler = Handler(Looper.getMainLooper())

    init {
        initStateMachine()
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    suspend fun waitForState(state: State) {
        if (stateMachine.state == state) return
        when (state) {
            State.OPENED -> stateMachine.fire(Trigger.OPEN)
            State.CLOSED -> stateMachine.fire(Trigger.CLOSE)
            else -> Unit
        }
        webSocketState.waitForValue(state)
        if (webSocketState.value == state) return
        else throw IllegalStateException("Expected $state state")
    }

    fun connect() {
        stateMachine.fire(Trigger.OPEN)
        Timber.d("connect")
    }

    fun disconnect() {
        stateMachine.fire(Trigger.CLOSE)
        Timber.d("disconnect")
    }

    fun send(request: String) {
        webSocket?.send(request)
        Timber.d("send $request")
    }

    private fun initStateMachine() {
        stateMachineConfig.configure(State.CLOSED)
            .onEntry(Action { cancelAll() })
            .permit(Trigger.OPEN, State.OPENING)
            .permit(Trigger.WS_OPENED, State.OPENED)
            .ignore(Trigger.CLOSE)
            .ignore(Trigger.WS_FAILED)
            .ignore(Trigger.WS_CLOSING)
            .ignore(Trigger.WS_CLOSED)
            .ignore(Trigger.NETWORK_AVAILABLE)
            .ignore(Trigger.NETWORK_LOST)

        stateMachineConfig.configure(State.OPENING)
            .onEntry(Action { open() })
            .permit(Trigger.WS_OPENED, State.OPENED)
            .permit(Trigger.CLOSE, State.CLOSING)
            .permit(Trigger.WS_FAILED, State.CLOSED)
            .permit(Trigger.WS_CLOSING, State.CLOSED)
            .permit(Trigger.WS_CLOSED, State.CLOSED)
            .permit(Trigger.NETWORK_LOST, State.WAITING_FOR_REOPEN)
            .ignore(Trigger.OPEN)
            .ignore(Trigger.NETWORK_AVAILABLE)

        stateMachineConfig.configure(State.OPENED)
            .onEntry(Action { resetReopen() })
            .permit(Trigger.WS_FAILED, State.WAITING_FOR_REOPEN)
            .permit(Trigger.WS_CLOSING, State.CLOSING)
            .permit(Trigger.CLOSE, State.CLOSING)
            .permit(Trigger.WS_CLOSED, State.CLOSED)
            .permit(Trigger.NETWORK_LOST, State.WAITING_FOR_REOPEN)
            .ignore(Trigger.OPEN)
            .ignore(Trigger.WS_OPENED)
            .ignore(Trigger.NETWORK_AVAILABLE)

        stateMachineConfig.configure(State.CLOSING)
            .onEntry(Action { close() })
            .permit(Trigger.WS_CLOSED, State.CLOSED)
            .permit(Trigger.WS_FAILED, State.CLOSED)
            .permit(Trigger.NETWORK_LOST, State.CLOSED)
            .ignore(Trigger.CLOSE)
            .ignore(Trigger.OPEN)
            .ignore(Trigger.WS_OPENED)
            .ignore(Trigger.WS_CLOSING)
            .ignore(Trigger.NETWORK_AVAILABLE)

        stateMachineConfig.configure(State.WAITING_FOR_REOPEN)
            .onEntry(Action { reopen() })
            .permitIf(Trigger.OPEN, State.OPENING)  { isNetworkAvailable }
            .permitReentryIf(Trigger.OPEN) { !isNetworkAvailable }
            .permit(Trigger.NETWORK_AVAILABLE, State.OPENING)
            .permit(Trigger.CLOSE, State.CLOSED)
            .ignore(Trigger.NETWORK_LOST)
            .ignore(Trigger.WS_FAILED)

        stateMachine.setTrace(object : Trace<State, Trigger> {
            override fun transition(trigger: Trigger?, source: State?, destination: State?) {
                destination?.run {
                    _webSocketState.value = this
                    Timber.d("$source --> $this")
                }
            }

            override fun trigger(trigger: Trigger?) = Unit
        })

        stateMachine.onUnhandledTrigger { state, trigger -> Timber.d("Unhandled trigger $trigger in state $state") }
    }

    private fun open() {
        val request = Request.Builder()
            .url(url)
            .build()
        httpClient.newWebSocket(request, this)
    }

    private fun close() {
        webSocket?.close(WS_NORMAL_CLOSE_CODE, "Socket closed")
    }

    private fun cancelAll() {
        httpClient.dispatcher.cancelAll()
        messageChannel.close()
        messageChannel = createMessageChannel()
    }

    private fun reopen() {
        val exponentialBackOffDelay = MAX_BACKOFF_INTERVAL
            .coerceAtMost(
                INITIAL_BACKOFF_INTERVAL * (2.0).pow(reopenCount.toDouble()).toLong()
            )
        handler.postDelayed(reopenRunnable, exponentialBackOffDelay)
    }

    private fun resetReopen() {
        reopenCount = 0
    }

    private fun createMessageChannel(): BroadcastChannel<String> = BroadcastChannel(CHANNEL_BUFFER_CAPACITY)

    override fun onOpen(webSocket: WebSocket, response: Response) {
        this.webSocket = webSocket
        stateMachine.fire(Trigger.WS_OPENED)
        Timber.d("onOpen")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        stateMachine.fire(Trigger.WS_FAILED)
        Timber.d("onFailure ${t.message}")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        stateMachine.fire(Trigger.WS_CLOSING)
        Timber.d("onClosing $code $reason")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        this.webSocket = null
        stateMachine.fire(Trigger.WS_CLOSED)
        Timber.d("onClosed $code $reason")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        messageChannel.offer(text)
        Timber.d("onMessage(text) $text")
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        val text = bytes.utf8()
        messageChannel.offer(text)
        Timber.d("onMessage(bytes) $text")
    }

    enum class State {
        OPENED,
        CLOSED,
        OPENING,
        CLOSING,
        WAITING_FOR_REOPEN
    }

    enum class Trigger {
        OPEN,
        CLOSE,
        WS_OPENED,
        WS_FAILED,
        WS_CLOSING,
        WS_CLOSED,
        NETWORK_AVAILABLE,
        NETWORK_LOST,
    }

    companion object {
        private const val INITIAL_BACKOFF_INTERVAL = 1000L // 1 second
        private const val MAX_BACKOFF_INTERVAL = 120000L // 2 minutes
        private const val CHANNEL_BUFFER_CAPACITY = 1000 //
        const val WS_NORMAL_CLOSE_CODE = 1000
    }
}
