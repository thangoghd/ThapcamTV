package com.thangoghd.thapcamtv.iconheader;

import androidx.leanback.widget.PresenterSelector;
import androidx.leanback.widget.Presenter;

public class IconHeaderItemPresenterSelector extends PresenterSelector {
    private final IconHeaderItemPresenter mIconHeaderItemPresenter = new IconHeaderItemPresenter();

    @Override
    public Presenter getPresenter(Object item) {
        return mIconHeaderItemPresenter;
    }
}