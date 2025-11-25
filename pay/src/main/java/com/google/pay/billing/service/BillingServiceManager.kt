package com.google.pay.billing.service

import com.google.pay.billing.GooglePayClient
import com.google.pay.billing.service.onetime.OneTimeService
import com.google.pay.billing.service.onetime.OneTimeServiceEmptyImpl
import com.google.pay.billing.service.onetime.OneTimeServiceImpl
import com.google.pay.billing.service.subscription.SubscriptionService
import com.google.pay.billing.service.subscription.SubscriptionServiceEmptyImpl
import com.google.pay.billing.service.subscription.SubscriptionServiceImpl

/**
 * <pre>
 *     类描述  : Billing sdk服务管理者，提供服务&创建服务
 *     @author : never
 *     @since   : 2023/11/14
 * </pre>
 */
internal object BillingServiceManager {

    // 真实服务映射
    private val realServices = mapOf<Class<*>, Any>(
        OneTimeService::class.java to OneTimeServiceImpl(),
        SubscriptionService::class.java to SubscriptionServiceImpl()
    )

    // 空实现服务映射
    private val emptyServices = mapOf<Class<*>, Any>(
        OneTimeService::class.java to OneTimeServiceEmptyImpl(),
        SubscriptionService::class.java to SubscriptionServiceEmptyImpl()
    )

    /**
     * 获取服务 (内联函数，支持泛型实化)
     * 使用方式: BillingServiceManager.getService<OneTimeService>()
     */
    inline fun <reified T> getService(): T {
        return getService(T::class.java)
    }

    /**
     * 获取服务
     * @param clazz 服务类型
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getService(clazz: Class<T>): T {
        // 根据 Google Play 服务是否可用，决定使用真实服务还是空实现
        val map = if (GooglePayClient.getInstance().isGoogleAvailable()) {
            realServices
        } else {
            emptyServices
        }

        return (map[clazz] as? T)
            ?: throw IllegalArgumentException("The ${clazz.simpleName} service does not exist")
    }
}