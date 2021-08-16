/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

#include "sun_java2d_d3d_D3DMaskFill.h"

#include "D3DMaskFill.h"
#include "D3DRenderQueue.h"

/**
 * This implementation first copies the alpha tile into a texture and then
 * maps that texture to the destination surface.  This approach appears to
 * offer the best performance despite being a two-step process.
 *
 * Here are some descriptions of the many variables used in this method:
 *   x,y     - upper left corner of the tile destination
 *   w,h     - width/height of the mask tile
 *   x0      - placekeeper for the original destination x location
 *   tw,th   - width/height of the actual texture tile in pixels
 *   sx1,sy1 - upper left corner of the mask tile source region
 *   sx2,sy2 - lower left corner of the mask tile source region
 *   sx,sy   - "current" upper left corner of the mask tile region of interest
 */
HRESULT
D3DMaskFill_MaskFill(D3DContext *d3dc,
                     jint x, jint y, jint w, jint h,
                     jint maskoff, jint maskscan, jint masklen,
                     unsigned char *pMask)
{
    HRESULT res = S_OK;

    J2dTraceLn(J2D_TRACE_INFO, "D3DMaskFill_MaskFill");

    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);

    J2dTraceLn4(J2D_TRACE_VERBOSE, "  x=%d y=%d w=%d h=%d", x, y, w, h);
    J2dTraceLn2(J2D_TRACE_VERBOSE, "  maskoff=%d maskscan=%d",
                maskoff, maskscan);

    {
        D3DMaskCache *maskCache = d3dc->GetMaskCache();
        jint tw, th, x0;
        jint sx1, sy1, sx2, sy2;
        jint sx, sy, sw, sh;

        res = d3dc->BeginScene(STATE_MASKOP);
        RETURN_STATUS_IF_FAILED(res);

        x0 = x;
        tw = D3D_MASK_CACHE_TILE_WIDTH;
        th = D3D_MASK_CACHE_TILE_HEIGHT;
        sx1 = maskoff % maskscan;
        sy1 = maskoff / maskscan;
        sx2 = sx1 + w;
        sy2 = sy1 + h;

        for (sy = sy1; sy < sy2; sy += th, y += th) {
            x = x0;
            sh = ((sy + th) > sy2) ? (sy2 - sy) : th;

            for (sx = sx1; sx < sx2; sx += tw, x += tw) {
                sw = ((sx + tw) > sx2) ? (sx2 - sx) : tw;

                res = maskCache->AddMaskQuad(sx, sy, x, y, sw, sh,
                                             maskscan, pMask);
            }
        }
    }
    return res;
}

JNIEXPORT void JNICALL
Java_sun_java2d_d3d_D3DMaskFill_maskFill
    (JNIEnv *env, jobject self,
     jint x, jint y, jint w, jint h,
     jint maskoff, jint maskscan, jint masklen,
     jbyteArray maskArray)
{
    D3DContext *d3dc = D3DRQ_GetCurrentContext();
    unsigned char *mask;

    J2dTraceLn(J2D_TRACE_ERROR, "D3DMaskFill_maskFill");

    if (maskArray != NULL) {
        mask = (unsigned char *)
            env->GetPrimitiveArrayCritical(maskArray, NULL);
    } else {
        mask = NULL;
    }

    D3DMaskFill_MaskFill(d3dc,
                         x, y, w, h,
                         maskoff, maskscan, masklen, mask);

    // reset current state, and ensure rendering is flushed to dest
    if (d3dc != NULL) {
        d3dc->FlushVertexQueue();
    }

    if (mask != NULL) {
        env->ReleasePrimitiveArrayCritical(maskArray, mask, JNI_ABORT);
    }
}
