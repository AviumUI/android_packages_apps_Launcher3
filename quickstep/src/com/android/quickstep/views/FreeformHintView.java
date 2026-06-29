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
import android.graphics.Bitmap;
import android.graphics.Path;
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
import com.android.quickstep.util.TaskCornerRadius;

import android.util.Log;
import static android.view.Surface.ROTATION_0;
import static android.view.Surface.ROTATION_90;
import static android.view.Surface.ROTATION_180;
import static android.view.Surface.ROTATION_270;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A hint view for Freeform gesture.  Uses canvas and child-view alpha
 * directly instead of View visibility/alpha on the container itself.
 */
public class FreeformHintView extends FrameLayout {
    private static final String TAG = "FreeformHintView";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ROTATION_0, ROTATION_90, ROTATION_180, ROTATION_270})
    public @interface SurfaceRotation {}

    public enum HintPhase { HIDDEN, SWIPE_UP_HINT, EXPAND, TRANSITION }

    private static final int CARD_HEIGHT_DP = 56, CORNER_RADIUS_DP = 28, ICON_SIZE_DP = 24;
    private static final int ICON_PADDING_DP = 16, TEXT_SIZE_SP = 14, CARD_MARGIN_DP = 8;
    private static final int ANIM_DURATION_MS = 250;

    private final Paint mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mCardRect = new RectF();
    private final String mSwipeUpText;
    private final Rect mTaskBounds = new Rect();

    private final float mCardHeight, mInnerCornerRadius, mIconSize, mIconPadding;
    private final int mCardMargin;
    private final float taskViewCornerRadius;

    private final float mInnerPadding;

    @SurfaceRotation
    private int mRotation = ROTATION_0;
    private HintPhase mPhase = HintPhase.HIDDEN;
    private boolean mIsVisible, mHasTaskBounds;
    private float mHintAlpha = 0f, mScale = 0.85f, mExpandProgress = 0f;
    private String mDisplayText;
    private float mContentAlpha;

    private ValueAnimator mProgressAnimator;
    private AnimatorSet mVisibilityAnimator;
    private final int[] mPosTmp = new int[2];

    private ImageView mIconView;
	private Bitmap mTaskBitmap;
	private final Rect mTransitionFromRect = new Rect(), mTransitionToRect = new Rect();
	private float mTransitionSrcCornerRadius, mTransitionDstCornerRadius, mCurrentTransitionCornerRadius;
	private Runnable mTransitionOnEnd;
	private float mTransitionProgress = 0f;
	private int[] mTransitionStartMargins = new int[2];
	private int[] mTransitionStartSize = new int[2];
	
    public FreeformHintView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setWillNotDraw(false);
        Resources res = context.getResources();
        float density = res.getDisplayMetrics().density;

        mCardHeight = CARD_HEIGHT_DP * density;
        mInnerCornerRadius = CORNER_RADIUS_DP * density;
        mIconSize = ICON_SIZE_DP * density;
        mIconPadding = ICON_PADDING_DP * density;
        mCardMargin = (int) (CARD_MARGIN_DP * density);
        mInnerPadding = 4 * density;
        taskViewCornerRadius = TaskCornerRadius.get(context);

        mBgPaint.setColor(Themes.getColorAccent(getContext()));
        mBgPaint.setStyle(Paint.Style.FILL);

        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTextSize(TEXT_SIZE_SP * density);
        mTextPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        mSwipeUpText = context.getString(R.string.avium_gesture_swipe_up_hint);

        mIconView = new ImageView(context);
        mIconView.setImageResource(R.drawable.desktop_mode_ic_taskbar_menu_manage_windows);
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
        // Unlock mPhase in onEnd
        if (mPhase == HintPhase.TRANSITION) {
            Log.d(TAG, "setPhase: blocking " + phase + ", transition still running");
            return;
        }
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
                    adjustProgressAnimation(1f, null);
                }
                break;

            case TRANSITION:
                startTransitionAnimation();
                break;

        }
    }

    public void setDisplayRotation(@SurfaceRotation int rotation) {
        if (mRotation == rotation) return;
        mRotation = rotation;
        mIconView.setRotation(mRotation*90f);
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
            requestLayout();
        }
    }

    /**
     * Initialize layout params and position after being added to DragLayer.
     * Call once after the view is attached to its parent (DragLayer).
     */
    public void initLayout() {
        // Reuse existing LayoutParams from DragLayer (InsettableFrameLayout.LayoutParams)
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        if (lp == null) {
            lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        lp.gravity = Gravity.TOP | Gravity.START;
        setLayoutParams(lp);
        mIsVisible = true;
        setVisibility(View.VISIBLE);
        setAlpha(1f);
        applyContentAlpha();

        post(() -> {
            if (getParent() != null) {
                updatePositionAndSize();
            }
        });
    }


    /**
     * Plays the transition animation from task thumbnail to pinned window position.
     */
    public void playTransitionToPinned(Bitmap bitmap, Rect fromRect, Rect toRect,
            float dstCornerRadiusPx, Runnable onEnd) {
        Log.d(TAG, "playTransitionToPinned: bitmap=" + (bitmap != null) + " from=" + fromRect + " to=" + toRect + " radius=" + dstCornerRadiusPx);
        fromRect = new Rect(fromRect.left - (int) mCardMargin, fromRect.top - (int) mCardMargin, fromRect.right + (int) mCardMargin, fromRect.bottom + (int) mCardMargin);
        mTaskBitmap = bitmap;
        mTransitionFromRect.set(fromRect);
        mTransitionToRect.set(toRect);
        float density = getResources().getDisplayMetrics().density;
        mTransitionSrcCornerRadius = 28f * density;
        mTransitionDstCornerRadius = dstCornerRadiusPx;
        mCurrentTransitionCornerRadius = mTransitionSrcCornerRadius;
        mTransitionOnEnd = onEnd;
        mContentAlpha = 0f;
        applyContentAlpha();
        mIconView.setAlpha(0f);
        mIconView.setVisibility(View.INVISIBLE);

        // Position and size the view to match the source rect
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        if (lp == null) {
            lp = new FrameLayout.LayoutParams(fromRect.width(), fromRect.height());
        }
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.leftMargin = fromRect.left;
        lp.topMargin = fromRect.top;
        lp.width = fromRect.width();
        lp.height = fromRect.height();
        setLayoutParams(lp);
        setScaleX(1f);
        setScaleY(1f);
        setAlpha(1f);

        Log.d(TAG, "playTransitionToPinned: calling setPhase(TRANSITION)");
        setPhase(HintPhase.TRANSITION);
    }

    private void startTransitionAnimation() {
        Log.d(TAG, "startTransitionAnimation: from " + mTransitionFromRect + " to " + mTransitionToRect);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
        if (lp != null) {
            mTransitionStartMargins[0] = lp.leftMargin;
            mTransitionStartMargins[1] = lp.topMargin;
            mTransitionStartSize[0] = lp.width;
            mTransitionStartSize[1] = lp.height;
        } else {
            mTransitionStartMargins[0] = mTransitionFromRect.left;
            mTransitionStartMargins[1] = mTransitionFromRect.top;
            mTransitionStartSize[0] = mTransitionFromRect.width();
            mTransitionStartSize[1] = mTransitionFromRect.height();
        }
        mTransitionProgress = 0f;
        adjustTransitionAnimation(1f);
    }

    private void adjustTransitionAnimation(float target) {
        if (mProgressAnimator != null) mProgressAnimator.cancel();
        mProgressAnimator = ValueAnimator.ofFloat(mTransitionProgress, target);
        mProgressAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        mProgressAnimator.setDuration(350);
        mProgressAnimator.addUpdateListener(a -> {
            mTransitionProgress = (float) a.getAnimatedValue();
            updatePositionAndSize();
            invalidate();
        });
        mProgressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mTaskBitmap = null;
                if (mTransitionOnEnd != null) {
                    mTransitionOnEnd.run();
                    mTransitionOnEnd = null;
                    initLayout();
                    mPhase = HintPhase.HIDDEN;
                    mIconView.setVisibility(View.VISIBLE);
                }
            }
        });
        mProgressAnimator.start();
    }

    private void adjustProgressAnimation(float target, Runnable onEnd) {
        if (mProgressAnimator != null) mProgressAnimator.cancel();
        mProgressAnimator = ValueAnimator.ofFloat(mExpandProgress, target);
        mProgressAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        mProgressAnimator.addUpdateListener(a -> {
            mExpandProgress = (float) a.getAnimatedValue();
            updatePositionAndSize();
            applyContentAlpha();
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
        mIsVisible = true;// Here must init once.
        mHintAlpha = 1f;
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
                requestLayout();
            }
        });
        mVisibilityAnimator = set;
        set.start();
    }

    private void applyContentAlpha() {
        mContentAlpha = (mPhase == HintPhase.EXPAND)
                ? Math.max(0f, 1f - mExpandProgress)
                : 1f;

        // Icon – handled by the child ImageView
        if (mIconView != null) {
            int iconAlpha = (int) (255 * mHintAlpha * mContentAlpha);
            mIconView.setAlpha(iconAlpha / 255f);  // setAlpha expects 0..1
        }

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

        // TRANSITION: interpolate size between start and toRect
        if (mPhase == HintPhase.TRANSITION) {
            if (mTransitionProgress > 0f) {
                int mw = (int) (mTransitionStartSize[0] + (mTransitionToRect.width() - mTransitionStartSize[0] + 2 * mCardMargin) * mTransitionProgress);
                int mh = (int) (mTransitionStartSize[1] + (mTransitionToRect.height() - mTransitionStartSize[1] + 2 * mCardMargin) * mTransitionProgress);
                setMeasuredDimension(Math.max(mw, 1), Math.max(mh, 1));
            } else {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
                if (lp != null && lp.width > 0 && lp.height > 0) {
                    setMeasuredDimension(lp.width, lp.height);
                } else {
                    setMeasuredDimension(0, 0);
                }
            }
            return;
        }

        int hintW, hintH;
        if (mRotation==ROTATION_90||mRotation==ROTATION_270) {
            hintH = computeHintCardWidth();
            hintW = (int) mCardHeight;
        } else {
            hintW = computeHintCardWidth();
            hintH = (int) mCardHeight;
        }

		int measuredW = hintW;
		int measuredH = hintH;
        int taskW = mTaskBounds.width() + mCardMargin * 2;
        int taskH = mTaskBounds.height() + mCardMargin * 2;

		if (mHasTaskBounds && mExpandProgress > 0f) {
			measuredW = (int) (hintW + (taskW - hintW) * mExpandProgress);
			measuredH = (int) (hintH + (taskH - hintH) * mExpandProgress);
		}

        setMeasuredDimension(measuredW, measuredH);

        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
	}

    private void updatePositionAndSize() {
        // TRANSITION: interpolate toward toRect via mTransitionProgress
        if (mPhase == HintPhase.TRANSITION) {
            int l = (int) (mTransitionStartMargins[0] + (mTransitionToRect.left - mTransitionStartMargins[0]) * mTransitionProgress);
            int t = (int) (mTransitionStartMargins[1] + (mTransitionToRect.top - mTransitionStartMargins[1]) * mTransitionProgress);
            int cw = (int) (mTransitionStartSize[0] + (mTransitionToRect.width() - mTransitionStartSize[0]) * mTransitionProgress);
            int ch = (int) (mTransitionStartSize[1] + (mTransitionToRect.height() - mTransitionStartSize[1]) * mTransitionProgress);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) getLayoutParams();
            if (lp != null) {
                lp.gravity = Gravity.TOP | Gravity.START;
                lp.leftMargin = l;
                lp.topMargin = t;
                lp.width = Math.max(cw, 1);
                lp.height = Math.max(ch, 1);
                setLayoutParams(lp);
            }
            invalidate();
            return;
        }

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
            float startCx = mPosTmp[0] + hintW / 2f;
            float startCy = mPosTmp[1] + hintH / 2f;
            float curCx = startCx + (mTaskBounds.centerX() - startCx) * mExpandProgress;
            float curCy = startCy + (mTaskBounds.centerY() - startCy) * mExpandProgress;
            l = (int) (curCx - cw / 2f);
            t = (int) (curCy - ch / 2f);
        } else {
            cw = hintW;
            ch = hintH;
            getSmallCardPos(pw, ph, cw, ch, mPosTmp);
            l = mPosTmp[0];
            t = mPosTmp[1];
        }

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
            case ROTATION_270:
                out[0] = mCardMargin + sbH;
                out[1] = mCardMargin;
                break;
            case ROTATION_90:
                out[0] = pw - ch - mCardMargin - sbH;
                out[1] = ph - cw - mCardMargin;
                break;
            case ROTATION_180:
                out[0] = pw - ch - mCardMargin;
                out[1] = ph - ch - mCardMargin - nbH;
                break;
            default: // ROTATION_0
                out[0] = pw - cw - mCardMargin;
                out[1] = mCardMargin + sbH;
                break;
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (mHintAlpha <= 0.01f || !mIsVisible) {
            return;
        }

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        if (mPhase == HintPhase.TRANSITION && mTaskBitmap != null) {
            canvas.save();
            float cr = mTransitionSrcCornerRadius + (mTransitionDstCornerRadius - mTransitionSrcCornerRadius) * mTransitionProgress;
            RectF cardRect = new RectF(0, 0, w, h);
            Path clip = new Path();
            clip.addRoundRect(cardRect, cr, cr, Path.Direction.CW);
            canvas.clipPath(clip);
            mBgPaint.setAlpha(255);
            canvas.drawRoundRect(cardRect, cr, cr, mBgPaint);

            int bmLeft = (int) (mCardMargin * (1 - mTransitionProgress));
            int bmTop = (int) (mCardMargin * (1 - mTransitionProgress));
            int bmRight = w - bmLeft;
            int bmBottom = h - bmTop;
            float bmCr = Math.max(0, cr - mCardMargin * (1 - mTransitionProgress));
            RectF bmRect = new RectF(bmLeft, bmTop, bmRight, bmBottom);
            Path bmClip = new Path();
            bmClip.addRoundRect(bmRect, bmCr, bmCr, Path.Direction.CW);
            canvas.clipPath(bmClip);
            canvas.drawBitmap(mTaskBitmap, null, bmRect, null);
            canvas.restore();
            return;
        }

        // Background opacity
        int bgAlpha = (int) (255 * mHintAlpha);
        float mCornerRadius = mInnerCornerRadius - (mInnerCornerRadius - taskViewCornerRadius) * mExpandProgress + mInnerPadding;
        mBgPaint.setAlpha(bgAlpha);
        mCardRect.set(0, 0, w, h);
        canvas.drawRoundRect(mCardRect, mCornerRadius, mCornerRadius, mBgPaint);
        canvas.save();

		if (mContentAlpha > 0f && mDisplayText != null) {
			canvas.save();
			
			int textAlpha = (int) (255 * mHintAlpha * mContentAlpha);
			mTextPaint.setAlpha(textAlpha);
			float logicalW = (mRotation == ROTATION_90 || mRotation == ROTATION_270) ? h : w;
			float logicalH = (mRotation == ROTATION_90 || mRotation == ROTATION_270) ? w : h;

			// icon 处理旋转太麻烦了，干脆直接让文字避让一下。手性异构（
			float tx = (mRotation==ROTATION_270 ? 0 : (mIconSize)) + mInnerPadding + mIconPadding;
			float ty = logicalH / 2f - (mTextPaint.descent() + mTextPaint.ascent()) / 2f;

			canvas.rotate(mRotation * 90f, w / 2f, h / 2f);

			if (mRotation == ROTATION_90 || mRotation == ROTATION_270) {
				canvas.translate((w - logicalW) / 2f, (h - logicalH) / 2f);
			}

			canvas.drawText(mDisplayText, tx, ty, mTextPaint);
			canvas.restore();
		}
    }
}