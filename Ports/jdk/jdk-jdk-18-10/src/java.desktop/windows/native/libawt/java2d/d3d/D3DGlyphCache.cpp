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

#include "D3DGlyphCache.h"
#include "D3DTextRenderer.h"
#include "D3DRenderQueue.h"

void D3DGlyphCache_FlushGlyphVertexCache();

// static
HRESULT
D3DGlyphCache::CreateInstance(D3DContext *pCtx, GlyphCacheType gcType,
                              D3DGlyphCache **ppGlyphCache)
{
    HRESULT res;

    J2dTraceLn(J2D_TRACE_INFO, "D3DGlyphCache::CreateInstance");

    *ppGlyphCache = new D3DGlyphCache(gcType);
    if (FAILED(res = (*ppGlyphCache)->Init(pCtx))) {
        delete *ppGlyphCache;
        *ppGlyphCache = NULL;
    }
    return res;
}

D3DGlyphCache::D3DGlyphCache(GlyphCacheType type)
{
    J2dTraceLn1(J2D_TRACE_INFO, "D3DGlyphCache::D3DGlyphCache gcType=%d", type);

    pCtx = NULL;
    gcType = type;
    pGlyphCacheRes = NULL;
    pGlyphCache = NULL;
    tileFormat = (gcType == CACHE_GRAY) ? TILEFMT_1BYTE_ALPHA : TILEFMT_UNKNOWN;
    lastRGBOrder = JNI_FALSE;
}

D3DGlyphCache::~D3DGlyphCache()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DGlyphCache::~D3DGlyphCache");

    ReleaseDefPoolResources();

    pCtx = NULL;
    if (pGlyphCache != NULL) {
        AccelGlyphCache_Free(pGlyphCache);
        pGlyphCache = NULL;
    }
}

void
D3DGlyphCache::ReleaseDefPoolResources()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DGlyphCache::ReleaseDefPoolResources");

    AccelGlyphCache_Invalidate(pGlyphCache);
    // REMIND: the glyph cache texture is not in the default pool, so
    // this can be optimized not to release the texture
    pCtx->GetResourceManager()->ReleaseResource(pGlyphCacheRes);
    pGlyphCacheRes = NULL;
}

HRESULT
D3DGlyphCache::Init(D3DContext *pCtx)
{
    D3DFORMAT format;

    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    J2dTraceLn1(J2D_TRACE_INFO, "D3DGlyphCache::Init pCtx=%x", pCtx);

    this->pCtx = pCtx;

    if (pGlyphCache == NULL) {
        // init glyph cache data structure
        pGlyphCache = AccelGlyphCache_Init(D3DTR_CACHE_WIDTH,
                                           D3DTR_CACHE_HEIGHT,
                                           D3DTR_CACHE_CELL_WIDTH,
                                           D3DTR_CACHE_CELL_HEIGHT,
                                           D3DGlyphCache_FlushGlyphVertexCache);
        if (pGlyphCache == NULL) {
            J2dRlsTraceLn(J2D_TRACE_ERROR,
                          "D3DGlyphCache::Init: "\
                          "could not init D3D glyph cache");
            return E_FAIL;
        }
    }

    if (gcType == CACHE_GRAY) {
        format = pCtx->IsTextureFormatSupported(D3DFMT_A8) ?
            D3DFMT_A8 : D3DFMT_A8R8G8B8;
    } else { // gcType == CACHE_LCD
        format = pCtx->IsTextureFormatSupported(D3DFMT_R8G8B8) ?
            D3DFMT_R8G8B8 : D3DFMT_A8R8G8B8;
    }

    HRESULT res = pCtx->GetResourceManager()->
        CreateTexture(D3DTR_CACHE_WIDTH, D3DTR_CACHE_HEIGHT,
                      FALSE/*isRTT*/, FALSE/*isOpaque*/, &format, 0/*usage*/,
                      &pGlyphCacheRes);
    if (FAILED(res)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "D3DGlyphCache::Init: "\
                      "could not create glyph cache texture");
    }

    return res;
}

HRESULT
D3DGlyphCache::AddGlyph(GlyphInfo *glyph)
{
    HRESULT res = S_OK;

    RETURN_STATUS_IF_NULL(pGlyphCacheRes, E_FAIL);

    CacheCellInfo *cellInfo = AccelGlyphCache_AddGlyph(pGlyphCache, glyph);
    if (cellInfo != NULL) {
        jint pixelsTouchedL = 0, pixelsTouchedR = 0;
        // store glyph image in texture cell
        res = pCtx->UploadTileToTexture(pGlyphCacheRes,
                                        glyph->image,
                                        cellInfo->x, cellInfo->y,
                                        0, 0,
                                        glyph->width, glyph->height,
                                        glyph->rowBytes, tileFormat,
                                        &pixelsTouchedL,
                                        &pixelsTouchedR);
        // LCD text rendering optimization: if the number of pixels touched on
        // the first or last column of the glyph image is less than 1/3 of the
        // height of the glyph we do not consider them touched.
        // See D3DTextRenderer.cpp:UpdateCachedDestination for more information.
        // The leftOff/rightOff are only used in LCD cache case.
        if (gcType == CACHE_LCD) {
            jint threshold = glyph->height/3;

            cellInfo->leftOff  = pixelsTouchedL < threshold ?  1 : 0;
            cellInfo->rightOff = pixelsTouchedR < threshold ? -1 : 0;
        } else {
            cellInfo->leftOff  = 0;
            cellInfo->rightOff = 0;
        }
    }

    return res;
}

HRESULT
D3DGlyphCache::CheckGlyphCacheByteOrder(jboolean rgbOrder)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DGlyphCache::CheckGlyphCacheByteOrder");

    if (gcType != CACHE_LCD) {
        J2dTraceLn(J2D_TRACE_ERROR, "D3DGlyphCache::CheckGlyphCacheByteOrder"\
                   " invoked on CACHE_GRAY cache type instance!");
        return E_FAIL;
    }

    if (rgbOrder != lastRGBOrder) {
        // need to invalidate the cache in this case; see comments
        // for lastRGBOrder
        AccelGlyphCache_Invalidate(pGlyphCache);
        lastRGBOrder = rgbOrder;
    }
    tileFormat = rgbOrder ? TILEFMT_3BYTE_RGB : TILEFMT_3BYTE_BGR;

    return S_OK;
}

/**
 * This method is invoked in the (relatively rare) case where one or
 * more glyphs is about to be kicked out of the glyph cache texture.
 * Here we simply flush the vertex queue of the current context in case
 * any pending vertices are dependent upon the current glyph cache layout.
 */
static void
D3DGlyphCache_FlushGlyphVertexCache()
{
    D3DContext *d3dc = D3DRQ_GetCurrentContext();
    if (d3dc != NULL) {
        J2dTraceLn(J2D_TRACE_INFO, "D3DGlyphCache_FlushGlyphVertexCache");
        d3dc->FlushVertexQueue();
    }
}
