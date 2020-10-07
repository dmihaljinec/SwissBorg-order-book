package com.swissborg.orderbook.model

data class Ticker(
    val lastTradePrice: Double,
    val volume: Double,
    val low: Double,
    val high: Double,
    val dailyChangePercentage: Double
)
