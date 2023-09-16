/**
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

package org.serenityos.ladybird

import android.content.Context
import android.content.Intent
import android.os.Message
import android.util.Log

class WebContentService : LadybirdServiceBase("WebContentService") {
    override fun handleServiceSpecificMessage(msg: Message): Boolean {
        return false
    }

    init {
        nativeInit();
    }

    private fun bindRequestServer(ipcFd: Int, fdPassingFd: Int)
    {
        val connector = LadybirdServiceConnection(ipcFd, fdPassingFd, resourceDir)
        connector.onDisconnect = {
            // FIXME: Notify impl that service is dead and might need restarted
            Log.e(TAG, "RequestServer Died! :(")
        }
        // FIXME: Unbind this at some point maybe
        bindService(
            Intent(this, RequestServerService::class.java),
            connector,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun bindWebSocket(ipcFd: Int, fdPassingFd: Int)
    {
        val connector = LadybirdServiceConnection(ipcFd, fdPassingFd, resourceDir)
        connector.onDisconnect = {
            // FIXME: Notify impl that service is dead and might need restarted
            Log.e(TAG, "RequestServer Died! :(")
        }
        // FIXME: Unbind this at some point maybe
        bindService(
            Intent(this, WebSocketService::class.java),
            connector,
            Context.BIND_AUTO_CREATE
        )
    }

    external fun nativeInit()

    companion object {
        init {
            System.loadLibrary("webcontent")
        }
    }
}
