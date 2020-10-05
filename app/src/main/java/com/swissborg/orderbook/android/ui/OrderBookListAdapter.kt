package com.swissborg.orderbook.android.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.swissborg.orderbook.android.BR
import com.swissborg.orderbook.android.R
import com.swissborg.orderbook.model.OrderBook

class OrderBookListAdapter : ListAdapter<OrderBook, DataBindingViewHolder>(diffCallback) {
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
        private val diffCallback = object : DiffUtil.ItemCallback<OrderBook>() {
            override fun areItemsTheSame(oldItem: OrderBook, newItem: OrderBook): Boolean {
                return oldItem.price == newItem.price
            }
            override fun areContentsTheSame(oldItem: OrderBook, newItem: OrderBook): Boolean {
                return oldItem == newItem
            }
        }
    }
}