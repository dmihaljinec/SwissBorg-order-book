package com.swissborg.orderbook.android.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.swissborg.orderbook.android.BR
import com.swissborg.orderbook.android.R
import com.swissborg.orderbook.android.ui.viewmodel.DataBindingViewHolder
import com.swissborg.orderbook.android.ui.viewmodel.OrderBookViewModel

class OrderBookListAdapter : ListAdapter<OrderBookViewModel, DataBindingViewHolder>(diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataBindingViewHolder {
        return DataBindingViewHolder(
            parent,
            R.layout.listitem_order_book,
            BR.orderBook
        )
    }

    override fun onBindViewHolder(holder: DataBindingViewHolder, position: Int) {
        val item = getItem(position)
        holder.viewModel = item
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<OrderBookViewModel>() {
            override fun areItemsTheSame(oldItem: OrderBookViewModel, newItem: OrderBookViewModel): Boolean {
                return oldItem.orderBook.buy?.price == newItem.orderBook.buy?.price &&
                        oldItem.orderBook.sell?.price == oldItem.orderBook.sell?.price
            }
            override fun areContentsTheSame(oldItem: OrderBookViewModel, newItem: OrderBookViewModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}