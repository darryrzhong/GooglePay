package com.google.pay.billing.service

/**
 * <pre>
 *     类描述  :
 *
 *
 *     @author : never
 *     @since   : 2023/11/14
 * </pre>
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class BillingBindService(val serviceName: String)
