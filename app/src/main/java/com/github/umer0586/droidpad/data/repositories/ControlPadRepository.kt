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

package com.github.umer0586.droidpad.data.repositories

import com.github.umer0586.droidpad.data.database.entities.ControlPad
import com.github.umer0586.droidpad.data.database.entities.ControlPadItem
import kotlinx.coroutines.flow.Flow


interface ControlPadRepository {

    suspend fun getControlPads(): Flow<List<ControlPad>>
    suspend fun getControlPadById(id: Long): ControlPad?
    suspend fun saveControlPad(controlPad: ControlPad): Long
    suspend fun updateControlPad(controlPad: ControlPad)
    suspend fun deleteControlPad(controlPad: ControlPad)
    suspend fun getControlPadItemsOf(controlPad: ControlPad) : List<ControlPadItem>
}
