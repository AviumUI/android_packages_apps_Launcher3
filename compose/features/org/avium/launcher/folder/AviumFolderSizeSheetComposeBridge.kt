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

import android.content.Context
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.launcher3.R
import java.util.function.IntPredicate

object AviumFolderSizeSheetComposeBridge {

    @JvmStatic
    @Suppress("UNUSED_PARAMETER")
    fun createContentView(
        context: Context,
        spanXs: IntArray,
        spanYs: IntArray,
        selectedIndex: Int,
        maxSpanX: Int,
        maxSpanY: Int,
        onSelected: IntPredicate,
    ): View =
        ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    AviumFolderSizeSheetContent(
                        spanXs = spanXs,
                        spanYs = spanYs,
                        selectedIndex = selectedIndex,
                        onSelected = onSelected,
                    )
                }
            }
        }
}

@Composable
private fun AviumFolderSizeSheetContent(
    spanXs: IntArray,
    spanYs: IntArray,
    selectedIndex: Int,
    onSelected: IntPredicate,
) {
    val options =
        remember(spanXs, spanYs) {
            spanXs.indices.map { index -> SizeOption(spanXs[index], spanYs[index]) }
        }
    var currentSelectedIndex by remember(selectedIndex, options) { mutableIntStateOf(selectedIndex) }
    val surface = colorResource(R.color.materialColorSurfaceContainer)
    val onSurface = colorResource(R.color.materialColorOnSurface)
    val primary = colorResource(R.color.materialColorPrimary)
    val primaryContainer = colorResource(R.color.materialColorPrimaryContainer)
    val onPrimaryContainer = colorResource(R.color.materialColorOnPrimaryContainer)
    val surfaceHigh = colorResource(R.color.materialColorSurfaceContainerHigh)
    val outlineVariant = colorResource(R.color.materialColorOutlineVariant)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = surface,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                )
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(outlineVariant)
        )
        Spacer(Modifier.height(22.dp))
        Text(
            text = stringResource(R.string.avium_folder_size_title),
            color = onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = options.size > 12,
        ) {
            itemsIndexed(options) { index, option ->
                val selected = index == currentSelectedIndex
                SizeOptionButton(
                    option = option,
                    selected = selected,
                    colors =
                        SizeOptionColors(
                            onSurface = onSurface,
                            primary = primary,
                            primaryContainer = primaryContainer,
                            onPrimaryContainer = onPrimaryContainer,
                            surfaceHigh = surfaceHigh,
                            outlineVariant = outlineVariant,
                        ),
                    onClick = {
                        if (onSelected.test(index)) {
                            currentSelectedIndex = index
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SizeOptionButton(
    option: SizeOption,
    selected: Boolean,
    colors: SizeOptionColors,
    onClick: () -> Unit,
) {
    val backgroundColor = if (selected) colors.primaryContainer else colors.surfaceHigh
    val borderColor = if (selected) colors.primary else colors.outlineVariant.copy(alpha = 0.72f)
    val textColor = if (selected) colors.onPrimaryContainer else colors.onSurface
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(backgroundColor)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(18.dp),
                )
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.avium_folder_size_value, option.spanX, option.spanY),
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

private data class SizeOption(val spanX: Int, val spanY: Int)

private data class SizeOptionColors(
    val onSurface: Color,
    val primary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val surfaceHigh: Color,
    val outlineVariant: Color,
)
