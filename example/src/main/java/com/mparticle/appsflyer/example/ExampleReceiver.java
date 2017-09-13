package com.mparticle.appsflyer.example;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mparticle.ReferrerReceiver;

/**
 * If you have implemented your own receiver for the INSTALL_REFERRER intent,
 * then you must forward the received intent on to mParticle, and mParticle will then
 * forward the Intent into any kits and server-side integrations.
 *
 * See here for more information:
 * http://docs.mparticle.com/developers/sdk/android/getting-started/#google-play-install-referrer
 *
 */
public class ExampleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        new ReferrerReceiver().onReceive(context, intent);
    }
}
