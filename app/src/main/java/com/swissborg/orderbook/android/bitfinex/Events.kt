package com.swissborg.orderbook.android.bitfinex

import com.google.gson.Gson
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Event(
    @Expose
    val event: String,
)

data class ChannelEvent(
    @Expose
    val event: String,
    @Expose
    val channel: String,
    @Expose
    val pair: String
)

data class Subscribe(
    @Expose
    val event: String = Api.EVENT_SUBSCRIBE,
    @Expose
    val channel: String,
    @Expose
    val pair: String,
    @Expose(serialize = false, deserialize = true)
    @SerializedName(Api.FIELD_CHANNEL_ID)
    val channelId: Int = -1
)

data class SubscribeBook(
    @Expose
    val event: String = Api.EVENT_SUBSCRIBE,
    @Expose
    val channel: String,
    @Expose
    val pair: String,
    @Expose(serialize = false, deserialize = true)
    @SerializedName(Api.FIELD_CHANNEL_ID)
    val channelId: Int = -1,
    @Expose
    @SerializedName(Api.FIELD_PRECISION)
    val precision: String = Api.PRECISION_LEVEL_0,
    @Expose
    @SerializedName(Api.FIELD_FREQUENCY)
    val frequency: String = Api.FREQUENCY_UPDATES_REALTIME
)

data class Unsubscribe(
    @Expose
    val event: String = Api.EVENT_UNSUBSCRIBE,
    @Expose
    @SerializedName(Api.FIELD_CHANNEL_ID)
    val channelId: Int
)

data class Error(
    @Expose
    val event: String,
    @Expose
    val msg: String,
    @Expose
    val code: Int,
    @Expose
    @SerializedName(Api.FIELD_CHANNEL_ID)
    val channelId: Int
)

fun String.isEvent(gson: Gson): Boolean {
    return try {
        val eventType = gson.fromJson(this, Event::class.java)
        eventType.event.isNotBlank()
    } catch (e: Exception) {
        false
    }
}

fun String.channelEvent(gson: Gson): ChannelEvent? {
    return try {
        gson.fromJson(this, ChannelEvent::class.java)
    } catch (e: Exception) {
        null
    }
}

fun String.subscribeEvent(gson: Gson): Subscribe? {
    return try {
        gson.fromJson(this, Subscribe::class.java)
    } catch (e: Exception) {
        null
    }
}

fun String.subscribeBookEvent(gson: Gson): SubscribeBook? {
    return try {
        gson.fromJson(this, SubscribeBook::class.java)
    } catch (e: Exception) {
        null
    }
}

fun String.unsubscribeEvent(gson: Gson): Unsubscribe? {
    return try {
        gson.fromJson(this, Unsubscribe::class.java)
    } catch (e: Exception) {
        null
    }
}

fun String.errorEvent(gson: Gson): Error? {
    return try {
        gson.fromJson(this, Error::class.java)
    } catch (e: Exception) {
        null
    }
}
