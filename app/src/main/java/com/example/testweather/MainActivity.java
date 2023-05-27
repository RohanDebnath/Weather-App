package com.example.testweather;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
public class MainActivity extends AppCompatActivity {

    private AutoCompleteTextView autoCompleteTextViewCity;
    private Button buttonSearch;
    private ProgressBar progressBar;
    private TextView textViewWeather,tempview,Humidity,WindSpeed,Condition;
    private ImageView imageViewWeather;

    private static final String API_KEY = "5373f6fe03ad4d92a2b180421232705";

    private static final String SHARED_PREFS_KEY = "WeatherAppPrefs";
    private static final String LAST_SEARCHED_CITY_KEY = "LastSearchedCity";
    private SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        autoCompleteTextViewCity = findViewById(R.id.autoCompleteTextViewCity);
        buttonSearch = findViewById(R.id.buttonSearch);
        progressBar = findViewById(R.id.progressBar);
        textViewWeather = findViewById(R.id.textViewWeather);
        tempview=findViewById(R.id.temp_View);
        Humidity=findViewById(R.id.humidity_id);
        WindSpeed=findViewById(R.id.wind_id);
        Condition=findViewById(R.id.condition_id);
        imageViewWeather = findViewById(R.id.imageViewWeather);
        sharedPreferences = getSharedPreferences(SHARED_PREFS_KEY, MODE_PRIVATE);


        // Set up the adapter for auto-suggestions
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new String[]{});
        autoCompleteTextViewCity.setAdapter(adapter);
        String lastSearchedCity = sharedPreferences.getString(LAST_SEARCHED_CITY_KEY, "");
        if (!lastSearchedCity.isEmpty()) {
            autoCompleteTextViewCity.setText(lastSearchedCity);
            new FetchWeatherTask().execute(lastSearchedCity);
        }

        // Set up item click listener for auto-suggestions
        autoCompleteTextViewCity.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String city = (String) parent.getItemAtPosition(position);
                autoCompleteTextViewCity.setText(city);
            }
        });

        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = autoCompleteTextViewCity.getText().toString().trim();
                if (!city.isEmpty()) {
                    new FetchWeatherTask().execute(city);
                    sharedPreferences.edit().putString(LAST_SEARCHED_CITY_KEY, city).apply();
                }
            }
        });

        // Retrieve auto-suggestions for city names
        new FetchCitySuggestionsTask().execute();
    }

    private class FetchCitySuggestionsTask extends AsyncTask<Void, Void, String[]> {

        @Override
        protected String[] doInBackground(Void... voids) {
            String apiUrl = "https://maps.googleapis.com/maps/api/place/autocomplete/json" +
                    "?input=" + autoCompleteTextViewCity.getText().toString().trim() +
                    "&types=(cities)" +
                    "&key=" + API_KEY;

            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                reader.close();

                JSONObject jsonObject = new JSONObject(stringBuilder.toString());
                int predictionsCount = jsonObject.getJSONArray("predictions").length();
                String[] suggestions = new String[predictionsCount];
                for (int i = 0; i < predictionsCount; i++) {
                    suggestions[i] = jsonObject.getJSONArray("predictions")
                            .getJSONObject(i).getString("description");
                }
                return suggestions;

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String[] suggestions) {
            super.onPostExecute(suggestions);

            if (suggestions != null) {
                // Update the auto-suggestions adapter
                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_dropdown_item_1line, suggestions);
                autoCompleteTextViewCity.setAdapter(adapter);
            }
        }
    }

    private class FetchWeatherTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            textViewWeather.setVisibility(View.GONE);
        }

        @Override
        protected String doInBackground(String... params) {
            String city = params[0];

            String apiUrl = "https://api.weatherapi.com/v1/current.json" +
                    "?key=" + API_KEY +
                    "&q=" + city;

            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                reader.close();

                return stringBuilder.toString();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressBar.setVisibility(View.GONE);
            textViewWeather.setVisibility(View.VISIBLE);

            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONObject currentObject = jsonObject.getJSONObject("current");

                    // Retrieve weather information
                    String temperature = currentObject.getString("temp_c");
                    String humidity = currentObject.getString("humidity");
                    String windSpeed = currentObject.getString("wind_kph");
                    String condition = currentObject.getJSONObject("condition").getString("text");

                    // Update weather information TextView
                    String weatherInfo = "Temperature: " + temperature + "°C\n"
                            + "Humidity: " + humidity + "%\n"
                            + "Wind Speed: " + windSpeed + " km/h\n"
                            + "Condition: " + condition;
                    String Temp=temperature + " °C\n";
                    String Humi=humidity + " °%\n";
                    String WS=windSpeed + " Kmp/H\n";
                    String Condi=condition + " °C\n";
                //    textViewWeather.setText(weatherInfo);
                    tempview.setText(Temp);
                    Humidity.setText(Humi);
                    WindSpeed.setText(WS);
                    Condition.setText(Condi);


                    // Update weather condition image
                    int imageResId = R.drawable.default_image; // Set a default image resource
                    if (condition.equals("Sunny")) {
                        imageResId = R.drawable.sunny_image;
                    } else if (condition.equals("Cloudy")) {
                        imageResId = R.drawable.cloudy_image;
                    } else if (condition.equals("Rainy")) {
                        imageResId = R.drawable.rainy_image;
                    }
                    imageViewWeather.setImageResource(imageResId);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                textViewWeather.setText("Failed to fetch weather information.");
            }
        }
    }
}