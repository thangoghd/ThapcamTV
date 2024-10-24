package com.thangoghd.thapcamtv;

import androidx.leanback.widget.HeaderItem;

public class IconHeaderItem extends HeaderItem {
    private int iconResId;

    public IconHeaderItem(long id, String name, int iconResId) {
        super(id, name);
        this.iconResId = iconResId;
    }

    public int getIconResId() {
        return iconResId;
    }
}