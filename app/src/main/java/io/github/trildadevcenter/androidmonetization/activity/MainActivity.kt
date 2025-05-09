package io.github.trildadevcenter.androidmonetization.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.trildadevcenter.androidmonetization.adapter.CountryClickListener
import io.github.trildadevcenter.androidmonetization.adapter.CountryListAdapter
import io.github.trildadevcenter.androidmonetization.model.BannerAd
import io.github.trildadevcenter.androidmonetization.model.Country
import io.github.trildadevcenter.androidmonetization.model.ListItem
import io.github.trildadevcenter.androidmonetization.presenter.CountriesPresenter
import io.github.trildadevcenter.androidmonetization.util.AdMonitor
import io.github.trildadevcenter.androidmonetization.util.BillingAgent
import io.github.trildadevcenter.androidmonetization.util.BillingCallback
import io.github.trildadevcenter.androidmonetization.util.GoogleMobileAdsConsentManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import io.github.trildadevcenter.androidmonetization.BuildConfig
import io.github.trildadevcenter.androidmonetization.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity(), CountryClickListener, CountriesPresenter.View,
    BillingCallback {

    private val countriesList = arrayListOf<ListItem>()
    private val countriesAdapter = CountryListAdapter(arrayListOf(), this)
    private lateinit var binding: ActivityMainBinding

    private lateinit var mAdMonitor: AdMonitor
    private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager
    private  var mRewardedAd: RewardedAd? = null

    private var mBillingAgent: BillingAgent? = null
    private var mClickedCountry: Country? = null

    private val mPresenter = CountriesPresenter(this)

    private var mCoinCount: Int = 0
    private var mGameOver = false
    private var mGamePaused = false

    private var mShowProgress = false
    private var mShowRetry = true
    private var mShowList = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAdMonitor = AdMonitor(this@MainActivity)

        binding.list.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = countriesAdapter
        }

        Timber.tag(TAG).d("Google Mobile Ads SDK Version: " + MobileAds.getVersion())

        mShowProgress = false
        mShowRetry = true
        mShowList = false

        updateUi()

        mAdMonitor.checkOrAskUserConsent(this@MainActivity)
        mAdMonitor.initializeMobileAdsSdk(this@MainActivity)


//        googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(applicationContext)
//
//        // [START can_request_ads]
//        googleMobileAdsConsentManager.gatherConsent(this) { error ->
//            if (error != null) {
//                // Consent not obtained in current session.
//                Timber.tag(TAG).d( "${error.errorCode}: ${error.message}")
//            }
//
//            if (googleMobileAdsConsentManager.canRequestAds) {
//                Timber.tag(TAG).d( "Consent is validated by User")
//            }
//
//            if (googleMobileAdsConsentManager.isPrivacyOptionsRequired) {
//                // Regenerate the options menu to include a privacy setting.
//                this.invalidateOptionsMenu()
//            }
//        }

//        val backgroundScope = CoroutineScope(Dispatchers.IO)
//        backgroundScope.launch {
//            // Initialize the Google Mobile Ads SDK on a background thread.
//            MobileAds.initialize(this@MainActivity) {}
//        }

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

            mShowProgress = false
            mShowRetry = false
            mShowList = false
            updateUi()

            // do we have enough ad coins  ?
            if (canCoins(DETAILS_COST)) {
                // yes : we start the country details activity
                StartDetailsActivity(country)
            }
            else {
                // no : we load an ad to win 10 ad coins
                loadRewardedAd()
            }
        } else {
            StartDetailsActivity(country)
        }

        mClickedCountry = country
        mBillingAgent?.purchaseView()
        mBillingAgent?.purchaseSubscription()
    }

    override fun onTokenConsumed() {
        StartDetailsActivity(mClickedCountry)
    }

    private fun loadRewardedAd() {
        mShowProgress = true
        mShowRetry = false
        mShowList = false
        updateUi()

        var adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            this,
            AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Timber.tag("TAG").d(adError.message)
                    mShowProgress = false
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

                    mShowProgress = false
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
            with(progress) {
                if (mShowProgress xor (this.visibility == View.VISIBLE)) {
                    // something has to change, we manage a flip flop
                    this.visibility = if (mShowProgress) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
        }
    }


    private fun SetupRetryButton() {
        with(binding) {
            with(retryButton) {
                if (mShowRetry xor (this.visibility == View.VISIBLE)) {
                    // something has to change, we manage a flip flop
                    this.visibility = if (mShowRetry) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
        }
    }


    private fun SetupCountriesList() {
        with(binding) {
            with(list) {
                if (mShowList xor (this.visibility == View.VISIBLE)) {
                    // something has to change, we manage a flip flop
                    this.visibility = if (mShowList) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
        }
    }

    private fun updateUi() {
        SetupRetryButton()
        SetupCountriesList()
        SetupProgress()
    }

    private fun showRewardedVideo(country: Country?) {

        mRewardedAd?.let() {
            mShowProgress = true
            mShowList = false

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

                    StartDetailsActivity(country)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Timber.tag("TAG").d("Ad failed to show.")
                    Toast.makeText(this@MainActivity, "Ad failed to show", Toast.LENGTH_SHORT).show()

                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    mRewardedAd = null

                    StartDetailsActivity(country)
                }

                override fun onAdImpression(){
                    Timber.tag("TAG").d("On ad Impression, reward was won.")
                    Toast.makeText(this@MainActivity, "on ad Impression", Toast.LENGTH_SHORT).show()
                }

                override fun onAdShowedFullScreenContent() {
                    Timber.tag("TAG").d("Ad showed fullscreen content.")
                    Toast.makeText(this@MainActivity, "Ad showed fullscreen content", Toast.LENGTH_SHORT).show()

                    mShowProgress = false
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
                    mShowProgress = false
                    mShowRetry = false
                    mShowList = true
                    updateUi()
                },
            )
        }
    }

    fun onRetry(v: View) {
        mShowProgress = true
        mShowRetry = false
        mShowList = false

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

        mShowProgress = false
        mShowRetry = false
        mShowList = true

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

    private fun StartDetailsActivity(country: Country?) {

        mShowProgress = false
        mShowRetry = false
        mShowList = true

        updateUi()

        country?.let {
            startActivity(DetailActivity.getIntent(this@MainActivity, country))
        }
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
