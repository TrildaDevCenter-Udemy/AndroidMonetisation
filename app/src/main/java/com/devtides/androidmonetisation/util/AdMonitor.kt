package com.devtides.androidmonetisation.util

import com.devtides.androidmonetisation.activity.SplashActivity
import com.devtides.androidmonetisation.application.Monitor
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AdMonitor {

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        // Set your test devices.
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(Monitor.TEST_DEVICE_HASHED_ID))
                .build()
        )

//        CoroutineScope(Dispatchers.IO).launch {
//            // Initialize the Google Mobile Ads SDK on a background thread.
//            MobileAds.initialize(this@SplashActivity) {}
//            runOnUiThread {
//                // Load an ad on the main thread.
//                (application as Monitor).loadAd(this@SplashActivity)
//            }
//        }

        // Load an ad.
    }

    companion object {
        lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
        val isMobileAdsInitializeCalled = AtomicBoolean(false)
        val gatherConsentFinished = AtomicBoolean(false)
    }
}