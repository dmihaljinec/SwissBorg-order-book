package com.swissborg.orderbook.android.ui

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.swissborg.orderbook.interactor.OrderBookInteractors
import com.swissborg.orderbook.repository.OrderBookRepository
import com.swissborg.orderbook.repository.Ticker
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class OrderBookListFragmentViewModel @ViewModelInject constructor(
    private val interactors: OrderBookInteractors,
    @Assisted savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _tickerViewModel = MutableLiveData<Ticker>()
    val tickerViewModel: LiveData<Ticker>
        get() = _tickerViewModel

    init {
        viewModelScope.launch {
            interactors.getTicker(OrderBookRepository.CurrencyPair.BTCUSD).collect { ticker ->
                _tickerViewModel.value = ticker
            }
        }
    }
}