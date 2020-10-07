package com.swissborg.orderbook.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.SimpleItemAnimator
import com.swissborg.orderbook.android.R
import com.swissborg.orderbook.android.databinding.FragmentOrderBookListBinding
import com.swissborg.orderbook.repository.OrderBookRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.order_book_list.view.*

@AndroidEntryPoint
class OrderBookListFragment : Fragment() {
    private val adapter = OrderBookListAdapter()
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
        binding.orderBookList.list.adapter = adapter
        binding.orderBookList.list.removeAdapter(viewLifecycleOwner)
        binding.orderBooks = orderBookListFragmentViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.toolbar.title = OrderBookRepository.CurrencyPair.BTCUSD.toString()
        orderBookListFragmentViewModel.orderBookList.observe(viewLifecycleOwner) { adapter.submitList(it) }
        (binding.orderBookList.list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        return binding.root
    }
}