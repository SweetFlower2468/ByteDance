package com.edu.neu.homework03;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.json.JSONArray;
import org.json.JSONObject;

public class HomeFragment extends Fragment {

    private TextView tvCityName, tvWeatherMain, tvCurrentTemp, tvTempRange;
    private TextView tvDayWeather, tvDayTemp, tvDayWind;
    private TextView tvNightWeather, tvNightTemp, tvNightWind;

    public interface OnCitySelectedListener {
        void onCitySelected(String cityName);
    }

    private OnCitySelectedListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnCitySelectedListener) {
            listener = (OnCitySelectedListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvCityName = view.findViewById(R.id.tv_city_name);
        tvWeatherMain = view.findViewById(R.id.tv_weather_main);
        tvCurrentTemp = view.findViewById(R.id.tv_current_temp);
        tvTempRange = view.findViewById(R.id.tv_temp_range);

        tvDayWeather = view.findViewById(R.id.tv_day_weather);
        tvDayTemp = view.findViewById(R.id.tv_day_temp);
        tvDayWind = view.findViewById(R.id.tv_day_wind);

        tvNightWeather = view.findViewById(R.id.tv_night_weather);
        tvNightTemp = view.findViewById(R.id.tv_night_temp);
        tvNightWind = view.findViewById(R.id.tv_night_wind);

        // 绑定按钮点击事件
        View.OnClickListener clickListener = v -> {
            if (listener != null) {
                Button btn = (Button) v;
                // 不改变 Activity 的输入框
                listener.onCitySelected(btn.getText().toString());
            }
        };

        view.findViewById(R.id.btn_bj).setOnClickListener(clickListener);
        view.findViewById(R.id.btn_sh).setOnClickListener(clickListener);
        view.findViewById(R.id.btn_sy).setOnClickListener(clickListener);
        view.findViewById(R.id.btn_lz).setOnClickListener(clickListener);
    }

    public void updateData(String cityName, JSONObject today) {
        try {
            tvCityName.setText(cityName);
            tvWeatherMain.setText(today.getString("dayweather"));

            String dayTemp = today.getString("daytemp");
            String nightTemp = today.getString("nighttemp");
            tvCurrentTemp.setText(dayTemp + "°");
            tvTempRange.setText("最高: " + dayTemp + "°  最低: " + nightTemp + "°");

            tvDayWeather.setText(today.getString("dayweather"));
            tvDayTemp.setText(dayTemp + "°");
            tvDayWind.setText(today.getString("daypower") + "级");

            tvNightWeather.setText(today.getString("nightweather"));
            tvNightTemp.setText(nightTemp + "°");
            tvNightWind.setText(today.getString("nightpower") + "级");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}