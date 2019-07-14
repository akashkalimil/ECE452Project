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
import com.teamred.candid.data.EmotionClassifier.Emotion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private List<Object> unrolledGroups;
    private final Set<String> selected;
    private final Listener listener;
    private boolean inSelectionMode = false;

    SessionAdapter(Map<Emotion, List<String>> groups, Listener listener) {
        this.listener = listener;
        this.selected = new HashSet<>();
        unrolledGroups = new ArrayList<>();
        for (Map.Entry<Emotion, List<String>> entry : groups.entrySet()) {
            unrolledGroups.add(entry.getKey());
            unrolledGroups.addAll(entry.getValue());
        }
    }

    @Override
    public int getItemViewType(int position) {
        return unrolledGroups.get(position) instanceof Emotion ? HEADER : IMAGE;
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
            String file = (String) unrolledGroups.get(position);
            Bitmap bitmap = BitmapFactory.decodeFile(file);
            if (!inSelectionMode) {
                imageHolder.check.setVisibility(View.GONE);
                setMargin(imageHolder.imageView, 0);
                imageHolder.imageView.setOnClickListener(null);
            } else {
                if (selected.contains(file)) { // selected
                    imageHolder.check.setVisibility(View.VISIBLE);
                    setMargin(imageHolder.imageView, 28);
                    imageHolder.imageView.setOnClickListener(v -> {
                        selected.remove(file);
                        if (selected.isEmpty()) exitSelectionMode();
                        else notifyItemChanged(position);
                    });
                } else { // not selected
                    imageHolder.check.setVisibility(View.GONE);
                    setMargin(imageHolder.imageView, 0);
                    imageHolder.imageView.setOnClickListener(v -> {
                        selected.add(file);
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
                selected.add(file);

                return true;
            });
        } else {
            HeaderHolder header = (HeaderHolder) holder;
            header.textView.setText(unrolledGroups.get(position).toString());
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
        return unrolledGroups.size();
    }

    List<String> getSelectedFiles() {
        return new ArrayList<>(selected);
    }

    void exitSelectionMode() {
        inSelectionMode = false;
        selected.clear();
        notifyDataSetChanged();
        listener.onSelectionModeExited();
    }
}