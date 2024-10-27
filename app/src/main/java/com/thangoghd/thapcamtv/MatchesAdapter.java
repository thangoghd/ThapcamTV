package com.thangoghd.thapcamtv;

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MatchesAdapter extends RecyclerView.Adapter<MatchesAdapter.MatchViewHolder> {
    private List<Match> matches;

    public MatchesAdapter(List<Match> matches) {
        this.matches = matches;
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match, parent, false);
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        holder.bind(matches.get(position));
    }

    @Override
    public int getItemCount() {
        return matches.size();
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
            if (match.getTournament() != null) {
                tournamentNameView.setText(match.getTournament().getName());
            } else {
                tournamentNameView.setText(view.getContext().getString(R.string.unknown_tournament));
            }

            // Set home team info
            TextView homeNameView = view.findViewById(R.id.homeName);
            ImageView homeLogoView = view.findViewById(R.id.homeLogo);
            if (match.getHome() != null) {
                homeNameView.setText(truncateText(match.getHome().getName()));
                if (match.getHome().getLogo() != null) {
                    Glide.with(view.getContext())
                            .load(match.getHome().getLogo())
                            .into(homeLogoView);
                } else {
                    homeLogoView.setImageResource(R.drawable.default_team_logo);
                }
            } else {
                homeNameView.setText(view.getContext().getString(R.string.unknown_team));
                homeLogoView.setImageResource(R.drawable.default_team_logo);
            }

            // Set away team info
            TextView awayNameView = view.findViewById(R.id.awayName);
            ImageView awayLogoView = view.findViewById(R.id.awayLogo);
            if (match.getAway() != null) {
                awayNameView.setText(truncateText(match.getAway().getName()));
                if (match.getAway().getLogo() != null) {
                    Glide.with(view.getContext())
                            .load(match.getAway().getLogo())
                            .into(awayLogoView);
                } else {
                    awayLogoView.setImageResource(R.drawable.default_team_logo);
                }
            } else {
                awayNameView.setText(truncateText(match.getHome().getName()));
                if (match.getHome().getLogo() != null) {
                    Glide.with(view.getContext())
                            .load(match.getHome().getLogo())
                            .into(awayLogoView);
                } else {
                    awayLogoView.setImageResource(R.drawable.default_team_logo);
                }
            }

            // Set match score
            TextView scoreView = view.findViewById(R.id.matchScore);
            if (match.getScores() != null) {
                String score = view.getContext().getString(R.string.match_score, match.getScores().getHome(), match.getScores().getAway());
                scoreView.setText(score);
            } else {
                scoreView.setText(view.getContext().getString(R.string.versus));
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

//                if (commentator.getAvatar() != null && !commentator.getAvatar().isEmpty()) {
//                    Glide.with(view.getContext())
//                            .load(commentator.getAvatar())
//                            .circleCrop()
//                            .into(avatarView);
//                } else {
//                    avatarView.setImageResource(R.drawable.default_avatar);
//                }

                    nameView.setText(commentator.getName());
                    avatarView.setImageResource((R.drawable.baseline_mic_15));
                    avatarView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    commentatorLayout.addView(commentatorView);
                }
            } else {
                commentatorLayout.setVisibility(View.VISIBLE);
            }

            view.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.setBackgroundResource(R.drawable.match_background_focused);
                    ((MainActivity) v.getContext()).focusedPosition = getAdapterPosition();
                } else {
                    v.setBackgroundResource(R.drawable.match_background_normal);
                    if (((MainActivity) v.getContext()).focusedPosition == getAdapterPosition()) {
                        ((MainActivity) v.getContext()).focusedPosition = RecyclerView.NO_POSITION;
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