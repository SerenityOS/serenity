/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef MTLGlyphCache_h_Included
#define MTLGlyphCache_h_Included

#ifdef __cplusplus
extern "C" {
#endif

#include "jni.h"
#include "fontscalerdefs.h"
#import <Metal/Metal.h>

typedef void (MTLFlushFunc)();

typedef struct _MTLCacheCellInfo MTLCacheCellInfo;

typedef struct {
    CacheCellInfo *head;
    CacheCellInfo *tail;
    id<MTLTexture> texture;
    jint          width;
    jint          height;
    jint          cellWidth;
    jint          cellHeight;
    MTLFlushFunc     *Flush;
} MTLGlyphCacheInfo;

struct _MTLCacheCellInfo {
    MTLGlyphCacheInfo   *cacheInfo;
    struct GlyphInfo *glyphInfo;
    // next cell info in the cache's list
    MTLCacheCellInfo    *next;
    // REMIND: find better name?
    // next cell info in the glyph's cell list (next Glyph Cache Info)
    MTLCacheCellInfo    *nextGCI;
    jint             timesRendered;
    jint             x;
    jint             y;
    // number of pixels from the left or right edge not considered touched
    // by the glyph
    jint             leftOff;
    jint             rightOff;
    jfloat           tx1;
    jfloat           ty1;
    jfloat           tx2;
    jfloat           ty2;
};

MTLGlyphCacheInfo *
MTLGlyphCache_Init(jint width, jint height,
                     jint cellWidth, jint cellHeight,
                     MTLFlushFunc *func);
MTLCacheCellInfo *
MTLGlyphCache_AddGlyph(MTLGlyphCacheInfo *cache, struct GlyphInfo *glyph);
bool
MTLGlyphCache_IsCacheFull(MTLGlyphCacheInfo *cache, GlyphInfo *glyph);
void
MTLGlyphCache_Invalidate(MTLGlyphCacheInfo *cache);
void
MTLGlyphCache_AddCellInfo(struct GlyphInfo *glyph, MTLCacheCellInfo *cellInfo);
void
MTLGlyphCache_RemoveCellInfo(struct GlyphInfo *glyph, MTLCacheCellInfo *cellInfo);
MTLCacheCellInfo *
MTLGlyphCache_GetCellInfoForCache(struct GlyphInfo *glyph,
                                    MTLGlyphCacheInfo *cache);
JNIEXPORT void
MTLGlyphCache_RemoveAllCellInfos(struct GlyphInfo *glyph);
void
MTLGlyphCache_Free(MTLGlyphCacheInfo *cache);

#ifdef __cplusplus
};
#endif

#endif /* MTLGlyphCache_h_Included */
