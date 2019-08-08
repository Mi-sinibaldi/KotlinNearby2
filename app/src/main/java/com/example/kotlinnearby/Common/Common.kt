package com.example.kotlinnearby.Common

import com.example.kotlinnearby.Model.Results
import com.example.kotlinnearby.Remote.IGoogleApiService
import com.example.kotlinnearby.Remote.RetrofitClient
import com.example.kotlinnearby.Remote.RetrofitScarlarsClient
import retrofit2.Retrofit

object Common {

    private var GOOGLE_API_URI = "https://maps.googleapis.com/"

    var currentResult: Results? = null

    val googleApiService: IGoogleApiService
        get() = RetrofitClient.getClient(GOOGLE_API_URI).create(IGoogleApiService::class.java)

    val googleApiServiceScalars: IGoogleApiService
    get() = RetrofitScarlarsClient.getClient(GOOGLE_API_URI).create(IGoogleApiService::class.java)
}