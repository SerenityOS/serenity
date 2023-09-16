/**
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

package org.serenityos.ladybird

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.serenityos.ladybird.databinding.ActivityMainBinding
import java.net.URL
import kotlin.io.path.Path

class LadybirdActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var resourceDir: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        resourceDir = TransferAssets.transferAssets(this)
        initNativeCode(resourceDir, "Ladybird", timerService)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        view = binding.webView
        view.initialize(resourceDir)

        mainExecutor.execute {
            callNativeEventLoopForever()
        }
    }

    override fun onStart() {
        super.onStart()

        // FIXME: This is not the right place to load the homepage :^)
        val initialURL = URL("https://ladybird.dev")
        view.loadURL(initialURL)
    }

    override fun onDestroy() {
        view.dispose()
        super.onDestroy()
    }

    private lateinit var view: WebView
    private var timerService = TimerExecutorService()

    /**
     * A native method that is implemented by the 'ladybird' native library,
     * which is packaged with this application.
     */
    private external fun initNativeCode(
        resourceDir: String,
        tag: String,
        timerService: TimerExecutorService
    )

    // FIXME: Instead of doing this, can we push a message to the message queue of the java Looper
    //        when an event is pushed to the main thread, and use that to clear out the
    //        Core::ThreadEventQueues?
    private fun callNativeEventLoopForever() {
        execMainEventLoop()
        mainExecutor.execute { callNativeEventLoopForever() }
    }

    private external fun execMainEventLoop();

    companion object {
        // Used to load the 'ladybird' library on application startup.
        init {
            System.loadLibrary("ladybird")
        }
    }
}
