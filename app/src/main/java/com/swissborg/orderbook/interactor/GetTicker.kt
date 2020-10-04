package com.swissborg.orderbook.interactor

import com.swissborg.orderbook.repository.OrderBookRepository
import javax.inject.Inject

class GetTicker @Inject constructor(
    private val repository: OrderBookRepository
) {
    suspend operator fun invoke(currencyPair: OrderBookRepository.CurrencyPair) = repository.getTicker(currencyPair)
}