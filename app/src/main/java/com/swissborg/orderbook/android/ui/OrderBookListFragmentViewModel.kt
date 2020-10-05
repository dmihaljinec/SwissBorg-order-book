package com.swissborg.orderbook.android.ui

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swissborg.orderbook.interactor.OrderBookInteractors
import com.swissborg.orderbook.model.OrderBook
import com.swissborg.orderbook.model.Ticker
import com.swissborg.orderbook.repository.OrderBookRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class OrderBookListFragmentViewModel @ViewModelInject constructor(
    private val interactors: OrderBookInteractors
) : ViewModel() {
    private val _tickerViewModel = MutableLiveData<Ticker>()
    val tickerViewModel: LiveData<Ticker>
        get() = _tickerViewModel
    private val _orderBookList = MutableLiveData<List<OrderBook>>()
    val orderBookList: LiveData<List<OrderBook>>
        get() = _orderBookList

    private val _connectionStateViewModel = MutableLiveData<ConnectionStateViewModel>()
    val connectionStateViewModel: LiveData<ConnectionStateViewModel>
        get() = _connectionStateViewModel

    init {
        viewModelScope.launch {
            val ticker = async {
                interactors.getTicker(OrderBookRepository.CurrencyPair.BTCUSD).collect { ticker ->
                    _tickerViewModel.value = ticker
                }
            }
            val orderBookList = async {
                interactors.getOrderBooks(OrderBookRepository.CurrencyPair.BTCUSD).collect { orderBookList ->
                    _orderBookList.value = orderBookList
                }
            }
            val connectionState = async {
                interactors.getConnectionState()
                    .map { connectionState ->
                        connectionState.toConnectionStateViewModel(coroutineContext)
                    }
                    .collect { connectionStateViewModel ->
                        _connectionStateViewModel.value = connectionStateViewModel
                    }
            }
            ticker.await()
            orderBookList.await()
            connectionState.await()
        }
    }
}