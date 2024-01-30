package com.example.atismobileapplication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.atismobileapplication.api.ApiService
import com.example.atismobileapplication.api.ApiResponse
import com.example.atismobileapplication.api.metar
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var metarApiService: ApiService
    private lateinit var tv_header: TextView
    private lateinit var metar_text: String

    //handler untuk menjalankan runnable
    private val handler = Handler()
    private val getDataMetarRunnable = object : Runnable {
        override fun run() {
            getDataMetar()

            val currentTime = Calendar.getInstance()
            val currentMinute = currentTime.get(Calendar.MINUTE)
            val delayMillis = calculateDelayMillis(currentMinute)
            handler.postDelayed(this, delayMillis)
        }
    }

    private fun calculateDelayMillis(currentMinute: Int): Long {
        val minutesToNextTrigger = when {
            currentMinute < 30 -> 30 - currentMinute
            currentMinute < 59 -> 60 - currentMinute
            else -> 31
        }

        return minutesToNextTrigger * 60 * 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            showToast("You are not logged in")
        } else {
            Log.w("USER", auth.currentUser.toString())
        }

        // api handler
        val authentication = "Bearer c000561fd431399e706fc6d495606ebd655871a46807171c24c2e3704a46"
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(authentication))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://web-aviation.bmkg.go.id/api/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

        metarApiService = retrofit.create(ApiService::class.java)

        //panggil fungsi getDataMetar setiap 30 menit
        handler.postDelayed(getDataMetarRunnable, 30 * 60 * 1000)
        getDataMetar()

        startBackgroundService()

    }
    private fun startBackgroundService() {
        val serviceIntent = Intent(this, BackgroundService::class.java)
        startForegroundService(serviceIntent)
    }

    private fun showToast(message: String) {
        Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

    override fun onDestroy() {
        handler.removeCallbacks(getDataMetarRunnable)
        super.onDestroy()
    }


    private fun getDataMetar() {
        val call = metarApiService.getMetarData("WAHI", "Bearer c000561fd431399e706fc6d495606ebd655871a46807171c24c2e3704a46")

        call.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(
                call: Call<ApiResponse>,
                response: Response<ApiResponse>
            ) {
                Log.d("METAR", "Response Code: ${response.code()}")
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null) {
                        val metarList = apiResponse.metarList
                        if (metarList.isNotEmpty()) {
                            val firstMetar = metarList[0]
                            val dataText = firstMetar.data_text
                            parsingMETAR(dataText)

                            tv_header = findViewById(R.id.tv_header)
                            tv_header.text = dataText

                            Log.w("METAR", dataText)
                        } else {
                            Toast.makeText(baseContext, "METAR list is null or empty", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(baseContext, "API response is null", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("METAR", "Error: ${response.code()}: ${response.message()}")
                    Toast.makeText(baseContext, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("METAR", "Error: ${t.message}")
                Toast.makeText(baseContext, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                getDataMetar()
            }
        })
    }

    private fun parsingMETAR(dataText: String){
        metar_text = dataText
        parsingTimestamp(metar_text)
        parsingWindDirectionAndSpeed(metar_text)
        parsingWindVariation(metar_text)
        parsingVisibility(metar_text)
        parsingRain(metar_text)

    }

    private fun parsingRain(metarText: String){
        val pattern = "(\\+|-)?(?:RA|RERA|TSRA|VCSH)\\b".toRegex()
        val matchResult = pattern.find(metarText)

        if(matchResult != null){
            val rainCode = matchResult.value
            val tv_rain = findViewById<TextView>(R.id.tv_weather)
            val iv_weather = findViewById<TextView>(R.id.img_weather)

            when (rainCode) {
                "RA" -> {
                    tv_rain.text = "Rain"
                }
                "-RA" -> {
                    tv_rain.text = "Light Rain"
                }
                "+RA" -> {
                    tv_rain.text = "Heavy Rain"
                }
                "RERA" -> {
                    tv_rain.text = "Rain Ending"
                }
                "TSRA" -> {
                    tv_rain.text = "Thunderstorm with Rain"
                }
                "-TSRA" -> {
                    tv_rain.text = "Thunderstorm with Light Rain"
                }
                "+TSRA" -> {
                    tv_rain.text = "Thunderstorm with Heavy Rain"
                }
                "VCSH" -> {
                    tv_rain.text = "Showers in Vicinity"
                }
                else -> tv_rain.text = "Sunny Clear Sky"

            }
        } else {
            // Jika tidak ditemukan, tampilkan pesan bahwa tidak ada hujan
            val tv_rain = findViewById<TextView>(R.id.tv_weather)
            tv_rain.text = "No Rain Detected"
        }
    }


    private fun parsingVisibility(metarText: String) {
        val pattern = "\\b(\\d{1,4})\\b".toRegex()
        val matchResult = pattern.find(metarText)

        if (matchResult != null) {
            val visibility = matchResult.groupValues[1]
            Log.w("VISIBILITY", "Visibility: $visibility")
            if (visibility == "9999"){
                val tv_visibility = findViewById<TextView>(R.id.visibility)
                tv_visibility.text = ">10 KM"
            }else{
                val visibilityInKm = visibility.toDouble() / 1000
                val tv_visibility = findViewById<TextView>(R.id.visibility)
                tv_visibility.text = "$visibilityInKm KM"
            }
        } else {
            Log.w("VISIBILITY", "Visibility tidak ditemukan dalam string METAR.")
        }
    }

    private fun parsingWindVariation(variationString: String) {
        // Pola regex untuk memisahkan nilai dari string METAR
        val pattern = "(\\d{3})V(\\d{3})".toRegex()
        val matchResult = pattern.find(variationString)

        if (matchResult != null) {
            val windDirection1 = matchResult.groupValues[1]
            val windDirection2 = matchResult.groupValues[2]

            Log.w("WIND", "Wind Direction 1: $windDirection1")
            Log.w("WIND", "Wind Direction 2: $windDirection2")
        } else {
            Log.w("WIND", "Wind variation tidak ditemukan dalam string METAR.")
        }
    }
    private fun parsingWindDirectionAndSpeed(metarText: String) {
        val pattern = "(\\d{3}|VRB)(\\d{2,3})(G(\\d{2,3}))?KT".toRegex()
        val matchResult = pattern.find(metarText)

        if (matchResult != null) {
            val windDirection = matchResult.groupValues[1]
            val windSpeed = matchResult.groupValues[2]
            val windGust = matchResult.groupValues[4]

            Log.w("WIND", "Wind Direction: $windDirection")
            Log.w("WIND", "Wind Speed: $windSpeed")
            Log.w("WIND", "Wind Gust: $windGust")

            val tv_wind = findViewById<TextView>(R.id.wind_speed)
            tv_wind.text = "$windSpeed KT"
        } else {
            Log.w("WIND", "Wind direction and speed tidak ditemukan dalam string METAR.")
        }

    }

    private fun parsingTimestamp(metar_text: String) {
        val pattern = "\\b\\d{6}Z\\b".toRegex()
        val matchResult = pattern.find(metar_text)

        if (matchResult != null) {
            val timestamp = matchResult.value
            try {
                // Dapatkan tahun saat ini
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val timestampWithYear = "$currentYear$timestamp"
                val dateFormat = SimpleDateFormat("yyyyddHHmm'Z'", Locale.US)
                dateFormat.timeZone = TimeZone.getTimeZone("UTC")
                val parsedDate = dateFormat.parse(timestampWithYear)

                // Format tanggal (30 January 2024)
                val datePattern = SimpleDateFormat("dd MMMM yyyy", Locale.US)
                datePattern.timeZone = TimeZone.getTimeZone("UTC")
                val formattedDate = datePattern.format(parsedDate)

                // Format waktu UTC (07:30 UTC)
                val utcTimePattern = SimpleDateFormat("HH:mm 'UTC'", Locale.US)
                utcTimePattern.timeZone = TimeZone.getTimeZone("UTC")
                val formattedUtcTime = utcTimePattern.format(parsedDate)

                Log.w("TIMESTAMP", "Formatted Date: $formattedDate")
                Log.w("TIMESTAMP", "Formatted UTC Time: $formattedUtcTime")

                val formattedDateText = "Today, $formattedDate | $formattedUtcTime"
                val tv_timestamp = findViewById<TextView>(R.id.tv_infoclock)
                tv_timestamp.text = formattedDateText
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("TIMESTAMP", "Gagal melakukan parsing timestamp: ${e.message}")
            }
        } else {
            Log.w("TIMESTAMP", "Timestamp tidak ditemukan dalam string METAR.")
        }
    }

    private class AuthInterceptor(private val authentication: String) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Authorization", authentication)
            val request = requestBuilder.build()
            return chain.proceed(request)
        }
    }
}


