/*
 * Copyright (C) 2026 The AviumUI Project
 */
package com.android.quickstep.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.IntDef;
import com.android.app.animation.Interpolators;
import com.android.launcher3.R;
import com.android.launcher3.util.Themes;

import static android.view.Surface.ROTATION_0;
import static android.view.Surface.ROTATION_90;
import static android.view.Surface.ROTATION_180;
import static android.view.Surface.ROTATION_270;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A hint view for freeform gesture.  Uses canvas and child-view alpha
 * directly instead of View visibility/alpha on the container itself.
 */
public class FreeformHintView extends FrameLayout {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ROTATION_0, ROTATION_90, ROTATION_180, ROTATION_270})
    public @interface SurfaceRotation {}

    public enum HintPhase { HIDDEN, SWIPE_UP_HINT, EXPAND }

    private static final int CARD_HEIGHT_DP = 56, CORNER_RADIUS_DP = 28, ICON_SIZE_DP = 24;
    private static final int ICON_PADDING_DP = 16, TEXT_SIZE_SP = 14, CARD_MARGIN_DP = 12;
    private static final int ANIM_DURATION_MS = 250;

    private final Paint mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mCardRect = new RectF();
    private final String mSwipeUpText;
    private final Rect mTaskBounds = new Rect();

    private final float mCardHeight, mCornerRadius, mIconSize, mIconPadding;
    private final int mCardMargin;

    private final float mInnerPadding;

    @SurfaceRotation
    private int mRotation = ROTATION_0;
    private HintPhase mPhase = HintPhase.HIDDEN;
    private boolean mIsVisible, mHasTaskBounds;
    private float mHintAlpha = 0f, mScale = 0.85f, mExpandProgress = 0f;
    private String mDisplayText;

    private ValueAnimator mProgressAnimator;
    private AnimatorSet mVisibilityAnimator;
    private final int[] mPosTmp = new int[2];

    private ImageView mIconView;

    public FreeformHintView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setWillNotDraw(false);
        Resources res = context.getResources();
        float density = res.getDisplayMetrics().density;

        mCardHeight = CARD_HEIGHT_DP * density;
        mCornerRadius = CORNER_RADIUS_DP * density;
        mIconSize = ICON_SIZE_DP * density;
        mIconPadding = ICON_PADDING_DP * density;
        mCardMargin = (int) (CARD_MARGIN_DP * density);
        mInnerPadding = 12 * density;

        mBgPaint.setColor(Themes.getColorAccent(getContext()));
        mBgPaint.setStyle(Paint.Style.FILL);

        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(TEXT_SIZE_SP * density);
        mTextPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        mSwipeUpText = context.getString(R.string.avium_gesture_swipe_up_hint);

        mIconView = new ImageView(context);
        mIconView.setBackgroundResource(R.drawable.desktop_mode_ic_taskbar_menu_manage_windows);
        mIconView.setColorFilter(Color.WHITE);
        int iconSizePx = (int) mIconSize;
        int topMargin = (int) ((mCardHeight - mIconSize) / 2);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(iconSizePx, iconSizePx);
        iconLp.leftMargin = (int) mIconPadding;
        iconLp.topMargin = topMargin;
        iconLp.gravity = Gravity.TOP | Gravity.START;
        addView(mIconView, iconLp);

        setVisibility(View.VISIBLE);
        setScaleX(0.85f);
        setScaleY(0.85f);
    }


    public void setPhase(@NonNull HintPhase phase) {
        if (mPhase == phase) return;
        HintPhase prev = mPhase;
        mPhase = phase;

        switch (phase) {
            case HIDDEN:
                if (prev == HintPhase.EXPAND) {
                    adjustProgressAnimation(0f, () -> adjustVisibilityAnimation(false));
                } else {
                    adjustVisibilityAnimation(false);
                }
                break;

            case SWIPE_UP_HINT:
                mDisplayText = mSwipeUpText;
                if (prev == HintPhase.HIDDEN) {
                    adjustVisibilityAnimation(true);
                } else if (prev == HintPhase.EXPAND) {
                    adjustVisibilityAnimation(true);
                    adjustProgressAnimation(0f, null);
                }
                break;

            case EXPAND:
                if (prev == HintPhase.SWIPE_UP_HINT) {
                    adjustProgressAnimation(1f, () -> mDisplayText = null);
                }
                break;
        }
    }

    public void setDisplayRotation(@SurfaceRotation int rotation) {
        if (mRotation == rotation) return;
        mRotation = rotation;
        if (mIsVisible) {
            updatePositionAndSize();
        }
    }

    public void setTaskBounds(Rect bounds) {
        if (bounds == null) {
            mHasTaskBounds = false;
            return;
        }
        mTaskBounds.set(bounds);
        mHasTaskBounds = true;
        if (mPhase == HintPhase.EXPAND) {
            updatePositionAndSize();
        }
    }

    public void attachToContainer(RecentsViewContainer container) {
        ViewGroup dragLayer = container.getDragLayer();
        if (dragLayer == null || getParent() != null) return;

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.TOP | Gravity.START;

        dragLayer.addView(this, lp);

        post(() -> {
            if (getParent() != null) {
                updatePositionAndSize();
            }
        });
    }

    public void detachFromContainer() {
        cancelAnimators();
        if (getParent() instanceof ViewGroup) {
            ((ViewGroup) getParent()).removeView(this);
        }
    }

    public void destroy() {
        detachFromContainer();
    }


    private void adjustProgressAnimation(float target, Runnable onEnd) {
        if (mProgressAnimator != null) mProgressAnimator.cancel();

        mProgressAnimator = ValueAnimator.ofFloat(mExpandProgress, target);
        mProgressAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        mProgressAnimator.addUpdateListener(a -> {
            mExpandProgress = (float) a.getAnimatedValue();
            updatePositionAndSize();
            applyContentAlpha();   // keep icon + text in sync
        });
        mProgressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onEnd != null) onEnd.run();
            }
        });

        long duration = (long) (ANIM_DURATION_MS * Math.abs(target - mExpandProgress));
        mProgressAnimator.setDuration(Math.max(duration, 1));
        mProgressAnimator.start();
    }

    private void adjustVisibilityAnimation(boolean visible) {
        if (mVisibilityAnimator != null) mVisibilityAnimator.cancel();

        mIsVisible = visible;
        requestLayout();

        ValueAnimator alpha = ValueAnimator.ofFloat(mHintAlpha, visible ? 1f : 0f);
        alpha.addUpdateListener(a -> {
            mHintAlpha = (float) a.getAnimatedValue();
            applyContentAlpha();
            invalidate();          // redraw bg + text with new alpha
        });

        ValueAnimator scale = ValueAnimator.ofFloat(mScale, visible ? 1f : 0.85f);
        scale.addUpdateListener(a -> {
            mScale = (float) a.getAnimatedValue();
            setScaleX(mScale);
            setScaleY(mScale);
        });

        AnimatorSet set = new AnimatorSet();
        set.playTogether(alpha, scale);
        set.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        set.setDuration(ANIM_DURATION_MS);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mIsVisible = visible;
                mExpandProgress = 0f;
                mHintAlpha = visible ? 1f : 0f;
                applyContentAlpha();
                invalidate();

                if (!visible) {
                    requestLayout(); // shrink to 0 size when fully hidden
                }
            }
        });
        mVisibilityAnimator = set;
        set.start();
    }

    private void applyContentAlpha() {
        // Compute transparency of the icon and text (fades out during expand)
        float contentAlpha = (mPhase == HintPhase.EXPAND)
                ? Math.max(0f, 1f - mExpandProgress * 2f)
                : 1f;

        // Icon – handled by the child ImageView
        if (mIconView != null) {
            int iconAlpha = (int) (255 * mHintAlpha * contentAlpha);
            mIconView.setAlpha(iconAlpha / 255f);  // setAlpha expects 0..1
        }

        // Text – will be drawn in onDraw, so just invalidate
        invalidate();
    }

    private void cancelAnimators() {
        if (mProgressAnimator != null) {
            mProgressAnimator.cancel();
            mProgressAnimator = null;
        }
        if (mVisibilityAnimator != null) {
            mVisibilityAnimator.cancel();
            mVisibilityAnimator = null;
        }
    }


    private int computeHintCardWidth() {
        String text = mDisplayText != null ? mDisplayText : mSwipeUpText;
        float w = mTextPaint.measureText(text);
        return (int) (mIconPadding + mIconSize + mInnerPadding + w + mIconPadding + 0.5f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!mIsVisible && mHintAlpha == 0f
                && mVisibilityAnimator == null && mProgressAnimator == null) {
            setMeasuredDimension(0, 0);
            return;
        }

        int hintW = computeHintCardWidth();
        int hintH = (int) mCardHeight;

        if (mHasTaskBounds && mExpandProgress > 0f) {
            int taskW = mTaskBounds.width() + mCardMargin * 2;
            int taskH = mTaskBounds.height() + mCardMargin * 2;
            setMeasuredDimension(
                    (int) (hintW + (taskW - hintW) * mExpandProgress),
                    (int) (hintH + (taskH - hintH) * mExpandProgress));
        } else {
            setMeasuredDimension(hintW, hintH);
        }

        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
	}

    private void updatePositionAndSize() {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null || parent.getWidth() <= 0) return;

        int pw = parent.getWidth();
        int ph = parent.getHeight();

        int hintW = computeHintCardWidth();
        int hintH = (int) mCardHeight;
        int cw, ch, l, t;

        if (mHasTaskBounds && mExpandProgress > 0f) {
            int taskW = mTaskBounds.width() + mCardMargin * 2;
            int taskH = mTaskBounds.height() + mCardMargin * 2;
            cw = (int) (hintW + (taskW - hintW) * mExpandProgress);
            ch = (int) (hintH + (taskH - hintH) * mExpandProgress);

            getSmallCardPos(pw, ph, hintW, hintH, mPosTmp);
            float halfW = cw / 2f;
            float halfH = ch / 2f;
            int cx = (int) (mPosTmp[0] + halfW + (mTaskBounds.centerX() - (mPosTmp[0] + halfW)) * mExpandProgress);
            int cy = (int) (mPosTmp[1] + halfH + (mTaskBounds.centerY() - (mPosTmp[1] + halfH)) * mExpandProgress);
            l = cx - cw / 2;
            t = cy - ch / 2;
        } else {
            cw = hintW;
            ch = hintH;
            getSmallCardPos(pw, ph, cw, ch, mPosTmp);
            l = mPosTmp[0];
            t = mPosTmp[1];
        }

        l = Math.max(0, Math.min(l, pw - cw));
        t = Math.max(0, Math.min(t, ph - ch));

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        if (lp != null) {
            if (lp.leftMargin != l || lp.topMargin != t || lp.width != cw || lp.height != ch) {
                lp.gravity = Gravity.TOP | Gravity.START;
                lp.leftMargin = l;
                lp.topMargin = t;
                lp.width = cw;
                lp.height = ch;
                setLayoutParams(lp);
            }
        }

        invalidate();
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : (int) (24 * getResources().getDisplayMetrics().density);
    }

    private int navigationBarHeight() {
        int id = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : (int) (48 * getResources().getDisplayMetrics().density);
    }

    private void getSmallCardPos(int pw, int ph, int cw, int ch, int[] out) {
        int sbH = statusBarHeight();
        int nbH = navigationBarHeight();
        switch (mRotation) {
            case ROTATION_90:
                out[0] = mCardMargin;
                out[1] = mCardMargin + sbH;
                break;
            case ROTATION_270:
                out[0] = pw - cw - mCardMargin;
                out[1] = mCardMargin + sbH;
                break;
            case ROTATION_180:
                out[0] = mCardMargin;
                out[1] = mCardMargin + nbH;
                break;
            default: // ROTATION_0
                out[0] = pw - cw - mCardMargin;
                out[1] = mCardMargin + sbH;
                break;
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (mHintAlpha <= 0.01f || (mPhase == HintPhase.HIDDEN && !mIsVisible)) {
            return;
        }

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Background opacity
        int bgAlpha = (int) (255 * mHintAlpha);
        mBgPaint.setAlpha(bgAlpha);
        mCardRect.set(0, 0, w, h);
        canvas.drawRoundRect(mCardRect, mCornerRadius, mCornerRadius, mBgPaint);

        // Text (only when there is something to show)
        float contentAlpha = (mPhase == HintPhase.EXPAND)
                ? Math.max(0f, 1f - mExpandProgress * 2f)
                : (mPhase == HintPhase.SWIPE_UP_HINT ? 1f : 0f);

        if (contentAlpha > 0f && mDisplayText != null) {
            int textAlpha = (int) (255 * mHintAlpha * contentAlpha);
            mTextPaint.setAlpha(textAlpha);
            float tx = mIconPadding + mIconSize + mInnerPadding;
            float ty = h / 2f - (mTextPaint.descent() + mTextPaint.ascent()) / 2f;
            canvas.drawText(mDisplayText, tx, ty, mTextPaint);
        }

    }
}