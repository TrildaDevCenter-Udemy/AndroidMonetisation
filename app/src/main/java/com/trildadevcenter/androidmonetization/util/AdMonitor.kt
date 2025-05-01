package com.trildadevcenter.androidmonetization.util

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.trildadevcenter.androidmonetization.application.Monitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class AdMonitor(val applicationContext: Context) {

    fun checkOrAskUserConsent(activity: Activity) {
        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(applicationContext)

        // [START can_request_ads]
        googleMobileAdsConsentManager.gatherConsent(activity) { error ->
            if (error != null) {
                // Consent not obtained in current session.
                Timber.tag(TAG).d( "${error.errorCode}: ${error.message}")
            }

            if (googleMobileAdsConsentManager.canRequestAds) {
                Timber.tag(TAG).d( "Consent is validated by User")
            }

            if (googleMobileAdsConsentManager.isPrivacyOptionsRequired) {
                // Regenerate the options menu to include a privacy setting.
                activity.invalidateOptionsMenu()
            }
        }
    }

    fun initializeMobileAdsSdk( activity :Activity) {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        // Set your test devices.
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(Monitor.Companion.TEST_DEVICE_HASHED_ID))
                .build()
        )

        CoroutineScope(Dispatchers.IO).launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(applicationContext) {
               //loadAd(activity)
            }
        }
    }

    companion object {
        private const val TAG = "AD_MONITOR"
        lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
        val isMobileAdsInitializeCalled = AtomicBoolean(false)
        val gatherConsentFinished = AtomicBoolean(false)

        @Volatile
        private var instance: AdMonitor? = null

        fun getInstance(context: Context) =
            instance
                ?: synchronized(this) {
                    instance ?: AdMonitor(context).also { instance = it }
                }
    }
}