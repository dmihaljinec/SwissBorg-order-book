package com.swissborg.orderbook.repository

import kotlinx.coroutines.flow.Flow

interface OrderBookDataSource {
    suspend fun getOrderBook(): Flow<List<OrderBook>>
    suspend fun getTicker(currencyPair: OrderBookRepository.CurrencyPair): Flow<Ticker>
}