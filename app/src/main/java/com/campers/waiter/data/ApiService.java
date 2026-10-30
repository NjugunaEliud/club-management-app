package com.campers.waiter.data;

import com.campers.waiter.data.Models.CreateSale;
import com.campers.waiter.data.Models.LoginRequest;
import com.campers.waiter.data.Models.LoginResponse;
import com.campers.waiter.data.Models.MyStats;
import com.campers.waiter.data.Models.Ping;
import com.campers.waiter.data.Models.Product;
import com.campers.waiter.data.Models.RestockRequest;
import com.campers.waiter.data.Models.Sale;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @GET("api/v1/ping") Call<Ping> ping();
    @POST("api/v1/auth/login") Call<LoginResponse> login(@Body LoginRequest body);
    @GET("api/v1/products") Call<List<Product>> products();
    @POST("api/v1/sales") Call<Sale> sell(@Body CreateSale body);
    @GET("api/v1/sales") Call<List<Sale>> mySales(@Query("from") String from, @Query("to") String to);
    @POST("api/v1/stock/restock") Call<Product> restock(@Body RestockRequest body);
    @GET("api/v1/stats/me") Call<MyStats> stats(@Query("from") String from, @Query("to") String to);
}
