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
import android.view.ViewParent;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.views.ActivityContext;

public final class AviumLargeFolderSizer {

    public static void enlargeFolder(ActivityContext activityContext, ItemInfo itemInfo, View view) {
        AbstractFloatingView.closeAllOpenViews(activityContext);
        if (itemInfo instanceof FolderInfo folderInfo && view instanceof FolderIcon folderIcon) {
            setFolderSize(activityContext, folderIcon, folderInfo,
                    AviumLargeFolderManager.LARGE_FOLDER_SPAN,
                    AviumLargeFolderManager.LARGE_FOLDER_SPAN);
        }
    }

    public static void shrinkFolder(ActivityContext activityContext, ItemInfo itemInfo, View view) {
        AbstractFloatingView.closeAllOpenViews(activityContext);
        if (itemInfo instanceof FolderInfo folderInfo && view instanceof FolderIcon folderIcon) {
            setNormalFolder(activityContext, folderIcon, folderInfo);
        }
    }

    public static boolean setFolderSize(
            ActivityContext activityContext, FolderIcon folderIcon, FolderInfo folderInfo,
            int spanX, int spanY) {
        if (folderInfo == null || folderIcon == null) {
            return false;
        }
        spanX = Math.max(AviumLargeFolderManager.MIN_LARGE_FOLDER_SPAN, spanX);
        spanY = Math.max(AviumLargeFolderManager.MIN_LARGE_FOLDER_SPAN, spanY);
        if (spanX == 1 && spanY == 1) {
            return setNormalFolder(activityContext, folderIcon, folderInfo);
        }
        if (!(folderIcon.getLayoutParams() instanceof CellLayoutLayoutParams lp)) {
            return false;
        }
        CellLayout cellLayout = getParentCellLayout(folderIcon);
        if (cellLayout == null) {
            return false;
        }

        int maxSpanX = Math.max(AviumLargeFolderManager.MIN_LARGE_FOLDER_SPAN,
                cellLayout.getCountX() - lp.getCellX());
        int maxSpanY = Math.max(AviumLargeFolderManager.MIN_LARGE_FOLDER_SPAN,
                cellLayout.getCountY() - lp.getCellY());
        spanX = Math.min(spanX, maxSpanX);
        spanY = Math.min(spanY, maxSpanY);
        if (AviumLargeFolderManager.isLargeFolder(folderInfo)
                && folderInfo.spanX == spanX
                && folderInfo.spanY == spanY) {
            AviumLargeFolderManager.syncIconState(folderIcon);
            return true;
        }

        boolean canResize = lp.getCellX() + spanX <= cellLayout.getCountX()
                && lp.getCellY() + spanY <= cellLayout.getCountY()
                && cellLayout.createAreaForAviumLargeFolder(folderIcon, spanX, spanY);
        if (!canResize) {
            return false;
        }

        lp.cellHSpan = spanX;
        lp.cellVSpan = spanY;
        folderInfo.spanX = spanX;
        folderInfo.spanY = spanY;
        folderInfo.setOption(AviumLargeFolderManager.FLAG_LARGE_FOLDER, true, null);
        AviumLargeFolderManager.syncIconState(folderIcon);
        folderIcon.requestLayout();

        if (activityContext != null) {
            activityContext.getModelWriter().updateItemInDatabase(folderInfo);
        }
        return true;
    }

    public static boolean setNormalFolder(
            ActivityContext activityContext, FolderIcon folderIcon, FolderInfo folderInfo) {
        if (folderInfo == null || folderIcon == null) {
            return false;
        }
        if (!(folderIcon.getLayoutParams() instanceof CellLayoutLayoutParams lp)) {
            return false;
        }
        CellLayout cellLayout = getParentCellLayout(folderIcon);
        if (cellLayout == null) {
            return false;
        }

        cellLayout.markCellsAsUnoccupiedForView(folderIcon);
        lp.cellHSpan = 1;
        lp.cellVSpan = 1;
        folderInfo.spanX = 1;
        folderInfo.spanY = 1;
        folderInfo.setOption(AviumLargeFolderManager.FLAG_LARGE_FOLDER, false, null);
        cellLayout.markCellsAsOccupiedForView(folderIcon);
        AviumLargeFolderManager.syncIconState(folderIcon);
        folderIcon.requestLayout();

        if (activityContext != null) {
            activityContext.getModelWriter().updateItemInDatabase(folderInfo);
        }
        return true;
    }

    private static CellLayout getParentCellLayout(View view) {
        ViewParent parent = view.getParent();
        if (parent instanceof ShortcutAndWidgetContainer container
                && container.getParent() instanceof CellLayout cellLayout) {
            return cellLayout;
        }
        return null;
    }

    private AviumLargeFolderSizer() {
    }
}
