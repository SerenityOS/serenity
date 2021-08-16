/*
 * Copyright 2021 JetBrains s.r.o.
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

#include "jni_util.h"
#include "fontscalerdefs.h"
#include "SurfaceData.h"

typedef struct _GlyphOps {
    SurfaceDataOps sdOps;
    GlyphInfo*     glyph;
} GlyphOps;

static jint Glyph_Lock(JNIEnv *env,
                       SurfaceDataOps *ops,
                       SurfaceDataRasInfo *pRasInfo,
                       jint lockflags)
{
    SurfaceDataBounds bounds;
    GlyphInfo *glyph;
    if (lockflags &
        (SD_LOCK_WRITE | SD_LOCK_LUT | SD_LOCK_INVCOLOR | SD_LOCK_INVGRAY)) {
        JNU_ThrowInternalError(env, "Unsupported mode for glyph image surface");
        return SD_FAILURE;
    }
    glyph = ((GlyphOps*)ops)->glyph;
    bounds.x1 = 0;
    bounds.y1 = 0;
    bounds.x2 = glyph->width;
    bounds.y2 = glyph->height;
    SurfaceData_IntersectBounds(&pRasInfo->bounds, &bounds);
    return SD_SUCCESS;
}

static void Glyph_GetRasInfo(JNIEnv *env,
                             SurfaceDataOps *ops,
                             SurfaceDataRasInfo *pRasInfo)
{
    GlyphInfo *glyph = ((GlyphOps*)ops)->glyph;

    pRasInfo->rasBase = glyph->image;
    pRasInfo->pixelBitOffset = 0;
    pRasInfo->pixelStride = 4;
    pRasInfo->scanStride = glyph->rowBytes;
}

JNIEXPORT void JNICALL
Java_sun_font_ColorGlyphSurfaceData_initOps(JNIEnv *env,
                                            jobject sData)
{
    GlyphOps *ops =
        (GlyphOps*) SurfaceData_InitOps(env, sData, sizeof(GlyphOps));
    if (ops == NULL) {
        JNU_ThrowOutOfMemoryError(env,
            "Initialization of ColorGlyphSurfaceData failed");
        return;
    }
    ops->sdOps.Lock = Glyph_Lock;
    ops->sdOps.GetRasInfo = Glyph_GetRasInfo;
}

JNIEXPORT void JNICALL
Java_sun_font_ColorGlyphSurfaceData_setCurrentGlyph(JNIEnv *env,
                                                    jobject sData,
                                                    jlong imgPtr)
{
    GlyphOps *ops = (GlyphOps*) SurfaceData_GetOps(env, sData);
    if (ops == NULL) {
        return;
    }
    ops->glyph = (GlyphInfo*) jlong_to_ptr(imgPtr);
}