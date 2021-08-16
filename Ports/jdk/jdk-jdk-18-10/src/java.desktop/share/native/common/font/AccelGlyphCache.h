/*
 * Copyright (c) 2003, 2008, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AccelGlyphCache_h_Included
#define AccelGlyphCache_h_Included

#ifdef __cplusplus
extern "C" {
#endif

#include "jni.h"
#include "fontscalerdefs.h"

typedef void (FlushFunc)();

typedef struct _CacheCellInfo CacheCellInfo;

typedef struct {
    CacheCellInfo *head;
    CacheCellInfo *tail;
    unsigned int  cacheID;
    jint          width;
    jint          height;
    jint          cellWidth;
    jint          cellHeight;
    jboolean      isFull;
    FlushFunc     *Flush;
} GlyphCacheInfo;

struct _CacheCellInfo {
    GlyphCacheInfo   *cacheInfo;
    struct GlyphInfo *glyphInfo;
    // next cell info in the cache's list
    CacheCellInfo    *next;
    // REMIND: find better name?
    // next cell info in the glyph's cell list (next Glyph Cache Info)
    CacheCellInfo    *nextGCI;
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

GlyphCacheInfo *
AccelGlyphCache_Init(jint width, jint height,
                     jint cellWidth, jint cellHeight,
                     FlushFunc *func);
CacheCellInfo *
AccelGlyphCache_AddGlyph(GlyphCacheInfo *cache, struct GlyphInfo *glyph);
void
AccelGlyphCache_Invalidate(GlyphCacheInfo *cache);
void
AccelGlyphCache_AddCellInfo(struct GlyphInfo *glyph, CacheCellInfo *cellInfo);
void
AccelGlyphCache_RemoveCellInfo(struct GlyphInfo *glyph, CacheCellInfo *cellInfo);
CacheCellInfo *
AccelGlyphCache_GetCellInfoForCache(struct GlyphInfo *glyph,
                                    GlyphCacheInfo *cache);
JNIEXPORT void
AccelGlyphCache_RemoveAllCellInfos(struct GlyphInfo *glyph);
void
AccelGlyphCache_Free(GlyphCacheInfo *cache);

#ifdef __cplusplus
};
#endif

#endif /* AccelGlyphCache_h_Included */
