package com.devtides.androidmonetisation.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.devtides.androidmonetisation.adapter.CountryClickListener
import com.devtides.androidmonetisation.adapter.CountryListAdapter
import com.devtides.androidmonetisation.databinding.ActivityMainBinding
import com.devtides.androidmonetisation.model.BannerAd
import com.devtides.androidmonetisation.model.Country
import com.devtides.androidmonetisation.model.ListItem
import com.devtides.androidmonetisation.presenter.CountriesPresenter
import com.devtides.androidmonetisation.util.BillingAgent
import com.devtides.androidmonetisation.util.BillingCallback
import com.devtides.androidmonetisation.util.GoogleMobileAdsConsentManager
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd

class MainActivity : AppCompatActivity(), CountryClickListener, CountriesPresenter.View, BillingCallback {

    private val countriesList = arrayListOf<ListItem>()
    private val countriesAdapter = CountryListAdapter(arrayListOf(), this)
    private lateinit var binding: ActivityMainBinding

    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager

    private lateinit var rewardedAd: RewardedAd
    private var billingAgent: BillingAgent? = null
    private var clickedCountry: Country? = null

    private val presenter = CountriesPresenter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.list.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = countriesAdapter
        }

        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(applicationContext)
        // [START can_request_ads]
        googleMobileAdsConsentManager.gatherConsent(this) { error ->
            if (error != null) {
                // Consent not obtained in current session.
                Log.d(TAG, "${error.errorCode}: ${error.message}")
            }

            if (googleMobileAdsConsentManager.canRequestAds) {


            }
        }


        billingAgent = BillingAgent(this, this)
    }

    override fun onDestroy() {
        billingAgent?.onDestroy()
        billingAgent = null
        super.onDestroy()
    }

    override fun onCountryClick(country: Country) {
//        if(BuildConfig.FLAVOR == "free") {
//            progress.visibility = View.VISIBLE
//            retryButton.visibility = View.GONE
//            list.visibility = View.GONE
//            showRewardedAd(country)
//        } else {
//            startActivity(DetailActivity.getIntent(this, country))
//        }

        clickedCountry = country
//        billingAgent?.purchaseView()
        billingAgent?.purchaseSubscription()
    }

    override fun onTokenConsumed() {
        startActivity(DetailActivity.getIntent(this@MainActivity, clickedCountry))
    }

    private fun showRewardedAd(country: Country) {
       val listener = object: RewardedVideoAdListener
         {
            fun onRewardedVideoAdClosed() {
                showList()
            }

            fun onRewardedVideoAdLeftApplication() {
                showList()
            }

            fun onRewardedVideoAdLoaded() {
               // rewardedAd.show()
            }

            fun onRewardedVideoAdOpened() {
            }

            fun onRewardedVideoCompleted() {
                showList()
            }

            fun onRewarded(p0: RewardItem?) {
/               rewardedAd. destroy(this@MainActivity)
//                startActivity(DetailActivity.getIntent(this@MainActivity, country))
            }

            fun onRewardedVideoStarted() {
            }

            fun onRewardedVideoAdFailedToLoad(p0: Int) {
//                showList()
//                rewardedAd.destroy(this@MainActivity)
//                startActivity(DetailActivity.getIntent(this@MainActivity, country))
            }
        }

//        rewardedAd = MobileAds.getRewardedVideoAdInstance(this)
//        rewardedAd.rewardedVideoAdListener = listener
//        rewardedAd.loadAd(getString(R.string.rewarded_ad_id), AdRequest.Builder().build())
    }

    fun showList() {
        with(binding) {
            progress.visibility = View.GONE
            list.visibility = View.VISIBLE
            retryButton.visibility = View.GONE
        }
    }

    fun onRetry(v: View) {
        presenter.onRetry()
        with(binding) {
            retryButton.visibility = View.GONE
            progress.visibility = View.VISIBLE
            list.visibility = View.GONE
        }
    }

    override fun setCountries(countries: List<Country>?) {
        countriesList.clear()

        var i = 0
        countries?.let {
            for (country in countries) {
                i++
                if(i % 5 == 0) {
                    countriesList.add(BannerAd())
                }
                countriesList.add(country)
            }
        }

        countriesAdapter.updateCountries(countriesList)

        with(binding) {
            retryButton.visibility = View.GONE
            progress.visibility = View.GONE
            list.visibility = View.VISIBLE
        }
    }

    override fun onError() {
        Toast.makeText(this, "Unable to get Countries list. Please try again later", Toast.LENGTH_SHORT).show()

        with(binding) {
            retryButton.visibility = View.VISIBLE
            progress.visibility = View.GONE
            list.visibility = View.GONE
        }
    }

    companion object {
        // This is an ad unit ID for a test ad. Replace with your own banner ad unit ID.
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
        private const val TAG = "MainActivity"

        // Check your logcat output for the test device hashed ID e.g.
        // "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("ABCDEF012345"))
        // to get test ads on this device" or
        // "Use new ConsentDebugSettings.Builder().addTestDeviceHashedId("ABCDEF012345") to set this as
        // a debug device".
        const val TEST_DEVICE_HASHED_ID = "ABCDEF012345"
    }
}
