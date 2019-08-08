package com.example.kotlinnearby.Remote

import com.example.kotlinnearby.Model.MyPlaces
import com.example.kotlinnearby.Model.PlaceDetail
import com.google.android.gms.location.places.Place
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface IGoogleApiService {
    @GET
    fun getNearbyPalces(@Url url: String): Call<MyPlaces>

    @GET
    fun getDetailPlace(@Url url: String): Call<PlaceDetail>

    @GET("maps/api/directions/json")
    fun getDiretions(@Query("origin") origin: String, @Query("destination") destination: String): Call<String>
}