/*
 * Copyright (C) 2026 The AviumUI Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.avium.launcher;

import android.content.Context;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.launcher3.Launcher;
import com.android.launcher3.model.data.AppInfo;

import org.avium.launcher.anim.OverlayAnimator;
import org.avium.launcher.anim.WorkspaceAnimator;
import org.avium.launcher.data.AppCategorizer;

import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

public class AppIndexController {

    private static final int APP_GRID_WIDTH_DP = 300;
    private static final int APP_GRID_HEIGHT_DP = 360;

    private final Launcher mLauncher;
    private final Vibrator mVibrator;
    private final AppCategorizer mCategorizer;
    private final WorkspaceAnimator mWorkspaceAnimator;
    private final OverlayAnimator mOverlayAnimator;

    private AppIndexOverlay mOverlay;
    private TreeMap<String, List<AppInfo>> mAppsMap;
    private List<String> mLetters;

    private boolean mIsShowing;
    private boolean mIsLeftEdge;
    private int mSelectedLetterIndex;
    private int mSelectedAppIndex;
    private AppInfo mSelectedApp;

    public AppIndexController(Launcher launcher) {
        mLauncher = launcher;
        mVibrator = (Vibrator) launcher.getSystemService(Context.VIBRATOR_SERVICE);
        mCategorizer = new AppCategorizer();
        mWorkspaceAnimator = new WorkspaceAnimator(launcher);
        mOverlayAnimator = new OverlayAnimator();
    }

    public void init() {
        loadApps();
    }

    public void onAppsChanged() {
        loadApps();
        if (mOverlay != null) {
            mOverlay.setLetters(mLetters);
        }
    }

    private void loadApps() {
        if (mLauncher.getAppsView() == null) return;
        AppInfo[] apps = mLauncher.getAppsView().getAppsStore().getApps();
        AppCategorizer.CategorizedApps result = mCategorizer.categorize(apps);
        mAppsMap = result.appsMap;
        mLetters = result.letters;
    }

    private void performHapticFeedback() {
        if (mVibrator != null && mVibrator.hasVibrator()) {
            mVibrator.vibrate(10);
        }
    }

    public void start(boolean isLeftEdge) {
        if (mIsShowing) return;
        mIsShowing = true;
        mIsLeftEdge = isLeftEdge;
        mSelectedLetterIndex = 0;
        mSelectedAppIndex = -1;
        mSelectedApp = null;

        if (mOverlay == null) {
            createOverlay();
        }

        mOverlay.setLetters(mLetters);
        mOverlay.setEdgeSide(isLeftEdge);
        mOverlay.setVisibility(View.VISIBLE);
        mOverlay.bringToFront();
        mOverlay.requestLayout();
        mOverlay.invalidate();

        mOverlayAnimator.animate(mOverlay, true, isLeftEdge, null);
        mWorkspaceAnimator.animate(true, null);
        updateLetterApps();
    }

    private void createOverlay() {
        mOverlay = new AppIndexOverlay(mLauncher);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mOverlay.setLayoutParams(params);
        mLauncher.getDragLayer().addView(mOverlay);
    }

    public void onTouchMove(float x, float y) {
        if (!mIsShowing || mOverlay == null) return;

        boolean inLetterArea = mOverlay.isInLetterArea(x);

        if (inLetterArea) {
            int index = mOverlay.getLetterIndexAtY(y);
            if (index >= 0 && index < mLetters.size() && index != mSelectedLetterIndex) {
                mSelectedLetterIndex = index;
                mSelectedAppIndex = -1;
                mSelectedApp = null;
                mOverlay.clearAppSelection();
                performHapticFeedback();
                updateLetterApps();
            }
        } else {
            List<AppInfo> apps = getCurrentLetterApps();
            if (!apps.isEmpty() && mOverlay.isInAppArea(x, y)) {
                int index = mOverlay.getAppIndexAt(x, y);
                if (index >= 0 && index < apps.size() && index != mSelectedAppIndex) {
                    mSelectedAppIndex = index;
                    mSelectedApp = apps.get(index);
                    mOverlay.setSelectedAppIndex(index);
                    performHapticFeedback();
                } else if (index < 0 && (mSelectedAppIndex != -1 || mSelectedApp != null)) {
                    mSelectedAppIndex = -1;
                    mSelectedApp = null;
                    mOverlay.clearAppSelection();
                }
            } else {
                if (mSelectedAppIndex != -1 || mSelectedApp != null) {
                    mSelectedAppIndex = -1;
                    mSelectedApp = null;
                    mOverlay.clearAppSelection();
                }
            }
        }
    }

    public void onTouchUp() {
        if (!mIsShowing) return;
        mIsShowing = false;

        if (mSelectedApp != null && mSelectedAppIndex != -1) {
            mLauncher.startActivitySafely(null, mSelectedApp.getIntent(), mSelectedApp);
        }

        mSelectedAppIndex = -1;
        mSelectedApp = null;
        mOverlay.clearAppSelection();

        mOverlayAnimator.animate(mOverlay, false, mIsLeftEdge, null);
        mWorkspaceAnimator.animate(false, null);
    }

    private void updateLetterApps() {
        if (mOverlay == null || mLetters.isEmpty()) return;
        String letter = mLetters.get(mSelectedLetterIndex);
        List<AppInfo> apps = mAppsMap.getOrDefault(letter, Collections.emptyList());
        mOverlay.setCurrentLetter(letter);
        mOverlay.setApps(apps);
        mOverlay.setSelectedLetterIndex(mSelectedLetterIndex);
        mSelectedAppIndex = -1;
        mSelectedApp = null;
    }

    private List<AppInfo> getCurrentLetterApps() {
        if (mLetters.isEmpty()) return Collections.emptyList();
        String letter = mLetters.get(mSelectedLetterIndex);
        return mAppsMap.getOrDefault(letter, Collections.emptyList());
    }
}
