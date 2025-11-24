package com.google.pay.billing.service

import com.google.pay.billing.AppBillingClient
import com.google.pay.billing.service.onetime.OneTimeService
import com.google.pay.billing.service.onetime.OneTimeServiceEmptyImpl
import com.google.pay.billing.service.onetime.OneTimeServiceImpl
import com.google.pay.billing.service.subscription.SubscriptionService
import com.google.pay.billing.service.subscription.SubscriptionServiceEmptyImpl
import com.google.pay.billing.service.subscription.SubscriptionServiceImpl
import java.lang.IllegalArgumentException

/**
 * <pre>
 *     类描述  : Billing sdk服务管理者，提供服务&创建服务
 *
 *
 *     @author : never
 *     @since   : 2023/11/14
 * </pre>
 */
internal object BillingServiceManager {

    private val services = hashMapOf<String, Any>()
    private val servicesEmpty = hashMapOf<String, Any>()
    private val servicesMap = hashMapOf<String, String>()

    private val oneTimeServiceImpl: OneTimeServiceImpl by lazy { OneTimeServiceImpl() }
    private val subscriptionServiceImpl: SubscriptionServiceImpl by lazy { SubscriptionServiceImpl() }

    //空实现
    private val oneTimeServiceEmptyImpl: OneTimeServiceEmptyImpl by lazy { OneTimeServiceEmptyImpl() }
    private val subscriptionServiceEmptyImpl: SubscriptionServiceEmptyImpl by lazy { SubscriptionServiceEmptyImpl() }

    init {
        servicesMap[OneTimeService::class.java.simpleName] = OneTimeServiceImpl::class.java.name
        servicesMap[SubscriptionService::class.java.simpleName] =
            SubscriptionServiceImpl::class.java.name
        services[OneTimeService::class.java.simpleName] = oneTimeServiceImpl
        services[SubscriptionService::class.java.simpleName] = subscriptionServiceImpl

        servicesEmpty[OneTimeService::class.java.simpleName] = oneTimeServiceEmptyImpl
        servicesEmpty[SubscriptionService::class.java.simpleName] = subscriptionServiceEmptyImpl


    }

    /**
     * 获取服务
     * @param clazz 服务类型
     * */
    fun <T> getService(clazz: Class<T>): T {
        val clazzName = clazz.simpleName

        val ann = clazz.getAnnotation(BillingBindService::class.java)
        if (ann != null) {
            val serviceName = ann.serviceName
            if (!servicesMap.containsKey(serviceName)) {
                throw IllegalArgumentException("The $clazzName service does not exist")
            }
            if (AppBillingClient.getInstance().isGoogleAvailable()) {
                if (services.containsKey(serviceName)) {
                    return services[serviceName] as T
                }
            } else {
                if (servicesEmpty.containsKey(serviceName)) {
                    return servicesEmpty[serviceName] as T
                }
            }

        }
        throw IllegalArgumentException("The $clazzName service does not exist")
    }

}