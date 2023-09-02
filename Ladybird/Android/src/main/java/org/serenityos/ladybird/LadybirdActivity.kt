/**
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

package org.serenityos.ladybird

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import org.serenityos.ladybird.databinding.ActivityMainBinding
import org.serenityos.ladybird.TransferAssets

class LadybirdActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var resourceDir: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        resourceDir = TransferAssets.transferAssets(this)
        initNativeCode(resourceDir)
        view = WebViewImplementationNative()
    }

    override fun onDestroy() {
        view.dispose()
        super.onDestroy()
    }

    private lateinit var view: WebViewImplementationNative

    /**
     * A native method that is implemented by the 'ladybird' native library,
     * which is packaged with this application.
     */
    private external fun initNativeCode(resourceDir: String)

    companion object {
        // Used to load the 'ladybird' library on application startup.
        init {
            System.loadLibrary("ladybird")
        }
    }
}
