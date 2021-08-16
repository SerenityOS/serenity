/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OGLVertexCache_h_Included
#define OGLVertexCache_h_Included

#include "j2d_md.h"
#include "OGLContext.h"

/**
 * Constants that control the size of the vertex cache.
 */
#define OGLVC_MAX_INDEX         1024

/**
 * Constants that control the size of the texture tile cache used for
 * mask operations.
 */
#define OGLVC_MASK_CACHE_TILE_WIDTH       32
#define OGLVC_MASK_CACHE_TILE_HEIGHT      32
#define OGLVC_MASK_CACHE_TILE_SIZE \
   (OGLVC_MASK_CACHE_TILE_WIDTH * OGLVC_MASK_CACHE_TILE_HEIGHT)

#define OGLVC_MASK_CACHE_WIDTH_IN_TILES   8
#define OGLVC_MASK_CACHE_HEIGHT_IN_TILES  4

#define OGLVC_MASK_CACHE_WIDTH_IN_TEXELS \
   (OGLVC_MASK_CACHE_TILE_WIDTH * OGLVC_MASK_CACHE_WIDTH_IN_TILES)
#define OGLVC_MASK_CACHE_HEIGHT_IN_TEXELS \
   (OGLVC_MASK_CACHE_TILE_HEIGHT * OGLVC_MASK_CACHE_HEIGHT_IN_TILES)

/*
 * We reserve one (fully opaque) tile in the upper-right corner for
 * operations where the mask is null.
 */
#define OGLVC_MASK_CACHE_MAX_INDEX \
   ((OGLVC_MASK_CACHE_WIDTH_IN_TILES * OGLVC_MASK_CACHE_HEIGHT_IN_TILES) - 1)
#define OGLVC_MASK_CACHE_SPECIAL_TILE_X \
   (OGLVC_MASK_CACHE_WIDTH_IN_TEXELS - OGLVC_MASK_CACHE_TILE_WIDTH)
#define OGLVC_MASK_CACHE_SPECIAL_TILE_Y \
   (OGLVC_MASK_CACHE_HEIGHT_IN_TEXELS - OGLVC_MASK_CACHE_TILE_HEIGHT)

/**
 * Exported methods.
 */
jboolean OGLVertexCache_InitVertexCache(OGLContext *oglc);
void OGLVertexCache_FlushVertexCache();
void OGLVertexCache_RestoreColorState(OGLContext *oglc);

void OGLVertexCache_EnableMaskCache(OGLContext *oglc);
void OGLVertexCache_DisableMaskCache(OGLContext *oglc);
void OGLVertexCache_AddMaskQuad(OGLContext *oglc,
                                jint srcx, jint srcy,
                                jint dstx, jint dsty,
                                jint width, jint height,
                                jint maskscan, void *mask);

void OGLVertexCache_AddGlyphQuad(OGLContext *oglc,
                                 jfloat tx1, jfloat ty1,
                                 jfloat tx2, jfloat ty2,
                                 jfloat dx1, jfloat dy1,
                                 jfloat dx2, jfloat dy2);

#endif /* OGLVertexCache_h_Included */
