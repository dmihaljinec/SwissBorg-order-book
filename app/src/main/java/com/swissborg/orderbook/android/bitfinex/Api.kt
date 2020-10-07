package com.swissborg.orderbook.android.bitfinex

import com.google.gson.*
import com.swissborg.orderbook.model.Order
import com.swissborg.orderbook.model.Ticker
import com.swissborg.orderbook.repository.OrderBookRepository
import java.lang.reflect.Type
import kotlin.math.absoluteValue

class Api {
    data class Ticker(
        val bid: Double,
        val bidSize: Double,
        val ask: Double,
        val askSize: Double,
        val dailyChange: Double,
        val dailyChangePercentage: Double,
        val lastPrice: Double,
        val volume: Double,
        val high: Double,
        val low: Double
    ) {
        fun toTicker(): com.swissborg.orderbook.model.Ticker {
            return Ticker(
                lastPrice,
                volume,
                low,
                high,
                dailyChangePercentage
            )
        }
    }

    data class OrderBook(
        val price: Double,
        val count: Int,
        val amount: Double
    ) {
        fun toOrder(): Order {
            // Amount in orderBook contains information if this is a sell or buy order.
            // Positive numbers are buy order, while negative are sell order. When we split
            // buying and selling OrderBooks, Order amount should always be positive.
            return Order(
                price,
                amount.absoluteValue,
                count
            )
        }
    }

    companion object {
        const val WS_URL_V1 = "wss://api-pub.bitfinex.com/ws/1"
        const val WS_URL_V2 = "wss://api-pub.bitfinex.com/ws/2"

        const val CHANNEL_TICKER = "ticker"
        const val CHANNEL_ORDER_BOOK = "book"

        const val EVENT_INFO = "info"
        const val EVENT_SUBSCRIBE = "subscribe"
        const val EVENT_SUBSCRIBED = "subscribed"
        const val EVENT_UNSUBSCRIBE = "unsubscribe"
        const val EVENT_UNSUBSCRIBED = "unsubscribed"
        const val EVENT_ERROR = "error"

        const val FIELD_CHANNEL_ID = "chanId"
        const val FIELD_PRECISION = "prec"
        const val FIELD_FREQUENCY = "freq"

        const val PRECISION_LEVEL_0 = "P0"
        const val PRECISION_LEVEL_1 = "P1"
        const val PRECISION_LEVEL_2 = "P2"
        const val PRECISION_LEVEL_3 = "P3"

        const val FREQUENCY_UPDATES_REALTIME = "F0"
        const val FREQUENCY_UPDATES_EVERY_2_SECONDS = "F1"
    }
}

fun OrderBookRepository.CurrencyPair.toApiString(): String {
    return when (this) {
        OrderBookRepository.CurrencyPair.BTCUSD -> "BTCUSD"
    }
}

fun String.toTicker(gson: Gson): Api.Ticker? {
    return try {
        gson.fromJson(this, Api.Ticker::class.java)
    } catch (e: Exception) {
        null
    }
}

class TickerDeserializer : JsonDeserializer<Api.Ticker> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Api.Ticker {
        val jsonArray = json?.asJsonArray ?: throw JsonParseException("JsonElement is null")
        if (jsonArray.size() < 11) throw JsonParseException("Not enough elements")
        // Position 0 is channel id
        return Api.Ticker(
            jsonArray.get(1).asDouble,
            jsonArray.get(2).asDouble,
            jsonArray.get(3).asDouble,
            jsonArray.get(4).asDouble,
            jsonArray.get(5).asDouble,
            jsonArray.get(6).asDouble,
            jsonArray.get(7).asDouble,
            jsonArray.get(8).asDouble,
            jsonArray.get(9).asDouble,
            jsonArray.get(10).asDouble,
        )
    }
}

fun String.toOrderBookList(gson: Gson): List<Api.OrderBook> {
    val list = mutableListOf<Api.OrderBook>()
    try {
        gson.fromJson(this, List::class.java).forEach { element ->
            (element as? Api.OrderBook)?.run { list.add(this) }
        }
    } catch (e: Exception) {

    }
    return list
}

class OrderBookDeserializer : JsonDeserializer<Api.OrderBook> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Api.OrderBook {
        val jsonArray = json?.asJsonArray ?: throw JsonParseException("JsonElement is null")
        if (jsonArray.size() < 3) throw JsonParseException("Not enough elements")
        return Api.OrderBook(
            jsonArray.get(0).asDouble,
            jsonArray.get(1).asInt,
            jsonArray.get(2).asDouble
        )
    }
}

class OrderBookListDeserializer : JsonDeserializer<List<Api.OrderBook>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<Api.OrderBook> {
        val list = mutableListOf<Api.OrderBook>()
        val jsonArray = json?.asJsonArray ?: throw JsonParseException("JsonElement is null")
        if (jsonArray.size() == 2) {
            // list or heart beat
            if (jsonArray.get(1).isJsonArray) {
                jsonArray.get(1).asJsonArray.forEach { jsonElement ->
                    val orderBook = context?.deserialize<Api.OrderBook>(jsonElement, Api.OrderBook::class.java)
                    orderBook?.run { list.add(orderBook) }
                }
            }
        } else {
            if (jsonArray.size() < 4) throw JsonParseException("Not enough elements")
            // update
            // position 0 is channel id
            val orderBook = Api.OrderBook(
                jsonArray.get(1).asDouble,
                jsonArray.get(2).asInt,
                jsonArray.get(3).asDouble
            )
            list.add(orderBook)
        }
        return list
    }
}
