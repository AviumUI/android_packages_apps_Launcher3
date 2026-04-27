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

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.content.res.Resources;

import com.android.launcher3.R;
import com.android.launcher3.util.MultiPropertyFactory.MultiProperty;
import com.android.launcher3.views.ActivityContext;

import org.avium.launcher.FolderDepthController;

public final class AviumFolderDepthAnimator {

    private static final float OPEN_FOLDER_DEPTH = 0.3f;

    public static void addAnimator(
            AnimatorSet animatorSet, ActivityContext activityContext, Resources resources,
            boolean isOpening) {
        FolderDepthController folderDepthController = getFolderDepthController(activityContext);
        if (folderDepthController == null) {
            return;
        }
        folderDepthController.animateFolderBlur(isOpening);
        MultiProperty folderDepth = folderDepthController.getFolderDepthProperty();
        Animator depthAnimator = folderDepth.animateToValue(isOpening ? OPEN_FOLDER_DEPTH : 0f);
        depthAnimator.setDuration(resources.getInteger(
                R.integer.config_materialFolderExpandDuration));
        animatorSet.play(depthAnimator);
    }

    public static void setDepth(ActivityContext activityContext, float depth) {
        FolderDepthController folderDepthController = getFolderDepthController(activityContext);
        if (folderDepthController == null) {
            return;
        }
        folderDepthController.animateFolderBlur(depth > 0f);
        MultiProperty folderDepth = folderDepthController.getFolderDepthProperty();
        folderDepth.setValue(depth);
    }

    private static FolderDepthController getFolderDepthController(ActivityContext activityContext) {
        if (activityContext instanceof FolderDepthController controller) {
            return controller;
        }
        return null;
    }

    private AviumFolderDepthAnimator() {
    }
}
