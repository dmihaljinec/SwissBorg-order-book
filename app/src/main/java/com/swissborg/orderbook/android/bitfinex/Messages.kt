package com.swissborg.orderbook.android.bitfinex

import com.google.gson.*
import com.google.gson.annotations.Expose
import java.lang.reflect.Type

data class ChannelMessage(
    @Expose val channelId: Int,
)

class ChannelMessageDeserializer : JsonDeserializer<ChannelMessage> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ChannelMessage {
        val jsonArray = json?.asJsonArray ?: throw JsonParseException("JsonElement is null")
        val jsonPrimitive = jsonArray.get(0).asJsonPrimitive ?: throw JsonParseException("First element must be channel id")
        return ChannelMessage(jsonPrimitive.asInt)
    }
}

fun String.channelMessage(gson: Gson): ChannelMessage? {
    return try {
        gson.fromJson(this, ChannelMessage::class.java)
    } catch (e: Exception) {
        null
    }
}
