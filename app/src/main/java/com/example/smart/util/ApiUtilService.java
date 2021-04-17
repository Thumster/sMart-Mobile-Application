package com.example.smart.util;

import android.os.Build;

import com.example.smart.BuildConfig;
import com.example.smart.model.response.InitNavigateResponseVO;
import com.example.smart.model.response.PathResponseVO;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiUtilService {
    public String BASE_URL = BuildConfig.EDGE_SERVER_API_URL;

    @GET("path")
    Call<PathResponseVO> path(@Query("ox") int ox, @Query("oy") int oy, @Query("item") String itemId, @Query("user") String userId);

    @GET("initNavigate")
    Call<InitNavigateResponseVO> initNavigate(@Query("ox") int ox, @Query("oy") int oy, @Query("user") String userId);
}
