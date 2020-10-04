package com.swissborg.orderbook.android.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.swissborg.orderbook.android.R
import com.swissborg.orderbook.android.bitfinex.ChannelConnection
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import javax.inject.Inject

@AndroidEntryPoint
@ExperimentalCoroutinesApi
@FlowPreview
class MainActivity : AppCompatActivity() {
    @Inject lateinit var channelConnection: ChannelConnection
    @Inject lateinit var gson: Gson

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onBackPressed() {
        finishAfterTransition()
    }
}

fun RecyclerView.removeAdapter(lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) adapter = null
    })
}
