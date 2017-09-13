package com.mparticle.appsflyer.example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.mparticle.DeepLinkError;
import com.mparticle.DeepLinkListener;
import com.mparticle.DeepLinkResult;
import com.mparticle.MParticle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MParticle.getInstance().checkForDeepLink(new DeepLinkListener() {
            @Override
            public void onResult(DeepLinkResult deepLinkResult) {

            }

            @Override
            public void onError(DeepLinkError deepLinkError) {

            }
        });
    }
}
