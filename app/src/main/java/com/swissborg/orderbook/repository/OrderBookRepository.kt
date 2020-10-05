package com.swissborg.orderbook.repository

import com.swissborg.orderbook.model.ConnectionState
import com.swissborg.orderbook.model.OrderBook
import com.swissborg.orderbook.model.Ticker
import kotlinx.coroutines.flow.Flow

class OrderBookRepository(
    private val dataSource: OrderBookDataSource
) {

    suspend fun getTicker(currencyPair: CurrencyPair): Flow<Ticker> = dataSource.getTicker(currencyPair)

    suspend fun getOrderBooks(currencyPair: CurrencyPair): Flow<List<OrderBook>> = dataSource.getOrderBooks(currencyPair)

    suspend fun getConnectionState(): Flow<ConnectionState> = dataSource.getConnectionState()

    enum class CurrencyPair {
        BTCUSD
    }
}