package com.trildadevcenter.androidmonetization.presenter

import com.trildadevcenter.androidmonetization.activity.MainActivity
import com.trildadevcenter.androidmonetization.model.CountriesService
import com.trildadevcenter.androidmonetization.model.Country
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.observers.DisposableSingleObserver
import io.reactivex.rxjava3.schedulers.Schedulers

class CountriesPresenter(val view: View) {

    private val service = CountriesService()

    init {
        fetchCountries()
    }

    private fun fetchCountries() {
        service.getCountries()
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableSingleObserver<List<Country>>() {

                override fun onSuccess(t: List<Country>) {
                    view.setCountries(t)
                }

                override fun onError(e: Throwable) {
                    e?.printStackTrace()

                    view.onError()
                }
            })
    }

    fun onRetry() {
        fetchCountries()
    }

    interface View {
        fun setCountries(countries: List<Country>?)
        fun onError()
    }
}