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

#ifndef D3DGLYPHCACHE_H
#define D3DGLYPHCACHE_H

#include "AccelGlyphCache.h"
#include "D3DContext.h"
#include "D3DResourceManager.h"

typedef enum {
    CACHE_GRAY,
    CACHE_LCD
} GlyphCacheType;

class D3DContext;
class D3DResource;

class D3DGlyphCache {
public:
    // creates accel. glyph cache if it wasn't created, and the glyph
    // cache texure
    HRESULT Init(D3DContext *pCtx);
    // releases the glyph cache texture, invalidates the accel. glyph cache
    void    ReleaseDefPoolResources();
    // releases texture and deletes the accel. glyph cache
           ~D3DGlyphCache();

    // adds the glyph to the accel. glyph cache and uploads it into the glyph
    // cache texture
    HRESULT AddGlyph(GlyphInfo *glyph);

    GlyphCacheInfo* GetGlyphCache() { return pGlyphCache; }
    D3DResource* GetGlyphCacheTexture() { return pGlyphCacheRes; }

    // Note: only applicable to CACHE_LCD type of the cache
    // if the new rgb order doesn't match the current one, invalidates
    // the accel. glyph cache, also resets the current tileFormat
    HRESULT CheckGlyphCacheByteOrder(jboolean rgbOrder);

static
    HRESULT CreateInstance(D3DContext *pCtx,
                           GlyphCacheType gcType,
                           D3DGlyphCache **ppGlyphCache);

private:
    D3DGlyphCache(GlyphCacheType gcType);

    D3DContext *pCtx;
    GlyphCacheType gcType;
    D3DResource *pGlyphCacheRes;
    GlyphCacheInfo *pGlyphCache;
    TileFormat tileFormat;
    /**
     * Relevant only for the CACHE_LCD cache type.
     *
     * This value tracks the previous LCD rgbOrder setting, so if the rgbOrder
     * value has changed since the last time, it indicates that we need to
     * invalidate the cache, which may already store glyph images in the
     * reverse order.  Note that in most real world applications this value
     * will not change over the course of the application, but tests like
     * Font2DTest allow for changing the ordering at runtime, so we need to
     * handle that case.
     */
    jboolean lastRGBOrder;
};
#endif // D3DGLYPHCACHE_H
