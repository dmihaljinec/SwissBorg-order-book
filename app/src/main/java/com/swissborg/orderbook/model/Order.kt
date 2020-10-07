package com.swissborg.orderbook.model

data class Order(
    val price: Double,
    val amount: Double,
    val count: Int
)
