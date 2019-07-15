package com.teamred.candid.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.teamred.candid.R;
import com.teamred.candid.model.Emotion;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class SessionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final int IMAGE = 1;
    static final int HEADER = 2;

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView, check;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.imageView = itemView.findViewById(R.id.image_view);
            this.check = itemView.findViewById(R.id.check);
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView textView;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            this.textView = (TextView) itemView;
        }
    }

    interface Listener {
        void onSelectionModeEntered();

        void onSelectionModeExited();
    }

    private final List<Object> data;
    private final Listener listener;
    private boolean inSelectionMode = false;
    private final Set<Integer> selectedIndices;

    SessionAdapter(Map<Emotion, List<String>> groups, Listener listener) {
        this.listener = listener;
        this.selectedIndices = new HashSet<>();
        this.data = unrollClassifications(groups);
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position) instanceof Emotion ? HEADER : IMAGE;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (type == IMAGE) {
            return new ViewHolder(inf.inflate(R.layout.image_list_item, parent, false));
        } else {
            return new HeaderHolder(inf.inflate(R.layout.header_list_item, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == IMAGE) {
            ViewHolder imageHolder = (ViewHolder) holder;
            String file = (String) data.get(position);
            Bitmap bitmap = BitmapFactory.decodeFile(file);
            if (!inSelectionMode) {
                imageHolder.check.setVisibility(View.GONE);
                setMargin(imageHolder.imageView, 0);
                imageHolder.imageView.setOnClickListener(null);
            } else {
                if (selectedIndices.contains(position)) { // selected
                    imageHolder.check.setVisibility(View.VISIBLE);
                    setMargin(imageHolder.imageView, 28);
                    imageHolder.imageView.setOnClickListener(v -> {
                        selectedIndices.remove(position);
                        if (selectedIndices.isEmpty()) exitSelectionMode();
                        else notifyItemChanged(position);
                    });
                } else { // not selected
                    imageHolder.check.setVisibility(View.GONE);
                    setMargin(imageHolder.imageView, 0);
                    imageHolder.imageView.setOnClickListener(v -> {
                        selectedIndices.add(position);
                        notifyItemChanged(position);
                    });
                }
            }

            imageHolder.imageView.setImageBitmap(bitmap);
            imageHolder.imageView.setOnLongClickListener(v -> {
                if (inSelectionMode) return false;

                inSelectionMode = true;
                listener.onSelectionModeEntered();
                notifyDataSetChanged();
                selectedIndices.add(position);

                return true;
            });
        } else {
            HeaderHolder header = (HeaderHolder) holder;
            header.textView.setText(data.get(position).toString());
        }
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
        return data.size();
    }

    Set<Integer> getSelectedIndices() {
        return selectedIndices;
    }

    Set<File> getSelectedFiles() {
        return selectedIndices.stream()
                .map(i -> new File((String) data.get(i)))
                .collect(Collectors.toSet());
    }

    void removeAtIndices(Set<Integer> indices) {
        for (int i : indices) {
            data.remove(i);
        }
        Set<Integer> emptySections = new HashSet<>();
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i) instanceof Emotion &&
                    (i == data.size() - 1 || data.get(i+1) instanceof Emotion)) {
                emptySections.add(i);
            }
        }
        for (int i : emptySections) {
            data.remove(i);
        }
        notifyDataSetChanged();
    }

    Map<Emotion, List<String>> getData() {
        Map<Emotion, List<String>> map = new HashMap<>();
        Emotion key = null;
        for (Object o : data) {
            if (o instanceof Emotion) {
                key = (Emotion) o;
                map.put(key, new ArrayList<>());
            } else {
                map.get(key).add((String) o);
            }
        }
        return map;
    }

    void exitSelectionMode() {
        inSelectionMode = false;
        selectedIndices.clear();
        notifyDataSetChanged();
        listener.onSelectionModeExited();
    }

    private List<Object> unrollClassifications(Map<Emotion, List<String>> classifications) {
        List<Object> list = new ArrayList<>();
        for (Map.Entry<Emotion, List<String>> entry : classifications.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            list.add(entry.getKey());
            list.addAll(entry.getValue());
        }
        return list;
    }
}