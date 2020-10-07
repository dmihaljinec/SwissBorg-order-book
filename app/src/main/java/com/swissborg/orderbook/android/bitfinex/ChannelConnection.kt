package com.swissborg.orderbook.android.bitfinex

import com.google.gson.Gson
import com.swissborg.orderbook.android.ws.WebSocketClient
import com.swissborg.orderbook.model.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
@FlowPreview
class ChannelConnection(
    private val webSocketClient: WebSocketClient,
    private val gson: Gson
) : Channel.Connection {
    private var connectedChannels = 0

    override suspend fun connect() {
        webSocketClient.waitForState(WebSocketClient.State.OPENED)
        connectedChannels++ // TODO this should be more error prone regarding channels
    }

    override suspend fun disconnect() {
        if (--connectedChannels == 0) webSocketClient.disconnect()
    }

    override fun send(request: String) {
        webSocketClient.send(request)
    }

    override fun connectionState(): Flow<ConnectionState> {
        return webSocketClient.webSocketState.map { state ->
            when (state) {
                WebSocketClient.State.OPENED -> ConnectionState.CONNECTED
                WebSocketClient.State.CLOSED -> ConnectionState.DISCONNECTED
                WebSocketClient.State.OPENING,
                WebSocketClient.State.CLOSING,
                WebSocketClient.State.WAITING_FOR_REOPEN -> ConnectionState.CONNECTING
            }
        }
    }

    override fun messages(channel: Channel): Flow<String> {
        return webSocketClient.messages
            .filter { message ->
                if (message.isEvent(gson)) {
                    val channelEvent = message.channelEvent(gson)
                    if (channelEvent != null) {
                        channelEvent.channel == channel.name && channelEvent.pair == channel.currencyPair
                    } else {
                        val unsubscribeEvent = message.unsubscribeEvent(gson)
                        if (unsubscribeEvent != null) {
                            unsubscribeEvent.channelId == channel.id
                        } else {
                            message.errorEvent(gson) != null
                        }
                    }
                } else {
                    // channel should receive only messages with it's channel id
                    val channelId = message.channelMessage(gson)?.channelId ?: -1
                    channel.id == channelId
                }
            }
            .flowOn(Dispatchers.IO)
    }
}

suspend fun <T> Flow<T>.waitForValue(value: T) {
    try {
        collect { _value ->
            if (value == _value) throw IllegalStateException("")
        }
    } catch (t: Throwable) {

    }
}
