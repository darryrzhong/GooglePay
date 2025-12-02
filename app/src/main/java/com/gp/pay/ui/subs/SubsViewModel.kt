package com.gp.pay.ui.subs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.pay.model.BillingSubsParams
import com.google.pay.model.SubsOfferParams

class SubsViewModel : ViewModel() {

    private val _subs = MutableLiveData<List<SubsOfferParams>>().apply {
        value = arrayListOf(
            SubsOfferParams("com.example.subs.1", "plan1", ""),
            SubsOfferParams("com.example.subs.2", "plan2", "plan1-offer1"),
            SubsOfferParams("com.example.subs.3", "", ""),
            SubsOfferParams("com.example.subs.4", "", "")
        )
    }
    val subs: LiveData<List<SubsOfferParams>> = _subs
}