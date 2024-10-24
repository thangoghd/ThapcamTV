package com.thangoghd.thapcamtv;

import androidx.annotation.DrawableRes;

public enum SportType {
    FOOTBALL("football", "Bóng đá", R.drawable.ic_football),
    BASKETBALL("basketball", "Bóng rổ", R.drawable.ic_basketball),
    ESPORTS("esports", "Thể thao điện tử", R.drawable.ic_esports),
    TENNIS("tennis", "Quần vợt", R.drawable.ic_tennis),
    VOLLEYBALL("volleyball", "Bóng chuyền", R.drawable.ic_volleyball),
    BADMINTON("badminton", "Cầu lông", R.drawable.ic_badminton),
    BILLIARD("pool", "Bida", R.drawable.ic_billiard),
    RACE("race", "Đua xe", R.drawable.ic_race_car),
    BOXING("wwe", "Đấm bốc", R.drawable.ic_boxing),
    EVENT("event", "Sự kiện", R.drawable.ic_event),
    OTHER("other", "Khác", R.drawable.ic_other_sport);

    private final String key;
    private final String vietnameseName;
    private final int iconResourceId;

    SportType(String key, String vietnameseName, @DrawableRes int iconResourceId) {
        this.key = key;
        this.vietnameseName = vietnameseName;
        this.iconResourceId = iconResourceId;
    }

    public String getKey() {
        return key;
    }

    public String getVietnameseName() {
        return vietnameseName;
    }

    public int getIconResourceId() {
        return iconResourceId;
    }

    public static SportType fromKey(String key) {
        for (SportType sportType : values()) {
            if (sportType.key.equals(key)) {
                return sportType;
            }
        }
        // Default to OTHER if no match is found
        return OTHER;
    }
}
