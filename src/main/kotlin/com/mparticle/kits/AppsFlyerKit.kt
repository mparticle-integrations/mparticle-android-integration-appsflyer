package com.mparticle.kits

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import com.appsflyer.AFInAppEventParameterName.CONTENT_ID
import com.appsflyer.AFInAppEventParameterName.CONTENT_TYPE
import com.appsflyer.AFInAppEventParameterName.CURRENCY
import com.appsflyer.AFInAppEventParameterName.PRICE
import com.appsflyer.AFInAppEventParameterName.QUANTITY
import com.appsflyer.AFInAppEventParameterName.REVENUE
import com.appsflyer.AFInAppEventType
import com.appsflyer.AFInAppEventType.ADD_TO_CART
import com.appsflyer.AFInAppEventType.ADD_TO_WISH_LIST
import com.appsflyer.AFInAppEventType.INITIATED_CHECKOUT
import com.appsflyer.AFInAppEventType.PURCHASE
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.AppsFlyerProperties
import com.appsflyer.deeplink.DeepLinkListener
import com.appsflyer.deeplink.DeepLinkResult
import com.mparticle.AttributionError
import com.mparticle.AttributionResult
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.commerce.CommerceEvent
import com.mparticle.commerce.Product
import com.mparticle.internal.Logger
import com.mparticle.internal.MPUtility
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.util.LinkedList


/**
 * mParticle Kit wrapper for the AppsFlyer SDK
 */
class AppsFlyerKit : KitIntegration(), KitIntegration.EventListener,
    KitIntegration.AttributeListener, KitIntegration.CommerceListener,
    AppsFlyerConversionListener, KitIntegration.ActivityListener {


    override fun getInstance(): AppsFlyerLib = AppsFlyerLib.getInstance();

    override fun getName() = NAME

    public override fun onKitCreate(
        setting: Map<String?, String?>?,
        context: Context
    ): List<ReportingMessage> {
        AppsFlyerLib.getInstance()
            .setDebugLog(MParticle.getInstance()?.environment == MParticle.Environment.Development)
        settings[DEV_KEY]?.let { AppsFlyerLib.getInstance().init(it, this, context) }
        AppsFlyerLib.getInstance().start(context.applicationContext)
        AppsFlyerLib.getInstance().setCollectAndroidID(MParticle.isAndroidIdEnabled())
        val integrationAttributes = HashMap<String, String?>(1)
        integrationAttributes[APPSFLYERID_INTEGRATION_KEY] =
            AppsFlyerLib.getInstance().getAppsFlyerUID(context)
        setIntegrationAttributes(integrationAttributes)
        AppsFlyerLib.getInstance().subscribeForDeepLink(deepLinkListener())

        val messages: MutableList<ReportingMessage> = ArrayList()
        messages.add(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.APP_STATE_TRANSITION,
                System.currentTimeMillis(),
                null
            )
        )
        return messages
    }

    override fun leaveBreadcrumb(breadcrumb: String): List<ReportingMessage> = emptyList()


    override fun logError(
        message: String,
        eventData: Map<String, String>
    ): List<ReportingMessage> = emptyList()


    override fun logException(
        exception: Exception,
        eventData: Map<String, String>,
        message: String
    ): List<ReportingMessage> = emptyList()

    override fun logLtvIncrease(
        valueIncreased: BigDecimal,
        valueTotal: BigDecimal,
        eventName: String,
        contextInfo: Map<String, String>
    ): List<ReportingMessage> = emptyList()


    override fun logEvent(event: CommerceEvent): List<ReportingMessage> {
        val messages: MutableList<ReportingMessage> = LinkedList()
        val eventValues: MutableMap<String, Any?> = HashMap()
        val productList = event.products

        if (isSalesEvent(event)) {
            logSalesEvent(event, eventValues, productList, messages)
        } else {
            logNotSalesEvent(event, messages)
        }
        return messages
    }

    private fun logNotSalesEvent(
        event: CommerceEvent,
        messages: MutableList<ReportingMessage>
    ) {
        val eventList = CommerceEventUtils.expand(event)
        if (eventList.isNotEmpty()) {
            for (e in eventList) {
                try {
                    logEvent(e)
                    messages.add(ReportingMessage.fromEvent(this, event))
                } catch (e: Exception) {
                    Logger.warning("Failed to call logCustomEvent to AppsFlyer kit: $e")
                }
            }
        }
    }

    private fun logSalesEvent(
        event: CommerceEvent,
        eventValues: MutableMap<String, Any?>,
        productList: MutableList<Product>?,
        messages: MutableList<ReportingMessage>
    ) {
        event.customAttributes?.let { eventValues.putAll(it) }

        if (!KitUtils.isEmpty(event.currency)) {
            eventValues[CURRENCY] = event.currency
        }

        if (event.productAction == Product.ADD_TO_CART
            || event.productAction == Product.ADD_TO_WISHLIST
        ) {
            val eventName =
                if (event.productAction == Product.ADD_TO_CART) ADD_TO_CART
                else ADD_TO_WISH_LIST

            productList?.iterator()?.forEach { product ->
                val productEventValues: MutableMap<String, Any?> = hashMapOf()
                productEventValues.putAll(eventValues)
                with(product) {
                    productEventValues[PRICE] = unitPrice
                    productEventValues[QUANTITY] = quantity
                    if (!KitUtils.isEmpty(sku)) {
                        productEventValues[CONTENT_ID] = sku
                    }
                    if (!KitUtils.isEmpty(category)) {
                        productEventValues[CONTENT_TYPE] = category
                    }
                }
                instance.logEvent(context, eventName, productEventValues)
                messages.add(ReportingMessage.fromEvent(this, event))
            }
        } else {
            val eventName =
                if (event.productAction == Product.CHECKOUT)
                    INITIATED_CHECKOUT else PURCHASE
            eventValues[CONTENT_ID] = generateProductIdList(event)

            if (!productList.isNullOrEmpty()) {
                var totalQuantity = 0.0
                for (product in productList) {
                    totalQuantity += product.quantity
                }
                eventValues[QUANTITY] = totalQuantity
            }

            val transactionAttributes = event.transactionAttributes
            if ((transactionAttributes != null) && (transactionAttributes.revenue != 0.0)) {
                val revenue = transactionAttributes.revenue
                if (event.productAction == Product.PURCHASE) {
                    eventValues[REVENUE] = revenue
                    if (!MPUtility.isEmpty(transactionAttributes.id)) {
                        eventValues[AFInAppEventType.ORDER_ID] = transactionAttributes.id
                    }
                } else {
                    eventValues[PRICE] = revenue
                }
            }
            instance.logEvent(context, eventName, eventValues)
            messages.add(ReportingMessage.fromEvent(this, event))
        }
    }

    private fun isSalesEvent(event: CommerceEvent) =
        (event.productAction == Product.ADD_TO_CART
                || event.productAction == Product.ADD_TO_WISHLIST
                || event.productAction == Product.CHECKOUT
                || event.productAction == Product.PURCHASE)

    override fun logEvent(event: MPEvent): List<ReportingMessage> {
        var hashMap: HashMap<String?, Any?>? = hashMapOf()
        if (event.customAttributes?.isNotEmpty() == true) {
            hashMap = event.customAttributes?.let { HashMap(it) }
        }
        instance.logEvent(context, event.eventName, hashMap)
        val messages: MutableList<ReportingMessage> = LinkedList()
        messages.add(ReportingMessage.fromEvent(this, event))
        return messages
    }

    override fun logScreen(
        screenName: String,
        eventAttributes: Map<String, String>
    ): List<ReportingMessage> = emptyList()


    override fun setOptOut(optOutStatus: Boolean): List<ReportingMessage> {
        instance.anonymizeUser(optOutStatus)
        val messageList: MutableList<ReportingMessage> = LinkedList()
        messageList.add(
            ReportingMessage(
                this,
                ReportingMessage.MessageType.OPT_OUT,
                System.currentTimeMillis(),
                null
            ).setOptOut(optOutStatus)
        )
        return messageList
    }

    override fun setUserAttribute(attributeKey: String, attributeValue: String) {}
    override fun setUserAttributeList(s: String, list: List<String>) {}
    override fun supportsAttributeLists(): Boolean = true
    override fun setAllUserAttributes(map: Map<String, String>, map1: Map<String, List<String>>) {}
    override fun removeUserAttribute(key: String) {}
    override fun removeUserIdentity(identityType: MParticle.IdentityType) {
        with(instance) {
            if (MParticle.IdentityType.CustomerId == identityType) {
                setCustomerUserId("")
            } else if (MParticle.IdentityType.Email == identityType) {
                setUserEmails(AppsFlyerProperties.EmailsCryptType.NONE, "")
            }
        }
    }

    override fun setUserIdentity(identityType: MParticle.IdentityType, identity: String) {
        with(instance) {
            if (MParticle.IdentityType.CustomerId == identityType) {
                setCustomerUserId(identity)
            } else if (MParticle.IdentityType.Email == identityType) {
                setUserEmails(AppsFlyerProperties.EmailsCryptType.NONE, identity)
            }
        }
    }

    override fun logout(): List<ReportingMessage> = emptyList()

    override fun onConversionDataSuccess(conversionDataN: MutableMap<String, Any>?) {
        var conversionData = conversionDataN
        val jsonResult = JSONObject()

        if (conversionData == null) {
            conversionData = hashMapOf()
        }

        conversionData[INSTALL_CONVERSION_RESULT] = "true"

        for ((key, value) in conversionData) {
            try {
                jsonResult.put(key, value)
            } catch (e: JSONException) {
            }
        }

        val result = AttributionResult()
            .setParameters(jsonResult)
            .setServiceProviderId(configuration.kitId)
        kitManager.onResult(result)
    }

    override fun onConversionDataFail(conversionFailure: String) {
        if (!KitUtils.isEmpty(conversionFailure)) {
            val error = AttributionError()
                .setMessage(conversionFailure)
                .setServiceProviderId(configuration.kitId)
            kitManager.onError(error)
        }
    }


    fun deepLinkListener() = DeepLinkListener { deepLinkResult ->
        val deepLinkObj = deepLinkResult.deepLink

        when (deepLinkResult.status) {
            DeepLinkResult.Status.FOUND -> {
                try {
                    deepLinkObj.clickEvent.put(APP_OPEN_ATTRIBUTION_RESULT, true.toString())
                    val result = AttributionResult()
                        .setParameters(deepLinkObj.clickEvent)
                        .setServiceProviderId(configuration.kitId)
                    kitManager.onResult(result)
                } catch (e: Exception) {
                    return@DeepLinkListener
                }
            }
            DeepLinkResult.Status.NOT_FOUND -> {
                return@DeepLinkListener
            }
            else -> {
                val dlError = deepLinkResult.error
                if (!KitUtils.isEmpty(dlError.toString())) {
                    val error = AttributionError()
                        .setMessage(dlError.toString())
                        .setServiceProviderId(configuration.kitId)
                    kitManager.onError(error)
                }
                return@DeepLinkListener
            }
        }
    }


    override fun onAppOpenAttribution(attributionDataN: MutableMap<String, String>?) {
        //do nothing, Appsflyer new UDL implementation will use new deep linking method with both
        // custom URI and appsflyer's Onelink
    }

    override fun onAttributionFailure(attributionFailure: String) {
        //do nothing, Appsflyer new UDL implementation will use new deep linking method with both
        // custom URI and appsflyer's Onelink
    }

    override fun setInstallReferrer(intent: Intent) {
        //do nothing, Appsflyer will fetch the install referrer data internally,
        // as long as the proper Play Install Referrer dependency is present.
    }

    override fun setLocation(location: Location) {
        instance.logLocation(context, location.latitude, location.longitude)
    }

    override fun onActivityCreated(
        activity: Activity,
        bundle: Bundle?
    ): List<ReportingMessage> {
        instance.start(activity)
        return emptyList()
    }

    override fun onActivityStarted(activity: Activity): List<ReportingMessage> = emptyList()

    override fun onActivityResumed(activity: Activity): List<ReportingMessage> = emptyList()

    override fun onActivityPaused(activity: Activity): List<ReportingMessage> = emptyList()

    override fun onActivityStopped(activity: Activity): List<ReportingMessage> = emptyList()

    override fun onActivitySaveInstanceState(
        activity: Activity,
        bundle: Bundle?
    ): List<ReportingMessage> = emptyList()

    override fun onActivityDestroyed(activity: Activity): List<ReportingMessage> = emptyList()


    companion object {
        const val DEV_KEY = "devKey"
        const val APPSFLYERID_INTEGRATION_KEY = "appsflyer_id_integration_setting"
        const val NAME = "AppsFlyer"
        const val COMMA = ","

        /**
         * This key will be present when returning a result from AppsFlyer's onInstallConversionDataLoaded API
         */
        const val INSTALL_CONVERSION_RESULT = "MPARTICLE_APPSFLYER_INSTALL_CONVERSION_RESULT"

        /**
         * This key will be present when returning a result from AppsFlyer's onAppOpenAttribution API
         */
        const val APP_OPEN_ATTRIBUTION_RESULT =
            "MPARTICLE_APPSFLYER_APP_OPEN_ATTRIBUTION_RESULT"

        fun generateProductIdList(event: CommerceEvent?): List<String>? {
            return event?.products?.filter { !KitUtils.isEmpty(it.sku) }?.let {
                if (it.isNotEmpty()) {
                    it.map { it.sku.replace(COMMA, "%2C") }
                } else { null }
            }
        }
    }
}
