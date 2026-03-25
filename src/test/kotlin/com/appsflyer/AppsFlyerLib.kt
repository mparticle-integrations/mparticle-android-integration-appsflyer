package com.appsflyer

import android.content.Context
import com.appsflyer.AppsFlyerProperties
import java.util.HashMap

class AppsFlyerLib {
    private var consentData: AppsFlyerConsent? = null

    var startCallCount = 0
        private set

    var customerUserId: String? = null
        private set

    var setCustomerUserIdCallCount = 0
        private set

    var lastSetCustomerUserId: String? = null
        private set

    var lastLogEventName: String? = null
        private set

    var lastLogEventValues: Map<String, Any?>? = null
        private set

    fun setConsentData(consent: AppsFlyerConsent) {
        consentData = consent
    }

    fun getConsentData(): AppsFlyerConsent? = consentData

    fun start(context: Context) {
        startCallCount++
    }

    fun setCustomerUserId(id: String?) {
        setCustomerUserIdCallCount++
        lastSetCustomerUserId = id
        customerUserId = id
    }

    fun setUserEmails(
        cryptType: AppsFlyerProperties.EmailsCryptType,
        vararg emails: String,
    ) {}

    fun logEvent(
        context: Context,
        name: String,
        values: Map<String, Any?>?,
    ) {
        lastLogEventName = name
        lastLogEventValues = values?.let { HashMap<String, Any?>(it) }
    }

    fun anonymizeUser(optOut: Boolean) {}

    fun init(
        devKey: String,
        conversionListener: Any?,
        context: Context,
    ) {}

    fun setCollectAndroidID(collect: Boolean) {}

    fun getAppsFlyerUID(context: Context): String = "test-appsflyer-uid"

    fun subscribeForDeepLink(listener: Any?) {}

    fun setDebugLog(debug: Boolean) {}

    fun getConsentState(): MutableMap<Any, Any> {
        val stateMap = mutableMapOf<Any, Any>()
        consentData?.let { consent ->
            // Use property names directly instead of getter methods
            consent.isUserSubjectToGDPR?.let { stateMap["isUserSubjectToGDPR"] = it }
            consent.hasConsentForDataUsage?.let { stateMap["hasConsentForDataUsage"] = it }
            consent.hasConsentForAdsPersonalization?.let { stateMap["hasConsentForAdsPersonalization"] = it }
            consent.hasConsentForAdStorage?.let { stateMap["hasConsentForAdStorage"] = it }
        }
        return stateMap
    }

    companion object {
        private var _instance: AppsFlyerLib? = null

        @JvmStatic
        fun getInstance(): AppsFlyerLib? {
            if (_instance == null) {
                _instance = AppsFlyerLib()
            }
            return _instance
        }

        @JvmStatic
        fun getInstance(context: Context?): AppsFlyerLib? = getInstance()

        /**
         * Access Methods
         */
        fun clearInstance() {
            _instance = null
        }
    }
}
