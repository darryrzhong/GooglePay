package com.gp.pay.ui.subs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SubsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is subs fragment"
    }
    val text: LiveData<String> = _text
}