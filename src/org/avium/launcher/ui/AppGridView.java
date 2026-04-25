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
package org.avium.launcher.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.widget.FrameLayout;

import com.android.launcher3.model.data.AppInfo;

import java.util.ArrayList;
import java.util.List;

public class AppGridView extends View {

    private static final int APPS_PER_ROW = 4;
    private static final float ITEM_HEIGHT_DP = 58f;
    private static final float ICON_CORNER_RADIUS_DP = 12f;
    private static final float TOUCH_HORIZONTAL_PADDING_DP = 8f;
    private static final float TOUCH_VERTICAL_PADDING_DP = 14f;
    private static final float TOUCH_Y_OFFSET_DP = 10f;

    private List<AppInfo> mApps = new ArrayList<>();
    private int mSelectedIndex = -1;
    private Paint mTextPaint;
    private Paint mIconBackgroundPaint;
    private float mItemWidth;
    private float mItemHeight;
    private int mAppsPerRow;
    private float mScrollY = 0f;
    private float mMaxScrollY = 0f;
    private float mTotalContentHeight = 0f;
    private float mAnchorY = -1f;
    private float mTouchHorizontalPadding;
    private float mTouchVerticalPadding;
    private float mTouchYOffset;
    private boolean mIsLeftEdge = true;

    public AppGridView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        mAppsPerRow = APPS_PER_ROW;
        mItemHeight = ITEM_HEIGHT_DP * density;
        mTouchHorizontalPadding = TOUCH_HORIZONTAL_PADDING_DP * density;
        mTouchVerticalPadding = TOUCH_VERTICAL_PADDING_DP * density;
        mTouchYOffset = TOUCH_Y_OFFSET_DP * density;

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(11 * density);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setShadowLayer(1 * density, 0, 0, Color.BLACK);

        mIconBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mIconBackgroundPaint.setColor(Color.WHITE);
    }

    public void setApps(List<AppInfo> apps) {
        mApps = apps != null ? apps : new ArrayList<>();
        mSelectedIndex = -1;
        mScrollY = 0f;
        updateContentHeight();
        invalidate();
    }

    private void updateContentHeight() {
        if (mApps.isEmpty()) {
            mTotalContentHeight = 0f;
            mMaxScrollY = 0f;
            return;
        }
        int rowCount = (mApps.size() + mAppsPerRow - 1) / mAppsPerRow;
        mTotalContentHeight = rowCount * mItemHeight;
        mMaxScrollY = Math.max(0f, mTotalContentHeight - getHeight());
    }

    public void setSelectedIndex(int index) {
        if (index != mSelectedIndex) {
            mSelectedIndex = index;
            if (index >= 0) {
                scrollToIndex(index);
            }
            invalidate();
        }
    }

    private void scrollToIndex(int index) {
        if (mMaxScrollY <= 0) return;
        int row = index / mAppsPerRow;
        float itemTop = row * mItemHeight;
        float itemBottom = itemTop + mItemHeight;
        float viewHeight = getHeight();
        if (itemTop < mScrollY) {
            mScrollY = itemTop;
        } else if (itemBottom > mScrollY + viewHeight) {
            mScrollY = itemBottom - viewHeight;
        }
        mScrollY = Math.max(0f, Math.min(mScrollY, mMaxScrollY));
    }

    public int getIndexAt(float x, float y) {
        if (mApps.isEmpty()) return -1;
        if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight() || mItemWidth <= 0) return -1;

        float width = getWidth();
        int hitIndex = -1;
        float minDistance = Float.MAX_VALUE;
        for (int i = 0; i < mApps.size(); i++) {
            int row = i / mAppsPerRow;
            int col = i % mAppsPerRow;
            float left = mIsLeftEdge ? col * mItemWidth : width - (col + 1) * mItemWidth;
            float top = row * mItemHeight - mScrollY;
            float hitTop = top + mTouchYOffset - mTouchVerticalPadding;
            float hitBottom = top + mItemHeight + mTouchYOffset + mTouchVerticalPadding;
            if (x >= left - mTouchHorizontalPadding && x < left + mItemWidth + mTouchHorizontalPadding
                    && y >= hitTop && y < hitBottom) {
                float centerY = top + mItemHeight / 2f + mTouchYOffset;
                float distance = Math.abs(y - centerY);
                if (distance < minDistance) {
                    minDistance = distance;
                    hitIndex = i;
                }
            }
        }
        return hitIndex;
    }

    public void setLeftEdge(boolean isLeftEdge) {
        mIsLeftEdge = isLeftEdge;
        invalidate();
    }

    public void setAnchorY(float anchorY) {
        mAnchorY = anchorY;
        updatePosition();
    }

    private void updatePosition() {
        if (mAnchorY < 0) return;
        int height = getHeight();
        int parentHeight = ((View) getParent()).getHeight();
        if (height == 0 || parentHeight == 0) return;

        int newTop = (int) (mAnchorY - mItemHeight / 2f);
        newTop = Math.max(20, Math.min(newTop, parentHeight - height - 20));

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        if (params.topMargin != newTop) {
            params.topMargin = newTop;
            setLayoutParams(params);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updatePosition();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mItemWidth = w / (float) mAppsPerRow;
        updateContentHeight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mApps.isEmpty()) return;

        float width = getWidth();
        float height = getHeight();
        float density = getResources().getDisplayMetrics().density;
        float iconSize = 44 * density;
        float padding = 4 * density;
        float liftOffset = 8 * density;
        float cornerRadius = ICON_CORNER_RADIUS_DP * density;

        for (int i = 0; i < mApps.size(); i++) {
            int row = i / mAppsPerRow;
            int col = i % mAppsPerRow;

            float left;
            if (mIsLeftEdge) {
                left = col * mItemWidth;
            } else {
                left = width - (col + 1) * mItemWidth;
            }
            float top = row * mItemHeight - mScrollY;
            float centerX = left + mItemWidth / 2;
            float centerY = top + mItemHeight / 2;

            if (top + mItemHeight < -liftOffset || top > height + liftOffset) {
                continue;
            }

            boolean isSelected = (i == mSelectedIndex);
            float offsetY = isSelected ? -liftOffset : 0;

            AppInfo app = mApps.get(i);
            Bitmap icon = app.bitmap != null ? app.bitmap.icon : null;
            if (icon != null) {
                float iconLeft = centerX - iconSize / 2;
                float iconTop = centerY - iconSize / 2 - 6 * density + offsetY;
                RectF iconRect = new RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);

                BitmapShader shader = new BitmapShader(icon, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                Paint roundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                roundPaint.setShader(shader);
                android.graphics.Matrix matrix = new android.graphics.Matrix();
                matrix.setRectToRect(new RectF(0, 0, icon.getWidth(), icon.getHeight()), iconRect, android.graphics.Matrix.ScaleToFit.FILL);
                shader.setLocalMatrix(matrix);
                canvas.drawRoundRect(iconRect, cornerRadius, cornerRadius, roundPaint);
            }

            String title = app.title != null ? app.title.toString() : "";
            if (title.length() > 5) {
                title = title.substring(0, 4) + "...";
            }
            canvas.drawText(title, centerX, centerY + iconSize / 2 + 10 * density + offsetY, mTextPaint);
        }
    }
}
