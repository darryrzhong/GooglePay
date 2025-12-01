package com.gp.pay

import android.app.Application
import com.google.pay.billing.GooglePayClient
import com.google.pay.billing.service.onetime.OneTimeService
import com.google.pay.model.SubscriptionMode

/**
 * <pre>
 *     类描述  :
 *
 *     @author : never
 *     @since  : 2025/11/24
 * </pre>
 */
class GpApp : Application() {
    override fun onCreate() {
        super.onCreate()

        GooglePayClient.getInstance()
            .setDebug(true)
            .setSubscriptionMode(SubscriptionMode.SingleMode)
            .setSubscription(false)
            .setInterval(15)
            .registerActivitys(arrayListOf(MainActivity::class.java))
            .initBillingClient(this, GooglePayServiceImpl.instance)
    }
}