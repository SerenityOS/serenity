/**
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

package org.serenityos.ladybird

import android.app.Service
import android.content.Intent
import android.util.Log
import android.os.ParcelFileDescriptor
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger

const val MSG_TRANSFER_SOCKETS = 1

class WebContentService : Service() {
    private val TAG = "WebContentService"

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Creating Service")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Destroying Service")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Start command received")
        return super.onStartCommand(intent, flags, startId)
    }

    private fun handleTransferSockets(msg: Message) {
        val bundle = msg.data
        // FIXME: Handle garbage messages from wierd clients
        val ipcSocket = bundle.getParcelable<ParcelFileDescriptor>("IPC_SOCKET")!!
        val fdSocket = bundle.getParcelable<ParcelFileDescriptor>("FD_PASSING_SOCKET")!!
        nativeHandleTransferSockets(ipcSocket.detachFd(), fdSocket.detachFd())
    }

    private external fun nativeHandleTransferSockets(ipcSocket: Int, fdPassingSocket: Int)

    internal class IncomingHandler(
        context: WebContentService,
        private val owner: WebContentService = context
    ) : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_TRANSFER_SOCKETS -> this.owner.handleTransferSockets(msg)
                else -> super.handleMessage(msg)
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        // FIXME: Check the intent to make sure it's legit
        return Messenger(IncomingHandler(this)).binder
    }

    companion object {
        init {
            System.loadLibrary("webcontent")
        }
    }
}
