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

#ifndef MTLVertexCache_h_Included
#define MTLVertexCache_h_Included
#include "j2d_md.h"
#include "MTLContext.h"
#include "fontscalerdefs.h"

/**
 * Constants that control the size of the vertex cache.
 */
#define MTLVC_MAX_INDEX         250

/**
 * Constants that control the size of the texture tile cache used for
 * mask operations.
 */
#define MTLVC_MASK_CACHE_TILE_WIDTH       32
#define MTLVC_MASK_CACHE_TILE_HEIGHT      32
#define MTLVC_MASK_CACHE_TILE_SIZE \
   (MTLVC_MASK_CACHE_TILE_WIDTH * MTLVC_MASK_CACHE_TILE_HEIGHT)

#define MTLVC_MASK_CACHE_WIDTH_IN_TILES   8
#define MTLVC_MASK_CACHE_HEIGHT_IN_TILES  4

#define MTLVC_MASK_CACHE_WIDTH_IN_TEXELS \
   (MTLVC_MASK_CACHE_TILE_WIDTH * MTLVC_MASK_CACHE_WIDTH_IN_TILES)
#define MTLVC_MASK_CACHE_HEIGHT_IN_TEXELS \
   (MTLVC_MASK_CACHE_TILE_HEIGHT * MTLVC_MASK_CACHE_HEIGHT_IN_TILES)

/*
 * We reserve one (fully opaque) tile in the upper-right corner for
 * operations where the mask is null.
 */
#define MTLVC_MASK_CACHE_MAX_INDEX \
   ((MTLVC_MASK_CACHE_WIDTH_IN_TILES * MTLVC_MASK_CACHE_HEIGHT_IN_TILES) - 1)
#define MTLVC_MASK_CACHE_SPECIAL_TILE_X \
   (MTLVC_MASK_CACHE_WIDTH_IN_TEXELS - MTLVC_MASK_CACHE_TILE_WIDTH)
#define MTLVC_MASK_CACHE_SPECIAL_TILE_Y \
   (MTLVC_MASK_CACHE_HEIGHT_IN_TEXELS - MTLVC_MASK_CACHE_TILE_HEIGHT)

/**
 * Exported methods.
 */
jboolean MTLVertexCache_InitVertexCache();
void MTLVertexCache_FlushVertexCache(MTLContext *mtlc);
void MTLVertexCache_FlushGlyphVertexCache();
void MTLVertexCache_FreeVertexCache();

void MTLVertexCache_EnableMaskCache(MTLContext *mtlc, BMTLSDOps *dstOps);
void MTLVertexCache_DisableMaskCache(MTLContext *mtlc);
void MTLVertexCache_AddMaskQuad(MTLContext *mtlc,
                                jint srcx, jint srcy,
                                jint dstx, jint dsty,
                                jint width, jint height,
                                jint maskscan, void *mask,
                                BMTLSDOps *dstOps);
void
MTLVertexCache_AddGlyphQuad(MTLContext *mtlc,
                            jfloat tx1, jfloat ty1, jfloat tx2, jfloat ty2,
                            jfloat dx1, jfloat dy1, jfloat dx2, jfloat dy2);
void MTLVertexCache_CreateSamplingEncoder(MTLContext *mtlc, BMTLSDOps *dstOps);
#endif /* MTLVertexCache_h_Included */
