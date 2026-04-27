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
import android.graphics.RectF;
import android.os.UserHandle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import com.android.launcher3.Launcher;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.views.FloatingIconView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public final class AviumLargeFolderManager {

    public static final int FLAG_LARGE_FOLDER = 0x00000010;

    public static final int LARGE_FOLDER_SPAN = 2;
    public static final int MIN_LARGE_FOLDER_SPAN = 1;

    private static WeakReference<FolderIcon> sLastLaunchedFolderIcon = new WeakReference<>(null);
    private static String sLastLaunchedPackage;
    private static UserHandle sLastLaunchedUser;
    private static final WeakHashMap<FolderIcon, List<Runnable>> sScheduledRestoreCallbacks =
            new WeakHashMap<>();

    public static boolean isLargeFolder(FolderInfo info) {
        return info != null && info.hasOption(FLAG_LARGE_FOLDER);
    }

    public static boolean isItemVisibleOnWorkspace(FolderInfo info, int rank) {
        return AviumLargeFolderGrid.isItemVisibleOnWorkspace(info, rank);
    }

    public static boolean isLargeFolderIcon(View view) {
        return view instanceof FolderIcon folderIcon && isLargeFolder(folderIcon.mInfo);
    }

    public static void prepareIconForTransition(FolderIcon folderIcon) {
        if (folderIcon == null || !isLargeFolder(folderIcon.mInfo)) {
            return;
        }
        folderIcon.clearLeaveBehindIfExists();
        folderIcon.setVisibility(View.VISIBLE);
        folderIcon.setAlpha(1f);
        folderIcon.setScaleX(1f);
        folderIcon.setScaleY(1f);
        folderIcon.setIconVisible(true);
        syncIconState(folderIcon);
        folderIcon.invalidate();
        ViewParent parent = folderIcon.getParent();
        if (parent instanceof View parentView) {
            parentView.invalidate();
        }
    }

    public static void prepareIconForTransition(View view) {
        if (view instanceof FolderIcon folderIcon) {
            prepareIconForTransition(folderIcon);
        }
    }

    public static void scheduleRestoreAfterTransition(View view) {
        if (!(view instanceof FolderIcon folderIcon) || !isLargeFolder(folderIcon.mInfo)) {
            return;
        }
        cancelScheduledRestore(folderIcon);
        prepareIconForTransition(folderIcon);
        ArrayList<Runnable> callbacks = new ArrayList<>();
        sScheduledRestoreCallbacks.put(folderIcon, callbacks);
        scheduleRestore(folderIcon, callbacks, 0);
        scheduleRestore(folderIcon, callbacks, 50);
        scheduleRestore(folderIcon, callbacks, 150);
        scheduleRestore(folderIcon, callbacks, 300);
        scheduleRestore(folderIcon, callbacks, 600);
        scheduleRestore(folderIcon, callbacks, 1000);
        Runnable clearCallback = () -> {
            prepareIconForTransition(folderIcon);
            clearLastLaunchedFolder(folderIcon);
            sScheduledRestoreCallbacks.remove(folderIcon);
        };
        callbacks.add(clearCallback);
        folderIcon.postDelayed(clearCallback, 1500);
    }

    public static void hideIconForOpenAnimation(FolderIcon folderIcon) {
        if (folderIcon == null || !isLargeFolder(folderIcon.mInfo)) {
            return;
        }
        cancelScheduledRestore(folderIcon);
        clearLastLaunchedFolder(folderIcon);
        folderIcon.clearLeaveBehindIfExists();
        folderIcon.setIconVisible(false);
        syncIconState(folderIcon);
    }

    public static boolean getIconBoundsInDragLayer(
            Launcher launcher, View view, RectF outBounds) {
        if (launcher == null || !isLargeFolderIcon(view)) {
            return false;
        }
        FolderIcon folderIcon = (FolderIcon) view;
        prepareIconForTransition(folderIcon);
        FloatingIconView.getLocationBoundsForView(
                launcher, folderIcon, false /* isOpening */, outBounds, new Rect());
        if (outBounds.width() <= 0 || outBounds.height() <= 0) {
            Rect fallbackBounds = new Rect();
            launcher.getDragLayer().getDescendantRectRelativeToSelf(folderIcon, fallbackBounds);
            outBounds.set(fallbackBounds);
        }
        return true;
    }

    public static void syncIconState(FolderIcon folderIcon) {
        boolean isLarge = isLargeFolder(folderIcon.mInfo);
        folderIcon.setTextVisible(!isLarge);
        folderIcon.invalidate();
    }

    public static boolean handleTouchUp(FolderIcon folderIcon, MotionEvent event) {
        if (!isLargeFolder(folderIcon.mInfo)) {
            return false;
        }
        ItemInfo itemInfo = AviumLargeFolderGrid.getDirectLaunchItem(
                folderIcon, event.getX(), event.getY());
        if (itemInfo instanceof WorkspaceItemInfo workspaceItemInfo) {
            rememberLaunchedFolder(folderIcon, workspaceItemInfo);
            ItemClickHandler.onClickAppShortcut(
                    null, workspaceItemInfo, Launcher.getLauncher(folderIcon.getContext()));
            return true;
        }
        return false;
    }

    public static View resolveClosingTarget(View preferredTarget, String packageName, UserHandle user) {
        if (isLargeFolderIcon(preferredTarget)) {
            return preferredTarget;
        }
        FolderIcon folderIcon = sLastLaunchedFolderIcon.get();
        if (folderIcon == null || !isLargeFolder(folderIcon.mInfo)) {
            return preferredTarget;
        }
        if (packageName == null || user == null || sLastLaunchedPackage == null
                || sLastLaunchedUser == null) {
            return preferredTarget;
        }
        if (!packageName.equals(sLastLaunchedPackage) || !user.equals(sLastLaunchedUser)) {
            return preferredTarget;
        }
        prepareIconForTransition(folderIcon);
        return folderIcon;
    }

    private static void rememberLaunchedFolder(
            FolderIcon folderIcon, WorkspaceItemInfo workspaceItemInfo) {
        sLastLaunchedFolderIcon = new WeakReference<>(folderIcon);
        sLastLaunchedPackage = workspaceItemInfo.getTargetPackage();
        sLastLaunchedUser = workspaceItemInfo.user;
        prepareIconForTransition(folderIcon);
    }

    private static void clearLastLaunchedFolder(FolderIcon folderIcon) {
        if (sLastLaunchedFolderIcon.get() == folderIcon) {
            sLastLaunchedFolderIcon.clear();
            sLastLaunchedPackage = null;
            sLastLaunchedUser = null;
        }
    }

    private static void scheduleRestore(
            FolderIcon folderIcon, ArrayList<Runnable> callbacks, long delayMillis) {
        Runnable callback = () -> prepareIconForTransition(folderIcon);
        callbacks.add(callback);
        if (delayMillis == 0) {
            folderIcon.post(callback);
        } else {
            folderIcon.postDelayed(callback, delayMillis);
        }
    }

    private static void cancelScheduledRestore(FolderIcon folderIcon) {
        List<Runnable> callbacks = sScheduledRestoreCallbacks.remove(folderIcon);
        if (callbacks == null) {
            return;
        }
        for (Runnable callback : callbacks) {
            folderIcon.removeCallbacks(callback);
        }
    }

    public static void getIconBounds(FolderIcon folderIcon, Rect outBounds) {
        AviumLargeFolderGrid.getIconBounds(folderIcon, outBounds);
    }

    public static float getGridIconSize(FolderIcon folderIcon) {
        return AviumLargeFolderGrid.getGridIconSize(folderIcon);
    }

    public static void getGridIconTopLeft(FolderIcon folderIcon, int index, Rect outBounds) {
        AviumLargeFolderGrid.getGridIconTopLeft(folderIcon, index, outBounds);
    }

    private AviumLargeFolderManager() {
    }
}
