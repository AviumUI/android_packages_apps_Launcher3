/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.launcher3.folder

import android.graphics.Rect
import android.view.View
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.android.launcher3.folder.FolderAnimationSpringBuilderManager.Companion.getBubbleTextView
import com.android.launcher3.folder.FolderAnimationSpringBuilderManager.Companion.getPreviewIconsOnPage
import com.android.launcher3.views.BaseDragLayer
import org.avium.launcher.folder.AviumLargeFolderManager

/** Position and Scale values for animating opened/closed [Folder] */
data class FolderAnimationData(
    /** is folder opening or closing */
    val isOpening: Boolean,
    /** scale to set folder to */
    val startScale: Float,
    /** ratio of folder scale to drag layer scale */
    val folderScale: Float,
    /** x distance to translate folder */
    val xDistance: Float,
    /** y distance to translate folder */
    val yDistance: Float,
    /** change in content area height from scaling */
    val backgroundXDistance: Float,
    val backgroundYDistance: Float,
    val backgroundScaleX: Float,
    val backgroundScaleY: Float,
    /** change in preview background radius from scaling */
    val folderRadiusDifference: Int,
    /** initial scale of folder icon before animation */
    val initialFolderScale: Float,
    /** initial size of preview background before animation */
    val initialFolderSize: Float,
    val initialFolderHeight: Float,
    /** initial x offset of folder content */
    val previewOffsetX: Int,
    /** scaled offset X for folder preview */
    val scaledPreviewOffsetX: Int,
    /** initial y padding of folder content */
    val contentOffsetY: Int,
    val contentTopOffset: Int,
    /** default duration */
    val defaultDuration: Int,
) {

    companion object Factory {
        fun Folder.getAnimationData(isOpening: Boolean): FolderAnimationData {
            /** Calculates all values required for Folder Animators. */
            // Position and Scale values
            val layoutParams = layoutParams as BaseDragLayer.LayoutParams
            val previewBackground = folderIcon.mBackground
            val isAviumLargeFolder = AviumLargeFolderManager.isLargeFolder(mInfo)
            val largeFolderIconBounds = Rect()
            if (isAviumLargeFolder) {
                AviumLargeFolderManager.getIconBounds(folderIcon, largeFolderIconBounds)
            }

            // Get items in Preview and their scaling
            val itemsInPreview: List<View> = getPreviewIconsOnPage(this, 0)
            val previewScale: Float = folderIcon.layoutRule.scaleForItem(itemsInPreview.size, 0)
            val previewSize: Float =
                if (isAviumLargeFolder) {
                    AviumLargeFolderManager.getGridIconSize(folderIcon)
                } else {
                    folderIcon.layoutRule.iconSize * previewScale
                }

            // Get scale and position of FolderIcon relative to DragLayer
            val folderIconWorkspacePosition = Rect()
            val scaleRelativeToDragLayer: Float =
                mActivityContext.dragLayer.getDescendantRectRelativeToSelf(
                    folderIcon,
                    folderIconWorkspacePosition,
                )
            val scaledFolderRadius: Int = previewBackground.scaledRadius
            val baseIconSize: Float = getBubbleTextView(itemsInPreview[0]).iconSize.toFloat()
            val initialFolderSize =
                if (isAviumLargeFolder) {
                    largeFolderIconBounds.width() * scaleRelativeToDragLayer
                } else {
                    (scaledFolderRadius * 2) * scaleRelativeToDragLayer
                }
            val initialFolderHeight =
                if (isAviumLargeFolder) {
                    largeFolderIconBounds.height() * scaleRelativeToDragLayer
                } else {
                    initialFolderSize
                }
            val initialFolderScale = previewSize / baseIconSize * scaleRelativeToDragLayer

            // Get offsets for Previews and Content
            val initialPreviewItemOffsetX =
                if (!isAviumLargeFolder && Utilities.isRtl(context.resources)) {
                    (layoutParams.width * initialFolderScale - initialFolderSize).toInt()
                } else 0
            val contentOffsetX = (content.paddingLeft * initialFolderScale).toInt()
            val contentOffsetY = (content.paddingTop * initialFolderScale).toInt()
            val contentTopOffset = contentAreaTop

            // Get initial position of folder
            val initialX =
                if (isAviumLargeFolder) {
                    folderIconWorkspacePosition.left +
                        Math.round(largeFolderIconBounds.left * scaleRelativeToDragLayer) -
                        contentOffsetX
                } else {
                    ((folderIconWorkspacePosition.left +
                        paddingLeft +
                        Math.round(previewBackground.offsetX * scaleRelativeToDragLayer)) -
                        contentOffsetX -
                        initialPreviewItemOffsetX)
                }
            val initialY =
                if (isAviumLargeFolder) {
                    folderIconWorkspacePosition.top +
                        Math.round(largeFolderIconBounds.top * scaleRelativeToDragLayer) -
                        contentTopOffset -
                        contentOffsetY
                } else {
                    ((folderIconWorkspacePosition.top +
                        paddingTop +
                        Math.round(previewBackground.offsetY * scaleRelativeToDragLayer)) -
                        contentTopOffset -
                        contentOffsetY)
                }
            val backgroundXDistance =
                if (isAviumLargeFolder) {
                    folderIconWorkspacePosition.left +
                        Math.round(largeFolderIconBounds.left * scaleRelativeToDragLayer) -
                        layoutParams.x
                } else {
                    initialX - layoutParams.x
                }
            val backgroundYDistance =
                if (isAviumLargeFolder) {
                    folderIconWorkspacePosition.top +
                        Math.round(largeFolderIconBounds.top * scaleRelativeToDragLayer) -
                        layoutParams.y -
                        contentTopOffset
                } else {
                    initialY - layoutParams.y
                }
            val backgroundScaleX =
                if (isAviumLargeFolder && layoutParams.width > 0) {
                    initialFolderSize / layoutParams.width
                } else {
                    initialFolderScale
                }
            val finalBackgroundHeight = layoutParams.height - contentTopOffset
            val backgroundScaleY =
                if (isAviumLargeFolder && finalBackgroundHeight > 0) {
                    initialFolderHeight / finalBackgroundHeight
                } else {
                    initialFolderScale
                }

            // Get scaled radius of background
            val folderRadiusDifference = previewBackground.scaledRadius - previewBackground.radius
            /**
             * Background can have a scaled radius in drag and drop mode, so we need to add the
             * difference to keep the preview items centered.
             */
            return FolderAnimationData(
                isOpening = isOpening,
                startScale = if (isOpening) initialFolderScale else 1f,
                folderScale = initialFolderScale / scaleRelativeToDragLayer,
                xDistance = (initialX - layoutParams.x).toFloat(),
                yDistance = (initialY - layoutParams.y).toFloat(),
                backgroundXDistance = backgroundXDistance.toFloat(),
                backgroundYDistance = backgroundYDistance.toFloat(),
                backgroundScaleX = backgroundScaleX,
                backgroundScaleY = backgroundScaleY,
                folderRadiusDifference = folderRadiusDifference,
                initialFolderScale = initialFolderScale,
                initialFolderSize = initialFolderSize,
                initialFolderHeight = initialFolderHeight,
                previewOffsetX = initialPreviewItemOffsetX + contentOffsetX,
                scaledPreviewOffsetX =
                    (initialPreviewItemOffsetX / scaleRelativeToDragLayer).toInt() +
                        folderRadiusDifference,
                contentOffsetY = contentOffsetY,
                contentTopOffset = contentTopOffset,
                defaultDuration =
                    content.resources.getInteger(R.integer.config_materialFolderExpandDuration),
            )
        }
    }
}
