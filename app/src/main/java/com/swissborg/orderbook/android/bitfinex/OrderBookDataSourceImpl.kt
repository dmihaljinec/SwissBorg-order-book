package com.swissborg.orderbook.android.bitfinex

import com.google.gson.Gson
import com.swissborg.orderbook.model.ConnectionState
import com.swissborg.orderbook.model.OrderBook
import com.swissborg.orderbook.model.Ticker
import com.swissborg.orderbook.repository.OrderBookDataSource
import com.swissborg.orderbook.repository.OrderBookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@FlowPreview
@ExperimentalCoroutinesApi
class OrderBookDataSourceImpl(
    private val channelConnection: ChannelConnection,
    private val gson: Gson
) : OrderBookDataSource {

    override suspend fun getTicker(currencyPair: OrderBookRepository.CurrencyPair): Flow<Ticker> {
        return TickerChannel(
            currencyPair,
            channelConnection,
            gson
        ).getTicker()
            .map { apiTicker -> apiTicker.toTicker() }
    }

    override suspend fun getOrderBooks(currencyPair: OrderBookRepository.CurrencyPair): Flow<List<OrderBook>> {
        return OrderBooksChannel(
            currencyPair,
            channelConnection,
            gson
        ).getOrderBooks()
            .map { apiOrderBookList ->
                // apiOrderBookList contains both buy and sell orders, we'll split them into separate
                // lists so that we can create list of OrderBook's. Each OrderBook will contain buy
                // order and sell order. First OrderBook in a list will have highest buy price and
                // lowest sell price, and so on
                val buyOrders = apiOrderBookList
                    .filter { apiOrderBook -> apiOrderBook.amount > 0.0 }
                    .sortedByDescending { it.price }
                    .toMutableList()
                val sellOrders = apiOrderBookList
                    .filter { apiOrderBook -> apiOrderBook.amount < 0.0 }
                    .sortedBy { it.price }
                    .toMutableList()
                val orderBookList = mutableListOf<OrderBook>()
                while (true) {
                    val buyApiOrderBook = if (buyOrders.isNotEmpty()) buyOrders.removeAt(0) else null
                    val sellApiOrderBook = if (sellOrders.isNotEmpty()) sellOrders.removeAt(0) else null
                    orderBookList.add(OrderBook(buyApiOrderBook?.toOrder(), sellApiOrderBook?.toOrder()))
                    if (buyOrders.isEmpty() && sellOrders.isEmpty()) break
                }
                orderBookList
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getConnectionState(): Flow<ConnectionState> {
        return channelConnection.connectionState()
    }
}
