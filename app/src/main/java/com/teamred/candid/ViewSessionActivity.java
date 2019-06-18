package com.teamred.candid;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;

public class ViewSessionActivity extends AppCompatActivity {

    private static final String TAG = "ViewSession";
    private static final String SESSION_DIR = "sessionDir";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_session);

        File sessionDirectory = (File) getIntent().getSerializableExtra(SESSION_DIR);
        File[] files = sessionDirectory.listFiles();
        Log.d(TAG, "Found images for the session: " + files.length);
        ImageView imageView = findViewById(R.id.image_view);
        Bitmap bitmap = BitmapFactory.decodeFile(files[0].getPath());
        imageView.setImageBitmap(bitmap);
    }

    static Intent newIntent(Context context, File sessionDirectory) {
        return new Intent(context, ViewSessionActivity.class)
                .putExtra(SESSION_DIR, sessionDirectory);
    }
}
