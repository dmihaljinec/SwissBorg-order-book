package com.swissborg.orderbook.android.ui

import com.swissborg.orderbook.model.ConnectionState
import kotlin.coroutines.CoroutineContext

data class ConnectionStateViewModel(
    val connectionState: String
)

fun ConnectionState.toConnectionStateViewModel(coroutineContext: CoroutineContext): ConnectionStateViewModel {
    val connectionState = when (this) {
        ConnectionState.CONNECTED -> "Connected"
        ConnectionState.CONNECTING -> "Connecting"
        ConnectionState.DISCONNECTED -> "Disconnected"
    }
    return ConnectionStateViewModel(connectionState)
}