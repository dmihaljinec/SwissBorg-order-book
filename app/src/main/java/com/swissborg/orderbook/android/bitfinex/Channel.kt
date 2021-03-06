package com.swissborg.orderbook.android.bitfinex

import android.os.Handler
import android.os.Looper
import com.github.oxo42.stateless4j.StateMachine
import com.github.oxo42.stateless4j.StateMachineConfig
import com.github.oxo42.stateless4j.delegates.Action
import com.github.oxo42.stateless4j.delegates.Trace
import com.google.gson.Gson
import com.swissborg.orderbook.model.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import timber.log.Timber

@ExperimentalCoroutinesApi
@FlowPreview
abstract class Channel(
    val name: String,
    val currencyPair: String,
    protected val connection: Connection,
    protected val gson: Gson,
    private val tag: String
) {
    private val stateMachineConfig = StateMachineConfig<State, Trigger>()
    protected val stateMachine = StateMachine(State.UNSUBSCRIBED, stateMachineConfig)
    private val _state = MutableStateFlow(State.UNSUBSCRIBED)
    val state: StateFlow<State>
        get() = _state
    var id: Int = -1
    private val watchDogRunnable = Runnable { stateMachine.fire(Trigger.SUBSCRIBING_TIMEOUT) }
    private val handler = Handler(Looper.getMainLooper())

    init {
        initStateMachine()
    }

    protected suspend fun messages(): Flow<String> {
        return flowOf(connection.connectionState(), connection.messages(this))
            .onStart {
                connection.connect()
                stateMachine.fire(Trigger.SUBSCRIBE)
            }
            .flattenMerge()
            .transform {
                if (it is ConnectionState) connectionStateChanged(it)
                else if (it is String) emit(it)
            }
            .onCompletion {
                stateMachine.fire(Trigger.UNSUBSCRIBE)
                connection.disconnect()
                Timber.tag(tag)
                Timber.d("onCompletion done")
            }
            .flowOn(Dispatchers.IO)
            .catch {
            }
    }

    abstract protected fun subscribe()

    protected fun unsubscribe() {
        if (id != -1) {
            connection.send(gson.toJson(Unsubscribe(channelId = id), Unsubscribe::class.java))
        }
    }

    private fun connectionStateChanged(state: ConnectionState) {
        when (state) {
            ConnectionState.CONNECTED -> stateMachine.fire(Trigger.CONNECTED)
            ConnectionState.CONNECTING -> stateMachine.fire(Trigger.CONNECTING)
            ConnectionState.DISCONNECTED -> stateMachine.fire(Trigger.DISCONNECTED)
        }
    }

    protected fun processEvent(event: String) {
        Timber.tag(tag)
        Timber.d("Process event $event")
        event.channelEvent(gson)?.run {
            when (this.event) {
                Api.EVENT_SUBSCRIBED -> {
                    event.subscribeEvent(gson)?.run {
                        id = this.channelId
                    }
                    stateMachine.fire(Trigger.SUBSCRIBED)
                }
                Api.EVENT_UNSUBSCRIBED -> stateMachine.fire(Trigger.UNSUBSCRIBED)
                Api.EVENT_ERROR -> processError(event.errorEvent(gson))
                else -> Unit
            }
        }
    }

    protected fun processError(error: Error?) {
        error?.run {
            val description = when (error.code) {
                10301 -> {
                    stateMachine.fire(Trigger.ERROR_ALREADY_SUBSCRIBED)
                    id = error.channelId
                    "Failed channel subscription: already subscribed"
                }
                10001 -> {
                    stateMachine.fire(Trigger.ERROR)
                    "Unknown pair: $currencyPair"
                }
                10305 -> {
                    stateMachine.fire(Trigger.ERROR)
                    "Reached limit of open channels"
                }
                10400 -> {
                    stateMachine.fire(Trigger.ERROR)
                    "Failed channel un-subscription: channel $id not found"
                }
                else -> {
                    stateMachine.fire(Trigger.ERROR)
                    "search for description at https://docs.bitfinex.com/docs/abbreviations-glossary"
                }
            }
            Timber.tag(tag)
            Timber.d("Error code: ${error.code} $description")
        }
    }

    private fun initStateMachine() {
        stateMachineConfig.configure(State.UNSUBSCRIBED)
            .permit(Trigger.SUBSCRIBE, State.SUBSCRIBING)
            .ignore(Trigger.UNSUBSCRIBED)
            .ignore(Trigger.CONNECTED)
            .ignore(Trigger.CONNECTING)
            .ignore(Trigger.DISCONNECTED)
            .ignore(Trigger.ERROR_ALREADY_SUBSCRIBED)
            .ignore(Trigger.ERROR)

        stateMachineConfig.configure(State.SUBSCRIBING)
            .onEntry(Action {
                subscribe()
                startWatchDog()
            })
            .onExit(Action {
                cancelWatchDog()
            })
            .permit(Trigger.SUBSCRIBED, State.SUBSCRIBED)
            .permit(Trigger.CONNECTING, State.WAITING_TO_RESUBSCRIBE)
            .permit(Trigger.DISCONNECTED, State.UNSUBSCRIBED)
            .permit(Trigger.ERROR_ALREADY_SUBSCRIBED, State.SUBSCRIBED)
            .permit(Trigger.ERROR, State.UNSUBSCRIBED)
            .permitReentry(Trigger.SUBSCRIBING_TIMEOUT)
            .ignore(Trigger.CONNECTED)

        stateMachineConfig.configure(State.SUBSCRIBED)
            .permit(Trigger.UNSUBSCRIBE, State.UNSUBSCRIBING)
            .permit(Trigger.CONNECTING, State.WAITING_TO_RESUBSCRIBE)
            .permit(Trigger.DISCONNECTED, State.UNSUBSCRIBED)
            .ignore(Trigger.SUBSCRIBE)
            .ignore(Trigger.CONNECTED)
            .ignore(Trigger.ERROR_ALREADY_SUBSCRIBED)
            .ignore(Trigger.ERROR) // TODO check if this is ok

        stateMachineConfig.configure(State.WAITING_TO_RESUBSCRIBE)
            .permit(Trigger.CONNECTED, State.SUBSCRIBING)
            .permit(Trigger.DISCONNECTED, State.UNSUBSCRIBED)
            .ignore(Trigger.SUBSCRIBE)
            .ignore(Trigger.CONNECTING)
            .ignore(Trigger.ERROR_ALREADY_SUBSCRIBED)
            .ignore(Trigger.ERROR)

        stateMachineConfig.configure(State.UNSUBSCRIBING)
            .onEntry(Action { unsubscribe() })
            .permit(Trigger.UNSUBSCRIBED, State.UNSUBSCRIBED)
            .permit(Trigger.CONNECTING, State.UNSUBSCRIBED)
            .permit(Trigger.DISCONNECTED, State.UNSUBSCRIBED)
            .permit(Trigger.ERROR, State.UNSUBSCRIBED)
            .ignore(Trigger.CONNECTED)
            .ignore(Trigger.ERROR_ALREADY_SUBSCRIBED)

        stateMachine.setTrace(object : Trace<State, Trigger> {
            override fun transition(trigger: Trigger?, source: State?, destination: State?) {
                destination?.run {
                    _state.value = this
                    Timber.tag(tag)
                    Timber.d("$source --> $this")
                }
            }

            override fun trigger(trigger: Trigger?) {
                Timber.tag(tag)
                Timber.d("fire trigger: $trigger")
            }
        })

        stateMachine.onUnhandledTrigger { state, trigger ->
            Timber.tag(tag)
            Timber.d("Unhandled trigger $trigger in state $state")
        }
    }

    /**
     * Sometimes we do not receive subscribed message, in that case we shell retry
     */
    private fun startWatchDog() {
        handler.postDelayed(watchDogRunnable, SUBSCRIBING_TIMEOUT_INTERVAL)
    }

    private fun cancelWatchDog() {
        handler.removeCallbacks(watchDogRunnable)
    }

    interface Connection {
        suspend fun connect()
        suspend fun disconnect()
        fun send(request: String)
        fun connectionState(): Flow<ConnectionState>
        fun messages(channel: Channel): Flow<String>
    }

    enum class State {
        UNSUBSCRIBED,
        SUBSCRIBING,
        SUBSCRIBED,
        WAITING_TO_RESUBSCRIBE,
        UNSUBSCRIBING,
    }

    enum class Trigger {
        SUBSCRIBE,
        SUBSCRIBED,
        UNSUBSCRIBE,
        UNSUBSCRIBED,
        ERROR_ALREADY_SUBSCRIBED,
        ERROR,
        CONNECTED,
        CONNECTING,
        DISCONNECTED,
        SUBSCRIBING_TIMEOUT,
    }

    companion object {
        private const val SUBSCRIBING_TIMEOUT_INTERVAL = 5000L // 5 seconds
    }
}
