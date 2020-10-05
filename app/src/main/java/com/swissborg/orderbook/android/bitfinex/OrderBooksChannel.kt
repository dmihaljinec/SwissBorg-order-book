package com.swissborg.orderbook.android.bitfinex

import com.google.gson.Gson
import com.swissborg.orderbook.repository.OrderBookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform

@ExperimentalCoroutinesApi
@FlowPreview
class OrderBooksChannel(
    currencyPair: OrderBookRepository.CurrencyPair,
    connection: ChannelConnection,
    gson: Gson
) : Channel(
    Api.CHANNEL_ORDER_BOOK,
    currencyPair.toApiString(),
    connection,
    gson,
    OrderBooksChannel::class.java.simpleName
) {
    suspend fun getOrderBooks(): Flow<List<Api.OrderBook>> {
        val orderBook = mutableMapOf<Double, Api.OrderBook>()
        return messages()
            .transform { message ->
                if (message.isEvent(gson)) processEvent(message)
                else {
                    val list = gson.fromJson(message, List::class.java)
                    list?.forEach { item ->
                        (item as? Api.OrderBook)?.run {
                            orderBook[this.price] = this
                        }
                    }
                    emit(orderBook.values.toList().sortedBy { it.price })
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override fun subscribe() {
        connection.send(gson.toJson(SubscribeBook(channel = name, pair = currencyPair), SubscribeBook::class.java))
    }
}