package com.mparticle.appsflyer.example;

import android.app.Application;
import com.mparticle.MParticle;

public class ExampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //Initialize the mParticle SDK
        //The SDK will automatically initialize the AppsFlyer SDK as well as any other kit-integrations
        MParticle.start(this, "REPLACE ME", "REPLACE ME");
    }
}
