package com.example.atismobileapplication.api

import com.example.atismobileapplication.api.ApiResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface ApiService {
    @GET("metar/{stationCode}")
    fun getMetarData(
        @Path("stationCode") stationCode: String,
        @Header("Authorization") token: String
    ): Call<ApiResponse>
}


