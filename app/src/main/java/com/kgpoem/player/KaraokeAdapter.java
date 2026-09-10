package com.kgpoem.player;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class KaraokeAdapter extends RecyclerView.Adapter<KaraokeAdapter.Holder> {
    private final List<KaraokeLine> lines = new ArrayList<>();
    private int activePosition = RecyclerView.NO_POSITION;

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lyric, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        KaraokeLine line = lines.get(position);
        boolean active = position == activePosition;
        holder.text.setText(line.text);
        holder.time.setText(formatTime(line.startMs));
        holder.text.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                active ? R.color.ink : R.color.white
        ));
        holder.time.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                active ? R.color.purple : R.color.lavender_text
        ));
        holder.itemView.setBackgroundResource(
                active ? R.drawable.bg_lyric_active : R.drawable.bg_lyric_idle
        );
    }

    @Override
    public int getItemCount() {
        return lines.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void submitLines(@NonNull List<KaraokeLine> newLines) {
        lines.clear();
        lines.addAll(newLines);
        activePosition = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    public boolean setActivePosition(int position) {
        if (position == activePosition) {
            return false;
        }
        int previous = activePosition;
        activePosition = position;
        if (previous != RecyclerView.NO_POSITION) {
            notifyItemChanged(previous);
        }
        if (activePosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(activePosition);
        }
        return true;
    }

    private static String formatTime(long milliseconds) {
        long totalSeconds = Math.max(0L, milliseconds / 1000L);
        return String.format(Locale.US, "%02d:%02d", totalSeconds / 60L, totalSeconds % 60L);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView text;
        final TextView time;

        Holder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.lyricText);
            time = itemView.findViewById(R.id.lyricTime);
        }
    }
}
