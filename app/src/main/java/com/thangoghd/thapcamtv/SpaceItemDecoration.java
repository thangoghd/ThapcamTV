package com.thangoghd.thapcamtv;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SpaceItemDecoration extends RecyclerView.ItemDecoration {
    private final int left;
    private final int right;
    private final int top;
    private final int bottom;


    public SpaceItemDecoration(int left, int right, int top, int bottom) {
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {


        outRect.left = left;
        outRect.right = right;
        outRect.bottom = bottom;

        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = top;
        } else {
            outRect.top = 0;
        }
    }
}