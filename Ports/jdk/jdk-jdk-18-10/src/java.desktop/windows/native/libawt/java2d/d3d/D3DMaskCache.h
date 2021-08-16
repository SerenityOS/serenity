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

#ifndef D3DMASKCACHE_H
#define D3DMASKCACHE_H

#include "jni.h"
#include "D3DContext.h"

/**
 * Constants that control the size of the texture tile cache used for
 * mask operations.
 */
#define D3D_MASK_CACHE_TILE_WIDTH       32
#define D3D_MASK_CACHE_TILE_HEIGHT      32
#define D3D_MASK_CACHE_TILE_SIZE \
   (D3D_MASK_CACHE_TILE_WIDTH * D3D_MASK_CACHE_TILE_HEIGHT)

#define D3D_MASK_CACHE_WIDTH_IN_TILES   8
#define D3D_MASK_CACHE_HEIGHT_IN_TILES  4

#define D3D_MASK_CACHE_WIDTH_IN_TEXELS \
   (D3D_MASK_CACHE_TILE_WIDTH * D3D_MASK_CACHE_WIDTH_IN_TILES)
#define D3D_MASK_CACHE_HEIGHT_IN_TEXELS \
   (D3D_MASK_CACHE_TILE_HEIGHT * D3D_MASK_CACHE_HEIGHT_IN_TILES)

/*
 * We reserve one (fully opaque) tile in the upper-right corner for
 * operations where the mask is null.
 */
#define D3D_MASK_CACHE_MAX_INDEX \
   ((D3D_MASK_CACHE_WIDTH_IN_TILES * D3D_MASK_CACHE_HEIGHT_IN_TILES) - 1)
#define D3D_MASK_CACHE_SPECIAL_TILE_X \
   (D3D_MASK_CACHE_WIDTH_IN_TEXELS - D3D_MASK_CACHE_TILE_WIDTH)
#define D3D_MASK_CACHE_SPECIAL_TILE_Y \
   (D3D_MASK_CACHE_HEIGHT_IN_TEXELS - D3D_MASK_CACHE_TILE_HEIGHT)

class D3DContext;

class D3DMaskCache {
public:
    HRESULT Init(D3DContext *pCtx);
    void    ReleaseDefPoolResources() {};
            ~D3DMaskCache();
    HRESULT Enable();
    HRESULT Disable();
    HRESULT AddMaskQuad(int srcx, int srcy,
                        int dstx, int dsty,
                        int width, int height,
                        int maskscan, void *mask);

static
    HRESULT CreateInstance(D3DContext *pCtx, D3DMaskCache **ppMaskCache);

private:
               D3DMaskCache();
    UINT       maskCacheIndex;
    D3DContext *pCtx;
};

#endif // D3DMASKCACHE_H
