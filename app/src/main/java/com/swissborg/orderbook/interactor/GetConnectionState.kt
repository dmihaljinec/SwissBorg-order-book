package com.swissborg.orderbook.interactor

import com.swissborg.orderbook.repository.OrderBookRepository
import javax.inject.Inject

class GetConnectionState @Inject constructor(
    private val repository: OrderBookRepository
) {
    suspend operator fun invoke() = repository.getConnectionState()
}
