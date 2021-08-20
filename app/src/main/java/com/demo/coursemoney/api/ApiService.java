package com.demo.coursemoney.api;

import com.demo.coursemoney.pojo.JsonResponse;

import io.reactivex.Observable;
import retrofit2.http.GET;

public interface ApiService {

    @GET("daily_json.js")
    Observable<JsonResponse> getJsonResponse();

}
