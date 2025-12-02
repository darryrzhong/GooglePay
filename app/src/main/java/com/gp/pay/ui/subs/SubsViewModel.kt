package com.gp.pay.ui.subs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.pay.model.BillingSubsParams
import com.google.pay.model.SubsOfferParams

class SubsViewModel : ViewModel() {

    private val _subs = MutableLiveData<List<SubsOfferParams>>().apply {
        value = arrayListOf(
            SubsOfferParams("com.niki.vip.1week", "1week", ""),
            SubsOfferParams("com.niki.vip.1month", "1month", "1month-free-3day"),
            SubsOfferParams("com.niki.vip.3month", "3month", ""),
            SubsOfferParams("com.niki.svip.1month", "1month", "")
        )
    }
    val subs: LiveData<List<SubsOfferParams>> = _subs
}