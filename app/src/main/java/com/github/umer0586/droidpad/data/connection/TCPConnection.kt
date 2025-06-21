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

package com.github.umer0586.droidpad.data.connection

import android.util.Log
import com.github.umer0586.droidpad.data.connectionconfig.TCPConfig
import com.github.umer0586.droidpad.data.database.entities.ConnectionType
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.SocketOptions
import io.ktor.network.sockets.TypeOfService
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class TCPConnection(
    val tcpConfig: TCPConfig,
    // Dispatchers should be injected for making testing easier
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : Connection() {

    private var selectorManager: SelectorManager? = null
    private val TAG: String = "TCPConnection"
    private var socket : Socket? = null
    private var writeChannel: ByteWriteChannel ? = null

    // sendData Coroutine write lock
    // Fixes a NullPointerExcept that occurs when using multiple joysticks at once
    private val writeMutex = Mutex()

    override val connectionType: ConnectionType
        get() = ConnectionType.TCP

    override suspend fun setup() = withContext(ioDispatcher) {

        notifyConnectionState(ConnectionState.TCP_CONNECTING)
        try {

            selectorManager = SelectorManager(ioDispatcher)

            selectorManager?.also{ selectorManager ->

                // Throws TimeoutCancellationException if timeout
                withTimeout(tcpConfig.timeoutSecs*1000L) {
                    socket = aSocket(selectorManager)
                        .tcp()
                        .configure {
                            if (this is SocketOptions.TCPClientSocketOptions) {
                                this.noDelay = false;
                                this.typeOfService = TypeOfService.IPTOS_LOWDELAY
                            }
                        }
                        .connect(tcpConfig.host, tcpConfig.port)
                }

                writeChannel = socket?.openWriteChannel(autoFlush = true)
            }

            notifyConnectionState(ConnectionState.TCP_CONNECTED)

        }catch (e : TimeoutCancellationException){
            notifyConnectionState(ConnectionState.TCP_CONNECTION_TIMEOUT)
        }
        catch (e: Exception) {
            notifyConnectionState(ConnectionState.TCP_CONNECTION_FAILED)
        }



    }

    override suspend fun sendData(data: String) = withContext<Unit>(ioDispatcher){

        try {
            writeMutex.withLock {
                writeChannel?.writeStringUtf8(data)
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendData: $e")
            e.printStackTrace()
            notifyConnectionState(ConnectionState.TCP_ERROR)
        }

    }

    override suspend fun tearDown() = withContext(ioDispatcher) {

        try {
            notifyConnectionState(ConnectionState.TCP_DISCONNECTING)
            socket?.close()
            writeChannel?.flushAndClose()
            selectorManager?.close()
            notifyConnectionState(ConnectionState.TCP_DISCONNECTED)
        } catch (e: Exception) {
            e.printStackTrace()
            notifyConnectionState(ConnectionState.TCP_ERROR)
        }

    }

}