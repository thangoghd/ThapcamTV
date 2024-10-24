package com.thangoghd.thapcamtv.iconheader;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowHeaderPresenter;

import com.thangoghd.thapcamtv.R;

public class IconHeaderItemPresenter extends RowHeaderPresenter {
    private float mUnselectedAlpha;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        mUnselectedAlpha = parent.getResources().getFraction(R.fraction.lb_browse_header_unselect_alpha, 1, 1);
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.icon_header_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        IconHeaderItem headerItem = (IconHeaderItem) ((Row) item).getHeaderItem();
        View rootView = viewHolder.view;
        ImageView iconView = rootView.findViewById(R.id.header_icon);
        TextView label = rootView.findViewById(R.id.header_label);
        iconView.setImageResource(headerItem.getIconResId());
        label.setText(headerItem.getName());
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        // no op
    }
}