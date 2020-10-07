package com.swissborg.orderbook.android.ui.viewmodel

import com.swissborg.orderbook.android.R
import com.swissborg.orderbook.model.ConnectionState

data class ConnectionStateViewModel(
    val connectionColorResId: Int
)

fun ConnectionState.toViewModel(): ConnectionStateViewModel {
    val connectionState = when (this) {
        ConnectionState.CONNECTED -> R.color.colorConnected
        ConnectionState.CONNECTING -> R.color.colorConnecting
        ConnectionState.DISCONNECTED -> R.color.colorDisconnected
    }
    return ConnectionStateViewModel(connectionState)
}
