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

import android.graphics.Rect;

import com.android.launcher3.Utilities;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;

import java.util.List;

public final class AviumLargeFolderGrid {

    static final float BACKGROUND_WIDTH_FACTOR = 0.98f;
    static final float BACKGROUND_HEIGHT_FACTOR = 0.96f;
    static final float BACKGROUND_RADIUS_FACTOR = 0.18f;
    static final float GRID_PADDING_FACTOR = 0.02f;
    static final float GRID_ICON_SIZE_FACTOR = 1.00f;

    public static boolean isItemVisibleOnWorkspace(FolderInfo info, int rank) {
        return AviumLargeFolderManager.isLargeFolder(info) && rank >= 0
                && rank < getDirectLaunchItemCount(info, info.getContents().size());
    }

    public static void getIconBounds(FolderIcon folderIcon, Rect outBounds) {
        int availableWidth = Math.max(0,
                folderIcon.getWidth() - folderIcon.getPaddingLeft() - folderIcon.getPaddingRight());
        int availableHeight = Math.max(0,
                folderIcon.getHeight() - folderIcon.getPaddingTop() - folderIcon.getPaddingBottom());
        int width = Math.round(availableWidth * BACKGROUND_WIDTH_FACTOR);
        int height = Math.round(availableHeight * BACKGROUND_HEIGHT_FACTOR);
        int left = folderIcon.getPaddingLeft() + (availableWidth - width) / 2;
        int top = folderIcon.getPaddingTop() + (availableHeight - height) / 2;
        outBounds.set(left, top, left + width, top + height);
    }

    public static float getGridIconSize(FolderIcon folderIcon) {
        Rect bounds = new Rect();
        getIconBounds(folderIcon, bounds);
        return Math.min(
                getGridCellWidth(bounds, getWorkspaceGridColumnCount(folderIcon.mInfo)),
                getGridCellHeight(bounds, getWorkspaceGridRowCount(folderIcon.mInfo)))
                * GRID_ICON_SIZE_FACTOR;
    }

    public static void getGridIconTopLeft(FolderIcon folderIcon, int index, Rect outBounds) {
        Rect bounds = new Rect();
        getIconBounds(folderIcon, bounds);
        float paddingX = bounds.width() * GRID_PADDING_FACTOR;
        float paddingY = bounds.height() * GRID_PADDING_FACTOR;
        int columnCount = getWorkspaceGridColumnCount(folderIcon.mInfo);
        int rowCount = getWorkspaceGridRowCount(folderIcon.mInfo);
        float cellWidth = getGridCellWidth(bounds, columnCount);
        float cellHeight = getGridCellHeight(bounds, rowCount);
        float iconSize = Math.min(cellWidth, cellHeight) * GRID_ICON_SIZE_FACTOR;
        int row = index / columnCount;
        int col = Utilities.isRtl(folderIcon.getResources())
                ? columnCount - 1 - index % columnCount : index % columnCount;
        int left = Math.round(bounds.left + paddingX + col * cellWidth
                + (cellWidth - iconSize) / 2f);
        int top = Math.round(bounds.top + paddingY + row * cellHeight
                + (cellHeight - iconSize) / 2f);
        outBounds.set(left, top, Math.round(left + iconSize), Math.round(top + iconSize));
    }

    static ItemInfo getDirectLaunchItem(FolderIcon folderIcon, float x, float y) {
        List<ItemInfo> contents = folderIcon.mInfo.getContents();
        int directCount = getDirectLaunchItemCount(folderIcon.mInfo, contents.size());
        int index = getGridIndexAt(folderIcon, x, y);
        if (index >= 0 && index < directCount) {
            Rect iconBounds = new Rect();
            getGridIconTopLeft(folderIcon, index, iconBounds);
            if (!iconBounds.contains((int) x, (int) y)) {
                return null;
            }
            return contents.get(index);
        }
        return null;
    }

    static int getDirectLaunchItemCount(FolderInfo folderInfo, int itemCount) {
        int capacity = getWorkspaceGridCapacity(folderInfo);
        return itemCount > capacity ? capacity - 1 : Math.min(itemCount, capacity);
    }

    static int getWorkspaceGridCapacity(FolderInfo folderInfo) {
        return Math.max(2, getWorkspaceGridColumnCount(folderInfo)
                * getWorkspaceGridRowCount(folderInfo));
    }

    private static int getGridIndexAt(FolderIcon folderIcon, float x, float y) {
        Rect bounds = new Rect();
        getIconBounds(folderIcon, bounds);
        if (!bounds.contains((int) x, (int) y)) {
            return -1;
        }
        float paddingX = bounds.width() * GRID_PADDING_FACTOR;
        float paddingY = bounds.height() * GRID_PADDING_FACTOR;
        float gridLeft = bounds.left + paddingX;
        float gridTop = bounds.top + paddingY;
        int columnCount = getWorkspaceGridColumnCount(folderIcon.mInfo);
        int rowCount = getWorkspaceGridRowCount(folderIcon.mInfo);
        int col = (int) ((x - gridLeft) / getGridCellWidth(bounds, columnCount));
        int row = (int) ((y - gridTop) / getGridCellHeight(bounds, rowCount));
        if (row < 0 || row >= rowCount || col < 0 || col >= columnCount) {
            return -1;
        }
        if (Utilities.isRtl(folderIcon.getResources())) {
            col = columnCount - 1 - col;
        }
        return row * columnCount + col;
    }

    private static float getGridCellWidth(Rect bounds, int columnCount) {
        float padding = bounds.width() * GRID_PADDING_FACTOR;
        return (bounds.width() - padding * 2f) / columnCount;
    }

    private static float getGridCellHeight(Rect bounds, int rowCount) {
        float padding = bounds.height() * GRID_PADDING_FACTOR;
        return (bounds.height() - padding * 2f) / rowCount;
    }

    private static int getWorkspaceGridColumnCount(FolderInfo folderInfo) {
        int spanX = folderInfo == null ? AviumLargeFolderManager.LARGE_FOLDER_SPAN
                : Math.max(1, folderInfo.spanX);
        return spanX == 1 ? 1 : Math.max(1, Math.round(spanX * 1.5f));
    }

    private static int getWorkspaceGridRowCount(FolderInfo folderInfo) {
        int spanY = folderInfo == null ? AviumLargeFolderManager.LARGE_FOLDER_SPAN
                : Math.max(1, folderInfo.spanY);
        return Math.max(1, spanY * 2);
    }

    private AviumLargeFolderGrid() {
    }
}
