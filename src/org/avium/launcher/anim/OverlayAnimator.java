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
package org.avium.launcher.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;

public class OverlayAnimator {

    private static final int FADE_ANIM_DURATION = 180;

    private ValueAnimator mAnimator;

    public void animate(View overlay, boolean show, boolean isLeftEdge, Runnable onHideEnd) {
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }

        float startAlpha = overlay.getAlpha();
        float targetAlpha = show ? 1f : 0f;
        float startTranslationX = show ? (isLeftEdge ? -60f : 60f) : 0f;
        float targetTranslationX = show ? 0f : (isLeftEdge ? -60f : 60f);

        overlay.setAlpha(startAlpha);
        overlay.setTranslationX(startTranslationX);

        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(FADE_ANIM_DURATION);
        mAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        mAnimator.addUpdateListener(anim -> {
            float fraction = anim.getAnimatedFraction();
            float alpha = startAlpha + (targetAlpha - startAlpha) * fraction;
            float translationX = startTranslationX + (targetTranslationX - startTranslationX) * fraction;
            overlay.setAlpha(alpha);
            overlay.setTranslationX(translationX);
        });

        if (!show) {
            mAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    overlay.setVisibility(View.GONE);
                    overlay.setTranslationX(0f);
                    if (onHideEnd != null) {
                        onHideEnd.run();
                    }
                }
            });
        }

        mAnimator.start();
    }

    public void cancel() {
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }
    }
}
