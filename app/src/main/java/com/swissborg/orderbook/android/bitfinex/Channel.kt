package com.swissborg.orderbook.android.bitfinex

import com.github.oxo42.stateless4j.StateMachine
import com.github.oxo42.stateless4j.StateMachineConfig
import com.github.oxo42.stateless4j.delegates.Action
import com.github.oxo42.stateless4j.delegates.Trace
import com.google.gson.Gson
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
    protected val gson: Gson
) {
    private val stateMachineConfig = StateMachineConfig<State, Trigger>()
    protected val stateMachine = StateMachine(State.UNSUBSCRIBED, stateMachineConfig)
    private val _state = MutableStateFlow(State.UNSUBSCRIBED)
    val state: StateFlow<State>
        get() = _state
    var id: Int = -1

    init {
        initStateMachine()
    }

    suspend fun waitForState(state: State) {
        if (stateMachine.state == state) return
        when (state) {
            State.SUBSCRIBED -> stateMachine.fire(Trigger.SUBSCRIBE)
            State.UNSUBSCRIBED -> stateMachine.fire(Trigger.UNSUBSCRIBE)
            else -> Unit
        }
        this.state.waitForValue(state)
        if (this.state.value == state) return
        else throw IllegalStateException("Expected $state state")
    }

    protected suspend fun getMessages(): Flow<String> {
        connection.connect()
        stateMachine.fire(Trigger.SUBSCRIBE)
        return flowOf(connection.connectionState(), connection.getMessages(this))
            .flattenMerge()
            .transform {
                if (it is Connection.State) connectionStateChanged(it)
                else if (it is String) emit(it)
            }
            .onCompletion {
                stateMachine.fire(Trigger.UNSUBSCRIBE)
                connection.disconnect()
                Timber.d("onCompletion done")
            }
            .catch {
                Timber.w("${it.message}")
            }
    }

    abstract protected fun subscribe()

    protected fun unsubscribe() {
        if (id != -1) {
            connection.send(gson.toJson(Unsubscribe(channelId = id), Unsubscribe::class.java))
        }
    }

    private fun connectionStateChanged(state: Connection.State) {
        when (state) {
            Connection.State.CONNECTED -> stateMachine.fire(Trigger.CONNECTED)
            Connection.State.CONNECTING -> stateMachine.fire(Trigger.CONNECTING)
            Connection.State.DISCONNECTED -> stateMachine.fire(Trigger.DISCONNECTED)
        }
    }

    protected fun processError(error: Error?) {
        error?.run {
            val description = when (error.code) {
                10301 -> {
                    stateMachine.fire(Trigger.ERROR_ALREADY_SUBSCRIBED)
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
                    ""
                }
            }
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
            .onEntry(Action { subscribe() })
            .permit(Trigger.SUBSCRIBED, State.SUBSCRIBED)
            .permit(Trigger.CONNECTING, State.WAITING_TO_RESUBSCRIBE)
            .permit(Trigger.DISCONNECTED, State.UNSUBSCRIBED)
            .permit(Trigger.ERROR_ALREADY_SUBSCRIBED, State.SUBSCRIBED)
            .permit(Trigger.ERROR, State.UNSUBSCRIBED)
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
                    Timber.d("$source --> $this")
                }
            }

            override fun trigger(trigger: Trigger?) = Unit
        })

        stateMachine.onUnhandledTrigger { state, trigger -> Timber.d("Unhandled trigger $trigger in state $state") }
    }

    interface Connection {
        suspend fun connect()
        suspend fun disconnect()
        fun send(request: String)
        fun connectionState(): Flow<State>
        fun getMessages(channel: Channel): Flow<String>

        enum class State {
            CONNECTED, // ready for subscription
            CONNECTING, // not connected, either connection/disconnecting or reconnecting
            DISCONNECTED, // disconnected
        }
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
    }
}
