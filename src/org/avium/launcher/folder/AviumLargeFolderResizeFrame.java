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

import static com.android.launcher3.AbstractFloatingView.TYPE_ACTION_POPUP;
import static com.android.launcher3.AbstractFloatingView.TYPE_WIDGET_RESIZE_FRAME;

import android.content.Context;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.celllayout.CellLayoutLayoutParams;
import com.android.launcher3.dragndrop.DragController.DragListener;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.model.data.FolderInfo;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.BaseDragLayer;
import com.android.launcher3.DropTarget.DragObject;

public class AviumLargeFolderResizeFrame extends AbstractFloatingView
        implements View.OnKeyListener, DragListener {

    private static final float RESIZE_THRESHOLD = 0.66f;
    private static final float VISIBLE_ALPHA = 1f;
    private static final float DIMMED_ALPHA = 0f;

    private static final int DIRECTION_HORIZONTAL_INDEX = 0;
    private static final int DIRECTION_VERTICAL_INDEX = 1;
    private static final int DIRECTION_NONE = 0;
    private static final int DIRECTION_LEFT = -1;
    private static final int DIRECTION_TOP = -1;
    private static final int DIRECTION_RIGHT = 1;
    private static final int DIRECTION_BOTTOM = 1;

    private final Launcher mLauncher;
    private final Rect mTempRect = new Rect();
    private final IntRange mTempRange1 = new IntRange();
    private final IntRange mTempRange2 = new IntRange();
    private final IntRange mDeltaXRange = new IntRange();
    private final IntRange mDeltaYRange = new IntRange();
    private final IntRange mBaselineXRange = new IntRange();
    private final IntRange mBaselineYRange = new IntRange();
    private final int[] mDirectionVector = new int[2];
    private final int[] mLastDirectionVector = new int[2];
    private final int mBackgroundPadding;
    private final int mTouchTargetWidth;

    private FolderIcon mFolderIcon;
    private FolderInfo mFolderInfo;
    private CellLayout mCellLayout;
    private DragLayer mDragLayer;
    private View mLeftHandle;
    private View mTopHandle;
    private View mRightHandle;
    private View mBottomHandle;

    private boolean mLeftBorderActive;
    private boolean mRightBorderActive;
    private boolean mTopBorderActive;
    private boolean mBottomBorderActive;
    private int mDeltaX;
    private int mDeltaY;
    private int mDeltaXAddOn;
    private int mDeltaYAddOn;
    private int mRunningHInc;
    private int mRunningVInc;
    private int mXDown;
    private int mYDown;
    private int mOriginalCellX;
    private int mOriginalCellY;
    private int mOriginalSpanX;
    private int mOriginalSpanY;
    private boolean mIsResizing;

    public AviumLargeFolderResizeFrame(Context context) {
        super(context, null);
        mLauncher = Launcher.getLauncher(context);
        mBackgroundPadding = getResources().getDimensionPixelSize(
                R.dimen.resize_frame_background_padding);
        mTouchTargetWidth = 2 * mBackgroundPadding;
        setWillNotDraw(false);
        setFocusableInTouchMode(true);
        setOnKeyListener(this);
        setLayoutParams(new BaseDragLayer.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        mLauncher.getDragController().addDragListener(this);
        buildContent();
    }

    public static void showForFolder(@Nullable View view, @Nullable CellLayout cellLayout) {
        if (!(view instanceof FolderIcon folderIcon)
                || !AviumLargeFolderManager.isLargeFolder(folderIcon.mInfo)
                || cellLayout == null
                || folderIcon.getParent() == null) {
            return;
        }

        Launcher launcher = Launcher.getLauncher(folderIcon.getContext());
        DragLayer dragLayer = launcher.getDragLayer();
        AbstractFloatingView.closeAllOpenViewsExcept(
                launcher, true /* animate */, TYPE_ACTION_POPUP);

        AviumLargeFolderResizeFrame frame = new AviumLargeFolderResizeFrame(launcher);
        frame.setupForFolder(folderIcon, cellLayout, dragLayer);
        frame.setTag(folderIcon.getTag());
        frame.setAccessibilityDelegate(launcher.getAccessibilityDelegate());
        frame.setContentDescription(launcher.getString(
                R.string.widget_frame_name, folderIcon.mInfo.contentDescription));
        ((BaseDragLayer.LayoutParams) frame.getLayoutParams()).customPosition = true;
        dragLayer.addView(frame);
        frame.mIsOpen = true;
        frame.post(() -> frame.snapToFolder());
    }

    private void buildContent() {
        FrameLayout content = new FrameLayout(getContext());
        addView(content, new LayoutParams(MATCH_PARENT, MATCH_PARENT));

        ImageView frame = new ImageView(getContext());
        frame.setId(R.id.widget_resize_frame);
        frame.setImageResource(R.drawable.widget_resize_frame);
        FrameLayout.LayoutParams frameLp =
                new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, Gravity.CENTER);
        int frameMargin = getResources().getDimensionPixelSize(R.dimen.resize_frame_margin);
        frameLp.setMargins(frameMargin, frameMargin, frameMargin, frameMargin);
        content.addView(frame, frameLp);

        mLeftHandle = addHandle(content, Gravity.LEFT | Gravity.CENTER_VERTICAL);
        mTopHandle = addHandle(content, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        mRightHandle = addHandle(content, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        mBottomHandle = addHandle(content, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
    }

    private View addHandle(FrameLayout parent, int gravity) {
        ImageView handle = new ImageView(getContext());
        handle.setImageResource(R.drawable.ic_widget_resize_handle);
        handle.setColorFilter(Themes.getAttrColor(getContext(), R.attr.workspaceAccentColor));
        int margin = getResources().getDimensionPixelSize(R.dimen.widget_handle_margin);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, gravity);
        lp.setMargins(margin, margin, margin, margin);
        parent.addView(handle, lp);
        return handle;
    }

    private void setupForFolder(FolderIcon folderIcon, CellLayout cellLayout, DragLayer dragLayer) {
        mFolderIcon = folderIcon;
        mFolderInfo = folderIcon.mInfo;
        mCellLayout = cellLayout;
        mDragLayer = dragLayer;
        if (folderIcon.getLayoutParams() instanceof CellLayoutLayoutParams lp) {
            mOriginalCellX = lp.getCellX();
            mOriginalCellY = lp.getCellY();
            mOriginalSpanX = lp.cellHSpan;
            mOriginalSpanY = lp.cellVSpan;
            lp.setTmpCellX(lp.getCellX());
            lp.setTmpCellY(lp.getCellY());
            lp.useTmpCoords = true;
        }
        mCellLayout.markCellsAsUnoccupiedForView(mFolderIcon);
    }

    private boolean beginResizeIfPointInRegion(int x, int y) {
        mLeftBorderActive = x < mTouchTargetWidth;
        mRightBorderActive = x > getWidth() - mTouchTargetWidth;
        mTopBorderActive = y < mTouchTargetWidth;
        mBottomBorderActive = y > getHeight() - mTouchTargetWidth;

        boolean anyBorderActive = mLeftBorderActive || mRightBorderActive
                || mTopBorderActive || mBottomBorderActive;
        if (!anyBorderActive) {
            return false;
        }

        mLeftHandle.setAlpha(mLeftBorderActive ? VISIBLE_ALPHA : DIMMED_ALPHA);
        mRightHandle.setAlpha(mRightBorderActive ? VISIBLE_ALPHA : DIMMED_ALPHA);
        mTopHandle.setAlpha(mTopBorderActive ? VISIBLE_ALPHA : DIMMED_ALPHA);
        mBottomHandle.setAlpha(mBottomBorderActive ? VISIBLE_ALPHA : DIMMED_ALPHA);

        if (mLeftBorderActive) {
            mDeltaXRange.set(-getLeft(), getWidth() - 2 * mTouchTargetWidth);
        } else if (mRightBorderActive) {
            mDeltaXRange.set(2 * mTouchTargetWidth - getWidth(),
                    mDragLayer.getWidth() - getRight());
        } else {
            mDeltaXRange.reset();
        }
        mBaselineXRange.set(getLeft(), getRight());

        if (mTopBorderActive) {
            mDeltaYRange.set(-getTop(), getHeight() - 2 * mTouchTargetWidth);
        } else if (mBottomBorderActive) {
            mDeltaYRange.set(2 * mTouchTargetWidth - getHeight(),
                    mDragLayer.getHeight() - getBottom());
        } else {
            mDeltaYRange.reset();
        }
        mBaselineYRange.set(getTop(), getBottom());
        return true;
    }

    private void visualizeResizeForDelta(int deltaX, int deltaY) {
        mDeltaX = mDeltaXRange.clamp(deltaX);
        mDeltaY = mDeltaYRange.clamp(deltaY);
        BaseDragLayer.LayoutParams lp = (BaseDragLayer.LayoutParams) getLayoutParams();

        mBaselineXRange.applyDelta(mLeftBorderActive, mRightBorderActive, mDeltaX, mTempRange1);
        lp.x = mTempRange1.start;
        lp.width = mTempRange1.size();

        mBaselineYRange.applyDelta(mTopBorderActive, mBottomBorderActive, mDeltaY, mTempRange1);
        lp.y = mTempRange1.start;
        lp.height = mTempRange1.size();

        resizeFolderIfNeeded(false /* onDismiss */);
        requestLayout();
    }

    private boolean resizeFolderIfNeeded(boolean onDismiss) {
        if (!(mFolderIcon.getLayoutParams() instanceof CellLayoutLayoutParams lp)) {
            return false;
        }
        int xThreshold = Math.max(1, mCellLayout.getCellWidth());
        int yThreshold = Math.max(1, mCellLayout.getCellHeight());
        int hSpanInc = getSpanIncrement((mDeltaX + mDeltaXAddOn) / (float) xThreshold
                - mRunningHInc);
        int vSpanInc = getSpanIncrement((mDeltaY + mDeltaYAddOn) / (float) yThreshold
                - mRunningVInc);

        if (!onDismiss && hSpanInc == 0 && vSpanInc == 0) {
            return false;
        }

        mDirectionVector[DIRECTION_HORIZONTAL_INDEX] = DIRECTION_NONE;
        mDirectionVector[DIRECTION_VERTICAL_INDEX] = DIRECTION_NONE;

        int cellX = lp.useTmpCoords ? lp.getTmpCellX() : lp.getCellX();
        int cellY = lp.useTmpCoords ? lp.getTmpCellY() : lp.getCellY();
        int spanX = lp.cellHSpan;
        int spanY = lp.cellVSpan;

        mTempRange1.set(cellX, cellX + spanX);
        int hSpanDelta = mTempRange1.applyDeltaAndBound(mLeftBorderActive, mRightBorderActive,
                hSpanInc, AviumLargeFolderManager.MIN_LARGE_FOLDER_SPAN, mCellLayout.getCountX(),
                mCellLayout.getCountX(), mTempRange2);
        cellX = mTempRange2.start;
        spanX = mTempRange2.size();
        if (hSpanDelta != 0) {
            mDirectionVector[DIRECTION_HORIZONTAL_INDEX] =
                    mLeftBorderActive ? DIRECTION_LEFT : DIRECTION_RIGHT;
        }

        mTempRange1.set(cellY, cellY + spanY);
        int hMaxSpan = Math.max(AviumLargeFolderManager.MIN_LARGE_FOLDER_SPAN,
                mCellLayout.getCountY() - 1);
        int vSpanDelta = mTempRange1.applyDeltaAndBound(mTopBorderActive, mBottomBorderActive,
                vSpanInc, AviumLargeFolderManager.MIN_LARGE_FOLDER_SPAN, hMaxSpan,
                mCellLayout.getCountY(), mTempRange2);
        cellY = mTempRange2.start;
        spanY = mTempRange2.size();
        if (vSpanDelta != 0) {
            mDirectionVector[DIRECTION_VERTICAL_INDEX] =
                    mTopBorderActive ? DIRECTION_TOP : DIRECTION_BOTTOM;
        }

        if (spanX == 1 && spanY == 1) {
            if (mRightBorderActive || mLeftBorderActive) {
                spanX = 2;
            } else {
                spanY = 2;
            }
        }
        if (!onDismiss && hSpanDelta == 0 && vSpanDelta == 0) {
            return false;
        }

        if (onDismiss) {
            mDirectionVector[DIRECTION_HORIZONTAL_INDEX] =
                    mLastDirectionVector[DIRECTION_HORIZONTAL_INDEX];
            mDirectionVector[DIRECTION_VERTICAL_INDEX] =
                    mLastDirectionVector[DIRECTION_VERTICAL_INDEX];
        } else {
            mLastDirectionVector[DIRECTION_HORIZONTAL_INDEX] =
                    mDirectionVector[DIRECTION_HORIZONTAL_INDEX];
            mLastDirectionVector[DIRECTION_VERTICAL_INDEX] =
                    mDirectionVector[DIRECTION_VERTICAL_INDEX];
        }

        if (mCellLayout.createAreaForAviumLargeFolderResize(cellX, cellY, spanX, spanY,
                mFolderIcon, mDirectionVector, onDismiss)) {
            lp.setTmpCellX(cellX);
            lp.setTmpCellY(cellY);
            lp.cellHSpan = spanX;
            lp.cellVSpan = spanY;
            if (onDismiss) {
                lp.setCellX(cellX);
                lp.setCellY(cellY);
                lp.useTmpCoords = false;
                mFolderInfo.cellX = cellX;
                mFolderInfo.cellY = cellY;
                mFolderInfo.spanX = spanX;
                mFolderInfo.spanY = spanY;
                mLauncher.getModelWriter().updateItemInDatabase(mFolderInfo);
            }
            mRunningHInc += hSpanDelta;
            mRunningVInc += vSpanDelta;
            AviumLargeFolderManager.syncIconState(mFolderIcon);
            mFolderIcon.requestLayout();
            mFolderIcon.invalidate();
            return true;
        }
        return false;
    }

    private void onTouchUp() {
        mDeltaXAddOn = mRunningHInc * Math.max(1, mCellLayout.getCellWidth());
        mDeltaYAddOn = mRunningVInc * Math.max(1, mCellLayout.getCellHeight());
        mDeltaX = 0;
        mDeltaY = 0;
        post(this::snapToFolder);
    }

    private void snapToFolder() {
        mDragLayer.getViewRectRelativeToSelf(mFolderIcon, mTempRect);
        BaseDragLayer.LayoutParams lp = (BaseDragLayer.LayoutParams) getLayoutParams();
        lp.width = mTempRect.width() + 2 * mBackgroundPadding;
        lp.height = mTempRect.height() + 2 * mBackgroundPadding;
        lp.x = mTempRect.left - mBackgroundPadding;
        lp.y = mTempRect.top - mBackgroundPadding;
        mLeftHandle.setAlpha(VISIBLE_ALPHA);
        mRightHandle.setAlpha(VISIBLE_ALPHA);
        mTopHandle.setAlpha(VISIBLE_ALPHA);
        mBottomHandle.setAlpha(VISIBLE_ALPHA);
        requestLayout();
        requestFocus();
    }

    private boolean handleTouchDown(MotionEvent ev) {
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        getHitRect(mTempRect);
        if (mTempRect.contains(x, y) && beginResizeIfPointInRegion(x - getLeft(), y - getTop())) {
            mXDown = x;
            mYDown = y;
            mIsResizing = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean onControllerTouchEvent(MotionEvent ev) {
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return handleTouchDown(ev);
            case MotionEvent.ACTION_MOVE:
                if (!mIsResizing) {
                    return false;
                }
                AbstractFloatingView.closeOpenViews(mLauncher, true, TYPE_ACTION_POPUP);
                visualizeResizeForDelta(x - mXDown, y - mYDown);
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (!mIsResizing) {
                    return false;
                }
                visualizeResizeForDelta(x - mXDown, y - mYDown);
                onTouchUp();
                mXDown = 0;
                mYDown = 0;
                mIsResizing = false;
                return true;
        }
        return true;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() != MotionEvent.ACTION_DOWN) {
            return mIsResizing;
        }
        if (handleTouchDown(ev)) {
            return true;
        }
        close(false /* animate */);
        return false;
    }

    @Override
    protected void handleClose(boolean animate) {
        if (!mIsOpen) {
            return;
        }
        mLauncher.getDragController().removeDragListener(this);
        if (!resizeFolderIfNeeded(true /* onDismiss */)) {
            restoreOriginalLayout();
        }
        mCellLayout.markCellsAsOccupiedForView(mFolderIcon);
        mDragLayer.removeView(this);
        mIsOpen = false;
    }

    @Override
    protected boolean isOfType(@FloatingViewType int type) {
        return (type & TYPE_WIDGET_RESIZE_FRAME) != 0;
    }

    @Override
    public void onDragStart(DragObject dragObject, DragOptions options) {
        close(true /* animate */);
    }

    @Override
    public void onDragEnd() {
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mLauncher.getDragController().removeDragListener(this);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                || keyCode == KeyEvent.KEYCODE_DPAD_UP
                || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_MOVE_HOME
                || keyCode == KeyEvent.KEYCODE_MOVE_END
                || keyCode == KeyEvent.KEYCODE_PAGE_UP
                || keyCode == KeyEvent.KEYCODE_PAGE_DOWN) {
            close(false /* animate */);
            mFolderIcon.requestFocus();
            return true;
        }
        return false;
    }

    private static int getSpanIncrement(float deltaFrac) {
        return Math.abs(deltaFrac) > RESIZE_THRESHOLD ? Math.round(deltaFrac) : 0;
    }

    private void restoreOriginalLayout() {
        if (!(mFolderIcon.getLayoutParams() instanceof CellLayoutLayoutParams lp)) {
            return;
        }
        lp.setCellX(mOriginalCellX);
        lp.setCellY(mOriginalCellY);
        lp.setTmpCellX(mOriginalCellX);
        lp.setTmpCellY(mOriginalCellY);
        lp.cellHSpan = mOriginalSpanX;
        lp.cellVSpan = mOriginalSpanY;
        lp.useTmpCoords = false;
        mFolderInfo.cellX = mOriginalCellX;
        mFolderInfo.cellY = mOriginalCellY;
        mFolderInfo.spanX = mOriginalSpanX;
        mFolderInfo.spanY = mOriginalSpanY;
        mFolderIcon.requestLayout();
    }

    private static final class IntRange {
        int start;
        int end;

        int clamp(int value) {
            return Utilities.boundToRange(value, start, end);
        }

        void reset() {
            set(0, 0);
        }

        void set(int start, int end) {
            this.start = start;
            this.end = end;
        }

        int size() {
            return end - start;
        }

        void applyDelta(boolean moveStart, boolean moveEnd, int delta, IntRange out) {
            out.start = moveStart ? start + delta : start;
            out.end = moveEnd ? end + delta : end;
        }

        int applyDeltaAndBound(boolean moveStart, boolean moveEnd, int delta, int minSize,
                int maxSize, int maxEnd, IntRange out) {
            applyDelta(moveStart, moveEnd, delta, out);
            out.start = Math.max(0, out.start);
            out.end = Math.min(maxEnd, out.end);
            if (out.size() < minSize) {
                if (moveStart) {
                    out.start = out.end - minSize;
                } else if (moveEnd) {
                    out.end = out.start + minSize;
                }
            }
            if (out.size() > maxSize) {
                if (moveStart) {
                    out.start = out.end - maxSize;
                } else if (moveEnd) {
                    out.end = out.start + maxSize;
                }
            }
            return moveEnd ? out.size() - size() : size() - out.size();
        }
    }
}
