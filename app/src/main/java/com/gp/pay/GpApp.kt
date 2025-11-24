package com.gp.pay

import android.app.Application
import com.google.pay.billing.AppBillingClient
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

        AppBillingClient.getInstance()
            .setDebug(true)
            .setSubscriptionMode(SubscriptionMode.SingleMode)
            .setSubscription(true)
            .initBillingClient(this, AppBillingServiceImpl.instance)

    }
}