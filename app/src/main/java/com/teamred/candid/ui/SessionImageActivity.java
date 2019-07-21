package com.teamred.candid.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.teamred.candid.R;

import java.io.File;

public class SessionImageActivity extends AppCompatActivity {

    static Intent newIntent(Context context, File image, String sessionDate) {
        return new Intent(context, SessionImageActivity.class)
                .putExtra(IMAGE_FILE, image)
                .putExtra(SESSION_DATE, sessionDate);
    }

    private static final String IMAGE_FILE = "imageFile";
    private static final String SESSION_DATE = "sessionDate";
    private static final String FILE_AUTHORITY = "com.teamred.candid.fileProvider";

    private File image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_image);

        Intent intent = getIntent();
        image = (File) intent.getSerializableExtra(IMAGE_FILE);

        ImageView imageView = findViewById(R.id.image_view);
        imageView.setImageBitmap(BitmapFactory.decodeFile(image.getPath()));

        TextView nameView = findViewById(R.id.file_name);
        nameView.setText(image.getName());

        TextView dateView = findViewById(R.id.date);
        String date = SessionActivity.getDisplayDate(intent.getStringExtra(SESSION_DATE));
        dateView.setText(date);
    }

    public void onShareClick(View v) {
        Uri uri = FileProvider.getUriForFile(this, FILE_AUTHORITY, image);
        Intent intent = new Intent(Intent.ACTION_SEND, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .setType("image/png");
        startActivity(Intent.createChooser(intent, "Shared from Candid!"));
    }
}
