package com.example.atismobileapplication.api

data class metar(
    val data_code: String,
    val type_code: String,
    val regional_code: String,
    val bulletin_code: String,
    val centre_code: String,
    val filling_time: String,
    val extra_code: String,
    val icao_code: String,
    val observed_time: String,
    val data_text: String,
    val valid: Int,
    val insert_time: String,
)
