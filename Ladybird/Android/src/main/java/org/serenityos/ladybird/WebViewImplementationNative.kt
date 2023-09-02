/**
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

package org.serenityos.ladybird

import android.util.Log

/**
 * Wrapper around WebView::ViewImplementation for use by Kotlin
 */
class WebViewImplementationNative {
    // Instance Pointer to native object, very unsafe :)
    private var nativeInstance = nativeObjectInit()

    init {
        Log.d(
            "Ladybird",
            "New WebViewImplementationNative (Kotlin) with nativeInstance ${this.nativeInstance}"
        )
    }

    fun dispose() {
        nativeObjectDispose(nativeInstance)
        nativeInstance = 0
    }

    private external fun nativeObjectInit(): Long
    private external fun nativeObjectDispose(instance: Long)

    companion object {
        /*
         * We use a static class initializer to allow the native code to cache some
         * field offsets. This native function looks up and caches interesting
         * class/field/method IDs. Throws on failure.
         */
        private external fun nativeClassInit()

        init {
            nativeClassInit()
        }
    }
};
