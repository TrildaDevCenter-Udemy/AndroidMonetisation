package com.devtides.androidmonetisation.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import com.devtides.androidmonetisation.databinding.ActivityDetailBinding
import com.devtides.androidmonetisation.model.Country
import com.devtides.androidmonetisation.util.getProgressDrawable
import com.devtides.androidmonetisation.util.loadImage
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import timber.log.Timber


class DetailActivity : AppCompatActivity() {

    lateinit var country: Country
    private lateinit var binding: ActivityDetailBinding

    private var mInterstitialAd: InterstitialAd? = null

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

        showInterstitialAd()

        populate()
    }

    private fun showInterstitialAd() {

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Timber.tag(TAG).d(adError?.toString())
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Timber.tag(TAG).d("Ad was loaded.")
                mInterstitialAd = interstitialAd
            }
        })
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
        val PARAM_COUNTRY = "country"
        val TAG = "DetailActivity"

        fun getIntent(context: Context, country: Country?): Intent {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra(PARAM_COUNTRY, country)
            return intent
        }
    }
}
