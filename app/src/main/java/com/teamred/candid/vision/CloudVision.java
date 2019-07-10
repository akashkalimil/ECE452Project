package com.teamred.candid.vision;

import android.graphics.Bitmap;

import com.teamred.candid.BuildConfig;
import com.teamred.candid.vision.BatchRequest.ImageRequest;
import com.teamred.candid.vision.BatchResponse.Response;

import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.Single;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class CloudVision {

    private static final String BASE_URL = "https://vision.googleapis.com/v1/";
    private static final String KEY = BuildConfig.CV_API_KEY;

    interface Service {

        @POST("./images:annotate")
        @Headers("Content-Type: application/json")
        Single<BatchResponse> annotateImage(
                @Query("key") String key,
                @Body BatchRequest requests
        );
    }

    private final Service service;

    public CloudVision() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        service = retrofit.create(Service.class);
    }

    public Single<List<Response>> annotateImages(List<Bitmap> bitmaps) {
        List<ImageRequest> requests = bitmaps.stream()
                .map(ImageRequest::new)
                .collect(Collectors.toList());

        return service.annotateImage(KEY, new BatchRequest(requests))
                .map(i -> i.responses);
    }


    public class FaceAnnotation {
        public String joyLikelihood;
        public String sorrowLikelihood;
        public String angerLikelihood;
        public String surpriseLikelihood;
    }
}
