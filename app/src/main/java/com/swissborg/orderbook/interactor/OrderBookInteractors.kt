package com.swissborg.orderbook.interactor

import javax.inject.Inject

data class OrderBookInteractors @Inject constructor(
    val getTicker: GetTicker
)