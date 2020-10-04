package com.swissborg.orderbook.android.bitfinex

import com.google.gson.Gson
import com.swissborg.orderbook.repository.OrderBook
import com.swissborg.orderbook.repository.OrderBookDataSource
import com.swissborg.orderbook.repository.OrderBookRepository
import com.swissborg.orderbook.repository.Ticker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@FlowPreview
@ExperimentalCoroutinesApi
class OrderBookDataSourceImpl(
    private val channelConnection: ChannelConnection,
    private val gson: Gson
) : OrderBookDataSource {

    override suspend fun getOrderBook(): Flow<List<OrderBook>> {
        TODO("Not yet implemented")
    }


    override suspend fun getTicker(currencyPair: OrderBookRepository.CurrencyPair): Flow<Ticker> {
        return TickerChannel(
            currencyPair,
            channelConnection,
            gson
        ).getTicker()
            .map { apiTicker -> apiTicker.toTicker() }
    }
}