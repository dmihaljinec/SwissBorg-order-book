package com.swissborg.orderbook.android.di

import android.content.Context
import android.net.ConnectivityManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.swissborg.orderbook.android.bitfinex.*
import com.swissborg.orderbook.android.ws.WebSocketClient
import com.swissborg.orderbook.repository.OrderBookDataSource
import com.swissborg.orderbook.repository.OrderBookRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object ApplicationModule {

    @Provides
    @Singleton
    fun providesOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun providesGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(ChannelMessage::class.java, ChannelMessageDeserializer())
            .registerTypeAdapter(Api.Ticker::class.java, TickerDeserializer())
            .registerTypeAdapter(Api.OrderBook::class.java, OrderBookDeserializer())
            .excludeFieldsWithoutExposeAnnotation()
            .create()
    }

    @Provides
    fun providesConnectivityManager(@ApplicationContext context: Context): ConnectivityManager {
        return context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @Provides
    @ExperimentalCoroutinesApi
    fun providesWebSocketClient(connectivityManager: ConnectivityManager, httpClient: OkHttpClient): WebSocketClient {
        return WebSocketClient(connectivityManager, httpClient, Api.WS_URL_V1)
    }

    @Provides
    @ExperimentalCoroutinesApi
    @FlowPreview
    fun providesChannelConnection(webSocketClient: WebSocketClient, gson: Gson): ChannelConnection {
        return ChannelConnection(webSocketClient, gson)
    }

    @Provides
    @ExperimentalCoroutinesApi
    @FlowPreview
    fun providesOrderBookDataSource(channelConnection: ChannelConnection, gson: Gson): OrderBookDataSource {
        return OrderBookDataSourceImpl(channelConnection, gson)
    }

    @Provides
    @Singleton
    fun providesOrderBookRepository(orderBookDataSource: OrderBookDataSource): OrderBookRepository {
        return OrderBookRepository(orderBookDataSource)
    }
}
