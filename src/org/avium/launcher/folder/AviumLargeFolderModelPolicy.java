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

import static com.android.launcher3.LauncherSettings.Favorites.DESKTOP_ICON_FLAG;
import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_DESKTOP;
import static com.android.launcher3.LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;

import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;

public final class AviumLargeFolderModelPolicy {

    public static void applyLoadedSpan(FolderInfo folderInfo, int dbSpanX, int dbSpanY) {
        if (AviumLargeFolderManager.isLargeFolder(folderInfo)) {
            boolean hasStoredLargeSpan = dbSpanX > 1 || dbSpanY > 1;
            folderInfo.spanX = hasStoredLargeSpan
                    ? Math.max(1, dbSpanX)
                    : AviumLargeFolderManager.LARGE_FOLDER_SPAN;
            folderInfo.spanY = hasStoredLargeSpan
                    ? Math.max(1, dbSpanY)
                    : AviumLargeFolderManager.LARGE_FOLDER_SPAN;
            return;
        }
        folderInfo.spanX = 1;
        folderInfo.spanY = 1;
    }

    public static boolean shouldLoadHighResIcon(
            FolderInfo folderInfo, WorkspaceItemInfo itemInfo, boolean isDefaultPreviewItem) {
        return itemInfo.getMatchingLookupFlag().isVisuallyLessThan(DESKTOP_ICON_FLAG)
                && ((itemInfo.itemType == ITEM_TYPE_APPLICATION && isDefaultPreviewItem)
                        || AviumLargeFolderManager.isItemVisibleOnWorkspace(
                                folderInfo, itemInfo.rank));
    }

    public static boolean shrinkIfOutsideGrid(FolderInfo folderInfo, int countX, int countY) {
        if (!AviumLargeFolderManager.isLargeFolder(folderInfo)
                || folderInfo.container != CONTAINER_DESKTOP) {
            return false;
        }
        if (folderInfo.cellX < 0 || folderInfo.cellY < 0
                || folderInfo.cellX + 1 > countX
                || folderInfo.cellY + 1 > countY) {
            return false;
        }
        if (folderInfo.cellX + folderInfo.spanX <= countX
                && folderInfo.cellY + folderInfo.spanY <= countY) {
            return false;
        }
        folderInfo.spanX = 1;
        folderInfo.spanY = 1;
        folderInfo.setOption(AviumLargeFolderManager.FLAG_LARGE_FOLDER, false, null);
        return true;
    }

    private AviumLargeFolderModelPolicy() {
    }
}
