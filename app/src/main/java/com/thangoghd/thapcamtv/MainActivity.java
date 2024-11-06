package com.thangoghd.thapcamtv;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.ListFragment;
import androidx.leanback.widget.BrowseFrameLayout;

public class MainActivity extends FragmentActivity implements View.OnKeyListener{
    private BrowseFrameLayout navBar;

    private TextView btnLive;
    private TextView btnHighlight;
    private TextView btnReplay;

    private boolean SIDE_MENU = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navBar = findViewById(R.id.blfNavBar);
        btnLive = findViewById(R.id.navLive);
        btnHighlight = findViewById(R.id.navHighlight);
        btnReplay = findViewById(R.id.navReplay);

        navBar.setOnKeyListener(this);
        btnLive.setOnKeyListener(this);
        btnHighlight.setOnKeyListener(this);
        btnReplay.setOnKeyListener(this);

        changeFragment(new LiveFragment());
    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER){
            if (view.getId() == R.id.navLive) {
                changeFragment(new LiveFragment());
            }
            else if (view.getId() == R.id.navHighlight) {
                changeFragment(new HighlightFragment());
            }
            else if (view.getId() == R.id.navReplay) {
                changeFragment(new FullMatchFragment());
            }
        }
        else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
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
            SIDE_MENU = false;
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
        navBar.requestLayout();
        navBar.getLayoutParams().width = getWidthInPercent(this, 16);
    }

    private void closeMenu() {
        navBar.requestLayout();
        navBar.getLayoutParams().width = getWidthInPercent(this, 5);
    }

    private int getWidthInPercent(Context context, int percent) {
        int width = context.getResources().getDisplayMetrics().widthPixels;
        return (width * percent) / 100;
    }

    private void changeFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.sideMenu, fragment)
                .commit();
        closeMenu();
    }
}