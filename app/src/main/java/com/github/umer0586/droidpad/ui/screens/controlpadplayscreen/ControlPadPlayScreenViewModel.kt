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

package com.github.umer0586.droidpad.ui.screens.controlpadplayscreen

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.umer0586.droidpad.data.ButtonEvent
import com.github.umer0586.droidpad.data.DPadEvent
import com.github.umer0586.droidpad.data.JoyStickEvent
import com.github.umer0586.droidpad.data.SliderEvent
import com.github.umer0586.droidpad.data.SwitchEvent
import com.github.umer0586.droidpad.data.connection.BluetoothLEConnection
import com.github.umer0586.droidpad.data.connection.Connection
import com.github.umer0586.droidpad.data.connection.ConnectionFactory
import com.github.umer0586.droidpad.data.connection.ConnectionState
import com.github.umer0586.droidpad.data.connection.Mqttv3Connection
import com.github.umer0586.droidpad.data.connection.Mqttv5Connection
import com.github.umer0586.droidpad.data.connection.TCPConnection
import com.github.umer0586.droidpad.data.connection.UDPConnection
import com.github.umer0586.droidpad.data.connection.WebsocketConnection
import com.github.umer0586.droidpad.data.database.entities.ConnectionType
import com.github.umer0586.droidpad.data.database.entities.ControlPad
import com.github.umer0586.droidpad.data.database.entities.ControlPadItem
import com.github.umer0586.droidpad.data.repositories.ConnectionConfigRepository
import com.github.umer0586.droidpad.data.repositories.ControlPadRepository
import com.github.umer0586.droidpad.data.util.BluetoothUtil
import com.github.umer0586.droidpad.ui.components.DPAD_BUTTON
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class ControlPadPlayScreenState(
    val controlPadItems: List<ControlPadItem> = emptyList(),
    val connectionState: ConnectionState = ConnectionState.NONE,
    val connectionType: ConnectionType = ConnectionType.TCP,
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val controlPadBackgroundColor : Long = Color.Red.value.toLong(),
    val hostAddress: String = "",
    val isBluetoothEnabled: Boolean = false
)
sealed interface ControlPadPlayScreenEvent {
    data object OnConnectClick : ControlPadPlayScreenEvent
    data object OnDisconnectClick : ControlPadPlayScreenEvent
    data class OnSwitchCheckedChange(val id: String, val checked: Boolean) : ControlPadPlayScreenEvent
    data class OnSliderValueChange(val id: String, val value: Float) : ControlPadPlayScreenEvent
    data class OnButtonPress(val id: String) : ControlPadPlayScreenEvent
    data class OnButtonRelease(val id: String) : ControlPadPlayScreenEvent
    data class OnButtonClick(val id: String) : ControlPadPlayScreenEvent
    data class OnDpadButtonPress(val id: String, val dPadButton: DPAD_BUTTON) : ControlPadPlayScreenEvent
    data class OnDpadButtonRelease(val id: String, val dPadButton: DPAD_BUTTON) : ControlPadPlayScreenEvent
    data class OnDpadButtonClick(val id: String, val dPadButton: DPAD_BUTTON) : ControlPadPlayScreenEvent
    data class OnJoyStickMove(val id: String, val x: Float, val y: Float) : ControlPadPlayScreenEvent
    data object OnBackPress : ControlPadPlayScreenEvent
}



@HiltViewModel
class ControlPadPlayScreenViewModel @Inject constructor(
    private val controlPadRepository: ControlPadRepository,
    private val connectionConfigRepository: ConnectionConfigRepository,
    private val connectionFactory: ConnectionFactory,
    private val bluetoothUtil: BluetoothUtil
) : ViewModel() {

    private var _uiState = MutableStateFlow(
        ControlPadPlayScreenState()
    )

    val uiState = _uiState.asStateFlow()

    private var connection: Connection? = null

    init {
        _uiState.update {
            it.copy(isBluetoothEnabled = bluetoothUtil.isBluetoothEnabled())
        }
    }


    fun loadControlPadItemsFor(controlPad: ControlPad) {

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                controlPadItems = controlPadRepository.getControlPadItemsOf(controlPad),
                controlPadBackgroundColor = controlPad.backgroundColor,
            )

            connectionConfigRepository.getConfigForControlPad(controlPad.id)
                ?.also { connectionConfig ->
                    connection = connectionFactory.getConnection(connectionConfig)

                    _uiState.update {
                        it.copy(
                            connectionType = connectionConfig.connectionType,
                            hostAddress = when(connectionConfig.connectionType){
                                ConnectionType.TCP -> (connection as TCPConnection).tcpConfig.address
                                ConnectionType.UDP -> (connection as UDPConnection).udpConfig.address
                                ConnectionType.WEBSOCKET -> (connection as WebsocketConnection).webSocketConfig.address
                                ConnectionType.MQTT_V5 -> (connection as Mqttv5Connection).mqttConfig.brokerAddress
                                ConnectionType.MQTT_V3 -> (connection as Mqttv3Connection).mqttConfig.brokerAddress
                                ConnectionType.BLUETOOTH_LE -> (connection as BluetoothLEConnection).bluetoothDisplayName

                            }
                        )
                    }


                    launch {
                        connection?.connectionState?.collect { connectionState ->
                            Log.d("Play", "Connection state: $connectionState")

                            val isConnecting = when(connectionState){
                                ConnectionState.TCP_CONNECTING -> true
                                ConnectionState.WEBSOCKET_CONNECTING ->true
                                ConnectionState.MQTT_CONNECTING -> true
                                else -> false
                            }


                            val isConnected = when(connectionState){
                                ConnectionState.TCP_CONNECTED -> true
                                ConnectionState.WEBSOCKET_CONNECTED -> true
                                ConnectionState.MQTT_CONNECTED -> true
                                ConnectionState.BLUETOOTH_CLIENT_CONNECTED -> true
                                else -> false
                            }


                            _uiState.update { uiState ->
                                uiState.copy(
                                    isConnecting = isConnecting,
                                    connectionState = connectionState,
                                    isConnected = isConnected
                                )
                            }


                        }
                    }
                }
        }


    }

    fun onEvent(event: ControlPadPlayScreenEvent) {
        when (event) {
            is ControlPadPlayScreenEvent.OnConnectClick -> {

                if(connection?.connectionType == ConnectionType.BLUETOOTH_LE){
                    _uiState.update {
                        it.copy(isBluetoothEnabled = bluetoothUtil.isBluetoothEnabled())
                    }
                    if(!bluetoothUtil.isBluetoothEnabled())
                        return
                }


                viewModelScope.launch {
                    connection?.setup()
                }
            }

            is ControlPadPlayScreenEvent.OnDisconnectClick -> {
                viewModelScope.launch {
                    connection?.tearDown()
                }
            }

            is ControlPadPlayScreenEvent.OnSwitchCheckedChange -> {
                val data = if(connection?.connectionType == ConnectionType.BLUETOOTH_LE)
                    SwitchEvent(id = event.id, state = event.checked).toCSV()
                else
                    SwitchEvent(id = event.id, state = event.checked).toJson()

                viewModelScope.launch {
                    connection?.sendData(data)
                }

            }


            is ControlPadPlayScreenEvent.OnSliderValueChange -> {

                val data = if(connection?.connectionType == ConnectionType.BLUETOOTH_LE)
                    SliderEvent(id = event.id, value = event.value).toCSV()
                else
                    SliderEvent(id = event.id, value = event.value).toJson()

                viewModelScope.launch {
                    connection?.sendData(data)
                }
            }

            is ControlPadPlayScreenEvent.OnButtonClick -> {

                val data = if(connection?.connectionType == ConnectionType.BLUETOOTH_LE)
                    ButtonEvent(id = event.id, state = "CLICK").toCSV()
                else
                    ButtonEvent(id = event.id, state = "CLICK").toJson()

                viewModelScope.launch {
                    connection?.sendData(data)
                }
            }

            is ControlPadPlayScreenEvent.OnBackPress -> {
                viewModelScope.launch {
                    connection?.tearDown()
                }
            }

            is ControlPadPlayScreenEvent.OnButtonPress -> {

                val data = if(connection?.connectionType == ConnectionType.BLUETOOTH_LE)
                    ButtonEvent(id = event.id, state = "PRESS").toCSV()
                else
                    ButtonEvent(id = event.id, state = "PRESS").toJson()

                viewModelScope.launch {
                    connection?.sendData(data)
                }
            }
            is ControlPadPlayScreenEvent.OnButtonRelease -> {
                val data = if(connection?.connectionType == ConnectionType.BLUETOOTH_LE)
                    ButtonEvent(id = event.id, state = "RELEASE").toCSV()
                else
                    ButtonEvent(id = event.id, state = "RELEASE").toJson()

                viewModelScope.launch {
                    connection?.sendData(data)
                }
            }

            is ControlPadPlayScreenEvent.OnDpadButtonClick -> {

                val data = if(connection?.connectionType == ConnectionType.BLUETOOTH_LE)
                    DPadEvent(id = event.id, button = event.dPadButton, state = "CLICK").toCSV()
                else
                    DPadEvent(id = event.id, button = event.dPadButton, state = "CLICK").toJson()

                viewModelScope.launch {
                    connection?.sendData(data)
                }
            }
            is ControlPadPlayScreenEvent.OnDpadButtonPress -> {

                val data = if(connection?.connectionType == ConnectionType.BLUETOOTH_LE)
                    DPadEvent(id = event.id, button = event.dPadButton, state = "PRESS").toCSV()
                else
                    DPadEvent(id = event.id, button = event.dPadButton, state = "PRESS").toJson()


                viewModelScope.launch {
                    connection?.sendData(data)
                }
            }
            is ControlPadPlayScreenEvent.OnDpadButtonRelease -> {

                val data = if(connection?.connectionType == ConnectionType.BLUETOOTH_LE)
                    DPadEvent(id = event.id, button = event.dPadButton, state = "RELEASE").toCSV()
                else
                    DPadEvent(id = event.id, button = event.dPadButton, state = "RELEASE").toJson()

                viewModelScope.launch {
                    connection?.sendData(data)
                }
            }

            is ControlPadPlayScreenEvent.OnJoyStickMove -> {
                val data = if(connection?.connectionType == ConnectionType.BLUETOOTH_LE)
                    JoyStickEvent(id = event.id, x = event.x, y = event.y).toCSV()
                else
                    JoyStickEvent(id = event.id, x = event.x, y = event.y).toJson()

                viewModelScope.launch {
                    connection?.sendData(data)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            connection?.tearDown()
        }
    }

}

