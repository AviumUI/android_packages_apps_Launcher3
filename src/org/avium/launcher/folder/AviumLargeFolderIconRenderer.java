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
import static com.android.launcher3.icons.BitmapInfo.FLAG_THEMED;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.apppairs.AppPairIconDrawingParams;
import com.android.launcher3.apppairs.AppPairIconGraphic;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.AppPairInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.util.Themes;

import java.util.IdentityHashMap;
import java.util.List;

public final class AviumLargeFolderIconRenderer {

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect mTempRect = new Rect();
    private final RectF mTempRectF = new RectF();
    private final IdentityHashMap<ItemInfo, Drawable> mIconCache = new IdentityHashMap<>();
    private final IdentityHashMap<ItemInfo, Boolean> mPendingIconLoads = new IdentityHashMap<>();

    public boolean draw(Canvas canvas, FolderIcon folderIcon) {
        if (!AviumLargeFolderManager.isLargeFolder(folderIcon.mInfo)) {
            return false;
        }

        AviumLargeFolderGrid.getIconBounds(folderIcon, mTempRect);
        mTempRectF.set(mTempRect);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Themes.getAttrColor(folderIcon.getContext(), R.attr.folderPreviewColor));
        float radius = Math.min(mTempRect.width(), mTempRect.height())
                * AviumLargeFolderGrid.BACKGROUND_RADIUS_FACTOR;
        canvas.drawRoundRect(mTempRectF, radius, radius, mPaint);

        List<ItemInfo> contents = folderIcon.mInfo.getContents();
        int capacity = AviumLargeFolderGrid.getWorkspaceGridCapacity(folderIcon.mInfo);
        int directCount = AviumLargeFolderGrid.getDirectLaunchItemCount(
                folderIcon.mInfo, contents.size());
        for (int i = 0; i < directCount; i++) {
            drawItem(canvas, folderIcon, contents.get(i), i);
        }
        if (contents.size() > capacity) {
            drawMoreIcon(canvas, folderIcon, capacity - 1);
        }
        return true;
    }

    public void clearIconCache() {
        mIconCache.clear();
        mPendingIconLoads.clear();
    }

    private void drawItem(Canvas canvas, FolderIcon folderIcon, ItemInfo itemInfo, int index) {
        Drawable drawable = getDrawable(folderIcon, itemInfo);
        if (drawable == null) {
            return;
        }
        AviumLargeFolderGrid.getGridIconTopLeft(folderIcon, index, mTempRect);
        drawable.setBounds(mTempRect);
        drawable.draw(canvas);
    }

    private Drawable getDrawable(FolderIcon folderIcon, ItemInfo itemInfo) {
        Drawable drawable = mIconCache.get(itemInfo);
        if (drawable != null) {
            return drawable;
        }
        if (itemInfo instanceof WorkspaceItemInfo workspaceItemInfo) {
            drawable = workspaceItemInfo.newIcon(folderIcon.getContext(), FLAG_THEMED);
            drawable.setCallback(folderIcon);
            if (workspaceItemInfo.getMatchingLookupFlag().isVisuallyLessThan(DESKTOP_ICON_FLAG)) {
                loadHighResIcon(folderIcon, workspaceItemInfo);
                return drawable;
            }
        } else if (itemInfo instanceof AppPairInfo appPairInfo) {
            appPairInfo.fetchHiResIconsIfNeeded(
                    LauncherAppState.getInstance(folderIcon.getContext()).getIconCache());
            AppPairIconDrawingParams params = new AppPairIconDrawingParams(
                    folderIcon.getContext(), com.android.launcher3.BubbleTextView.DISPLAY_FOLDER);
            drawable = AppPairIconGraphic.composeDrawable(appPairInfo, params);
        }
        if (drawable != null) {
            drawable.setCallback(folderIcon);
            mIconCache.put(itemInfo, drawable);
        }
        return drawable;
    }

    private void loadHighResIcon(FolderIcon folderIcon, WorkspaceItemInfo itemInfo) {
        if (mPendingIconLoads.containsKey(itemInfo)) {
            return;
        }
        mPendingIconLoads.put(itemInfo, true);
        LauncherAppState.getInstance(folderIcon.getContext()).getIconCache()
                .updateIconInBackground(updatedInfo -> folderIcon.post(() -> {
                    mPendingIconLoads.remove(updatedInfo);
                    mIconCache.remove(updatedInfo);
                    folderIcon.invalidate();
                }), itemInfo, DESKTOP_ICON_FLAG);
    }

    private void drawMoreIcon(Canvas canvas, FolderIcon folderIcon, int index) {
        AviumLargeFolderGrid.getGridIconTopLeft(folderIcon, index, mTempRect);
        float cell = mTempRect.width();
        float dotRadius = cell * 0.09f;
        float gap = cell * 0.26f;
        float cx = mTempRect.centerX() - gap / 2f;
        float cy = mTempRect.centerY() - gap / 2f;
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(withAlpha(Themes.getAttrColor(
                folderIcon.getContext(), R.attr.workspaceTextColor), 0.48f));
        canvas.drawCircle(cx, cy, dotRadius, mPaint);
        canvas.drawCircle(cx + gap, cy, dotRadius, mPaint);
        canvas.drawCircle(cx, cy + gap, dotRadius, mPaint);
        canvas.drawCircle(cx + gap, cy + gap, dotRadius, mPaint);
    }

    private static int withAlpha(int color, float alpha) {
        return Color.argb(Math.round(Color.alpha(color) * alpha),
                Color.red(color), Color.green(color), Color.blue(color));
    }
}
