package com.mparticle.appsflyer.example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.mparticle.DeepLinkError;
import com.mparticle.DeepLinkListener;
import com.mparticle.DeepLinkResult;
import com.mparticle.MParticle;
import com.mparticle.kits.AppsFlyerKit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MParticle.getInstance().checkForDeepLink(new DeepLinkListener() {
            @Override
            public void onResult(DeepLinkResult deepLinkResult) {
                if (deepLinkResult.getServiceProviderId() == MParticle.ServiceProviders.APPSFLYER) {
                    if (deepLinkResult.getParameters().has(AppsFlyerKit.INSTALL_CONVERSION_RESULT)) {
                        Log.d("Conversion result", deepLinkResult.toString());
                    } else if (deepLinkResult.getParameters().has(AppsFlyerKit.APP_OPEN_ATTRIBUTION_RESULT)) {
                        Log.d("App open result", deepLinkResult.toString());
                    }
                }
            }

            @Override
            public void onError(DeepLinkError deepLinkError) {
                Log.d("Deep link error", deepLinkError.toString());
            }
        });
    }
}
