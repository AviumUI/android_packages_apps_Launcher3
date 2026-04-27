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

import com.android.launcher3.Utilities;

public final class AviumFolderLayoutSpec {

    private static final float OPEN_VERTICAL_OFFSET_DP = 48f;
    private static final float TITLE_TEXT_SCALE = 2.00f;
    private static final float TITLE_AREA_HEIGHT_SCALE = 1.75f;

    public static float getTitleTextScale() {
        return TITLE_TEXT_SCALE;
    }

    public static int getOpenVerticalOffsetPx() {
        return Utilities.dpToPx(OPEN_VERTICAL_OFFSET_DP);
    }

    public static int getTitleAreaHeight(int footerHeight) {
        return Math.round(footerHeight * TITLE_AREA_HEIGHT_SCALE);
    }

    public static int getContentAreaTop(int paddingTop, int footerHeight) {
        return paddingTop + getTitleAreaHeight(footerHeight);
    }

    private AviumFolderLayoutSpec() {
    }
}
