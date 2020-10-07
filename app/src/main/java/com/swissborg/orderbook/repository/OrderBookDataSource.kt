package com.swissborg.orderbook.repository

import com.swissborg.orderbook.model.ConnectionState
import com.swissborg.orderbook.model.OrderBook
import com.swissborg.orderbook.model.Ticker
import kotlinx.coroutines.flow.Flow

interface OrderBookDataSource {
    suspend fun getOrderBooks(currencyPair: OrderBookRepository.CurrencyPair): Flow<List<OrderBook>>
    suspend fun getTicker(currencyPair: OrderBookRepository.CurrencyPair): Flow<Ticker>
    suspend fun getConnectionState(): Flow<ConnectionState>
}
