package com.devtides.androidmonetisation.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import com.devtides.androidmonetisation.databinding.ActivityDetailBinding
import com.devtides.androidmonetisation.model.Country
import com.devtides.androidmonetisation.util.getProgressDrawable
import com.devtides.androidmonetisation.util.loadImage
import com.google.android.gms.ads.interstitial.InterstitialAd

class DetailActivity : AppCompatActivity() {

    lateinit var country: Country
    private lateinit var binding: ActivityDetailBinding


    private lateinit var interstitialAd: InterstitialAd

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
//        interstitialAd = InterstitialAd(this)
//        interstitialAd.adUnitId = getString(R.string.interstitial_ad_id)
//        //interstitialAd.loadAd(AdRequest.Builder().build())
//        interstitialAd.adListener = object: AdListener() {
//            override fun onAdLoaded() {
//                interstitialAd.show()
//            }
//        }
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

        fun getIntent(context: Context, country: Country?): Intent {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra(PARAM_COUNTRY, country)
            return intent
        }
    }
}
