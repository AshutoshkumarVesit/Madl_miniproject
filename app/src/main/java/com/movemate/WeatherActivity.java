package com.movemate;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.squareup.picasso.Picasso;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class WeatherActivity extends AppCompatActivity {
    private TextView tempText, windText, humidityText, locationText;
    private ImageView weatherIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        tempText = findViewById(R.id.tempText);
        windText = findViewById(R.id.windText);
        humidityText = findViewById(R.id.humidityText);
        locationText = findViewById(R.id.locationText);
        weatherIcon = findViewById(R.id.weatherIcon);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_stats);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) { startActivity(new android.content.Intent(this, DashboardActivity.class)); return true; }
                if (id == R.id.nav_stats) return true;
                if (id == R.id.nav_run) { startActivity(new android.content.Intent(this, MainActivity.class)); return true; }
                if (id == R.id.nav_community) { startActivity(new android.content.Intent(this, CommunityActivity.class)); return true; }
                if (id == R.id.nav_profile) { startActivity(new android.content.Intent(this, ProfileActivity.class)); return true; }
                return false;
            });
        }

        fetchWeather("London");
    }

    private void fetchWeather(String city) {
        String apiKey = BuildConfig.OPEN_WEATHER_API_KEY;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openweathermap.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        WeatherService service = retrofit.create(WeatherService.class);
        service.current(city, "metric", apiKey).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                WeatherResponse w = response.body();
                tempText.setText(String.format("%.0f°C", w.main.temp));
                windText.setText(String.format("%.1f m/s", w.wind.speed));
                humidityText.setText(String.format("%d%%", w.main.humidity));
                locationText.setText(w.name);
                if (w.weather != null && !w.weather.isEmpty()) {
                    String icon = w.weather.get(0).icon;
                    Picasso.get().load("https://openweathermap.org/img/wn/" + icon + "@2x.png").into(weatherIcon);
                }
            }
            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) { }
        });
    }

    interface WeatherService {
        @GET("data/2.5/weather")
        Call<WeatherResponse> current(@Query("q") String city, @Query("units") String units, @Query("appid") String apiKey);
    }

    static class WeatherResponse {
        Main main; java.util.List<Weather> weather; Wind wind; String name;
        static class Main { double temp; int humidity; }
        static class Weather { String icon; }
        static class Wind { double speed; }
    }
}

