package com.kgpoem.player;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public final class PoemAdapter extends RecyclerView.Adapter<PoemAdapter.Holder> {
    public interface Listener {
        void play(@NonNull Poem poem);

        void openKaraoke(@NonNull Poem poem);

        void toggleFavorite(@NonNull Poem poem);
    }

    private final List<Poem> poems;
    private final Listener listener;
    private int nowPlayingNumber = -1;

    public PoemAdapter(@NonNull List<Poem> poems, @NonNull Listener listener) {
        this.poems = poems;
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return poems.get(position).number;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_poem, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Poem poem = poems.get(position);
        boolean isCurrent = poem.number == nowPlayingNumber;
        android.content.Context ctx = holder.itemView.getContext();

        holder.itemView.setBackgroundResource(isCurrent
                ? R.drawable.bg_poem_card_active
                : R.drawable.bg_poem_card);

        // Rotating colorful number badge
        int badgeRes;
        int textColor;
        switch (poem.number % 4) {
            case 0:
                badgeRes = R.drawable.bg_number_purple;
                textColor = 0xFF6D28D9;
                break;
            case 1:
                badgeRes = R.drawable.bg_number_yellow;
                textColor = 0xFFB45309;
                break;
            case 2:
                badgeRes = R.drawable.bg_number_mint;
                textColor = 0xFF0D9488;
                break;
            default:
                badgeRes = R.drawable.bg_number_pink;
                textColor = 0xFFE11D48;
                break;
        }
        holder.number.setBackgroundResource(badgeRes);
        holder.number.setTextColor(textColor);
        holder.number.setText(String.valueOf(poem.number));

        holder.title.setText(poem.title);
        holder.subtitle.setText(isCurrent
                ? R.string.now_playing_next_automatic
                : R.string.tap_to_play);
        holder.subtitle.setTextColor(ctx.getColor(
                isCurrent ? R.color.purple : R.color.ink_secondary
        ));

        holder.favorite.setImageResource(poem.favorite
                ? R.drawable.ic_star_filled
                : R.drawable.ic_star_outline);
        holder.favorite.setContentDescription(ctx.getString(
                poem.favorite ? R.string.remove_favorite : R.string.add_favorite
        ));

        holder.play.setImageResource(isCurrent
                ? R.drawable.ic_pause
                : R.drawable.ic_play_arrow);

        holder.itemView.setOnClickListener(view -> listener.play(poem));
        holder.play.setOnClickListener(view -> listener.play(poem));
        holder.karaoke.setOnClickListener(view -> listener.openKaraoke(poem));
        holder.favorite.setOnClickListener(view -> listener.toggleFavorite(poem));
    }

    @Override
    public int getItemCount() {
        return poems.size();
    }

    public void setNowPlayingNumber(int number) {
        if (nowPlayingNumber == number) {
            return;
        }
        int oldNumber = nowPlayingNumber;
        nowPlayingNumber = number;
        notifyPoemChanged(oldNumber);
        notifyPoemChanged(number);
    }

    private void notifyPoemChanged(int poemNumber) {
        if (poemNumber < 1) {
            return;
        }
        for (int index = 0; index < poems.size(); index++) {
            if (poems.get(index).number == poemNumber) {
                notifyItemChanged(index);
                return;
            }
        }
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView number;
        final TextView title;
        final TextView subtitle;
        final ImageButton favorite;
        final ImageButton karaoke;
        final ImageButton play;

        Holder(@NonNull View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.number);
            title = itemView.findViewById(R.id.title);
            subtitle = itemView.findViewById(R.id.subtitle);
            favorite = itemView.findViewById(R.id.fav);
            karaoke = itemView.findViewById(R.id.karaoke);
            play = itemView.findViewById(R.id.play);
        }
    }
}
