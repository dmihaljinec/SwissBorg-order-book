package com.swissborg.orderbook.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.swissborg.orderbook.android.R
import com.swissborg.orderbook.android.databinding.FragmentOrderBookListBinding
import com.swissborg.orderbook.repository.OrderBookRepository
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderBookListFragment : Fragment() {
    private val orderBookListFragmentViewModel: OrderBookListFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentOrderBookListBinding>(
            inflater,
            R.layout.fragment_order_book_list,
            container,
            false
        )
        binding.orderBook = orderBookListFragmentViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.toolbar.title = OrderBookRepository.CurrencyPair.BTCUSD.toString()
        return binding.root
    }
}