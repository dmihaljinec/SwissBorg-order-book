package com.swissborg.orderbook.repository

data class OrderBook(
    val price: Double,
    val count: Int,
    val amount: Double
)