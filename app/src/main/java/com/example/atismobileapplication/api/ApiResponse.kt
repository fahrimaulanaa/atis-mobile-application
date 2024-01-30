package com.example.atismobileapplication.api

import com.example.atismobileapplication.api.metar
import com.google.gson.annotations.SerializedName

data class ApiResponse(
    @SerializedName("WAHI") val metarList: List<metar>
)
