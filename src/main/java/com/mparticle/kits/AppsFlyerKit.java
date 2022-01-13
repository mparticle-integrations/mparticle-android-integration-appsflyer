package com.mparticle.kits;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

import com.appsflyer.AFInAppEventParameterName;
import com.appsflyer.AFInAppEventType;
import com.appsflyer.AppsFlyerConversionListener;
import com.appsflyer.AppsFlyerLib;
import com.appsflyer.AppsFlyerProperties;
import com.appsflyer.SingleInstallBroadcastReceiver;
import com.mparticle.AttributionError;
import com.mparticle.AttributionResult;
import com.mparticle.MPEvent;
import com.mparticle.MParticle;
import com.mparticle.commerce.CommerceEvent;
import com.mparticle.commerce.Product;
import com.mparticle.commerce.TransactionAttributes;
import com.mparticle.internal.Logger;
import com.mparticle.internal.MPUtility;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * mParticle Kit wrapper for the AppsFlyer SDK
 */
public class AppsFlyerKit extends KitIntegration implements KitIntegration.EventListener, KitIntegration.AttributeListener, KitIntegration.CommerceListener, AppsFlyerConversionListener, KitIntegration.ActivityListener {

    private static final String DEV_KEY = "devKey";
    private static final String APPSFLYERID_INTEGRATION_KEY = "appsflyer_id_integration_setting";

    /**
     * This key will be present when returning a result from AppsFlyer's onInstallConversionDataLoaded API
     */
    public static final String INSTALL_CONVERSION_RESULT = "MPARTICLE_APPSFLYER_INSTALL_CONVERSION_RESULT";

    /**
     * This key will be present when returning a result from AppsFlyer's onAppOpenAttribution API
     */
    public static final String APP_OPEN_ATTRIBUTION_RESULT = "MPARTICLE_APPSFLYER_APP_OPEN_ATTRIBUTION_RESULT";

    @Override
    public Object getInstance() {
        return AppsFlyerLib.getInstance();
    }

    @Override
    public String getName() {
        return "AppsFlyer";
    }

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> setting, Context context) {
        AppsFlyerLib.getInstance().setDebugLog(MParticle.getInstance().getEnvironment() == MParticle.Environment.Development);
        AppsFlyerLib.getInstance().init(getSettings().get(DEV_KEY), this, context);
        AppsFlyerLib.getInstance().start(context.getApplicationContext());
        AppsFlyerLib.getInstance().setCollectAndroidID(MParticle.isAndroidIdEnabled() == true);
        HashMap<String, String> integrationAttributes = new HashMap<String, String>(1);
        integrationAttributes.put(APPSFLYERID_INTEGRATION_KEY, AppsFlyerLib.getInstance().getAppsFlyerUID(context));
        setIntegrationAttributes(integrationAttributes);
        List<ReportingMessage> messages = new ArrayList<ReportingMessage>();
        messages.add(new ReportingMessage(this, ReportingMessage.MessageType.APP_STATE_TRANSITION, System.currentTimeMillis(), null));
        return messages;
    }


    @Override
    public List<ReportingMessage> leaveBreadcrumb(String breadcrumb) {
        return null;
    }

    @Override
    public List<ReportingMessage> logError(String message, Map<String, String> eventData) {
        return null;
    }

    @Override
    public List<ReportingMessage> logException(Exception exception, Map<String, String> eventData, String message) {
        return null;
    }

    @Override
    public List<ReportingMessage> logLtvIncrease(BigDecimal valueIncreased, BigDecimal valueTotal, String eventName, Map<String, String> contextInfo) {
        return null;
    }

    @Override
    public List<ReportingMessage> logEvent(CommerceEvent event) {
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();

        if (event.getProductAction().equals(Product.ADD_TO_CART)
                || event.getProductAction().equals(Product.ADD_TO_WISHLIST)
                || event.getProductAction().equals(Product.CHECKOUT)
                || event.getProductAction().equals(Product.PURCHASE)) {
            Map<String, Object> eventValues = new HashMap<String, Object>();

            if (event.getCustomAttributes() != null) {
                eventValues.putAll(event.getCustomAttributes());
            }
            if (!KitUtils.isEmpty(event.getCurrency())) {
                eventValues.put(AFInAppEventParameterName.CURRENCY, event.getCurrency());
            }
            if (event.getProductAction().equals(Product.ADD_TO_CART)
                    || event.getProductAction().equals(Product.ADD_TO_WISHLIST)) {
                String eventName = event.getProductAction().equals(Product.ADD_TO_CART) ? AFInAppEventType.ADD_TO_CART : AFInAppEventType.ADD_TO_WISH_LIST;
                if (event.getProducts().size() > 0) {
                    List<Product> productList = event.getProducts();
                    for (Product product : productList) {
                        Map<String, Object> productEventValues = new HashMap<String, Object>();
                        productEventValues.putAll(eventValues);
                        productEventValues.put(AFInAppEventParameterName.PRICE, product.getUnitPrice());
                        productEventValues.put(AFInAppEventParameterName.QUANTITY, product.getQuantity());
                        if (!KitUtils.isEmpty(product.getSku())) {
                            productEventValues.put(AFInAppEventParameterName.CONTENT_ID, product.getSku());
                        }
                        if (!KitUtils.isEmpty(product.getCategory())) {
                            productEventValues.put(AFInAppEventParameterName.CONTENT_TYPE, product.getCategory());
                        }
                        AppsFlyerLib.getInstance().logEvent(getContext(), eventName, productEventValues);
                        messages.add(ReportingMessage.fromEvent(this, event));
                    }
                }
            } else {
                String eventName = event.getProductAction().equals(Product.CHECKOUT) ? AFInAppEventType.INITIATED_CHECKOUT : AFInAppEventType.PURCHASE;
                eventValues.put(AFInAppEventParameterName.CONTENT_ID, AppsFlyerKit.generateProductIdList(event));
                if (event.getProducts() != null && event.getProducts().size() > 0) {
                    double totalQuantity = 0;
                    for (Product product : event.getProducts()) {
                        totalQuantity += product.getQuantity();
                    }
                    eventValues.put(AFInAppEventParameterName.QUANTITY, totalQuantity);
                }
                TransactionAttributes transactionAttributes = event.getTransactionAttributes();
                if (transactionAttributes != null && transactionAttributes.getRevenue() != 0) {
                    Double revenue = transactionAttributes.getRevenue();
                    if (event.getProductAction().equals(Product.PURCHASE)) {
                        eventValues.put(AFInAppEventParameterName.REVENUE, revenue);
                        if (!MPUtility.isEmpty(transactionAttributes.getId())) {
                            eventValues.put(AFInAppEventType.ORDER_ID, transactionAttributes.getId());
                        }
                    } else {
                        eventValues.put(AFInAppEventParameterName.PRICE, revenue);
                    }
                }
                AppsFlyerLib.getInstance().logEvent(getContext(), eventName, eventValues);
                messages.add(ReportingMessage.fromEvent(this, event));
            }
        } else {
            List<MPEvent> eventList = CommerceEventUtils.expand(event);
            if (eventList != null) {
                for (int i = 0; i < eventList.size(); i++) {
                    try {
                        logEvent(eventList.get(i));
                        messages.add(ReportingMessage.fromEvent(this, event));
                    } catch (Exception e) {
                        Logger.warning("Failed to call logCustomEvent to AppsFlyer kit: " + e.toString());
                    }
                }
            }
        }

        return messages;
    }

    static String generateProductIdList(CommerceEvent event) {
        if (event == null || event.getProducts() == null || event.getProducts().size() == 0) {
            return null;
        }
        StringBuilder productIdList = new StringBuilder();
        for (Product product : event.getProducts()) {
            String sku = product.getSku();
            if (!KitUtils.isEmpty(sku)) {
                productIdList.append(sku.replace(",","%2C"));
                productIdList.append(",");
            }
        }
        if (productIdList.length() > 0) {
            return productIdList
                    .substring(0, productIdList.length() - 1);
        }
        return null;
    }

    @Override
    public List<ReportingMessage> logEvent(MPEvent event) {
        HashMap<String, Object> hashMap = null;
        if (event.getCustomAttributes() != null && event.getCustomAttributes().size() > 0) {
            hashMap = new HashMap<String, Object>(event.getCustomAttributes());
        }
        AppsFlyerLib.getInstance().logEvent(getContext(), event.getEventName(), hashMap);
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        messages.add(ReportingMessage.fromEvent(this, event));
        return messages;
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> eventAttributes) {
        return null;
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optOutStatus) {
        AppsFlyerLib.getInstance().anonymizeUser(optOutStatus);
        List<ReportingMessage> messageList = new LinkedList<ReportingMessage>();
        messageList.add(
                new ReportingMessage(this, ReportingMessage.MessageType.OPT_OUT, System.currentTimeMillis(), null)
                        .setOptOut(optOutStatus)
        );
        return messageList;
    }


    @Override
    public void setUserAttribute(String attributeKey, String attributeValue) {

    }

    @Override
    public void setUserAttributeList(String s, List<String> list) {

    }

    @Override
    public boolean supportsAttributeLists() {
        return true;
    }

    @Override
    public void setAllUserAttributes(Map<String, String> map, Map<String, List<String>> map1) {

    }

    @Override
    public void removeUserAttribute(String key) {

    }

    @Override
    public void removeUserIdentity(MParticle.IdentityType identityType) {
        if (MParticle.IdentityType.CustomerId.equals(identityType)) {
            AppsFlyerLib.getInstance().setCustomerUserId("");
        } else if (MParticle.IdentityType.Email.equals(identityType)) {
            AppsFlyerLib.getInstance().setUserEmails(AppsFlyerProperties.EmailsCryptType.NONE, "");
        }
    }

    @Override
    public void setUserIdentity(MParticle.IdentityType identityType, String identity) {
        if (MParticle.IdentityType.CustomerId.equals(identityType)) {
            AppsFlyerLib.getInstance().setCustomerUserId(identity);
        } else if (MParticle.IdentityType.Email.equals(identityType)) {
            AppsFlyerLib.getInstance().setUserEmails(AppsFlyerProperties.EmailsCryptType.NONE, identity);
        }
    }

    @Override
    public List<ReportingMessage> logout() {
        return null;
    }

    @Override
    public void onConversionDataSuccess(Map<String, Object> conversionData) {
        if (conversionData == null) {
            conversionData = new HashMap<String, Object>();
        }
        conversionData.put(INSTALL_CONVERSION_RESULT, "true");
        JSONObject jsonResult = new JSONObject();
        for (Map.Entry<String, Object> entry : conversionData.entrySet()) {
            try {
                jsonResult.put(entry.getKey(), entry.getValue());
            } catch (JSONException e) {
            }
        }

        AttributionResult result = new AttributionResult()
                .setParameters(jsonResult)
                .setServiceProviderId(getConfiguration().getKitId());
        getKitManager().onResult(result);
    }

    @Override
    public void onConversionDataFail(String conversionFailure) {
        if (!KitUtils.isEmpty(conversionFailure)) {
            AttributionError error = new AttributionError()
                    .setMessage(conversionFailure)
                    .setServiceProviderId(getConfiguration().getKitId());
            getKitManager().onError(error);
        }
    }

    @Override
    public void onAppOpenAttribution(Map<String, String> attributionData) {
        if (attributionData == null) {
            attributionData = new HashMap<String, String>();
        }
        attributionData.put(APP_OPEN_ATTRIBUTION_RESULT, "true");
        JSONObject jsonResult = new JSONObject();
        for (Map.Entry<String, String> entry : attributionData.entrySet()) {
            try {
                jsonResult.put(entry.getKey(), entry.getValue());
            } catch (JSONException e) {
            }
        }
        AttributionResult result = new AttributionResult()
                .setParameters(jsonResult)
                .setServiceProviderId(getConfiguration().getKitId());
        getKitManager().onResult(result);
    }

    @Override
    public void onAttributionFailure(String attributionFailure) {
        if (!KitUtils.isEmpty(attributionFailure)) {
            AttributionError error = new AttributionError()
                    .setMessage(attributionFailure)
                    .setServiceProviderId(getConfiguration().getKitId());
            (getKitManager()).onError(error);
        }
    }

    @Override
    public void setInstallReferrer(Intent intent) {
        new SingleInstallBroadcastReceiver().onReceive(getContext(), intent);
    }

    @Override
    public void setLocation(Location location) {
        AppsFlyerLib.getInstance().logLocation(getContext(), location.getLatitude(), location.getLongitude());
    }

    @Override
    public List<ReportingMessage> onActivityCreated(Activity activity, Bundle bundle) {
        AppsFlyerLib.getInstance().start(activity);
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStarted(Activity activity) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityResumed(Activity activity) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityPaused(Activity activity) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityStopped(Activity activity) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        return null;
    }

    @Override
    public List<ReportingMessage> onActivityDestroyed(Activity activity) {
        return null;
    }
}