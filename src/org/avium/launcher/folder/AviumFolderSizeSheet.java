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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutAndWidgetContainer;
import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.views.AbstractSlideInView;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.views.BaseDragLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

public class AviumFolderSizeSheet extends AbstractSlideInView<Launcher> {

    private static final int DEFAULT_CLOSE_DURATION = 200;
    private static final int SCRIM_COLOR = 0x66000000;
    private static final int HANDLE_TOUCH_WIDTH_DP = 96;
    private static final int HANDLE_TOUCH_HEIGHT_DP = 40;

    private FolderIcon mFolderIcon;
    private FolderInfo mFolderInfo;
    private FrameLayout mContentContainer;
    private List<SizeOption> mOptions = List.of();
    private int mMaxSpanX = AviumLargeFolderManager.LARGE_FOLDER_SPAN;
    private int mMaxSpanY = AviumLargeFolderManager.LARGE_FOLDER_SPAN;
    private boolean mTouchStartedOnHandle;

    public AviumFolderSizeSheet(Context context) {
        this(context, null);
    }

    public AviumFolderSizeSheet(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AviumFolderSizeSheet(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        setGravity(Gravity.BOTTOM);
        setWillNotDraw(false);
        setLayoutParams(new BaseDragLayer.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        buildContent();
    }

    public static void show(ActivityContext activityContext, ItemInfo itemInfo, View view) {
        AbstractFloatingView.closeAllOpenViews(activityContext);
        if (!(itemInfo instanceof FolderInfo folderInfo) || !(view instanceof FolderIcon folderIcon)) {
            return;
        }
        Launcher launcher = Launcher.getLauncher(view.getContext());
        AviumFolderSizeSheet sheet = new AviumFolderSizeSheet(launcher);
        sheet.populateAndShow(folderIcon, folderInfo);
    }

    private void buildContent() {
        mContentContainer = new FrameLayout(getContext());
        mContent = mContentContainer;
        addView(mContent, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        setContentBackgroundWithParent(createSheetBackground(), mContent);
    }

    private void populateAndShow(FolderIcon folderIcon, FolderInfo folderInfo) {
        mFolderIcon = folderIcon;
        mFolderInfo = folderInfo;
        mOptions = createSizeOptions(folderIcon);
        if (mOptions.isEmpty()) {
            return;
        }

        int currentIndex = findCurrentSizeIndex(folderInfo);
        mContentContainer.removeAllViews();
        mContentContainer.addView(createContentView(currentIndex),
                new FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        attachToContainer();
        mIsOpen = true;
        setUpDefaultOpenAnimation().start();
    }

    private View createContentView(int selectedIndex) {
        int[] spanXs = new int[mOptions.size()];
        int[] spanYs = new int[mOptions.size()];
        for (int i = 0; i < mOptions.size(); i++) {
            SizeOption option = mOptions.get(i);
            spanXs[i] = option.spanX;
            spanYs[i] = option.spanY;
        }
        IntPredicate onSelected = this::applySelection;
        return AviumFolderSizeSheetComposeBridge.createContentView(
                getContext(), spanXs, spanYs, selectedIndex, mMaxSpanX, mMaxSpanY, onSelected);
    }

    private List<SizeOption> createSizeOptions(FolderIcon folderIcon) {
        ArrayList<SizeOption> options = new ArrayList<>();
        CellLayout cellLayout = getParentCellLayout(folderIcon);
        int maxSpanX = AviumLargeFolderManager.LARGE_FOLDER_SPAN;
        int maxSpanY = AviumLargeFolderManager.LARGE_FOLDER_SPAN;
        int cellX = 0;
        int cellY = 0;
        if (cellLayout != null
                && folderIcon.getLayoutParams() instanceof CellLayoutLayoutParams lp) {
            cellX = lp.getCellX();
            cellY = lp.getCellY();
            maxSpanX = Math.max(1, cellLayout.getCountX() - cellX);
            maxSpanY = Math.max(1, cellLayout.getCountY() - cellY);
        }
        mMaxSpanX = maxSpanX;
        mMaxSpanY = maxSpanY;
        for (int spanY = 1; spanY <= maxSpanY; spanY++) {
            for (int spanX = 1; spanX <= maxSpanX; spanX++) {
                if (spanX == 1 && spanY == 1) {
                    continue;
                }
                options.add(new SizeOption(spanX, spanY));
            }
        }
        options.sort((left, right) -> {
            int leftArea = left.spanX * left.spanY;
            int rightArea = right.spanX * right.spanY;
            if (leftArea != rightArea) {
                return Integer.compare(leftArea, rightArea);
            }
            if (left.spanY != right.spanY) {
                return Integer.compare(left.spanY, right.spanY);
            }
            return Integer.compare(left.spanX, right.spanX);
        });
        return options;
    }

    private int findCurrentSizeIndex(FolderInfo folderInfo) {
        int fallback = 0;
        for (int i = 0; i < mOptions.size(); i++) {
            SizeOption option = mOptions.get(i);
            if (option.spanX == AviumLargeFolderManager.LARGE_FOLDER_SPAN
                    && option.spanY == AviumLargeFolderManager.LARGE_FOLDER_SPAN) {
                fallback = i;
            }
            if (option.spanX == folderInfo.spanX && option.spanY == folderInfo.spanY) {
                return i;
            }
        }
        return fallback;
    }

    private boolean applySelection(int index) {
        if (mOptions.isEmpty() || mFolderIcon == null || mFolderInfo == null) {
            return false;
        }
        SizeOption option = mOptions.get(Math.max(0, Math.min(index, mOptions.size() - 1)));
        return AviumLargeFolderSizer.setFolderSize(
                mActivityContext, mFolderIcon, mFolderInfo, option.spanX, option.spanY);
    }

    private GradientDrawable createSheetBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(getContext().getColor(R.color.materialColorSurfaceContainer));
        float radius = dp(28);
        drawable.setCornerRadii(new float[] {radius, radius, radius, radius, 0, 0, 0, 0});
        return drawable;
    }

    @Nullable
    private CellLayout getParentCellLayout(View view) {
        if (view.getParent() instanceof ShortcutAndWidgetContainer container
                && container.getParent() instanceof CellLayout cellLayout) {
            return cellLayout;
        }
        return null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected int getScrimColor(Context context) {
        return SCRIM_COLOR;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mTouchStartedOnHandle = isEventOverDismissHandle(ev);
        }
        if (!mTouchStartedOnHandle && isEventOverContent(ev)) {
            return false;
        }
        return super.onControllerInterceptTouchEvent(ev);
    }

    @Override
    public boolean onControllerTouchEvent(MotionEvent ev) {
        if (!mTouchStartedOnHandle && isEventOverContent(ev)) {
            return false;
        }
        boolean handled = super.onControllerTouchEvent(ev);
        if (ev.getAction() == MotionEvent.ACTION_UP
                || ev.getAction() == MotionEvent.ACTION_CANCEL) {
            mTouchStartedOnHandle = false;
        }
        return handled;
    }

    @Override
    protected void handleClose(boolean animate) {
        handleClose(animate, DEFAULT_CLOSE_DURATION);
    }

    @Override
    protected boolean isOfType(@FloatingViewType int type) {
        return (type & TYPE_OPTIONS_POPUP) != 0;
    }

    private boolean isEventOverDismissHandle(MotionEvent ev) {
        int[] location = new int[2];
        mContent.getLocationInWindow(location);
        float localX = ev.getRawX() - location[0];
        float localY = ev.getRawY() - location[1];
        float handleWidth = dp(HANDLE_TOUCH_WIDTH_DP);
        float handleHeight = dp(HANDLE_TOUCH_HEIGHT_DP);
        float handleLeft = (mContent.getWidth() - handleWidth) / 2f;
        return localX >= handleLeft
                && localX <= handleLeft + handleWidth
                && localY >= 0
                && localY <= handleHeight;
    }

    private static final class SizeOption {
        final int spanX;
        final int spanY;

        SizeOption(int spanX, int spanY) {
            this.spanX = spanX;
            this.spanY = spanY;
        }
    }
}
