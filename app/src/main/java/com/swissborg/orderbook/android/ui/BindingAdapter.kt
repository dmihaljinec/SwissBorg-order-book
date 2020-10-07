package com.swissborg.orderbook.android.ui

import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter

@BindingAdapter("tint")
fun tint(imageView: ImageView, tintColorResId: Int) {
    imageView.imageTintList = ContextCompat.getColorStateList(imageView.context, tintColorResId)
}
