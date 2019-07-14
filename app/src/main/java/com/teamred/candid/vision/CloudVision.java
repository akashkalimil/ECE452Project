package com.teamred.candid.vision;

import android.graphics.Bitmap;

import com.teamred.candid.BuildConfig;
import com.teamred.candid.vision.BatchRequest.ImageRequest;
import com.teamred.candid.vision.BatchResponse.Response;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
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
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        service = retrofit.create(Service.class);
    }

    // Cloud vision max payload is 10MB
    // Each photo is around 1.5-2MB, so sending 4 * 2 = 8MB should be safe
    private static final int IMAGES_PER_BATCH = 4;

    public Single<List<Response>> annotateImages(List<Bitmap> bitmaps) {
        return Observable.fromIterable(bitmaps)
                .map(ImageRequest::new)
                .buffer(IMAGES_PER_BATCH)
                .concatMapSingle(requests -> service.annotateImage(KEY, new BatchRequest(requests)))
                .toList()
                .map(batchResponses -> {
                    List<Response> merged = new ArrayList<>();
                    for (BatchResponse response : batchResponses) {
                        merged.addAll(response.responses);
                    }
                    return merged;
                });
    }

    public class FaceAnnotation {
        public String joyLikelihood;
        public String sorrowLikelihood;
        public String angerLikelihood;
        public String surpriseLikelihood;
    }
}
