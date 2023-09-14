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
import java.util.concurrent.Executors

const val MSG_SET_RESOURCE_ROOT = 1
const val MSG_TRANSFER_SOCKETS = 2

class WebContentService : Service() {
    private val TAG = "WebContentService"

    private val threadPool = Executors.newCachedThreadPool()
    private lateinit var resourceDir: String

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
        createThread(ipcSocket, fdSocket)
    }

    private fun handleSetResourceRoot(msg: Message) {
        // FIXME: Handle this being already set, not being present, etc
        resourceDir = msg.data.getString("PATH")!!

        initNativeCode(resourceDir, TAG)
    }

    internal class IncomingHandler(
        context: WebContentService,
        private val owner: WebContentService = context
    ) : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_TRANSFER_SOCKETS -> this.owner.handleTransferSockets(msg)
                MSG_SET_RESOURCE_ROOT -> this.owner.handleSetResourceRoot(msg)
                else -> super.handleMessage(msg)
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        // FIXME: Check the intent to make sure it's legit
        return Messenger(IncomingHandler(this)).binder
    }

    private external fun nativeThreadLoop(ipcSocket: Int, fdPassingSocket: Int)

    private fun createThread(ipcSocket: ParcelFileDescriptor, fdSocket: ParcelFileDescriptor) {
        threadPool.execute {
            nativeThreadLoop(ipcSocket.detachFd(), fdSocket.detachFd())
        }
    }

    private external fun initNativeCode(resourceDir: String, tagName: String);

    companion object {
        init {
            System.loadLibrary("webcontent")
        }
    }
}
