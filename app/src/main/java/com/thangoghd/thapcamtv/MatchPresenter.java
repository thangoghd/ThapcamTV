package com.thangoghd.thapcamtv;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.thangoghd.thapcamtv.Commentator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MatchPresenter extends Presenter {
    private static final String TAG = "MatchPresenter";
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Log.d(TAG, "onCreateViewHolder");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Log.d(TAG, "onBindViewHolder: " + item);
        Match match = (Match) item;
        View view = viewHolder.view;
        
        // TODO: Set appropriate sport icon based on match.getSport_type()
    
        // Set tournament name
        TextView tournamentNameView = view.findViewById(R.id.tournamentName);
        if (match.getTournament() != null) {
            tournamentNameView.setText(match.getTournament().getName());
        } else {
            tournamentNameView.setText("Unknown Tournament");
        }
    
        // Set home team info
        TextView homeNameView = view.findViewById(R.id.homeName);
        ImageView homeLogoView = view.findViewById(R.id.homeLogo);
        if (match.getHome() != null) {
            homeNameView.setText(truncateText(match.getHome().getName(), 20));
            if (match.getHome().getLogo() != null) {
                Glide.with(view.getContext())
                        .load(match.getHome().getLogo())
                        .into(homeLogoView);
            } else {
                homeLogoView.setImageResource(R.drawable.default_team_logo);
            }
        } else {
            homeNameView.setText("Unknown Team");
            homeLogoView.setImageResource(R.drawable.default_team_logo);
        }
    
        // Set away team info
        TextView awayNameView = view.findViewById(R.id.awayName);
        ImageView awayLogoView = view.findViewById(R.id.awayLogo);
        if (match.getAway() != null) {
            awayNameView.setText(truncateText(match.getAway().getName(), 20));
            if (match.getAway().getLogo() != null) {
                Glide.with(view.getContext())
                        .load(match.getAway().getLogo())
                        .into(awayLogoView);
            } else {
                awayLogoView.setImageResource(R.drawable.default_team_logo);
            }
        } else {
            awayNameView.setText(truncateText(match.getHome().getName(), 20));
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
            scoreView.setText(match.getScores().getHome() + " - " + match.getScores().getAway());
        } else {
            scoreView.setText("vs");
        }
    
        // Set match status
        TextView statusView = view.findViewById(R.id.matchStatus);
        if ("live".equalsIgnoreCase(match.getMatch_status())) {
            statusView.setText("Đang diễn ra");
            statusView.setTextColor(ContextCompat.getColor(view.getContext(), android.R.color.holo_green_light));
        } else if ("finished".equalsIgnoreCase(match.getMatch_status())) {
            statusView.setText("Kết thúc");
            statusView.setTextColor(ContextCompat.getColor(view.getContext(), android.R.color.holo_red_light));
        } else {
            statusView.setText("Sắp diễn ra");
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

                ImageView avatarView = commentatorView.findViewById(R.id.commentatorAvatar);
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
            } else {
                v.setBackgroundResource(R.drawable.match_background_normal);
            }
        });
    }
    
    private String truncateText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() > maxLength) {
            return text.substring(0, maxLength - 3) + "...";
        }
        return text;
    }
    
    private String formatDateTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        Log.d(TAG, "onUnbindViewHolder");
        // Do nothing
    }
}
