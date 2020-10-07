package com.swissborg.orderbook.interactor

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.google.gson.GsonBuilder
import com.swissborg.orderbook.android.bitfinex.*
import com.swissborg.orderbook.model.ConnectionState
import com.swissborg.orderbook.repository.OrderBookRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@FlowPreview
@RunWith(AndroidJUnit4ClassRunner::class)
class GetTickerAndroidTest {
    private val gson = GsonBuilder()
        .registerTypeAdapter(ChannelMessage::class.java, ChannelMessageDeserializer())
        .registerTypeAdapter(Api.Ticker::class.java, TickerDeserializer())
        .registerTypeAdapter(Api.OrderBook::class.java, OrderBookDeserializer())
        .registerTypeAdapter(List::class.java, OrderBookListDeserializer())
        .excludeFieldsWithoutExposeAnnotation()
        .create()

    private val dataSource = OrderBookDataSourceImpl(object : Channel.Connection{
        override suspend fun connect() = Unit
        override suspend fun disconnect() = Unit
        override fun send(request: String) = Unit
        override fun connectionState(): Flow<ConnectionState> = flow {
            emit(ConnectionState.CONNECTED)
        }

        override fun messages(channel: Channel): Flow<String> = flow {
            val subscribed = "{\"event\":\"subscribed\",\"channel\":\"ticker\",\"chanId\":123456,\"pair\":\"BTCUSD\"}"
            emit(subscribed)
            val ticker = "[123456, 10737, 114.68038098999997, 10738, 128.18675985000004, 114.88367, 0.0108, 10737.88367, 2066.98594264, 10785, 10601.413784]"
            emit(ticker)
        }

    }, gson)

    private val repository = OrderBookRepository(dataSource)
    private val getTicker = GetTicker(repository)

    @Test
    fun ticker() = runBlocking {
        getTicker(OrderBookRepository.CurrencyPair.BTCUSD).collect { ticker ->
            with (ticker) {
                assertTrue(dailyChangePercentage == 0.0108)
                assertTrue(lastTradePrice == 10737.88367)
                assertTrue(volume == 2066.98594264)
                assertTrue(high == 10785.0)
                assertTrue(low == 10601.413784)
            }
        }
    }
}
