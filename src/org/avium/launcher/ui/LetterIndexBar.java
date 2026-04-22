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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class LetterIndexBar extends View {

    private static final int LETTER_TEXT_SIZE_DP = 13;
    private static final int LETTER_SPACING_DP = 5;

    private List<String> mLetters = new ArrayList<>();
    private int mSelectedIndex = -1;
    private Paint mTextPaint;
    private float mItemHeight;
    private float mSpacing;

    public LetterIndexBar(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(LETTER_TEXT_SIZE_DP * density);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setShadowLayer(1 * density, 0, 0, Color.BLACK);
        mSpacing = LETTER_SPACING_DP * density;
    }

    public void setLetters(List<String> letters) {
        mLetters = letters != null ? letters : new ArrayList<>();
        requestLayout();
        invalidate();
    }

    public void setSelectedIndex(int index) {
        mSelectedIndex = index;
        invalidate();
    }

    public int getSelectedIndex() {
        return mSelectedIndex;
    }

    public int getIndexAtY(float y) {
        if (mLetters.isEmpty()) return -1;
        float totalHeight = mLetters.size() * mItemHeight + (mLetters.size() - 1) * mSpacing;
        float top = (getHeight() - totalHeight) / 2;
        int index = (int) ((y - top) / (mItemHeight + mSpacing));
        return Math.max(0, Math.min(index, mLetters.size() - 1));
    }

    public float getLetterCenterY(int index) {
        if (mLetters.isEmpty() || index < 0 || index >= mLetters.size()) return -1;
        float totalHeight = mLetters.size() * mItemHeight + (mLetters.size() - 1) * mSpacing;
        float startY = (getHeight() - totalHeight) / 2 + mItemHeight / 2;
        return startY + index * (mItemHeight + mSpacing);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        float density = getResources().getDisplayMetrics().density;
        mItemHeight = LETTER_TEXT_SIZE_DP * density * 1.2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mLetters.isEmpty()) return;

        float width = getWidth();
        float totalHeight = mLetters.size() * mItemHeight + (mLetters.size() - 1) * mSpacing;
        float startY = (getHeight() - totalHeight) / 2 + mItemHeight / 2;
        float centerX = width / 2;

        for (int i = 0; i < mLetters.size(); i++) {
            float centerY = startY + i * (mItemHeight + mSpacing);
            if (i == mSelectedIndex) {
                mTextPaint.setColor(Color.WHITE);
                mTextPaint.setAlpha(255);
                mTextPaint.setTextSize(LETTER_TEXT_SIZE_DP * getResources().getDisplayMetrics().density * 1.3f);
            } else {
                mTextPaint.setColor(Color.WHITE);
                mTextPaint.setAlpha(180);
                mTextPaint.setTextSize(LETTER_TEXT_SIZE_DP * getResources().getDisplayMetrics().density);
            }
            String letter = mLetters.get(i);
            canvas.drawText(letter, centerX, centerY + mTextPaint.getTextSize() / 3, mTextPaint);
        }
    }
}
