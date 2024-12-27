/*
 *     This file is a part of DroidPad (https://www.github.com/umer0586/DroidPad)
 *     Copyright (C) 2025 Umer Farooq (umerfarooq2383@gmail.com)
 *
 *     DroidPad is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     DroidPad is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with DroidPad. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.github.umer0586.droidpad.ui.components


import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.umer0586.droidpad.data.properties.SliderProperties
import com.github.umer0586.droidpad.ui.theme.DroidPadTheme

@Composable
fun ControlPadSlider(
    modifier: Modifier = Modifier,
    offset: Offset,
    rotation: Float,
    scale: Float,
    transformableState: TransformableState? = null,
    properties: SliderProperties = SliderProperties(),
    showControls: Boolean = true,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    value: Float = 2.5f,
    onValueChange: ((Float) -> Unit)? = null,
    enabled: Boolean = true,

    ){

    ControlPadItemBase(
        modifier = modifier,
        offset = offset,
        rotation = rotation,
        scale = scale,
        transformableState = transformableState,
        showControls = showControls,
        onEditClick = onEditClick,
        onDeleteClick = onDeleteClick,

        ) {
        Slider(
            modifier = Modifier.padding(10.dp).fillMaxWidth(0.5f),
            enabled = enabled,
            value = value,
            valueRange = properties.minValue..properties.maxValue,
            onValueChange = { onValueChange?.invoke(it) },
            colors = SliderDefaults.colors(
                thumbColor = Color(properties.thumbColor),
                activeTrackColor = Color(properties.trackColor),
                disabledThumbColor = Color(properties.thumbColor),
                disabledActiveTrackColor = Color(properties.trackColor),

            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ControlPadSliderItemPreview(){
    DroidPadTheme {
        Box(Modifier.size(200.dp).padding(10.dp)){
            ControlPadSlider(
                modifier = Modifier.align(Alignment.Center),
                offset = Offset.Zero,
                rotation = 0f,
                scale = 1f,
                value = 1.5f
            )

        }
    }
}