package com.example.smart.util;

import com.example.smart.model.response.InitNavigateResponseVO;
import com.example.smart.model.response.PathResponseVO;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiUtilService {
//    public String BASE_URL = "http://jianyiee.ddns.net:5000/";
    public String BASE_URL = "http://192.168.1.219:5000/";

    @GET("path")
    Call<PathResponseVO> path(@Query("ox") int ox, @Query("oy") int oy, @Query("item") String itemId, @Query("user") String userId);

    @GET("initNavigate")
    Call<InitNavigateResponseVO> initNavigate(@Query("ox") int ox, @Query("oy") int oy, @Query("user") String userId);
}
