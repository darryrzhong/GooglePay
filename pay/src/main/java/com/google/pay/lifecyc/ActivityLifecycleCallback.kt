package com.google.pay.lifecyc

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.pay.billing.GooglePayClient

/**
 * <pre>
 *     类描述  :
 *
 *     @author : never
 *     @since  : 2025/11/26
 * </pre>
 */
internal class ActivityLifecycleCallback : Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {
    //内部刷新库存 & 商品的时间间隔
    internal var refreshInterval = 15
    internal val activityList = mutableListOf<Class<out Activity>>()

    private var lastTime = 0L

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityResumed(activity: Activity) {
        if (activityList.contains(activity::class.java)) {
            val now = System.currentTimeMillis()
            if (now - lastTime > refreshInterval * 1000) {
                //内部刷新Google pay相关状态
                lastTime = now
                GooglePayClient.getInstance().queryPurchases()
            }
        }
    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) {

    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        val now = System.currentTimeMillis()
        if (now - lastTime > 2000) {
            lastTime = now
            GooglePayClient.getInstance().refreshPurchases()
        }

    }


}