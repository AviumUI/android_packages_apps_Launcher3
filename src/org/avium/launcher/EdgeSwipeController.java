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

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.util.TouchController;

import java.util.Collections;
import java.util.List;

public class EdgeSwipeController implements TouchController {

    private static final int EDGE_WIDTH_DP = 20;
    private static final float MIN_VERTICAL_MOVE_DP = 4;
    private static final float MAX_HORIZONTAL_MOVE_DP = 20;
    private static final float VERTICAL_EXCLUSION_DP = 80;

    private final Launcher mLauncher;
    private final int mEdgeWidth;
    private final int mMinVerticalMove;
    private final int mMaxHorizontalMove;
    private final int mVerticalExclusion;

    private AppIndexController mAppIndexController;
    private boolean mIsActive;
    private boolean mIsInEdgeArea;
    private float mStartY;
    private float mStartX;
    private boolean mHasMoved;
    private boolean mIntercepting;

    public EdgeSwipeController(Launcher launcher) {
        mLauncher = launcher;
        float density = launcher.getResources().getDisplayMetrics().density;
        mEdgeWidth = (int) (EDGE_WIDTH_DP * density);
        mMinVerticalMove = (int) (MIN_VERTICAL_MOVE_DP * density);
        mMaxHorizontalMove = (int) (MAX_HORIZONTAL_MOVE_DP * density);
        mVerticalExclusion = (int) (VERTICAL_EXCLUSION_DP * density);
    }

    public void setAppIndexController(AppIndexController controller) {
        mAppIndexController = controller;
    }

    public void setupSystemGestureExclusion() {
        View dragLayer = mLauncher.getDragLayer();
        dragLayer.addOnLayoutChangeListener(mLayoutChangeListener);
        updateSystemGestureExclusion();
    }

    private final View.OnLayoutChangeListener mLayoutChangeListener =
            (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                updateSystemGestureExclusion();
            };

    private void updateSystemGestureExclusion() {
        View dragLayer = mLauncher.getDragLayer();
        int height = dragLayer.getHeight();
        int width = dragLayer.getWidth();
        if (height == 0 || width == 0) return;
        Rect leftRect = new Rect(0, 0, mEdgeWidth, height);
        Rect rightRect = new Rect(width - mEdgeWidth, 0, width, height);
        List<Rect> exclusionRects = java.util.Arrays.asList(leftRect, rightRect);
        dragLayer.setSystemGestureExclusionRects(exclusionRects);
    }

    public void clearSystemGestureExclusion() {
        View dragLayer = mLauncher.getDragLayer();
        dragLayer.setSystemGestureExclusionRects(Collections.emptyList());
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (mAppIndexController == null) {
            return false;
        }
        if (!mLauncher.isInState(LauncherState.NORMAL)) {
            return false;
        }
        if (AbstractFloatingView.getTopOpenView(mLauncher) != null) {
            return false;
        }

        int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            mIsInEdgeArea = isInEdgeArea(ev);
            if (mIsInEdgeArea) {
                mStartX = ev.getX();
                mStartY = ev.getY();
                mHasMoved = false;
                mIsActive = false;
                mIntercepting = false;
            }
            return false;
        }

        if (!mIsInEdgeArea) {
            return false;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            if (!mHasMoved) {
                float dy = Math.abs(ev.getY() - mStartY);
                float dx = Math.abs(ev.getX() - mStartX);
                
                if (dy > mMinVerticalMove && dx < mMaxHorizontalMove) {
                    mHasMoved = true;
                    mIsActive = true;
                    mIntercepting = true;
                    boolean isLeftEdge = ev.getX() < mEdgeWidth;
                    mAppIndexController.start(isLeftEdge);
                    return true;
                } else if (dx > mMaxHorizontalMove) {
                    mIsInEdgeArea = false;
                    return false;
                }
            } else if (mIsActive) {
                return true;
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (mIsActive) {
                mAppIndexController.onTouchUp();
            }
            resetState();
        }

        return mIntercepting;
    }

    @Override
    public boolean onControllerTouchEvent(MotionEvent ev) {
        if (mAppIndexController == null) {
            return false;
        }

        int action = ev.getAction();
        if (action == MotionEvent.ACTION_MOVE) {
            if (mIsActive) {
                mAppIndexController.onTouchMove(ev.getX(), ev.getY());
            }
            return true;
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (mIsActive) {
                mAppIndexController.onTouchUp();
            }
            resetState();
            return true;
        }
        return mIntercepting;
    }

    private void resetState() {
        mIsActive = false;
        mIsInEdgeArea = false;
        mHasMoved = false;
        mIntercepting = false;
    }

    private boolean isInEdgeArea(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();
        int width = mLauncher.getDragLayer().getWidth();
        int height = mLauncher.getDragLayer().getHeight();

        boolean inHorizontalEdge = x < mEdgeWidth || x > width - mEdgeWidth;
        boolean inVerticalSafeZone = y > mVerticalExclusion && y < height - mVerticalExclusion;

        return inHorizontalEdge && inVerticalSafeZone;
    }
}
