package com.teamred.candid.data;

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.teamred.candid.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Optional;

import javax.annotation.Nullable;

import io.reactivex.Single;

public class GoogleAuthManager {

    private static GoogleAuthManager instance;

    public static GoogleAuthManager getInstance(Context context) {
        return instance == null ? instance = new GoogleAuthManager(context, UserManager.getInstance(context.getFilesDir())) : instance;
    }

    private Optional<String> token = Optional.empty();
    private final GoogleSignInClient googleClient;
    private final UserManager userManager;

    private GoogleAuthManager(Context context, UserManager userManager) {
        GoogleSignInOptions options = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestScopes(new Scope("https://www.googleapis.com/auth/photoslibrary"))
                .requestIdToken(BuildConfig.GP_CLIENT_ID)
                .requestServerAuthCode(BuildConfig.GP_CLIENT_ID, true)
                .build();

        this.googleClient = GoogleSignIn.getClient(context, options);
        this.userManager = userManager;
    }

    public boolean isAuthenticated() {
        return token.isPresent();
    }

    public String getToken() {
        return token.orElse(null);
    }

    public Single<String> authenticateSignInResult(Task<GoogleSignInAccount> signInResult) {
        try {
            GoogleSignInAccount account = signInResult.getResult(ApiException.class);
            return getTokenForAccount(account).doOnSuccess(t ->
                    userManager.setCurrentUser(account.getEmail()));
        } catch (ApiException e) {
            return Single.error(e);
        }
    }

    private Single<String> getTokenForAccount(@Nullable GoogleSignInAccount account) {
        if (account == null) {
            return Single.error(new RuntimeException("Account is null!"));
        }

        RequestBody body = new FormEncodingBuilder()
                .add("grant_type", "authorization_code")
                .add("client_id", BuildConfig.GP_CLIENT_ID)
                .add("client_secret", BuildConfig.GP_CLIENT_SECRET)
                .add("redirect_uri", "")
                .add("code", account.getServerAuthCode())
                .add("id_token", account.getIdToken())
                .build();

        Request request = new Request.Builder()
                .url("https://www.googleapis.com/oauth2/v4/token")
                .post(body)
                .build();

        return Single.<String>create(emitter ->
                new OkHttpClient().newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(final Request request, final IOException e) {
                        emitter.onError(e);
                    }

                    @Override
                    public void onResponse(Response response) throws IOException {
                        try {
                            JSONObject json = new JSONObject(response.body().string());
                            String token = json.getString("access_token");
                            emitter.onSuccess(token);
                        } catch (JSONException e) {
                            emitter.onError(e);
                        }
                    }
                })).doOnSuccess(token -> this.token = Optional.of(token));
    }

    public Intent createSignInIntent() {
        return googleClient.getSignInIntent();
    }
}
