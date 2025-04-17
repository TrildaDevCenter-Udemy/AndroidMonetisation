package com.devtides.androidmonetisation.activity

import android.os.Bundle
import android.os.CountDownTimer
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
    private var mCountdownTimer: CountDownTimer? = null
    private var mGameOver = false
    private var mGamePaused = false
    private var mIsLoading = false
    private var mTimeRemaining: Long = 0L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.list.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = countriesAdapter
        }

        Timber.tag(TAG).d("Google Mobile Ads SDK Version: " + MobileAds.getVersion())

        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(applicationContext)

        // [START can_request_ads]
        googleMobileAdsConsentManager.gatherConsent(this) { error ->
            if (error != null) {
                // Consent not obtained in current session.
                Timber.tag(TAG).d( "${error.errorCode}: ${error.message}")
            }

            startGame()

            if (googleMobileAdsConsentManager.canRequestAds) {
                Timber.tag(TAG).d( "Consent is validated by User")
            }

            if (googleMobileAdsConsentManager.isPrivacyOptionsRequired) {
                // Regenerate the options menu to include a privacy setting.
                invalidateOptionsMenu()
            }
        }

       mBillingAgent = BillingAgent(this, this)

        startGame()
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

            SetupProgress(true)
            SetupRetryButton()
            SetupCountriesList()

            ShowHideUnwantedItems(false)

            loadRewardedAd()
            showRewardedVideo(country)
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
        var adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            this,
            AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Timber.tag("TAG").d(adError.message)
                    mIsLoading = false
                    mRewardedAd = null
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Timber.tag("TAG").d( "Ad was loaded.")
                    mRewardedAd = ad
                    mIsLoading = false
                }
            }
        )
    }

    private fun addCoins(coins: Int) {
        mCoinCount += coins
        binding.coinCountText.text = "Coins: $mCoinCount"
    }

    private fun SetupProgress(show: Boolean) {
        binding.progress.visibility = if (mIsLoading) View.VISIBLE else View.GONE
    }

    private fun SetupRetryButton() {
        binding.progress.visibility = if (!mIsLoading) View.VISIBLE else View.GONE
    }

    private fun SetupCountriesList() {
        binding.list.visibility = if (!mIsLoading) View.VISIBLE else View.GONE
    }


    private fun ShowHideUnwantedItems(show: Boolean) {
        if (show) {
            binding.gameTitle.visibility = View.VISIBLE
            binding.coinCountText.visibility = View.VISIBLE
            binding.showVideoButton.visibility = View.VISIBLE
            binding.timer.visibility = View.VISIBLE
        } else {
            binding.gameTitle.visibility = View.GONE
            binding.coinCountText.visibility = View.GONE
            binding.showVideoButton.visibility = View.GONE
            binding.timer.visibility = View.GONE
        }
    }

    private fun startGame() {
        // Hide the retry button, load the ad, and start the timer.
        SetupProgress(false)
        SetupRetryButton()
        SetupCountriesList()

        binding.showVideoButton.visibility = View.INVISIBLE

        ShowHideUnwantedItems(false)

        //createTimer(COUNTER_TIME)

        mGamePaused = false
        mGameOver = false
    }

    // Create the game timer, which counts down to the end of the level
    // and shows the "retry" button.
    private fun createTimer(time: Long) {
        mCountdownTimer?.cancel()

        mCountdownTimer =
            object : CountDownTimer(time * 1000, 50) {
                override fun onTick(millisUnitFinished: Long) {
                    mTimeRemaining = millisUnitFinished / 1000 + 1
                    binding.timer.text = "seconds remaining: $mTimeRemaining"
                }

                override fun onFinish() {
                    binding.showVideoButton.visibility = View.VISIBLE
                    binding.timer.text = "The game has ended!"
                    addCoins(GAME_OVER_REWARD)
                    binding.retryButton.visibility = View.VISIBLE
                    mGameOver = true
                }
            }

        mCountdownTimer?.start()
    }


    private fun showRewardedVideo(country: Country) {

        mRewardedAd?.let() {
            it.fullScreenContentCallback = object : FullScreenContentCallback() {

                override fun onAdClicked() {
                    Timber.tag("TAG").d("Ad was clicked.")
                }

                override fun onAdDismissedFullScreenContent() {
                    Timber.tag("TAG").d("Ad was dismissed.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    mRewardedAd = null
                    if (googleMobileAdsConsentManager.canRequestAds) {
                        //loadRewardedAd()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Timber.tag("TAG").d("Ad failed to show.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    mRewardedAd = null
                    startActivity(DetailActivity.getIntent(this@MainActivity, country))
                }

                override fun onAdImpression(){

                }

                override fun onAdShowedFullScreenContent() {
                    Timber.tag("TAG").d("Ad showed fullscreen content.")
                    // Called when ad is dismissed.
                    startActivity(DetailActivity.getIntent(this@MainActivity, country))
                }
            }

            mRewardedAd?.show(
                this,
                OnUserEarnedRewardListener { rewardItem ->
                    // Handle the reward.
                    val rewardAmount = rewardItem.amount
                    val rewardType = rewardItem.type
                    addCoins(rewardAmount)
                    Timber.tag("TAG").d("User earned the reward.")
                },
            )
        }
    }
//    private fun showRewardedAd(country: Country) {
//
//       val listener = object: RewardVideoAdListener
//         {
//            fun onRewardedVideoAdClosed() {
//                showHideList(true)
//            }
//
//            fun onRewardedVideoAdLeftApplication() {
//                showHideList(true)
//            }
//
//            fun onRewardedVideoAdLoaded() {
//               // rewardedAd.show()
//            }
//
//            fun onRewardedVideoAdOpened() {
//            }
//
//            fun onRewardedVideoCompleted() {
//                showHideList(true)
//            }
//
//            fun onRewarded(p0: RewardItem?) {
//              rewardedAd. destroy(this@MainActivity)
//                startActivity(DetailActivity.getIntent(this@MainActivity, country))
//            }
//
//            fun onRewardedVideoStarted() {
//            }
//
//            fun onRewardedVideoAdFailedToLoad(p0: Int) {
//                showHideList(true)
//                rewardedAd.destroy(this@MainActivity)
//                startActivity(DetailActivity.getIntent(this@MainActivity, country))
//            }
//        }
//
//        var adRequest = AdRequest.Builder().build()
//
//        RewardedAd.load(this,"ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
//            override fun onAdFailedToLoad(adError: LoadAdError) {
//                Timber.tag(TAG).d( adError?.toString())
//                rewardedAd = null
//            }
//
//            override fun onAdLoaded(ad: RewardedAd) {
//                Timber.tag(TAG).d("Ad was loaded.")
//                rewardedAd = ad
//            }
//        })
//
//        rewardedAd = MobileAds.getRewardedVideoAdInstance(this)
//        rewardedAd.rewardedVideoAdListener = listener
//        rewardedAd.loadAd(getString(R.string.rewarded_ad_id), AdRequest.Builder().build())
//    }

    fun showHideList(show: Boolean) {
        with(binding) {
            progress.visibility = View.GONE
            list.visibility = View.VISIBLE
            retryButton.visibility = View.GONE
        }
    }

    fun onRetry(v: View) {
        mPresenter.onRetry()
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

    private fun pauseGame() {
        if (mGameOver || mGamePaused) {
            return
        }
        //mCountdownTimer?.cancel()
        mGamePaused = true
    }

    private fun resumeGame() {
        if (mGameOver || !mGamePaused) {
            return
        }
        //createTimer(mTimeRemaining)
        mGamePaused = false
    }

    companion object {
        // This is an ad unit ID for a test ad. Replace with your own banner ad unit ID.
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val COUNTER_TIME = 10L
        private const val GAME_OVER_REWARD = 1
        private const val TAG = "MainActivity"

        // Check your logcat output for the test device hashed ID e.g.
        // "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("ABCDEF012345"))
        // to get test ads on this device" or
        // "Use new ConsentDebugSettings.Builder().addTestDeviceHashedId("ABCDEF012345") to set this as
        // a debug device".
        const val TEST_DEVICE_HASHED_ID = "ABCDEF012345"
    }
}
