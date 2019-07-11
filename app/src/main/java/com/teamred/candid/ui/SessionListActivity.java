package com.teamred.candid.ui;

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

import com.teamred.candid.R;
import com.teamred.candid.data.SessionManager;
import com.teamred.candid.data.SessionManager.Session;
import com.teamred.candid.vision.CloudVision;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SessionListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sessions);
        getSupportActionBar().setTitle("Sessions");

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        SessionManager sessionManager = new SessionManager(getFilesDir());
        List<Session> sessions = sessionManager.getSessions();

        SessionsAdapter adapter = new SessionsAdapter(sessions);
        recyclerView.setAdapter(adapter);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView dateTextView;
        private final TextView countTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.imageView = itemView.findViewById(R.id.image_view);
            this.dateTextView = itemView.findViewById(R.id.date);
            this.countTextView = itemView.findViewById(R.id.count);
        }
    }

    class SessionsAdapter extends RecyclerView.Adapter<ViewHolder> {

        private final List<Session> sessions;

        SessionsAdapter(List<Session> sessions) {
            this.sessions = sessions;
        }

        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.session_list_item, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Session session = sessions.get(position);
            File preview = session.getPreviewPicture();
            if (preview != null) {
                Bitmap bitmap = BitmapFactory.decodeFile(preview.getPath());
                holder.imageView.setImageBitmap(bitmap);
            }
            holder.dateTextView.setText(session.getDateString());
            holder.countTextView.setText(String.format("%s Photos", session.getPictureCount()));
            holder.itemView.setOnClickListener(v -> {
                Intent intent = SessionActivity.newIntent(SessionListActivity.this, session.getDirectory());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return sessions.size();
        }
    }
}

