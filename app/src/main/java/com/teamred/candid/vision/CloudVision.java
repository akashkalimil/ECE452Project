package com.teamred.candid.vision;

import android.graphics.Bitmap;
import android.util.Base64;

import com.teamred.candid.BuildConfig;
import com.teamred.candid.vision.CloudVision.BatchAnnotationResponse.AnnotationResponse;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
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
        Single<BatchAnnotationResponse> annotateImage(
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

    public Single<List<AnnotationResponse>> annotateImages(List<Bitmap> bitmaps) {
        List<ImageRequest> requests = bitmaps.stream()
                .map(ImageRequest::new)
                .collect(Collectors.toList());

        return service.annotateImage(KEY, new BatchRequest(requests)).map(i -> i.responses);
    }

    class BatchRequest {
        List<ImageRequest> requests;

        BatchRequest(List<ImageRequest> requests) {
            this.requests = requests;
        }
    }

    class BatchAnnotationResponse {
        class AnnotationResponse {
            List<FaceAnnotation> faceAnnotations;
        }
        List<AnnotationResponse> responses;
    }

    public class FaceAnnotation {
        public String joyLikelihood;
        public String sorrowLikelihood;
        public String angerLikelihood;
        public String surpriseLikelihood;
    }

    static class ImageRequest {
        static class Image {
            String content;

            Image(String content) {
                this.content = content;
            }
        }

        static class Feature {
            final String type = "FACE_DETECTION";
        }

        final Image image;
        final List<Feature> features = Collections.singletonList(new Feature());

        ImageRequest(Bitmap bitmap) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            byte[] bytes = out.toByteArray();
            String encoded = Base64.encodeToString(bytes, Base64.DEFAULT);
            image = new Image(encoded);
        }
    }
}
