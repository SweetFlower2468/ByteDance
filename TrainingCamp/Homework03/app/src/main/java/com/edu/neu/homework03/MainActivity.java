package com.edu.neu.homework03;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements HomeFragment.OnCitySelectedListener {

    private EditText etCity;
    private ViewPager2 viewPager;
    private RadioGroup rgTabs;

    private HomeFragment homeFragment;
    private ForecastFragment forecastFragment;

    private static final String API_KEY = "自己创建一个API_KEY吧！";
    private static final String BASE_URL = "https://restapi.amap.com/v3/weather/weatherInfo?extensions=all&key=" + API_KEY + "&city=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            findViewById(R.id.header_container).setPadding(0, insets.top, 0, 0);
            findViewById(R.id.bottom_tabs_container).setPadding(0, 0, 0, insets.bottom + 60);
            return windowInsets;
        });

        etCity = findViewById(R.id.et_city);
        viewPager = findViewById(R.id.view_pager);
        rgTabs = findViewById(R.id.rg_tabs);

        homeFragment = new HomeFragment();
        forecastFragment = new ForecastFragment();

        viewPager.setAdapter(new WeatherPagerAdapter(this));

        // 预加载
        viewPager.setOffscreenPageLimit(1);

        findViewById(R.id.btn_search).setOnClickListener(v -> {
            String city = etCity.getText().toString().trim();
            if (!TextUtils.isEmpty(city)) {
                hideKeyboard();
                getWeatherData(city);
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) rgTabs.check(R.id.rb_city);
                else rgTabs.check(R.id.rb_forecast);
            }
        });

        rgTabs.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_city) viewPager.setCurrentItem(0, true);
            else viewPager.setCurrentItem(1, true);
        });

        checkLocationPermissionAndLocate();
    }

    @Override
    public void onCitySelected(String cityName) {
        getWeatherData(cityName);
    }

    private void getWeatherData(String city) {
        new Thread(() -> {
            try {
                String encodedCity = java.net.URLEncoder.encode(city, "UTF-8");
                URL url = new URL(BASE_URL + encodedCity);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    parseAndShowData(response.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "查询失败", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void parseAndShowData(String json) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                JSONObject root = new JSONObject(json);
                if ("1".equals(root.getString("status"))) {
                    JSONArray forecasts = root.getJSONArray("forecasts");
                    if (forecasts.length() > 0) {
                        JSONObject cityObj = forecasts.getJSONObject(0);
                        String cityName = cityObj.getString("city");
                        JSONArray casts = cityObj.getJSONArray("casts");

                        // 两个页面都更新
                        if (homeFragment != null) {
                            homeFragment.updateData(cityName, casts.getJSONObject(0));
                        }
                        if (forecastFragment != null) {
                            forecastFragment.updateData(cityName, casts);
                        }

                    } else {
                        Toast.makeText(MainActivity.this, "未找到该城市", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private class WeatherPagerAdapter extends FragmentStateAdapter {
        public WeatherPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }
        @NonNull @Override
        public Fragment createFragment(int position) {
            if (position == 0) return homeFragment;
            else return forecastFragment;
        }
        @Override public int getItemCount() { return 2; }
    }

    private void checkLocationPermissionAndLocate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            getLocation();
        }
    }

    private void getLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        lm.removeUpdates(this);
                        reverseGeocode(location.getLatitude(), location.getLongitude());
                    }
                    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                    @Override public void onProviderEnabled(@NonNull String provider) {}
                    @Override public void onProviderDisabled(@NonNull String provider) {}
                });
            } else {
                getWeatherData("北京");
            }
        } catch (SecurityException e) {
            getWeatherData("北京");
        }
    }

    private void reverseGeocode(double lat, double lon) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    String city = addresses.get(0).getLocality();
                    if (city == null) city = addresses.get(0).getSubAdminArea();
                    final String finalCity = city;
                    runOnUiThread(() -> {
                        etCity.setText(finalCity);
                        getWeatherData(finalCity);
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> getWeatherData("北京"));
            }
        }).start();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        } else {
            getWeatherData("北京");
        }
    }
}