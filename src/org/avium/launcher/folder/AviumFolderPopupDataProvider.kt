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

package org.avium.launcher.folder

import com.android.launcher3.model.data.FolderInfo
import com.android.launcher3.model.data.ItemInfo
import com.android.launcher3.popup.PopupData
import com.android.launcher3.popup.PopupDataSource

object AviumFolderPopupDataProvider {
    fun getPopupData(popupDataSource: PopupDataSource, itemInfo: ItemInfo): List<PopupData> {
        val folderInfo = itemInfo as FolderInfo
        return listOf(
            if (AviumLargeFolderManager.isLargeFolder(folderInfo)) {
                popupDataSource.folderShrinkPopupData
            } else {
                popupDataSource.folderEnlargePopupData
            },
            popupDataSource.folderEditPopupData,
        )
    }
}
