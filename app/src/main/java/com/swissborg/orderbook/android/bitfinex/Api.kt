package com.swissborg.orderbook.android.bitfinex

import com.google.gson.*
import com.swissborg.orderbook.model.Ticker
import com.swissborg.orderbook.repository.OrderBookRepository
import java.lang.reflect.Type

class Api {
    data class Ticker(
        val bid: Double,
        val bidSize: Double,
        val ask: Double,
        val askSize: Double,
        val dailyChange: Double,
        val dailyChangePerc: Double,
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
                dailyChange
            )
        }
    }

    data class OrderBook(
        val price: Double,
        val count: Int,
        val amount: Double
    ) {
        fun toOrderBook(): com.swissborg.orderbook.model.OrderBook {
            return com.swissborg.orderbook.model.OrderBook(
                price,
                count,
                amount
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
        val jsonArray = json?.asJsonArray ?: throw JsonParseException("JsonArray expected")
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

class OrderBookDeserializer : JsonDeserializer<Api.OrderBook> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Api.OrderBook {
        val jsonArray = json?.asJsonArray ?: throw JsonParseException("JsonArray expected")
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
        val jsonArray = json?.asJsonArray ?: throw JsonParseException("JsonArray expected")
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