package com.swissborg.orderbook.android.bitfinex

import com.google.gson.Gson
import com.swissborg.orderbook.repository.OrderBookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.transform
import timber.log.Timber

@ExperimentalCoroutinesApi
@FlowPreview
class TickerChannel(
    currencyPair: OrderBookRepository.CurrencyPair,
    connection: Connection,
    gson: Gson
) : Channel(
    Api.CHANNEL_TICKER,
    currencyPair.toApiString(),
    connection,
    gson,
    TickerChannel::class.java.simpleName
) {
    suspend fun getTicker(): Flow<Api.Ticker> {
        return messages()
            .transform { message ->
                if (message.isEvent(gson)) processEvent(message)
                else message.toTicker(gson)?.run {
                    Timber.d("Api.Ticker last price = $lastPrice")
                    emit(this)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override fun subscribe() {
        connection.send(gson.toJson(Subscribe(channel = name, pair = currencyPair), Subscribe::class.java))
    }
}
