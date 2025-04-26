package com.devtides.androidmonetisation.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.devtides.androidmonetisation.BuildConfig
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
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : AppCompatActivity(), CountryClickListener, CountriesPresenter.View, BillingCallback {

    private val countriesList = arrayListOf<ListItem>()
    private val countriesAdapter = CountryListAdapter(arrayListOf(), this)
    private lateinit var binding: ActivityMainBinding

    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
    private  var mRewardedAd: RewardedAd? = null

    private var mBillingAgent: BillingAgent? = null
    private var mClickedCountry: Country? = null

    private val mPresenter = CountriesPresenter(this)

    private var mCoinCount: Int = 0
    private var mGameOver = false
    private var mGamePaused = false
    private var mIsLoading = true
    private var mCanRetry = true
    private var mShowCountries = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.list.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = countriesAdapter
        }

        Timber.tag(TAG).d("Google Mobile Ads SDK Version: " + MobileAds.getVersion())

        updateUi()

        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(applicationContext)

        // [START can_request_ads]
        googleMobileAdsConsentManager.gatherConsent(this) { error ->
            if (error != null) {
                // Consent not obtained in current session.
                Timber.tag(TAG).d( "${error.errorCode}: ${error.message}")
            }

            if (googleMobileAdsConsentManager.canRequestAds) {
                Timber.tag(TAG).d( "Consent is validated by User")
            }

            if (googleMobileAdsConsentManager.isPrivacyOptionsRequired) {
                // Regenerate the options menu to include a privacy setting.
                invalidateOptionsMenu()
            }
        }

        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(this@MainActivity) {}
        }

       mBillingAgent = BillingAgent(this, this)
    }

    public override fun onPause() {
        super.onPause()
        pauseGame()
    }

    public override fun onResume() {
        super.onResume()
        resumeGame()
    }

    override fun onDestroy() {
        mBillingAgent?.onDestroy()
        mBillingAgent = null
        super.onDestroy()
    }

    override fun onCountryClick(country: Country) {
        if(BuildConfig.FLAVOR == "free") {

            mIsLoading = true
            mCanRetry = false
            mShowCountries = false
            updateUi()

            // do we have enough ad coins  ?
            if (canCoins(DETAILS_COST)) {
                // yes : we start the country details activity
                startActivity(DetailActivity.getIntent(this, country))
            }
            else {
                // no : we load an ad to win 10 ad coins
                loadRewardedAd()
            }

        } else {
            startActivity(DetailActivity.getIntent(this, country))
        }

        mClickedCountry = country
        mBillingAgent?.purchaseView()
        mBillingAgent?.purchaseSubscription()
    }

    override fun onTokenConsumed() {
        startActivity(DetailActivity.getIntent(this@MainActivity, mClickedCountry))
    }


    private fun loadRewardedAd() {
        mIsLoading = true
        mCanRetry = false
        mShowCountries = false
        updateUi()

        var adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            this,
            AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Timber.tag("TAG").d(adError.message)
                    mIsLoading = false
                    updateUi()

                    mRewardedAd = null

                    val error = "domain: ${adError.domain}, code: ${adError.code}, " + "message: ${adError.message}"
                    Timber.tag("TAG").e(error)

                    Toast.makeText(
                        this@MainActivity,
                        "onAdFailedToLoad() with error $error",
                        Toast.LENGTH_SHORT,
                    ).show()
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Timber.tag("TAG").d("Ad was loaded.")
                    Toast.makeText(this@MainActivity, "onAdLoaded()", Toast.LENGTH_SHORT).show()

                    mIsLoading = false
                    updateUi()

                    mRewardedAd = ad
                    showRewardedVideo(mClickedCountry)
                }
            }
        )
    }

    private fun addCoins(coins: Int) {
        mCoinCount += coins
        updateUi()
    }

    private fun consumeCoins(coins: Int) {
        mCoinCount -= coins
        updateUi()
    }

    private fun canCoins(coins: Int) : Boolean {
        if (mCoinCount >= coins ){
            consumeCoins(coins)
            return true
        } else {
            return false
        }
    }

    private fun SetupProgress() {
        with(binding) {
            progress.visibility = if (mIsLoading xor (progress.visibility == View.VISIBLE)) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }


    private fun SetupRetryButton() {
        with(binding) {
            progress.visibility = if (mCanRetry xor (retryButton.visibility == View.VISIBLE)) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }


    private fun SetupCountriesList() {
        with(binding) {
            list.visibility =  if (mShowCountries xor (list.visibility == View.VISIBLE)) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun ShowHideUnwantedItems() {
        with(binding) {
            gameTitle.visibility = View.GONE
            coinCountText.visibility = View.GONE
            showVideoButton.visibility = View.GONE
            timer.visibility = View.GONE
        }
    }

    private fun updateUi() {
        SetupRetryButton()
        ShowHideUnwantedItems()
        SetupCountriesList()
        SetupProgress()
    }

    private fun showRewardedVideo(country: Country?) {

        mRewardedAd?.let() {
            mIsLoading = true
            mShowCountries = false

            it.fullScreenContentCallback = object : FullScreenContentCallback() {

                override fun onAdClicked() {
                    Timber.tag("TAG").d("Ad was clicked.")
                    Toast.makeText(this@MainActivity, "Ad was licked", Toast.LENGTH_SHORT).show()
                }

                override fun onAdDismissedFullScreenContent() {
                    Timber.tag("TAG").d("Ad was dismissed.")
                    Toast.makeText(this@MainActivity, "Ad was dismissed", Toast.LENGTH_SHORT).show()
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    mRewardedAd = null
                    if (googleMobileAdsConsentManager.canRequestAds) {
                        //loadRewardedAd()
                    }
                    mRewardedAd = null

                    country?.let() {
                        startActivity(DetailActivity.getIntent(this@MainActivity, country))
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Timber.tag("TAG").d("Ad failed to show.")
                    Toast.makeText(this@MainActivity, "Ad failed to show", Toast.LENGTH_SHORT).show()

                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    mRewardedAd = null

                    country?.let() {
                        startActivity(DetailActivity.getIntent(this@MainActivity, country))
                    }
                }

                override fun onAdImpression(){
                    Timber.tag("TAG").d("On ad Impression, reward was won.")
                    Toast.makeText(this@MainActivity, "on ad Impression", Toast.LENGTH_SHORT).show()
                }

                override fun onAdShowedFullScreenContent() {
                    Timber.tag("TAG").d("Ad showed fullscreen content.")
                    Toast.makeText(this@MainActivity, "Ad showed fullscreen content", Toast.LENGTH_SHORT).show()

                    mIsLoading = false
                }
            }

            it.show(
                this,
                OnUserEarnedRewardListener { rewardItem ->
                    // Handle the reward.
                    val rewardAmount = rewardItem.amount
                    val rewardType = rewardItem.type
                    addCoins(rewardAmount)
                    Timber.tag("TAG").d("User earned the reward.")
                    mIsLoading = false
                    mCanRetry = false
                    mShowCountries = true
                    updateUi()
                },
            )
        }
    }

    fun onRetry(v: View) {
        mIsLoading = true
        mShowCountries = false

        updateUi()

        mPresenter.onRetry()
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

        mIsLoading = false
        mCanRetry = false
        mShowCountries = true

        updateUi()
    }

    override fun onError() {
        Toast.makeText(this, "Unable to get Countries list. Please try again later", Toast.LENGTH_SHORT).show()

        with(binding) {
            retryButton.visibility = View.VISIBLE
            progress.visibility = View.GONE
            list.visibility = View.GONE
        }
    }

    private fun pauseGame() {
        if (mGameOver || mGamePaused) {
            return
        }
        mGamePaused = true
        updateUi()

    }

    private fun resumeGame() {
        if (mGameOver || !mGamePaused) {
            return
        }
        mGamePaused = false
        updateUi()
    }

    companion object {
        // This is an ad unit ID for a test ad. Replace with your own banner ad unit ID.
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val COUNTER_TIME = 10L
        private const val DETAILS_COST = 1
        private const val TAG = "MainActivity"

        // Check your logcat output for the test device hashed ID e.g.
        // "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("ABCDEF012345"))
        // to get test ads on this device" or
        // "Use new ConsentDebugSettings.Builder().addTestDeviceHashedId("ABCDEF012345") to set this as
        // a debug device".
        const val TEST_DEVICE_HASHED_ID = "ABCDEF012345"
    }
}
