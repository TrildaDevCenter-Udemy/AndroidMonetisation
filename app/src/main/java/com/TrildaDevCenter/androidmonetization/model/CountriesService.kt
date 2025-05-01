package com.trildadevcenter.androidmonetization.model

import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class CountriesService {

    private val BASE_URL = "https://raw.githubusercontent.com/DevTides/countries/master/"

    private var api = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
        .build()
        .create(CountriesApi::class.java)

    fun getCountries() = api.getCountries()
}