package com.swissborg.orderbook.android.bitfinex

import com.google.gson.GsonBuilder
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiTest {
    private val gson = GsonBuilder()
        .registerTypeAdapter(ChannelMessage::class.java, ChannelMessageDeserializer())
        .registerTypeAdapter(Api.Ticker::class.java, TickerDeserializer())
        .registerTypeAdapter(Api.OrderBook::class.java, OrderBookDeserializer())
        .registerTypeAdapter(List::class.java, OrderBookListDeserializer())
        .excludeFieldsWithoutExposeAnnotation()
        .create()

    @Test
    fun `Bitfinex ticker json string message should be JSonArray with at least 11 elements, channel id, bid, etc`() {
        val tickerJsonString = "[436732, 10737, 114.68038098999997, 10738, 128.18675985000004, 114.88367, 0.0108, 10737.88367, 2066.98594264, 10785, 10601.413784]"
        val ticker = tickerJsonString.toTicker(gson)
        assertTrue(ticker != null)
        ticker?.run {
            assertTrue(bid == 10737.0)
            assertTrue(bidSize == 114.68038098999997)
            assertTrue(ask == 10738.0)
            assertTrue(askSize == 128.18675985000004)
            assertTrue(dailyChange == 114.88367)
            assertTrue(dailyChangePerc == 0.0108)
            assertTrue(lastPrice == 10737.88367)
            assertTrue(volume == 2066.98594264)
            assertTrue(high == 10785.0)
            assertTrue(low == 10601.413784)
        }
    }

    @Test
    fun `Bitfinex ticker json string message is valid even if it has more elements then required`() {
        // According to Bitfinex docs:
        // Message (JSON array) lengths should never be hardcoded. New fields may be appended
        // at the end of a message without changing version.
        val tickerJsonString = "[0.1, 2.3, 4.5, 6.7, 8.9, 10.11, 12.13, 14.15, 16.17, 18.19, 20.21, 22.23]"
        val ticker = tickerJsonString.toTicker(gson)
        assertTrue(ticker != null)
        ticker?.run {
            assertTrue(bid == 2.3)
            assertTrue(bidSize == 4.5)
            assertTrue(ask == 6.7)
            assertTrue(askSize == 8.9)
            assertTrue(dailyChange == 10.11)
            assertTrue(dailyChangePerc == 12.13)
            assertTrue(lastPrice == 14.15)
            assertTrue(volume == 16.17)
            assertTrue(high == 18.19)
            assertTrue(low == 20.21)
        }
    }

    @Test
    fun `invalid ticker json string should return null`() {
        // Specialized deserializer expects JSonArray with channel id as first element,
        // followed by at least 10 float/double elements
        val tickerJsonStrings = arrayListOf(
            "[1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0]",
            "[1.0, \"a\", 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0]",
            "[123456, \"hb\"]"
        )
        tickerJsonStrings.forEach { tickerJsonString ->
            val ticker = tickerJsonString.toTicker(gson)
            assertTrue(tickerJsonString, ticker == null)
        }
    }

    @Test
    fun `Bitfinex subscribed book json string event should be JSonObject with at least event, channel, pair, chanId, prec and freq`() {
        val subscribedBookJsonString = "{\"event\":\"subscribed\",\"channel\":\"book\",\"chanId\":437334,\"prec\":\"P0\",\"freq\":\"F1\",\"len\":\"25\",\"pair\":\"BTCUSD\"}"
        val subscribeBook = subscribedBookJsonString.subscribeBookEvent(gson)
        assertTrue(subscribeBook != null)
        subscribeBook?.run {
            assertTrue(event == "subscribed")
            assertTrue(channel == "book")
            assertTrue(pair == "BTCUSD")
            assertTrue(channelId == 437334)
            assertTrue(precision == "P0")
            assertTrue(frequency == "F1")
        }
    }

    @Test
    fun `Bitfinex order book list json message should be JSonArray with channel and another JSonArray with order book JSonArrays with three elements, price, count and amount`() {
        val orderBooksJsonString = "[437334, [[10737, 6, 2.84073876], [10736, 8, 9.09503304], [10735, 7, 2.14583605]]]"
        val orderBookList = orderBooksJsonString.toOrderBookList(gson)
        assertTrue(orderBookList.size == 3)
        assertTrue(orderBookList[0].price == 10737.0)
        assertTrue(orderBookList[0].count == 6)
        assertTrue(orderBookList[0].amount == 2.84073876)
        assertTrue(orderBookList[1].price == 10736.0)
        assertTrue(orderBookList[1].count == 8)
        assertTrue(orderBookList[1].amount == 9.09503304)
        assertTrue(orderBookList[2].price == 10735.0)
        assertTrue(orderBookList[2].count == 7)
        assertTrue(orderBookList[2].amount == 2.14583605)
    }

    @Test
    fun `Bitfinex update order book json message should be JSonArray with four elements, channel id, price, count and amount`() {
        val orderBookJsonString = "[437334, 10736, 6, 3.3359]"
        val orderBookList = orderBookJsonString.toOrderBookList(gson)
        assertTrue(orderBookList.size == 1)
        assertTrue(orderBookList[0].price == 10736.0)
        assertTrue(orderBookList[0].count == 6)
        assertTrue(orderBookList[0].amount == 3.3359)
    }

    @Test
    fun `invalid order book json string should return empty list`() {
        // Bitfinex order book comes either as a list or as a single order book update
        val orderBookListJsonStrings = arrayListOf(
            "[123456, 7, 8]",
            "[123456, \"hb\"]"
        )
        orderBookListJsonStrings.forEach { orderBookJsonString ->
            val orderBookList = orderBookJsonString.toOrderBookList(gson)
            assertTrue(orderBookJsonString, orderBookList.isEmpty())
        }
    }
}
