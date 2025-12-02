package com.gp.pay.ui.inapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class InappViewModel : ViewModel() {

    private val _products = MutableLiveData<List<String>>().apply {
        value = arrayListOf(
            "com.niki.product.1",
            "com.niki.product.2",
            "com.niki.product.3",
            "com.niki.product.4"
        )
    }
    val products: LiveData<List<String>> = _products
}