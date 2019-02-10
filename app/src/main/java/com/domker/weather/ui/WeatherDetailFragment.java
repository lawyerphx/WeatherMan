package com.domker.weather.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.domker.weather.R;
import com.domker.weather.api.ApiManager;
import com.domker.weather.api.RxObserver;
import com.domker.weather.entity.SelectedCity;
import com.domker.weather.entity.WeatherDetail;
import com.google.gson.Gson;
import com.tencent.mmkv.MMKV;

/**
 * 天气的详情页
 * <p>
 * Created by wanlipeng on 2019/2/8 9:09 PM
 */
public class WeatherDetailFragment extends RxBaseFragment {
    private SelectedCity mSelectedCity;
    private ApiManager mApiManager;
    private WeatherDetail mWeatherDetail;
    private Gson mGson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_weather_detail, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toolbar toolbar = view.findViewById(R.id.toolBar);
        toolbar.setTitle(mSelectedCity.getCityName());
        if (getActivity() != null && getActivity() instanceof AppCompatActivity) {
            final AppCompatActivity activity = (AppCompatActivity) getActivity();
            toolbar.setNavigationIcon(R.drawable.ic_add_circle_white_24dp);
            activity.setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), CityListActivity.class);
                    startActivityForResult(intent, 0);
                }
            });
            final ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowHomeEnabled(true);
            }
        }
    }

    public void setSelectedCity(SelectedCity selectedCity) {
        mSelectedCity = selectedCity;
    }

    @SuppressLint("CheckResult")
    @Override
    protected void onFragmentFirstVisible() {
        mApiManager = new ApiManager();
    }

    @Override
    protected void onFragmentVisibleChange(boolean isVisible) {
        super.onFragmentVisibleChange(isVisible);
        if (isVisible) {
            loadData();
        }
    }

    private void loadData() {
        MMKV mmkv = MMKV.defaultMMKV();
        final String cityCode = mSelectedCity.getCityCode();
        try {
            if (mmkv.contains(cityCode)) {
                mWeatherDetail = mGson.fromJson(mmkv.decodeString(cityCode), WeatherDetail.class);
                bindData(mWeatherDetail);
            } else {
                loadDataFromNet(cityCode);
            }
        } catch (Exception e) {
            loadDataFromNet(cityCode);
        }
    }

    private void loadDataFromNet(String cityCode) {
        mApiManager.getCityDetail(cityCode, new RxObserver<WeatherDetail>() {
            @Override
            public void onSuccess(WeatherDetail weatherDetail) {
                bindData(weatherDetail);
                save(weatherDetail);
            }
        });
    }

    /**
     * 绑定数据到显示的View
     *
     * @param weatherDetail 天气信息的实体类
     */
    private void bindData(WeatherDetail weatherDetail) {
        if (weatherDetail != null && weatherDetail.getStatus() == 200) {
            mWeatherDetail = weatherDetail;
            WeatherDetail.WeatherInfo today = weatherDetail.getData().getForecast().get(0);
            setText(R.id.textViewTemp, weatherDetail.getData().getWendu() + "°");
            setText(R.id.textViewType, today.getType());
            setText(R.id.textViewUpdateTime, weatherDetail.getCityInfo().getUpdateTime() + "更新");
            setText(R.id.textViewWater, "湿度 " + weatherDetail.getData().getShidu());
            setText(R.id.textViewWind, today.getFx() + today.getFl());
            setText(R.id.textViewNotice, today.getNotice());

            // 今日气温和类型
            setText(R.id.textViewTodayType, today.getType());
            setText(R.id.textViewTodayTemp, today.getHigh() + "/" + today.getLow());

            // 明日气温和类型
            WeatherDetail.WeatherInfo tomorrow = weatherDetail.getData().getYesterday();
            setText(R.id.textViewTomorrowType, tomorrow.getType());
            setText(R.id.textViewTomorrowTemp, tomorrow.getHigh() + "/" + tomorrow.getLow());
        }
    }

    /**
     * 存储信息到MMKV中
     *
     * @param weatherDetail
     */
    private void save(WeatherDetail weatherDetail) {
        MMKV mmkv = MMKV.defaultMMKV();
        mmkv.encode(mSelectedCity.getCityCode(), mGson.toJson(weatherDetail));
    }
}