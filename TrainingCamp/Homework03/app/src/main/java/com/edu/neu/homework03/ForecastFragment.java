package com.edu.neu.homework03;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.json.JSONArray;
import org.json.JSONObject;

public class ForecastFragment extends Fragment {

    private LinearLayout container;
    private TextView tvCity;

    // 缓存数据
    private String mCityName;
    private JSONArray mCasts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forecast, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = view.findViewById(R.id.container_forecast_list);
        tvCity = view.findViewById(R.id.tv_forecast_city);
        updateUI();
    }

    public void updateData(String cityName, JSONArray casts) {
        this.mCityName = cityName;
        this.mCasts = casts;
        updateUI();
    }

    private void updateUI() {
        // 如果界面还没数据，就不执行
        if (container == null || tvCity == null || mCasts == null) {
            return;
        }

        try {
            tvCity.setText(mCityName);
            container.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(getContext());

            for (int i = 0; i < mCasts.length(); i++) {
                JSONObject dayData = mCasts.getJSONObject(i);
                View itemView = inflater.inflate(R.layout.item_forecast_card, container, false);

                TextView tvWeek = itemView.findViewById(R.id.item_week);
                TextView tvDate = itemView.findViewById(R.id.item_date);
                TextView tvWeather = itemView.findViewById(R.id.item_weather);
                TextView tvMax = itemView.findViewById(R.id.item_max_temp);
                TextView tvMin = itemView.findViewById(R.id.item_min_temp);

                String dateRaw = dayData.getString("date");
                String dateStr = dateRaw.length() > 5 ? dateRaw.substring(5) : dateRaw;

                String weekStr = "星期" + dayData.getString("week");
                if (i == 0) weekStr = "今天";
                else if (i == 1) weekStr = "明天";

                tvWeek.setText(weekStr);
                tvDate.setText(dateStr);
                tvWeather.setText(dayData.getString("dayweather"));
                tvMax.setText(dayData.getString("daytemp") + "°");
                tvMin.setText(dayData.getString("nighttemp") + "°");

                container.addView(itemView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}