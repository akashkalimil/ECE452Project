package com.teamred.candid.ui;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.teamred.candid.R;
import com.teamred.candid.data.EmotionExtractor.Emotion;
import com.teamred.candid.data.SessionManager;
import com.teamred.candid.data.SessionProcessor;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.teamred.candid.data.SessionManager.*;

public class SessionActivity extends AppCompatActivity implements SessionAdapter.Listener {

    private static final int NUM_COLUMNS = 3;
    private Disposable dispose;

    static Intent newIntent(Context context, File sessionDirectory) {
        return new Intent(context, SessionActivity.class)
                .putExtra(SESSION_DIR, sessionDirectory);
    }

    private static final String TAG = "ViewSession";
    private static final String SESSION_DIR = "sessionDir";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.US);

    private SessionAdapter adapter;
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
            Session session = new Session(sessionDirectory);
            SessionProcessor processor = new SessionProcessor(session);
            dispose = processor.groupByEmotion()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::setupRecyclerView, error -> {

                    });
        } else {
            // show default empty session view
        }

        findViewById(R.id.save).setOnClickListener(this::onSelectionMenuClick);
        findViewById(R.id.upload).setOnClickListener(this::onSelectionMenuClick);
        findViewById(R.id.delete).setOnClickListener(this::onSelectionMenuClick);

        menuContainer = findViewById(R.id.selection_menu_contaienr);
    }

    private void onSelectionMenuClick(View menuItem) {
        List<String> selected = adapter.getSelectedFiles();
        switch (menuItem.getId()) {
            case R.id.save:
                // TODO
                break;
            case R.id.upload:
                // TODO
                break;
            case R.id.delete:
//                for (String file : selected) file.delete();
//                adapter.setGroups(sessionDirectory.listFiles());
                break;
        }
        adapter.exitSelectionMode();
    }

    private void setupRecyclerView(Map<Emotion, List<String>> groups) {
        Log.d(TAG, groups.toString());
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        adapter = new SessionAdapter(groups, this);

        GridLayoutManager layoutManager = new GridLayoutManager(this, NUM_COLUMNS);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int i) {
                return adapter.getItemViewType(i) == SessionAdapter.HEADER ? NUM_COLUMNS : 1;
            }
        });
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

    }


    private static String getDisplayDate(File directory) {
        try {
            return DATE_FORMAT.format(SessionManager.DATE_FORMAT.parse(directory.getName()));
        } catch (ParseException e) {
            return null;
        }
    }

    @Override
    public void onSelectionModeEntered() {
        menuContainer.animate()
                .translationY(0)
                .setInterpolator(new AccelerateInterpolator());
    }

    @Override
    public void onSelectionModeExited() {
        menuContainer.animate()
                .translationY(menuContainer.getHeight())
                .setInterpolator(new DecelerateInterpolator());
    }
}
