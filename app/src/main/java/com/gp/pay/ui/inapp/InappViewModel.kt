package com.gp.pay.ui.inapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class InappViewModel : ViewModel() {

    private val _products = MutableLiveData<List<String>>().apply {
        value = arrayListOf(
            "com.example.product.1",
            "com.example.product.2",
            "com.example.product.3",
            "com.example.product.3",
            "com.example.product.5",
        )
    }
    val products: LiveData<List<String>> = _products
}