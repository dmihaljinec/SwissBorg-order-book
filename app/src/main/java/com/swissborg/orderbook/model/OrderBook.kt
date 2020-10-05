package com.swissborg.orderbook.model

data class OrderBook(
    val price: Double,
    val count: Int,
    val amount: Double
)