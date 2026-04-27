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
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
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
    ): View {
        val root =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(context, 20), dp(context, 12), dp(context, 20), dp(context, 24))
            }
        root.addView(
            View(context).apply {
                background =
                    rounded(
                        context,
                        dp(context, 2),
                        context.getColor(R.color.materialColorOutlineVariant),
                    )
            },
            LinearLayout.LayoutParams(dp(context, 32), dp(context, 4)).apply {
                bottomMargin = dp(context, 22)
            },
        )
        root.addView(
            TextView(context).apply {
                setText(R.string.avium_folder_size_title)
                setTextColor(context.getColor(R.color.materialColorOnSurface))
                textSize = 24f
                gravity = Gravity.CENTER
                typeface = Typeface.create(typeface, Typeface.BOLD)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val grid =
            GridLayout(context).apply {
                columnCount = 3
                useDefaultMargins = false
                setPadding(0, dp(context, 20), 0, 0)
            }
        var currentSelection = selectedIndex
        val buttons = mutableListOf<TextView>()
        spanXs.indices.forEach { index ->
            val button =
                TextView(context).apply {
                    text =
                        context.getString(
                            R.string.avium_folder_size_value,
                            spanXs[index],
                            spanYs[index],
                        )
                    gravity = Gravity.CENTER
                    textSize = 16f
                    typeface = Typeface.create(typeface, Typeface.BOLD)
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        if (onSelected.test(index)) {
                            currentSelection = index
                            buttons.forEachIndexed { buttonIndex, view ->
                                styleButton(context, view, buttonIndex == currentSelection)
                            }
                        }
                    }
                }
            buttons += button
            grid.addView(
                button,
                GridLayout.LayoutParams().apply {
                    width = 0
                    height = dp(context, 56)
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(
                        if (index % 3 == 0) 0 else dp(context, 8),
                        if (index > 2) dp(context, 10) else 0,
                        0,
                        0,
                    )
                },
            )
        }
        buttons.forEachIndexed { index, button ->
            styleButton(context, button, index == selectedIndex)
        }
        root.addView(
            grid,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        return root
    }

    private fun styleButton(context: Context, button: TextView, selected: Boolean) {
        button.setTextColor(
            context.getColor(
                if (selected) R.color.materialColorOnPrimaryContainer
                else R.color.materialColorOnSurface
            )
        )
        button.background =
            strokedRounded(
                context,
                dp(context, 18),
                context.getColor(
                    if (selected) R.color.materialColorPrimaryContainer
                    else R.color.materialColorSurfaceContainerHigh
                ),
                context.getColor(
                    if (selected) R.color.materialColorPrimary
                    else R.color.materialColorOutlineVariant
                ),
                dp(context, if (selected) 2 else 1),
            )
    }

    private fun rounded(context: Context, radius: Int, color: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius.toFloat()
            setColor(color)
        }

    private fun strokedRounded(
        context: Context,
        radius: Int,
        fill: Int,
        stroke: Int,
        strokeWidth: Int,
    ): GradientDrawable =
        rounded(context, radius, fill).apply {
            setStroke(strokeWidth, stroke)
        }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
