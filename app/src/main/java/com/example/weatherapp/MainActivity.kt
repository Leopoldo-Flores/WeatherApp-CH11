package com.example.weatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Class 1
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import com.example.weatherapp.network.AppConstants
import com.example.weatherapp.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// Class 2
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WeatherScreen()
            }
        }
    }
}

@Composable
fun WeatherScreen() {  // a value Compose watches changes for
    var city by remember { mutableStateOf("") }
    var cityResult by remember { mutableStateOf("City: --") }
    var tempResult by remember { mutableStateOf("Temperature: --") }
    var descResult by remember { mutableStateOf("Description: --") }

    var windResult by remember { mutableStateOf("Wind Speed: --") }
    var humidityResult by remember { mutableStateOf("Humidity: --") }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    // Three state variables so each Text below can recompose independently

    val context = LocalContext.current // Toast.makeText() requires an Android Context -- a composable isn't an Activity,
    // so we pull one from LocalContext instead

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("Enter city name") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            enabled = !isLoading, // disables the button mid-request
            onClick = {
                val trimmedCity = city.trim() // trim() removes accidental leading/trailing spaces before validating
                if (trimmedCity.isEmpty()) {
                    Toast.makeText(context, "Please enter a city name", Toast.LENGTH_SHORT).show()
                } else {
                    isLoading = true
                    scope.launch {
                        try {
                            val response = withContext(Dispatchers.IO) {
                                RetrofitClient.weatherApiService.getWeather(
                                    city = trimmedCity,
                                    apiKey = AppConstants.API_KEY,
                                    units = AppConstants.UNITS
                                )
                            }
                            // response.raw() gives the underlying OkHttp response
                            Log.d("WeatherApp", "Request URL: ${response.raw().request.url}")
                            // the exact URL Retrofit built. check log cat filter by WeatherApp
                            Log.d("WeatherApp", "Response code: ${response.code()}")
                            if (response.isSuccessful) {
                                val weather = response.body()
                                if (weather != null) {
                                    cityResult = "City: ${weather.name}"
                                    tempResult = "Temperature: ${weather.main.temp} F"
                                    descResult = "Description: ${weather.weather[0].description}"
                                    // Assignment 2
                                    // 1. Set windResult from weather.wind.speed (append " m/s")
                                    // 2. Set humidityResult from weather.main.humidity (append "%")
                                    // 3. Below in the else branch, replace the single Toast with a
                                    //    when (response.code()) -- see Hour 3 for the exact messages
                                // TODO: this Toast gets replaced by step 3 above -- a when (response.code())
                                // with different messages for 404, 401, and anything else
                                }
                            } else {
                                Toast.makeText(context, "City not found. Check the name and try again!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Network error. Check your connection.", Toast.LENGTH_SHORT).show()
                        }
                        finally { // finally always runs - success, HTTP error, or exception
                            isLoading = false
                        }
                    }
                    // 1. Add "val scope = rememberCoroutineScope()" near the top of
                    //    WeatherScreen(), next to where "val context = ..." already is
                    // 2. Replace the Toast line below with "scope.launch { }"
                    // 3. Inside launch, use "withContext(Dispatchers.IO) { }" for the network call
                    // 4. Call RetrofitClient.weatherApiService.getWeather(trimmedCity, AppConstants.API_KEY, AppConstants.UNITS)
                    // 5. If response.isSuccessful, update cityResult / tempResult / descResult
                    // 6. If NOT successful, show a Toast: "City not found. Check the name and try again."
                    // 7. Wrap steps 2-6 in try { } catch (e: Exception) { }, showing
                    //    "Network error. Check your connection." in the catch block

                    Toast.makeText(context, "Coming soon -- weather for $trimmedCity", Toast.LENGTH_SHORT).show()
                    // DELETE the line above when you implement the real fetch
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(if (isLoading) "Loading..." else "Get Weather")
        }
        Text(cityResult, fontSize = 20.sp, modifier = Modifier.padding(top = 24.dp))
        Text(tempResult, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
        Text(descResult, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
        Text(windResult, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
        Text(humidityResult, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
    }
}




























