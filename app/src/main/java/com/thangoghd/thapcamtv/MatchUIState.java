package com.thangoghd.thapcamtv;

public class MatchUIState {
    private boolean isFocused;
    private int scrollPosition;

    public MatchUIState() {
        this.isFocused = false;
        this.scrollPosition = 0;
    }

    public boolean isFocused() {
        return isFocused;
    }

    public void setFocused(boolean focused) {
        isFocused = focused;
    }

    public int getScrollPosition() {
        return scrollPosition;
    }

    public void setScrollPosition(int scrollPosition) {
        this.scrollPosition = scrollPosition;
    }
}