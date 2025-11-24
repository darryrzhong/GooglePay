package com.gp.pay.ui.inapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class InappViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is inapp fragment"
    }
    val text: LiveData<String> = _text
}