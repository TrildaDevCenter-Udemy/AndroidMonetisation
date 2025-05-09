package io.github.trildadevcenter.androidmonetization.model

import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET

interface CountriesApi {
    @GET("countriesV2.json")
    fun getCountries(): Single<List<Country>>
}