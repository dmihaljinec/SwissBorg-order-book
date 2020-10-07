package com.swissborg.orderbook.android.ui.viewmodel

import com.swissborg.orderbook.model.OrderBook
import java.text.DecimalFormat

data class OrderBookViewModel(
    val orderBook: OrderBook
) {
    fun buyPrice(): String {
        return when (val price = orderBook.buy?.price) {
            null -> ""
            else -> DecimalFormat("#,###.##").format(price)
        }
    }

    fun buyAmount(): String {
        return when (val amount = orderBook.buy?.amount) {
            null -> ""
            else -> DecimalFormat("#,###.########").format(amount)
        }
    }

    fun sellPrice(): String {
        return when (val price = orderBook.sell?.price) {
            null -> ""
            else -> DecimalFormat("#,###.##").format(price)
        }
    }

    fun sellAmount(): String {
        return when (val amount = orderBook.sell?.amount) {
            null -> ""
            else -> DecimalFormat("#,###.########").format(amount)
        }
    }
}

fun OrderBook.toViewModel() = OrderBookViewModel(this)
