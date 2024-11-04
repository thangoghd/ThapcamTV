package com.thangoghd.thapcamtv.widgets;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.thangoghd.thapcamtv.models.Commentator;
import com.thangoghd.thapcamtv.LiveActivity;
import com.thangoghd.thapcamtv.models.Match;
import com.thangoghd.thapcamtv.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MatchesAdapter extends RecyclerView.Adapter<MatchesAdapter.MatchViewHolder> {
    private List<Match> matches;
    private final OnMatchClickListener matchClickListener;

    public interface OnMatchClickListener {
        void onMatchClick(String matchId);
    }

    public MatchesAdapter(List<Match> matches, OnMatchClickListener listener) {
        this.matches = (matches != null) ? matches : new ArrayList<>();
        this.matchClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        Match match = matches.get(position);
        return (match.getAway() == null) ? 1 : 0;
    }
    
    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 1) { 
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_event, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_match, parent, false);
        }
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        Match match = matches.get(position);
        holder.bind(match);

        // Handle onClick event for items in RecyclerView
        holder.itemView.setOnClickListener(v -> {
            matchClickListener.onMatchClick(match.getId());
        });
    }

    @Override
    public int getItemCount() {
        return (matches != null) ? matches.size() : 0;
    }

    public void updateMatches(List<Match> newMatches) {
        this.matches = newMatches;
        notifyDataSetChanged();
    }
    static class MatchViewHolder extends RecyclerView.ViewHolder {
        View view;

        MatchViewHolder(View itemView) {
            super(itemView);
            view = itemView;
        }

        void bind(Match match) {
            TextView tournamentNameView = view.findViewById(R.id.tournamentName);
            tournamentNameView.setText(match.getTournament() != null ? match.getTournament().getName() : view.getContext().getString(R.string.unknown_tournament));

            // Set home team info
            TextView homeNameView = view.findViewById(R.id.homeName);
            ImageView homeLogoView = view.findViewById(R.id.homeLogo);
            homeNameView.setText(match.getAway() != null ? truncateText(match.getHome().getName()) : match.getHome().getName());
            if (match.getHome().getLogo() != null) {
                Glide.with(view.getContext())
                        .load(match.getHome().getLogo())
                        .error(R.drawable.default_team_logo)
                        .into(homeLogoView);

            } else {
                homeNameView.setText(view.getContext().getString(R.string.unknown_team));
                homeLogoView.setImageResource(R.drawable.default_team_logo);
            }

            // Set away team info
            if (match.getAway() != null) {
                TextView awayNameView = view.findViewById(R.id.awayName);
                ImageView awayLogoView = view.findViewById(R.id.awayLogo);
                awayNameView.setText(truncateText(match.getAway().getName()));
                Glide.with(view.getContext())
                        .load(match.getAway().getLogo())
                        .error(R.drawable.default_team_logo)
                        .into(awayLogoView);

                TextView scoreView = view.findViewById(R.id.matchScore);
                scoreView.setText(view.getContext().getString(R.string.match_score, match.getScores().getHome(), match.getScores().getAway()));
                
            }

            // Set live status
            TextView statusView = view.findViewById(R.id.matchStatus);
            LinearLayout liveLayout = view.findViewById(R.id.liveLayout);

            if(match.getLive())
            {
                liveLayout.setVisibility(View.VISIBLE);
            }
            else{
                liveLayout.setVisibility(View.GONE);
            }

            // Set match status
            if ("live".equalsIgnoreCase(match.getMatch_status())) {
                if(match.getTimeInMatch() != null)
                {
                    statusView.setText(match.getTimeInMatch());
                    statusView.setTextColor(ContextCompat.getColor(view.getContext(), android.R.color.holo_green_light));
                }
                else {
                    statusView.setText(view.getContext().getString(R.string.match_live));
                    statusView.setTextColor(ContextCompat.getColor(view.getContext(), android.R.color.holo_green_light));
                }

            } else if ("finished".equalsIgnoreCase(match.getMatch_status())) {
                statusView.setText(view.getContext().getString(R.string.match_finished));
                statusView.setTextColor(ContextCompat.getColor(view.getContext(), android.R.color.holo_red_light));

            } else if ("canceled".equalsIgnoreCase(match.getMatch_status())) {
                statusView.setText(view.getContext().getString(R.string.match_canceled));
                statusView.setTextColor(ContextCompat.getColor(view.getContext(), android.R.color.holo_red_light));
            }
            else {
                statusView.setText(view.getContext().getString(R.string.match_upcoming));
                statusView.setTextColor(ContextCompat.getColor(view.getContext(), android.R.color.holo_orange_light));
            }

            // Set match date and time
            TextView dateTimeView = view.findViewById(R.id.matchDateTime);
            dateTimeView.setText(formatDateTime(match.getTimestamp()));

            // Set commentator info
            LinearLayout commentatorLayout = view.findViewById(R.id.commentatorLayout);
            List<Commentator> commentators = match.getCommentators();
            if (commentators != null && !commentators.isEmpty()) {
                commentatorLayout.setVisibility(View.VISIBLE);
                commentatorLayout.removeAllViews(); // Clear existing views

                for (Commentator commentator : commentators) {
                    View commentatorView = LayoutInflater.from(view.getContext()).inflate(R.layout.item_commentator, commentatorLayout, false);

                    ImageView avatarView = commentatorView.findViewById(R.id.commentatorIcon);
                    TextView nameView = commentatorView.findViewById(R.id.commentatorName);

                if (commentator.getAvatar() != null && !commentator.getAvatar().isEmpty()) {
                    Glide.with(view.getContext())
                            .load(commentator.getAvatar())
                            .circleCrop()
                            .into(avatarView);
                } else {
                    avatarView.setImageResource(R.drawable.default_avatar);
                }

                    nameView.setText(commentator.getName());
                    commentatorLayout.addView(commentatorView);
                }
            } else {
                commentatorLayout.setVisibility(View.INVISIBLE);
            }

            view.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundResource(R.drawable.match_background_focused);
                    ((LiveActivity) v.getContext()).focusedPosition = getAdapterPosition();
                } else {
                    v.setBackgroundResource(R.drawable.match_background_normal);
                    if (((LiveActivity) v.getContext()).focusedPosition == getAdapterPosition()) {
                        ((LiveActivity) v.getContext()).focusedPosition = RecyclerView.NO_POSITION;
                    }
                }
            });
        }
        private String truncateText(String text) {
            if (text == null) {
                return "";
            }
            if (text.length() > 20) {
                return text.substring(0, 20 - 3) + "...";
            }
            return text;
        }

        private String formatDateTime(long timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

}