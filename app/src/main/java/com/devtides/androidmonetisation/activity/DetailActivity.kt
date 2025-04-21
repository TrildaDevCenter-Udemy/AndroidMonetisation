package com.devtides.androidmonetisation.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import com.devtides.androidmonetisation.R
import com.devtides.androidmonetisation.databinding.ActivityDetailBinding
import com.devtides.androidmonetisation.model.Country
import com.devtides.androidmonetisation.util.getProgressDrawable
import com.devtides.androidmonetisation.util.loadImage
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


class DetailActivity : AppCompatActivity() {

    lateinit var country: Country
    private lateinit var binding: ActivityDetailBinding
    private var wasShown = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.hasExtra(PARAM_COUNTRY) == false) {
            finish()
        }

        val value = IntentCompat.getParcelableExtra(intent, PARAM_COUNTRY, Country::class.java)
        if (value == null) {
            finish()
        }
        country = value!!

        populate()


        CoroutineScope(Dispatchers.IO).launch {
            runOnUiThread {
                // Load an ad on the main thread.
                loadAd()
            }
        }
    }


    private fun loadAd() {

        if (hasInterstitialAd != null) {
            return
        }

        var adRequest = AdRequest.Builder().build()

        isLoading = true

        InterstitialAd.load(
            this,
            getString(R.string.monetize_interstitial_ad_id),
            adRequest,
            object : InterstitialAdLoadCallback() {

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    hasInterstitialAd = null
                    isLoading = false

                    val error = "domain: ${adError.domain}, code: ${adError.code}, " + "message: ${adError.message}"
                    Timber.tag("TAG").e(error)

                    Toast.makeText(
                        this@DetailActivity,
                        "onAdFailedToLoad() with error $error",
                        Toast.LENGTH_LONG,
                    ).show()
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Timber.tag("TAG").d( "InterstitialAd was loaded.")
                    hasInterstitialAd = ad
                    isLoading = false
                    Toast.makeText(this@DetailActivity, "onAdLoaded()", Toast.LENGTH_LONG).show()
                    showInterstitialAd()
                }
            }
        )
    }


    private fun showInterstitialAd() {
        if (wasShown) {
            return
        }

        if (isLoading) {
            return
        }

        if (hasInterstitialAd == null) {
            loadAd()
        }

        hasInterstitialAd?.let {
            hasInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    Timber.tag("TAG").d("Ad Clicked, click is recorded for the ad.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    hasInterstitialAd = null
                }

                override fun onAdDismissedFullScreenContent() {
                    Timber.tag("TAG")
                        .d("Ad Dismissed fullscreen, called when the ad dismissed full screen content..")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    hasInterstitialAd = null
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Timber.tag("TAG")
                        .d("Ad failed , called when the ad failed to show full screen content.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    hasInterstitialAd = null
                }

                override fun onAdImpression() {
                    Timber.tag("TAG")
                        .d("Ad Impression, Called when an impression is recorded for an ad..")
                    // Called when ad is dismissed.
                }

                override fun onAdShowedFullScreenContent() {
                    Timber.tag("TAG")
                        .d("Ad showed fullscreen content, Called when the ad showed the full screen content..")
                    // Called when ad is dismissed.
                }
            }
            wasShown = true
            hasInterstitialAd?.show(this@DetailActivity)
        }

            //reload an interstitial add for next flag details
        loadAd()
    }

    fun populate() {
        with(binding) {
            countryFlag.loadImage(country.flag, getProgressDrawable(this.root.context))
            textName.text = country.countryName
            textCapital.text = "Capital: ${country.capital}"
            textArea.text = "Area: ${country.area}"
            textPopulation.text = "Population: ${country.population}"
            textRegion.text = "Region: ${country.region}"
        }
    }

    companion object {
        // TODO load from resources
        val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

        val PARAM_COUNTRY = "country"
        val TAG = "DetailActivity"

        var isLoading = false
        var hasInterstitialAd: InterstitialAd? = null



        fun getIntent(context: Context, country: Country?): Intent {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra(PARAM_COUNTRY, country)
            return intent
        }
    }
}
