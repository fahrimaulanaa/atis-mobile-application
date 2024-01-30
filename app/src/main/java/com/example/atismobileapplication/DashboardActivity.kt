package com.example.atismobileapplication

import android.os.Bundle
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

class DashboardActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var metarApiService: ApiService
    private lateinit var tv_header: TextView

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
        getDataMetar()
    }

    private fun showToast(message: String) {
        Toast.makeText(baseContext, message, Toast.LENGTH_SHORT).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
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
            }
        })
    }

    private fun parsingMETAR(dataText: String){
        Log.w("METAR", dataText)
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


