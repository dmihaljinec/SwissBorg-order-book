package com.swissborg.orderbook.android.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

class DataBindingViewHolder(
    parent: ViewGroup,
    layoutId: Int,
    private val bindingValueId: Int
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
) {
    private var binding: ViewDataBinding? = DataBindingUtil.bind(itemView)
    var viewModel: Any? = null
        set(value) {
            field = value
            value?.let {
                binding?.apply {
                    setVariable(bindingValueId, viewModel)
                    executePendingBindings()
                }
            }
        }
}