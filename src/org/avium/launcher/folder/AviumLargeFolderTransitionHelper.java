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

package org.avium.launcher.folder;

import android.graphics.RectF;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;

import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;

public final class AviumLargeFolderTransitionHelper {

    private static final float FADE_START_PROGRESS = 0.96f;
    private static final Interpolator FADE_INTERPOLATOR = new AccelerateInterpolator(1.5f);

    private final View mView;
    private boolean mRestoreScheduled;

    public AviumLargeFolderTransitionHelper(View view) {
        mView = view;
    }

    public static boolean getTargetBounds(Launcher launcher, View view, RectF outBounds) {
        return AviumLargeFolderManager.getIconBoundsInDragLayer(launcher, view, outBounds);
    }

    public static float getWindowAlpha(float progress) {
        if (progress < FADE_START_PROGRESS) {
            return 1f;
        }
        return Utilities.mapBoundToRange(
                progress, FADE_START_PROGRESS, 1f, 1f, 0f, FADE_INTERPOLATOR);
    }

    public void prepare() {
        AviumLargeFolderManager.prepareIconForTransition(mView);
    }

    public void restoreOnce() {
        AviumLargeFolderManager.prepareIconForTransition(mView);
        if (mRestoreScheduled) {
            return;
        }
        mRestoreScheduled = true;
        AviumLargeFolderManager.scheduleRestoreAfterTransition(mView);
    }
}
