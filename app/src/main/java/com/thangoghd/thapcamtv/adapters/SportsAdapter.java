package com.thangoghd.thapcamtv.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.thangoghd.thapcamtv.R;
import com.thangoghd.thapcamtv.SportType;

public class SportsAdapter extends RecyclerView.Adapter<SportsAdapter.ViewHolder> {
    private SportType[] sportTypes;
    private final OnSportClickListener listener;
    private long lastClickTime = 0;
    private static final long CLICK_DELAY = 500; // 500ms delay between clicks

    public interface OnSportClickListener {
        void onSportClick(int index);
    }

    public SportsAdapter(SportType[] sportTypes, OnSportClickListener listener) {
        this.sportTypes = sportTypes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sport_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SportType sportType = sportTypes[position];
        holder.sportName.setText(sportType.getVietnameseName());
        holder.sportIcon.setImageResource(sportType.getIconResourceId());
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            Log.d("SportAdapter", "Focus changed on position: " + holder.getAdapterPosition() + " hasFocus: " + hasFocus);
        });

        holder.itemView.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime >= CLICK_DELAY) {
                lastClickTime = currentTime;
                listener.onSportClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sportTypes.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView sportName;
        ImageView sportIcon;

        ViewHolder(View itemView) {
            super(itemView);
            sportName = itemView.findViewById(R.id.sportName);
            sportIcon = itemView.findViewById(R.id.sportIcon);
        }
    }
    
    public void updateSports(SportType[] newSportTypes) {
        if (this.sportTypes == newSportTypes) return;
        this.sportTypes = newSportTypes;
        notifyDataSetChanged();
    }
}