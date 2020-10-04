package com.swissborg.orderbook.repository

import kotlinx.coroutines.flow.Flow

class OrderBookRepository(
    private val dataSource: OrderBookDataSource
) {

    suspend fun getTicker(currencyPair: CurrencyPair): Flow<Ticker> = dataSource.getTicker(currencyPair)

    enum class CurrencyPair {
        BTCUSD
    }
}