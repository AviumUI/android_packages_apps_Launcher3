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
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.android.launcher3.model.data.AppInfo;

import org.avium.launcher.ui.AppGridView;
import org.avium.launcher.ui.LetterIndicatorView;
import org.avium.launcher.ui.LetterIndexBar;

import java.util.List;

public class AppIndexOverlay extends FrameLayout {

    private static final int LETTER_BAR_WIDTH_DP = 20;
    private static final int APP_GRID_WIDTH_DP = 280;
    private static final int APP_GRID_HEIGHT_DP = 380;
    private static final int GRID_TO_BAR_SPACING_DP = 2;

    private LetterIndexBar mLetterBar;
    private AppGridView mAppGrid;
    private LetterIndicatorView mCurrentLetterView;

    private boolean mIsLeftEdge;
    private int mLetterBarWidth;
    private int mAppGridWidth;
    private int mAppGridHeight;
    private int mSpacing;

    public AppIndexOverlay(Context context) {
        this(context, null);
    }

    public AppIndexOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        mLetterBarWidth = (int) (LETTER_BAR_WIDTH_DP * density);
        mAppGridWidth = (int) (APP_GRID_WIDTH_DP * density);
        mAppGridHeight = (int) (APP_GRID_HEIGHT_DP * density);
        mSpacing = (int) (GRID_TO_BAR_SPACING_DP * density);

        setClipChildren(false);
        setClipToPadding(false);

        mLetterBar = new LetterIndexBar(context);
        LayoutParams barParams = new LayoutParams(mLetterBarWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        barParams.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        barParams.rightMargin = (int) (4 * density);
        addView(mLetterBar, barParams);

        mAppGrid = new AppGridView(context);
        LayoutParams gridParams = new LayoutParams(mAppGridWidth, mAppGridHeight);
        gridParams.gravity = Gravity.LEFT | Gravity.TOP;
        gridParams.leftMargin = mLetterBarWidth + mSpacing;
        addView(mAppGrid, gridParams);

        mCurrentLetterView = new LetterIndicatorView(context);
        addView(mCurrentLetterView, new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void setEdgeSide(boolean isLeftEdge) {
        mIsLeftEdge = isLeftEdge;
        float density = getResources().getDisplayMetrics().density;
        int barMargin = (int) (4 * density);

        LayoutParams barParams = (LayoutParams) mLetterBar.getLayoutParams();
        barParams.gravity = isLeftEdge ? (Gravity.LEFT | Gravity.CENTER_VERTICAL) : (Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        barParams.leftMargin = isLeftEdge ? barMargin : 0;
        barParams.rightMargin = isLeftEdge ? 0 : barMargin;
        mLetterBar.setLayoutParams(barParams);

        mAppGrid.setLeftEdge(isLeftEdge);
        LayoutParams gridParams = (LayoutParams) mAppGrid.getLayoutParams();
        if (isLeftEdge) {
            gridParams.gravity = Gravity.LEFT | Gravity.TOP;
            gridParams.leftMargin = mLetterBarWidth + mSpacing;
            gridParams.rightMargin = 0;
        } else {
            gridParams.gravity = Gravity.RIGHT | Gravity.TOP;
            gridParams.leftMargin = 0;
            gridParams.rightMargin = mLetterBarWidth + mSpacing;
        }
        mAppGrid.setLayoutParams(gridParams);

        mCurrentLetterView.setLeftEdge(isLeftEdge);
        updateLetterIndicatorPosition();
    }

    private void updateLetterIndicatorPosition() {
        float density = getResources().getDisplayMetrics().density;
        LayoutParams params = (LayoutParams) mCurrentLetterView.getLayoutParams();
        int indicatorSize = mCurrentLetterView.getIndicatorSize();
        int indicatorToGridSpacing = (int) (8 * density);
        int indicatorBottomSpacing = (int) (16 * density);

        if (mIsLeftEdge) {
            params.gravity = Gravity.LEFT | Gravity.TOP;
            params.leftMargin = mLetterBarWidth + mSpacing + indicatorToGridSpacing;
            params.rightMargin = 0;
        } else {
            params.gravity = Gravity.RIGHT | Gravity.TOP;
            params.leftMargin = 0;
            params.rightMargin = mLetterBarWidth + mSpacing + indicatorToGridSpacing;
        }

        int gridTop = getAppGridTop();
        params.topMargin = Math.max(0, gridTop - indicatorSize - indicatorBottomSpacing);

        mCurrentLetterView.setLayoutParams(params);
    }

    public void setLetters(List<String> letters) {
        mLetterBar.setLetters(letters);
    }

    public void setCurrentLetter(String letter) {
        mCurrentLetterView.setLetter(letter);
    }

    public void setApps(List<AppInfo> apps) {
        mAppGrid.setApps(apps);
    }

    public void setSelectedLetterIndex(int index) {
        mLetterBar.setSelectedIndex(index);
        updateAppGridPosition(index);
        updateLetterIndicatorPosition();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int selectedIndex = mLetterBar.getSelectedIndex();
        if (selectedIndex >= 0) {
            updateAppGridPosition(selectedIndex);
            updateLetterIndicatorPosition();
        }
    }

    private void updateAppGridPosition(int letterIndex) {
        if (letterIndex < 0) return;
        float letterY = getLetterCenterYInOverlay(letterIndex);
        if (letterY >= 0) {
            mAppGrid.setAnchorY(letterY);
        }
    }

    private float getLetterCenterYInOverlay(int letterIndex) {
        float letterY = mLetterBar.getLetterCenterY(letterIndex);
        if (letterY < 0) return -1;
        return mLetterBar.getTop() + letterY;
    }

    private int getAppGridTop() {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mAppGrid.getLayoutParams();
        return params.topMargin;
    }

    public void setSelectedAppIndex(int index) {
        mAppGrid.setSelectedIndex(index);
    }

    public void clearAppSelection() {
        mAppGrid.setSelectedIndex(-1);
    }

    public boolean isInLetterArea(float x) {
        x -= getTranslationX();
        return x >= mLetterBar.getLeft() - mSpacing && x <= mLetterBar.getRight() + mSpacing;
    }

    public boolean isInAppArea(float x, float y) {
        x -= getTranslationX();
        int gridLeft = mAppGrid.getLeft();
        int gridRight = mAppGrid.getRight();
        int gridTop = mAppGrid.getTop();
        int gridBottom = mAppGrid.getBottom();
        return x >= gridLeft && x <= gridRight && y >= gridTop && y <= gridBottom;
    }

    public int getLetterIndexAtY(float y) {
        return mLetterBar.getIndexAtY(y - mLetterBar.getTop());
    }

    public int getAppIndexAt(float x, float y) {
        x -= getTranslationX();
        float relativeX = x - mAppGrid.getLeft();
        float relativeY = y - mAppGrid.getTop();
        return mAppGrid.getIndexAt(relativeX, relativeY);
    }
}
