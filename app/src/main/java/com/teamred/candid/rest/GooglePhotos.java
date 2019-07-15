package com.teamred.candid.rest;

import android.content.Context;

import com.teamred.candid.data.GoogleAuthManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.Single;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public class GooglePhotos extends RestApiClient<GooglePhotos.Service> {

    interface Service {

        @POST("uploads")
        @Headers({"Content-type: application/octet-stream", "X-Goog-Upload-Protocol: raw"})
        Single<String> upload(
                @Header("Authorization") String token,
                @Header("X-Goog-Upload-File-Name") String name,
                @Body RequestBody imageBinary
        );

        @POST("./mediaItems:batchCreate")
        @Headers("Content-Type: application/json")
        Single<MediaItemsResponses> batchCreate(
                @Header("Authorization") String token,
                @Body MediaItems mediaItems
        );
    }

    public GooglePhotos() {
        super(Service.class);
    }

    @Override
    String baseUrl() {
        return "https://photoslibrary.googleapis.com/v1/";
    }

    class MediaItems {
        List<MediaItem> newMediaItems;

        MediaItems(List<MediaItem> items) {
            newMediaItems = items;
        }
    }

    /**
     * Returns a Single containing the raw upload token string to be passed
     * into the create endpoint call.
     */
    public Single<String> upload(File file, Context context) {
        Throwable err = assertAuthorized(context);
        if (err != null) return Single.error(err);

        try {
            byte[] bytes = Files.readAllBytes(Paths.get(file.toURI()));
            RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"), bytes);
            return service.upload(getAuthHeaderValue(context), file.getName(), body);
        } catch (IOException e) {
            return Single.error(e);
        }
    }

    public Single<MediaItemsResponses> batchCreate(List<String> uploadTokens, Context context) {
        Throwable err = assertAuthorized(context);
        if (err != null) return Single.error(err);

        List<MediaItem> items = uploadTokens.stream()
                .map(MediaItem::new)
                .collect(Collectors.toList());

        return service.batchCreate(getAuthHeaderValue(context), new MediaItems(items));
    }

    private String getAuthHeaderValue(Context context) {
        return "Bearer " + GoogleAuthManager.getInstance(context).getToken();
    }

    private Exception assertAuthorized(Context context) {
        return GoogleAuthManager.getInstance(context).isAuthenticated()
                ? null
                : new RuntimeException("Not authenticated!");
    }

    /**
     * Request/Response models
     **/

    class MediaItem {
        String description;
        SimpleMediaItem simpleMediaItem;

        class SimpleMediaItem {
            String uploadToken;
        }

        MediaItem(String token) {
            this.description = "";
            this.simpleMediaItem = new SimpleMediaItem();
            simpleMediaItem.uploadToken = token;
        }
    }

    public class MediaItemsResponses {
        public List<MediaItemResponse> newMediaItemResults;
    }

    class MediaItemResponse {
        class Status {
            String message;
        }

        Status status;
    }
}
