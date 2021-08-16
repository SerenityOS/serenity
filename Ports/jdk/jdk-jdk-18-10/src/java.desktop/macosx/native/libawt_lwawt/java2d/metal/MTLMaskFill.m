/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "sun_java2d_metal_MTLMaskFill.h"

#include "MTLMaskFill.h"
#include "MTLRenderQueue.h"
#include "MTLVertexCache.h"

/**
 * In case of Metal we use shader for texture mapping.
 *
 * Descriptions of the many variables used in this method:
 *   x,y     - upper left corner of the tile destination
 *   w,h     - width/height of the mask tile
 *   x0      - placekeeper for the original destination x location
 *   tw,th   - width/height of the actual texture tile in pixels
 *   sx1,sy1 - upper left corner of the mask tile source region
 *   sx2,sy2 - lower left corner of the mask tile source region
 *   sx,sy   - "current" upper left corner of the mask tile region of interest
 */
void
MTLMaskFill_MaskFill(MTLContext *mtlc, BMTLSDOps * dstOps,
                     jint x, jint y, jint w, jint h,
                     jint maskoff, jint maskscan, jint masklen,
                     unsigned char *pMask)
{
    J2dTraceLn5(J2D_TRACE_INFO, "MTLMaskFill_MaskFill (x=%d y=%d w=%d h=%d pMask=%p)", x, y, w, h, dstOps->pTexture);
    jint tw, th, x0;
    jint sx1, sy1, sx2, sy2;
    jint sx, sy, sw, sh;

    x0 = x;
    tw = MTLVC_MASK_CACHE_TILE_WIDTH;
    th = MTLVC_MASK_CACHE_TILE_HEIGHT;
    sx1 = maskoff % maskscan;
    sy1 = maskoff / maskscan;
    sx2 = sx1 + w;
    sy2 = sy1 + h;


    for (sy = sy1; sy < sy2; sy += th, y += th) {
        x = x0;
        sh = ((sy + th) > sy2) ? (sy2 - sy) : th;

        for (sx = sx1; sx < sx2; sx += tw, x += tw) {
            sw = ((sx + tw) > sx2) ? (sx2 - sx) : tw;
            MTLVertexCache_AddMaskQuad(mtlc,
                    sx, sy, x, y, sw, sh,
                    maskscan, pMask, dstOps);
        }
    }
}

JNIEXPORT void JNICALL
Java_sun_java2d_metal_MTLMaskFill_maskFill
    (JNIEnv *env, jobject self,
     jint x, jint y, jint w, jint h,
     jint maskoff, jint maskscan, jint masklen,
     jbyteArray maskArray)
{
    MTLContext *mtlc = MTLRenderQueue_GetCurrentContext();
    BMTLSDOps *dstOps = MTLRenderQueue_GetCurrentDestination();
    unsigned char *mask;

    J2dTraceLn(J2D_TRACE_ERROR, "MTLMaskFill_maskFill");

    if (maskArray != NULL) {
        mask = (unsigned char *)
            (*env)->GetPrimitiveArrayCritical(env, maskArray, NULL);
    } else {
        mask = NULL;
    }

    MTLMaskFill_MaskFill(mtlc, dstOps,
                         x, y, w, h,
                         maskoff, maskscan, masklen, mask);
    if (mtlc != NULL) {
        RESET_PREVIOUS_OP();
        [mtlc.encoderManager endEncoder];
        MTLCommandBufferWrapper * cbwrapper = [mtlc pullCommandBufferWrapper];
        id<MTLCommandBuffer> commandbuf = [cbwrapper getCommandBuffer];
        [commandbuf addCompletedHandler:^(id <MTLCommandBuffer> commandbuf) {
            [cbwrapper release];
        }];
        [commandbuf commit];
    }

    if (mask != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, maskArray, mask, JNI_ABORT);
    }
}
