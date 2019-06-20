package com.teamred.candid.data;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.Collections;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;


public class GoogleDriveService {

    private static GoogleDriveService instance;

    private static GoogleDriveService getInstance() {
        return instance;
    }

    public static void authenticate(GoogleSignInAccount account, Context context) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(account.getAccount());
        instance = new GoogleDriveService(credential);
    }

    private final Drive drive;

    private GoogleDriveService(GoogleAccountCredential credential) {
        this.drive = new Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("Candid Android Client")
                .build();
    }

    Single<File> uploadImage(java.io.File file) {
        FileContent media = new FileContent("image/png", file);
        File metadata = new File();
        metadata.setName(file.getName());
        metadata.setMimeType("image/png");

        return Single.create(emitter -> {
            try {
                emitter.onSuccess(drive.files().create(metadata, media).execute());
            } catch (IOException e) {
                emitter.onError(e);
            }
        });
    }
}
