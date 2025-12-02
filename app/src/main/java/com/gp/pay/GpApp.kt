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
            .setSubscriptionMode(SubscriptionMode.SingleMode)  //单订阅
            .setSubscription(true) //支持订阅
            .setInterval(15) //自动刷新间隔
            .registerActivitys(arrayListOf(MainActivity::class.java))  //自动刷新页面注册
            .initBillingClient(this, GooglePayServiceImpl.instance)
    }
}