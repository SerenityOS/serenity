/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include <windows.h>
#include "MyCanvas.h"
#include "jawt_md.h"

/*
 * Class:     MyCanvas
 * Method:    paint
 * Signature: (Ljava/awt/Graphics;)V
 */

extern "C" {

JNIEXPORT void JNICALL Java_MyCanvas_paint
(JNIEnv* env, jobject canvas, jobject graphics)
{
    /* Get the AWT */
    JAWT awt;
    awt.version = JAWT_VERSION_1_4;
    if (JAWT_GetAWT(env, &awt) == JNI_FALSE) {
        printf("AWT Not found\n");
        return;
    }

    /* Lock the AWT */
    awt.Lock(env);

    /* Unlock the AWT */
    awt.Unlock(env);

    /* Get the drawing surface */
    JAWT_DrawingSurface* ds = awt.GetDrawingSurface(env, canvas);
    if (ds == NULL) {
        printf("NULL drawing surface\n");
        return;
    }

    /* Lock the drawing surface */
    jint lock = ds->Lock(ds);
    printf("Lock value %d\n", (int)lock);
    if((lock & JAWT_LOCK_ERROR) != 0) {
        printf("Error locking surface\n");
        return;
    }

    /* Get the drawing surface info */
    JAWT_DrawingSurfaceInfo* dsi = ds->GetDrawingSurfaceInfo(ds);
    if (dsi == NULL) {
        printf("Error getting surface info\n");
        ds->Unlock(ds);
        return;
    }

    /* Get the platform-specific drawing info */
    JAWT_Win32DrawingSurfaceInfo* dsi_win =
        (JAWT_Win32DrawingSurfaceInfo*)dsi->platformInfo;

    /* Now paint */
    PAINTSTRUCT ps;
    /* Do not use the HDC returned from BeginPaint()!! */
    ::BeginPaint(dsi_win->hwnd, &ps);
    HBRUSH hbrush = (HBRUSH)::GetStockObject(BLACK_BRUSH);
    RECT rect;
    rect.left = 5;
    rect.top = 5;
    rect.right = 95;
    rect.bottom = 95;
    ::FillRect(dsi_win->hdc, &rect, hbrush);
    ::EndPaint(dsi_win->hwnd, &ps);

    jobject ref = awt.GetComponent(env, (void*)(dsi_win->hwnd));
    if (!env->IsSameObject(ref, canvas)) {
        printf("Error! Different objects!\n");
    }

    /* Free the drawing surface info */
    ds->FreeDrawingSurfaceInfo(dsi);

    /* Unlock the drawing surface */
    ds->Unlock(ds);

    /* Free the drawing surface */
    awt.FreeDrawingSurface(ds);
}

}
