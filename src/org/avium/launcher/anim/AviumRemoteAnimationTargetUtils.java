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

import android.content.ComponentName;
import android.view.RemoteAnimationTarget;

import androidx.annotation.Nullable;

public final class AviumRemoteAnimationTargetUtils {

    @Nullable
    public static String getPackageName(@Nullable RemoteAnimationTarget runningTaskTarget) {
        if (runningTaskTarget == null || runningTaskTarget.taskInfo == null) {
            return null;
        }

        final ComponentName[] taskInfoActivities = new ComponentName[]{
                runningTaskTarget.taskInfo.baseActivity,
                runningTaskTarget.taskInfo.origActivity,
                runningTaskTarget.taskInfo.realActivity,
                runningTaskTarget.taskInfo.topActivity};

        for (ComponentName component : taskInfoActivities) {
            if (component != null && component.getPackageName() != null) {
                return component.getPackageName();
            }
        }
        return null;
    }

    private AviumRemoteAnimationTargetUtils() {
    }
}
