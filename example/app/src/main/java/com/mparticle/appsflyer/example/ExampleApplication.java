package com.mparticle.appsflyer.example;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;

import com.mparticle.AttributionError;
import com.mparticle.AttributionListener;
import com.mparticle.AttributionResult;
import com.mparticle.MParticle;
import com.mparticle.MParticleOptions;

public class ExampleApplication extends Application implements AttributionListener {
    @Override
    public void onCreate() {
        super.onCreate();
        //Initialize the mParticle SDK
        //The SDK will automatically initialize the AppsFlyer SDK as well as any other kit-integrations
        MParticleOptions options = MParticleOptions.builder( this)
                .credentials("REPLACE", "REPLACE")
                .logLevel(MParticle.LogLevel.VERBOSE)
                .attributionListener(this)
                .build();
        MParticle.start(options);
    }

    @Override
    public void onResult(@NonNull AttributionResult attributionResult) {
        if (attributionResult.getServiceProviderId() == MParticle.ServiceProviders.APPSFLYER) {
            // this attribution result came from the AppsFlyer kit, you can parse the parameters
            // that AppsFlyer has documented here:
            // https://support.appsflyer.com/hc/en-us/articles/207032096-Deferred-deep-linking-getting-the-conversion-data#response-keys
            Log.d("ATTRIBUTION RESULT", attributionResult.getParameters().toString());
        }
    }

    @Override
    public void onError(@NonNull AttributionError attributionError) {
        Log.d("ATTRIBUTION ERROR", attributionError.toString());
    }
}
