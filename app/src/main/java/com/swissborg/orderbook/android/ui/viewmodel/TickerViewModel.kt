package com.swissborg.orderbook.android.ui.viewmodel

import com.swissborg.orderbook.model.Ticker
import java.text.DecimalFormat

data class TickerViewModel(
    val ticker: Ticker
) {
    fun lastTradePrice(): String = DecimalFormat("#,###.##").format(ticker.lastTradePrice)
    fun volume(): String = DecimalFormat("#,###.####").format(ticker.volume)
    fun low(): String = DecimalFormat("#,###.##").format(ticker.low)
    fun high(): String = DecimalFormat("#,###.##").format(ticker.high)
    fun dailyChangePercentage() = "${DecimalFormat("#,###.####").format(ticker.dailyChangePercentage * 100)}%"
}

fun Ticker.toViewModel(): TickerViewModel = TickerViewModel(this)
