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

#include <stdlib.h>
#include <jlong.h>

#include "MTLMaskBlit.h"
#include "MTLRenderQueue.h"
#include "MTLBlitLoops.h"

/**
 * REMIND: This method assumes that the dimensions of the incoming pixel
 *         array are less than or equal to the cached blit texture tile;
 *         these are rather fragile assumptions, and should be cleaned up...
 */
void
MTLMaskBlit_MaskBlit(JNIEnv *env, MTLContext *mtlc, BMTLSDOps * dstOps,
                     jint dstx, jint dsty,
                     jint width, jint height,
                     void *pPixels)
{
    J2dTraceLn(J2D_TRACE_INFO, "MTLMaskBlit_MaskBlit");

    if (width <= 0 || height <= 0) {
        J2dTraceLn(J2D_TRACE_WARNING, "MTLMaskBlit_MaskBlit: invalid dimensions");
        return;
    }

    RETURN_IF_NULL(pPixels);
    RETURN_IF_NULL(mtlc);

    MTLPooledTextureHandle * texHandle = [mtlc.texturePool
                                                  getTexture:width
                                                      height:height
                                                      format:MTLPixelFormatBGRA8Unorm];
    if (texHandle == nil) {
        J2dTraceLn(J2D_TRACE_ERROR, "MTLMaskBlit_MaskBlit: can't obtain temporary texture object from pool");
        return;
    }
    [[mtlc getCommandBufferWrapper] registerPooledTexture:texHandle];

    id<MTLTexture> texBuff = texHandle.texture;
    MTLRegion region = MTLRegionMake2D(0, 0, width, height);
    [texBuff replaceRegion:region mipmapLevel:0 withBytes:pPixels bytesPerRow:4*width];

    drawTex2Tex(mtlc, texBuff, dstOps->pTexture, JNI_FALSE, dstOps->isOpaque, 0,
                0, 0, width, height, dstx, dsty, dstx + width, dsty + height);
}
