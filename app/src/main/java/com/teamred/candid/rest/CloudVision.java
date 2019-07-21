package com.teamred.candid.rest;

import android.graphics.Bitmap;

import com.teamred.candid.BuildConfig;
import com.teamred.candid.rest.BatchRequest.ImageRequest;
import com.teamred.candid.rest.BatchResponse.Response;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class CloudVision extends RestApiClient<CloudVision.Service> {

    private static final String KEY = BuildConfig.CV_API_KEY;
    private static final int IMAGES_PER_BATCH = 3;

    interface Service {

        @POST("./images:annotate")
        @Headers("Content-Type: application/json")
        Single<BatchResponse> annotateImage(
                @Query("key") String key,
                @Body BatchRequest requests
        );
    }

    public CloudVision() {
        super(Service.class);
    }

    @Override
    String baseUrl() {
        return "https://vision.googleapis.com/v1/";
    }

    public Single<List<Response>> annotateImages(List<Bitmap> bitmaps) {
        return Observable
                .fromIterable(bitmaps)
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
