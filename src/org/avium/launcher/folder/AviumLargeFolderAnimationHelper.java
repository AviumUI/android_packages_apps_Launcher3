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

import android.view.View;

import androidx.annotation.Nullable;

import com.android.launcher3.folder.Folder;

import java.util.List;

public final class AviumLargeFolderAnimationHelper {

    @Nullable
    public static List<View> getPreviewIconsOnPage(Folder folder, int page) {
        if (!AviumLargeFolderManager.isLargeFolder(folder.mInfo) || page != 0) {
            return null;
        }
        List<View> items = folder.getItemsOnPage(0);
        int max = Math.min(items.size(), folder.getItemCount() > 9 ? 8 : 9);
        return items.subList(0, max);
    }

    private AviumLargeFolderAnimationHelper() {
    }
}
