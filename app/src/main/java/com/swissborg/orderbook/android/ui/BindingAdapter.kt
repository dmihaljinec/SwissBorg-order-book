package com.swissborg.orderbook.android.ui

import android.widget.TextView
import androidx.databinding.BindingAdapter

@BindingAdapter("android:text")
fun TextView.setDouble(value: Double?) {
    val newValue = value?.toString() ?: ""
    if (newValue != text.toString()) text = newValue
}

@BindingAdapter("android:text")
fun TextView.setInt(value: Int?) {
    val newValue = value?.toString() ?: ""
    if (newValue != text.toString()) text = newValue
}