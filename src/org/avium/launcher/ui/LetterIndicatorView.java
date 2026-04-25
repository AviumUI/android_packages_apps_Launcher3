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

public class LetterIndicatorView extends View {

    private static final int CURRENT_LETTER_SIZE_DP = 48;

    private String mLetter = "";
    private Paint mBackgroundPaint;
    private Paint mTextPaint;
    private float mSize;
    private boolean mIsLeftEdge = true;

    public LetterIndicatorView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        mSize = CURRENT_LETTER_SIZE_DP * density;

        mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackgroundPaint.setColor(Color.argb(180, 80, 80, 80));

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(CURRENT_LETTER_SIZE_DP * density * 0.6f);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setLetter(String letter) {
        mLetter = letter != null ? letter : "";
        invalidate();
    }

    public void setLeftEdge(boolean isLeftEdge) {
        mIsLeftEdge = isLeftEdge;
        invalidate();
    }

    public int getIndicatorSize() {
        return (int) mSize;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension((int) mSize, (int) mSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = mSize / 2;
        float centerY = mSize / 2;
        float radius = mSize / 2 - 4;
        canvas.drawCircle(centerX, centerY, radius, mBackgroundPaint);
        if (!mLetter.isEmpty()) {
            float textY = centerY + mTextPaint.getTextSize() / 3;
            canvas.drawText(mLetter, centerX, textY, mTextPaint);
        }
    }
}
