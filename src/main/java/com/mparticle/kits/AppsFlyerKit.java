package com.mparticle.kits;

import android.app.Activity;
import android.app.Application;
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
import com.mparticle.DeepLinkError;
import com.mparticle.DeepLinkListener;
import com.mparticle.DeepLinkResult;
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
    private DeepLinkResult mLatestConversionData, mLatestOpenData;

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
        AppsFlyerLib.getInstance().init(getSettings().get(DEV_KEY), this);
        AppsFlyerLib.getInstance().startTracking((Application) context.getApplicationContext());
        AppsFlyerLib.getInstance().setCollectAndroidID(MParticle.isAndroidIdDisabled() == false);
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
                        AppsFlyerLib.getInstance().trackEvent(getContext(), eventName, productEventValues);
                        messages.add(ReportingMessage.fromEvent(this, event));
                    }
                }
            } else {
                String eventName = event.getProductAction().equals(Product.CHECKOUT) ? AFInAppEventType.INITIATED_CHECKOUT : AFInAppEventType.PURCHASE;
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
                AppsFlyerLib.getInstance().trackEvent(getContext(), eventName, eventValues);
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

    @Override
    public List<ReportingMessage> logEvent(MPEvent event) {
        HashMap<String, Object> hashMap = null;
        if (event.getInfo() != null && event.getInfo().size() > 0) {
            hashMap = new HashMap<String, Object>(event.getInfo());
        }
        AppsFlyerLib.getInstance().trackEvent(getContext(), event.getEventName(), hashMap);
        List<ReportingMessage> messages = new LinkedList<ReportingMessage>();
        messages.add(ReportingMessage.fromEvent(this, event));
        return messages;
    }

    @Override
    public List<ReportingMessage> logScreen(String screenName, Map<String, String> eventAttributes) {
        return null;
    }


    @Override
    public void checkForDeepLink() {
        if (mLatestOpenData != null) {
            ((DeepLinkListener) getKitManager()).onResult(mLatestOpenData);
        }
        if (mLatestConversionData != null) {
            ((DeepLinkListener) getKitManager()).onResult(mLatestConversionData);
        }
    }

    @Override
    public List<ReportingMessage> setOptOut(boolean optOutStatus) {
        AppsFlyerLib.getInstance().setDeviceTrackingDisabled(optOutStatus);
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
    public void onInstallConversionDataLoaded(Map<String, String> conversionData) {
        if (conversionData == null) {
            conversionData = new HashMap<String, String>();
        }
        conversionData.put(INSTALL_CONVERSION_RESULT, "true");
        JSONObject jsonResult = new JSONObject();
        for (Map.Entry<String, String> entry : conversionData.entrySet()) {
            try {
                jsonResult.put(entry.getKey(), entry.getValue());
            } catch (JSONException e) {
            }
        }

        DeepLinkResult result = new DeepLinkResult()
                .setParameters(jsonResult)
                .setServiceProviderId(getConfiguration().getKitId());
        mLatestConversionData = result;
        ((DeepLinkListener)getKitManager()).onResult(result);

    }

    @Override
    public void onInstallConversionFailure(String conversionFailure) {
        if (!KitUtils.isEmpty(conversionFailure)) {
            DeepLinkError error = new DeepLinkError()
                    .setMessage(conversionFailure)
                    .setServiceProviderId(getConfiguration().getKitId());
            ((DeepLinkListener)getKitManager()).onError(error);
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
        DeepLinkResult result = new DeepLinkResult()
                .setParameters(jsonResult)
                .setServiceProviderId(getConfiguration().getKitId());
        mLatestOpenData = result;
        ((DeepLinkListener)getKitManager()).onResult(result);
    }

    @Override
    public void onAttributionFailure(String attributionFailure) {
        if (!KitUtils.isEmpty(attributionFailure)) {
            DeepLinkError error = new DeepLinkError()
                    .setMessage(attributionFailure)
                    .setServiceProviderId(getConfiguration().getKitId());
            ((DeepLinkListener)getKitManager()).onError(error);
        }
    }

    @Override
    public void setInstallReferrer(Intent intent) {
        new SingleInstallBroadcastReceiver().onReceive(getContext(), intent);
    }

    @Override
    public void setLocation(Location location) {
        AppsFlyerLib.getInstance().trackLocation(getContext(), location.getLatitude(), location.getLongitude());
    }

    @Override
    public List<ReportingMessage> onActivityCreated(Activity activity, Bundle bundle) {
        AppsFlyerLib.getInstance().sendDeepLinkData(activity);
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