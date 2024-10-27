package com.thangoghd.thapcamtv;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SportsAdapter extends RecyclerView.Adapter<SportsAdapter.ViewHolder> {
    private SportType[] sportTypes;
    private final OnSportClickListener listener;

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
            if (hasFocus) {
                v.setBackgroundResource(R.drawable.sport_category_focused);
            } else {
                v.setBackgroundResource(R.drawable.sport_category_normal);
            }
        });

        holder.itemView.setOnClickListener(v -> listener.onSportClick(position));
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
        this.sportTypes = newSportTypes;
        notifyDataSetChanged();
    }
}