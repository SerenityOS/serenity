/**
 * Copyright (c) 2023, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

package org.serenityos.ladybird

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

// FIXME: This should (eventually) implement NestedScrollingChild3 and ScrollingView
class WebView(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {
    private val viewImpl = WebViewImplementation(context)
    private lateinit var contentBitmap: Bitmap

    fun dispose() {
        viewImpl.dispose()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        contentBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        // FIXME: Account for scroll offset when view supports scrolling
        viewImpl.setViewportGeometry(w, h)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        viewImpl.drawIntoBitmap(contentBitmap);
        canvas?.drawBitmap(contentBitmap, 0f, 0f, null)
    }
}
