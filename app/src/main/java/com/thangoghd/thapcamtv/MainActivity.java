package com.thangoghd.thapcamtv;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.leanback.widget.BrowseFrameLayout;

public class MainActivity extends FragmentActivity implements View.OnKeyListener{
    private BrowseFrameLayout navBar;

    private LinearLayout btnLive;
    private LinearLayout btnHighlight;
    private LinearLayout btnReplay;
    private LinearLayout lastSelectedMenu;

    private boolean SIDE_MENU = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navBar = findViewById(R.id.blfNavBar);
        btnLive = findViewById(R.id.navLive);
        btnHighlight = findViewById(R.id.navHighlight);
        btnReplay = findViewById(R.id.navFullMatch);

        navBar.setOnKeyListener(this);
        btnLive.setOnKeyListener(this);
        btnHighlight.setOnKeyListener(this);
        btnReplay.setOnKeyListener(this);

        lastSelectedMenu = btnLive;
        lastSelectedMenu.requestFocus();

        changeFragment(new LiveFragment());
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER){
            Log.d("MainActivity", String.valueOf(lastSelectedMenu));
            if (view.getId() == R.id.navLive) {
                changeFragment(new LiveFragment());
                lastSelectedMenu = (LinearLayout) view;
            }
            else if (view.getId() == R.id.navHighlight) {
                changeFragment(new HighlightFragment());
                lastSelectedMenu = (LinearLayout) view;
            }
            else if (view.getId() == R.id.navFullMatch) {
                changeFragment(new FullMatchFragment());
                lastSelectedMenu = (LinearLayout) view;
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (!SIDE_MENU) {
                openMenu();
                SIDE_MENU = true;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && SIDE_MENU) {
            closeMenu();
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            if (btnLive.hasFocus()) { 
                closeMenu();
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (btnReplay.hasFocus()) {
                closeMenu();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (SIDE_MENU) {
            SIDE_MENU = false;
            closeMenu();
        } else {
            super.onBackPressed();
        }
    }

    private void openMenu() {
        lastSelectedMenu.requestFocus();
        navBar.requestLayout();
        animateMenuWidth(navBar, getWidthInPercent(this, 5), getWidthInPercent(this, 16));
        SIDE_MENU = true;
    }

    private void closeMenu() {
        if(!SIDE_MENU) return;
        navBar.requestLayout();
        animateMenuWidth(navBar, getWidthInPercent(this, 16), getWidthInPercent(this, 5));
        SIDE_MENU = false;
    }

    private int getWidthInPercent(Context context, int percent) {
        int width = context.getResources().getDisplayMetrics().widthPixels;
        return (width * percent) / 100;
    }

    private void animateMenuWidth(View view, int startWidth, int endWidth) {
        ValueAnimator animator = ValueAnimator.ofInt(startWidth, endWidth);
        animator.addUpdateListener(animation -> {
            view.getLayoutParams().width = (int) animation.getAnimatedValue();
            view.requestLayout();
        });
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    private void changeFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.sideMenu, fragment)
                .commit();
        closeMenu();
    }
}