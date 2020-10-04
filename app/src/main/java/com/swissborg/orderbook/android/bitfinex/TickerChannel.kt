package com.swissborg.orderbook.android.bitfinex

import com.google.gson.Gson
import com.swissborg.orderbook.repository.OrderBookRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

@ExperimentalCoroutinesApi
@FlowPreview
class TickerChannel(
    currencyPair: OrderBookRepository.CurrencyPair,
    connection: ChannelConnection,
    gson: Gson
) : Channel(Api.CHANNEL_TICKER, currencyPair.toApiString(), connection, gson) {

    private fun processEvent(event: String) {
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

    suspend fun getTicker(): Flow<Api.Ticker> {
        return getMessages()
            .transform { message ->
                if (message.isEvent(gson)) processEvent(message)
                else message.toTicker(gson)?.run { emit(this) }
            }
    }

    override fun subscribe() {
        connection.send(gson.toJson(Subscribe(channel = name, pair = currencyPair), Subscribe::class.java))
    }
}
