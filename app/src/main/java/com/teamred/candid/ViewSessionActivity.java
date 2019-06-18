package com.teamred.candid;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

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
