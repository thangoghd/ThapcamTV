package com.thangoghd.thapcamtv.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.thangoghd.thapcamtv.R;
import com.thangoghd.thapcamtv.models.Commentator;
import com.thangoghd.thapcamtv.models.Match;

import java.util.ArrayList;
import java.util.List;

public class SimpleMatchAdapter extends RecyclerView.Adapter<SimpleMatchAdapter.ViewHolder> {
    private List<Match> matches;
    private final Context context;
    private final OnMatchSelectedListener listener;

    public interface OnMatchSelectedListener {
        void onMatchSelected(Match match, String from);
    }

    public SimpleMatchAdapter(Context context, OnMatchSelectedListener listener) {
        this.context = context;
        this.listener = listener;
        this.matches = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_simple_match, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Match match = matches.get(position);
        holder.bind(match);
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    public void updateMatches(List<Match> newMatches) {
        this.matches = newMatches;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView matchTitle;
        private final TextView matchInfo;
        private final ImageView homeLogo;
        private final ImageView awayLogo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            matchTitle = itemView.findViewById(R.id.match_title);
            matchInfo = itemView.findViewById(R.id.match_info);
            homeLogo = itemView.findViewById(R.id.homeLogo);
            awayLogo = itemView.findViewById(R.id.awayLogo);

            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                float scale = hasFocus ? 1.1f : 1.0f;
                v.animate()
                        .scaleX(scale)
                        .scaleY(scale)
                        .setDuration(150)
                        .start();
                
                // Start/stop marquee based on focus
                matchTitle.setSelected(hasFocus);
                matchInfo.setSelected(hasFocus);
            });

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    Match match = matches.get(position);
                    listener.onMatchSelected(match, match.getFrom());
                }
            });
        }

        void bind(Match match) {
            String title = match.getAway() == null ? match.getHome().getName() : match.getHome().getName() + " vs " + match.getAway().getName();
            matchTitle.setText(title);

            List<Commentator> commentators = match.getCommentators();
            StringBuilder commentatorsNames = new StringBuilder();
            if (commentators != null && !commentators.isEmpty()) {
                if (commentators.size() > 1) {
                    // Join commentator names with " - "
                    for (int i = 0; i < commentators.size(); i++) {
                        commentatorsNames.append(commentators.get(i).getName());
                        if (i < commentators.size() - 1) {
                            commentatorsNames.append(" - ");
                        }
                    }
                } else {
                    // Single commentator
                    commentatorsNames.append(commentators.get(0).getName());
                }
            } else {
                commentatorsNames.append("Nhà Đài");
            }

            matchInfo.setText(match.getTournament().getName() +" • " + commentatorsNames);

            homeLogo.setVisibility(View.VISIBLE);
            Glide.with(context)
                .load(match.getHome().getLogo())
                .into(homeLogo);


            // Load away team logo if away team exists
            if (match.getAway() != null) {
                awayLogo.setVisibility(View.VISIBLE);
                Glide.with(context)
                    .load(match.getAway().getLogo())
                    .into(awayLogo);
            } else {
                awayLogo.setVisibility(View.GONE);
            }
        }
    }
}
