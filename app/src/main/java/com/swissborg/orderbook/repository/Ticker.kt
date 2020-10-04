package com.swissborg.orderbook.repository

data class Ticker(
    val lastTradePrice: Double,
    val volume: Double,
    val low: Double,
    val high: Double,
    val dailyChange: Double
)