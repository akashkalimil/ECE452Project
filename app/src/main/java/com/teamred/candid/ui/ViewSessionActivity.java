package com.teamred.candid.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.teamred.candid.R;

import java.io.File;
import java.util.Collections;

public class ViewSessionActivity extends AppCompatActivity {


    static Intent newIntent(Context context, File sessionDirectory) {
        return new Intent(context, ViewSessionActivity.class)
                .putExtra(SESSION_DIR, sessionDirectory);
    }

    private static final String TAG = "ViewSession";
    private static final String SESSION_DIR = "sessionDir";

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    // Request codes
    private static final int REQUEST_CODE_SIGN_IN = 9420;
    private static final int REQUEST_CODE_AUTH_DRIVE = 9421;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_session);

        File sessionDirectory = (File) getIntent().getSerializableExtra(SESSION_DIR);
        File[] files = sessionDirectory.listFiles();
        Log.d(TAG, "Found images for the session: " + files.length);

        if (files.length > 0) {
            setupRecyclerView(files);
        } else {
            // show default empty session view
        }
//        try {
//            checkGoogleAuth();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        GoogleDriveService.getCredentials(this)
//                .subscribeOn(Schedulers.newThread())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe();

    }

    private void checkGoogleAuth() throws Exception {
        Scope DRIVE_FILE = new Scope("https://www.googleapis.com/auth/drive.file");
        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), DRIVE_FILE)) {
            GoogleSignIn.requestPermissions(
                    ViewSessionActivity.this,
                    REQUEST_CODE_AUTH_DRIVE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    DRIVE_FILE);
        } else {
            GoogleSignInAccount googleAccount = GoogleSignIn.getLastSignedInAccount(this);

            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    this,
                    Collections.singleton("https://www.googleapis.com/auth/drive.file")
            );
            credential.setSelectedAccount(googleAccount.getAccount());
            Log.d(TAG, credential.getToken());

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_AUTH_DRIVE) {
                try {
                    checkGoogleAuth();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void setupRecyclerView(File[] files) {
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new CandidSessionAdapter(files);
        recyclerView.setAdapter(adapter);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;

        public ViewHolder(@NonNull ImageView itemView) {
            super(itemView);
            this.imageView = itemView;
        }
    }

    class CandidSessionAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final File[] files;

        CandidSessionAdapter(File[] files) {
            this.files = files;
        }

        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            ImageView v = (ImageView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.session_list_item_view, parent, false);
            return new ViewHolder(v);

        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Bitmap bitmap = BitmapFactory.decodeFile(files[position].getPath());
            holder.imageView.setImageBitmap(bitmap);
        }

        @Override
        public int getItemCount() {
            return files.length;
        }
    }
}
