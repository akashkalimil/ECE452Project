package com.teamred.candid.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import com.teamred.candid.R;
import com.teamred.candid.data.EmotionClassificationStore;
import com.teamred.candid.data.SessionManager;
import com.teamred.candid.data.SessionProcessor;
import com.teamred.candid.model.Emotion;
import com.teamred.candid.model.Session;
import com.teamred.candid.rest.GooglePhotos;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

@SuppressWarnings("deprecation")
public class SessionActivity extends AppCompatActivity implements SessionAdapter.Listener {

    private static final int NUM_COLUMNS = 3;

    static Intent newIntent(Context context, File sessionDirectory) {
        return new Intent(context, SessionActivity.class)
                .putExtra(SESSION_DIR, sessionDirectory);
    }


    private static final String TAG = "ViewSession";
    private static final String SESSION_DIR = "sessionDir";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MMMM dd, yyyy 'at' h:mm a", Locale.US);

    private File sessionDirectory;
    private SessionAdapter adapter;
    private View menuContainer;
    private Disposable dispose;
    private EmotionClassificationStore classificationStore;
    private GooglePhotos googlePhotos = new GooglePhotos();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_session);

        sessionDirectory = (File) getIntent().getSerializableExtra(SESSION_DIR);

        String title = getDisplayDate(sessionDirectory.getName());
        getSupportActionBar().setTitle(title);

        File[] files = sessionDirectory.listFiles();
        Log.d(TAG, "Found images for the session: " + files.length);

        if (files.length > 0) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setCancelable(false);
            dialog.setMessage("Analyzing photos...");
            Session session = new Session(sessionDirectory);
            SessionProcessor processor = new SessionProcessor(session);
            dispose = processor.groupByEmotion()
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(d -> dialog.show())
                    .doOnSuccess(g -> dialog.dismiss())
                    .doOnError(e -> dialog.dismiss())
                    .subscribe(this::setupRecyclerView, Throwable::printStackTrace);

            Handler handler = new Handler();
            handler.postDelayed(() -> dialog.setMessage("Analyzing audio..."), 3000);
            handler.postDelayed(() -> dialog.setMessage("Classifying emotions..."), 6000);
            handler.postDelayed(() -> dialog.setMessage("Grouping photos..."), 9000);
            classificationStore = new EmotionClassificationStore(session);
        } else {
            // show default empty session view

        }
        findViewById(R.id.save).setOnClickListener(this::onSelectionMenuClick);
        findViewById(R.id.upload).setOnClickListener(this::onSelectionMenuClick);
        findViewById(R.id.delete).setOnClickListener(this::onSelectionMenuClick);

        menuContainer = findViewById(R.id.selection_menu_contaienr);
    }

    private void onSelectionMenuClick(View menuItem) {
        Set<Integer> indices = adapter.getSelectedIndices();
        Set<File> files = adapter.getSelectedFiles();

        switch (menuItem.getId()) {
            case R.id.save:
                for (File file : files) {
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                    MediaStore.Images.Media.insertImage(
                            getContentResolver(), bitmap, file.getName(), "");
                }
                Toast.makeText(this, "Saved to gallery", Toast.LENGTH_SHORT).show();
                break;
            case R.id.upload:
                Observable.fromIterable(files)
                        .flatMapSingle(f -> googlePhotos.upload(f, this))
                        .toList()
                        .flatMap(tokens -> googlePhotos.batchCreate(tokens, this))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(res -> {
                            Toast.makeText(this, "Uploaded to Google Photos", Toast.LENGTH_SHORT).show();
                        }, err -> {
                            err.printStackTrace();
                            Toast.makeText(this, "Failed upload to Google Photos", Toast.LENGTH_SHORT).show();
                        });
                break;
            case R.id.delete:
                for (File file : files) file.delete();
                adapter.removeAtIndices(indices);
                classificationStore.write(adapter.getData())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe();
                Toast.makeText(this, "Finished deleting", Toast.LENGTH_SHORT).show();
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

    static String getDisplayDate(String date) {
        try {
            return DATE_FORMAT.format(SessionManager.DATE_FORMAT.parse(date));
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

    @Override
    public void onImageClicked(String imagePath) {
        startActivity(SessionImageActivity.newIntent(
                this,
                new File(imagePath),
                sessionDirectory.getName())
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (dispose != null && !dispose.isDisposed()) {
            dispose.dispose();
        }
    }
}
