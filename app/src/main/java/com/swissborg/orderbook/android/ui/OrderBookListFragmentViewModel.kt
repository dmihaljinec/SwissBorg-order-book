package com.swissborg.orderbook.android.ui

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swissborg.orderbook.android.ui.viewmodel.ConnectionStateViewModel
import com.swissborg.orderbook.android.ui.viewmodel.OrderBookViewModel
import com.swissborg.orderbook.android.ui.viewmodel.TickerViewModel
import com.swissborg.orderbook.android.ui.viewmodel.toViewModel
import com.swissborg.orderbook.interactor.OrderBookInteractors
import com.swissborg.orderbook.repository.OrderBookRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class OrderBookListFragmentViewModel @ViewModelInject constructor(
    private val interactors: OrderBookInteractors
) : ViewModel() {
    private val _tickerViewModel = MutableLiveData<TickerViewModel>()
    val tickerViewModel: LiveData<TickerViewModel>
        get() = _tickerViewModel
    private val _orderBookList = MutableLiveData<List<OrderBookViewModel>>()
    val orderBookList: LiveData<List<OrderBookViewModel>>
        get() = _orderBookList

    private val _connectionStateViewModel = MutableLiveData<ConnectionStateViewModel>()
    val connectionStateViewModel: LiveData<ConnectionStateViewModel>
        get() = _connectionStateViewModel

    init {
        viewModelScope.launch {
            val ticker = async {
                interactors.getTicker(OrderBookRepository.CurrencyPair.BTCUSD)
                    .map { ticker -> ticker.toViewModel() }
                    .collect { ticker ->
                        _tickerViewModel.value = ticker
                    }
            }
            val orderBookList = async {
                interactors.getOrderBooks(OrderBookRepository.CurrencyPair.BTCUSD)
                    .map { orderBookList -> orderBookList.map { orderBook -> orderBook.toViewModel() } }
                    .collect { orderBookList ->
                        _orderBookList.value = orderBookList
                    }
            }
            val connectionState = async {
                interactors.getConnectionState()
                    .map { connectionState ->
                        connectionState.toViewModel()
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