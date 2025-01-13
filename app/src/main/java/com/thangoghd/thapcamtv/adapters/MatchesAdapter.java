package com.thangoghd.thapcamtv.adapters;

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
import com.thangoghd.thapcamtv.SportType;
import com.thangoghd.thapcamtv.models.Commentator;
import com.thangoghd.thapcamtv.fragments.LiveFragment;
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
    private final LiveFragment fragment;

    public interface OnMatchClickListener {
        void onMatchClick(String matchId);
    }

    public MatchesAdapter(List<Match> matches, LiveFragment fragment, OnMatchClickListener listener) {
        this.matches = (matches != null) ? matches : new ArrayList<>();
        this.fragment = fragment;
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
        holder.bind(match, fragment);

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
    public static class MatchViewHolder extends RecyclerView.ViewHolder {
        View view;

        MatchViewHolder(View itemView) {
            super(itemView);
            view = itemView;
        }

        public void bind(Match match, LiveFragment fragment) {
            TextView tournamentNameView = view.findViewById(R.id.tournamentName);
            // Add sport emoji before tournament name for live sport type
            if (match.getTournament() != null) {
                SportType sportType = SportType.fromKey(match.getSportType());
                String tournamentText = sportType.getEmoji() + " " + match.getTournament().getName();
                tournamentNameView.setText(tournamentText);
            } else {
                tournamentNameView.setText(view.getContext().getString(R.string.unknown_tournament));
            }

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
                liveLayout.setVisibility(View.INVISIBLE);
            }

            // Set match status
            if ("live".equalsIgnoreCase(match.getMatchStatus())) {
                if(match.getTimeInMatch() != null)
                {
                    statusView.setText(match.getTimeInMatch());
                    statusView.setTextColor(ContextCompat.getColor(view.getContext(), android.R.color.holo_green_light));
                }
                else {
                    statusView.setText(view.getContext().getString(R.string.match_live));
                    statusView.setTextColor(ContextCompat.getColor(view.getContext(), android.R.color.holo_green_light));
                }

            } else if ("finished".equalsIgnoreCase(match.getMatchStatus())) {
                statusView.setText(view.getContext().getString(R.string.match_finished));
                statusView.setTextColor(ContextCompat.getColor(view.getContext(), android.R.color.holo_red_light));

            } else if ("canceled".equalsIgnoreCase(match.getMatchStatus())) {
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
            TextView commentatorName = view.findViewById(R.id.commentatorName);
            List<Commentator> commentators = match.getCommentators();
            
            if (commentators != null && !commentators.isEmpty()) {
                commentatorLayout.setVisibility(View.VISIBLE);
                
                StringBuilder names = new StringBuilder("🎙 "); // Add microphone icon at the start
                
                if (commentators.size() > 1) {
                    // Join commentator names with " - "
                    for (int i = 0; i < commentators.size(); i++) {
                        names.append(commentators.get(i).getName());
                        if (i < commentators.size() - 1) {
                            names.append(" - ");
                        }
                    }
                    commentatorName.setText(names.toString());
                } else {
                    // Single commentator
                    names.append(commentators.get(0).getName());
                    commentatorName.setText(names.toString());
                }
            } else {
                commentatorLayout.setVisibility(View.INVISIBLE);
            }
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