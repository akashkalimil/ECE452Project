package com.teamred.candid.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.teamred.candid.R;
import com.teamred.candid.data.SessionManager;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SessionActivity extends AppCompatActivity {


    private static final int NUM_COLUMNS = 3;

    static Intent newIntent(Context context, File sessionDirectory) {
        return new Intent(context, SessionActivity.class)
                .putExtra(SESSION_DIR, sessionDirectory);
    }

    private static final String TAG = "ViewSession";
    private static final String SESSION_DIR = "sessionDir";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.US);

    private CandidSessionAdapter adapter;
    private File sessionDirectory;
    private View menuContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_session);

        sessionDirectory = (File) getIntent().getSerializableExtra(SESSION_DIR);

        String title = getDisplayDate(sessionDirectory);
        getSupportActionBar().setTitle(title);

        File[] files = sessionDirectory.listFiles();
        Log.d(TAG, "Found images for the session: " + files.length);

        if (files.length > 0) {
            setupRecyclerView(files);
        } else {
            // show default empty session view
        }

        findViewById(R.id.save).setOnClickListener(this::onSelectionMenuClick);
        findViewById(R.id.upload).setOnClickListener(this::onSelectionMenuClick);
        findViewById(R.id.delete).setOnClickListener(this::onSelectionMenuClick);

        menuContainer = findViewById(R.id.selection_menu_contaienr);
    }

    private void onSelectionMenuClick(View menuItem) {
        List<File> selected = adapter.getSelectedFiles();
        switch (menuItem.getId()) {
            case R.id.save:
                // TODO
                break;
            case R.id.upload:
                // TODO
                break;
            case R.id.delete:
                for (File file : selected) file.delete();
                adapter.setFiles(sessionDirectory.listFiles());
                break;
        }
        adapter.exitSelectionMode();
    }

    private void setupRecyclerView(File[] files) {
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(this, NUM_COLUMNS);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new CandidSessionAdapter(files);
        recyclerView.setAdapter(adapter);

    }

    private void notifySelectionModeEntered() {
        menuContainer.animate()
                .translationY(0)
                .setInterpolator(new AccelerateInterpolator());
    }

    public void notifySelectionModeExited() {
        menuContainer.animate()
                .translationY(menuContainer.getHeight())
                .setInterpolator(new DecelerateInterpolator());
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView, check;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.imageView = itemView.findViewById(R.id.image_view);
            this.check = itemView.findViewById(R.id.check);
        }
    }

    class CandidSessionAdapter extends RecyclerView.Adapter<ViewHolder> {

        private File[] files;
        private final Set<File> selected;
        private boolean inSelectionMode = false;

        CandidSessionAdapter(File[] files) {
            this.files = files;
            this.selected = new HashSet<>();
        }

        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.image_list_item, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            File file = files[position];
            Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
            if (!inSelectionMode) {
                holder.check.setVisibility(View.GONE);
                setMargin(holder.imageView, 0);
                holder.imageView.setOnClickListener(null);
            } else {
                if (selected.contains(file)) { // selected
                    holder.check.setVisibility(View.VISIBLE);
                    setMargin(holder.imageView, 28);
                    holder.imageView.setOnClickListener(v -> {
                        selected.remove(file);
                        if (selected.isEmpty()) exitSelectionMode();
                        else notifyItemChanged(position);
                    });
                } else { // not selected
                    holder.check.setVisibility(View.GONE);
                    setMargin(holder.imageView, 0);
                    holder.imageView.setOnClickListener(v -> {
                        selected.add(file);
                        notifyItemChanged(position);
                    });
                }
            }

            holder.imageView.setImageBitmap(bitmap);
            holder.imageView.setOnLongClickListener(v -> {
                if (inSelectionMode) return false;

                inSelectionMode = true;
                notifySelectionModeEntered();
                notifyDataSetChanged();
                selected.add(file);

                return true;
            });
        }

        private void setMargin(View view, int margin) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) view.getLayoutParams();
            lp.bottomMargin = margin;
            lp.topMargin = margin;
            lp.rightMargin = margin;
            lp.leftMargin = margin;
        }

        @Override
        public int getItemCount() {
            return files.length;
        }

        List<File> getSelectedFiles() {
            return new ArrayList<>(selected);
        }

        void exitSelectionMode() {
            inSelectionMode = false;
            selected.clear();
            notifyDataSetChanged();
            notifySelectionModeExited();
        }

        void setFiles(File[] files) {
            this.files = files;
            notifyDataSetChanged();
        }
    }

    private static String getDisplayDate(File directory) {
        try {
            return DATE_FORMAT.format(SessionManager.DATE_FORMAT.parse(directory.getName()));
        } catch (ParseException e) {
            return null;
        }
    }
}
