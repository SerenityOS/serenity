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

#include "D3DMaskCache.h"

HRESULT
D3DMaskCache::CreateInstance(D3DContext *pCtx, D3DMaskCache **ppMaskCache)
{
    HRESULT res;

    J2dTraceLn(J2D_TRACE_INFO, "D3DMaskCache::CreateInstance");

    *ppMaskCache = new D3DMaskCache();
    if (FAILED(res = (*ppMaskCache)->Init(pCtx))) {
        delete *ppMaskCache;
        *ppMaskCache = NULL;
    }
    return res;
}

D3DMaskCache::D3DMaskCache()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DMaskCache::D3DMaskCache");
    this->pCtx = NULL;
    maskCacheIndex = 0;
}

D3DMaskCache::~D3DMaskCache()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DMaskCache::~D3DMaskCache");
    pCtx = NULL;
    maskCacheIndex = 0;
}

HRESULT
D3DMaskCache::Init(D3DContext *pCtx)
{
    J2dTraceLn1(J2D_TRACE_INFO, "D3DMaskCache::Init pCtx=%x", pCtx);
    this->pCtx = pCtx;
    this->maskCacheIndex = 0;
    return S_OK;
}

HRESULT D3DMaskCache::Enable()
{
    HRESULT res;

    J2dTraceLn(J2D_TRACE_INFO, "D3DMaskCache::Enable");

    D3DResource *pMaskTexRes;
    res = pCtx->GetResourceManager()->GetMaskTexture(&pMaskTexRes);
    RETURN_STATUS_IF_FAILED(res);

    res = pCtx->SetTexture(pMaskTexRes->GetTexture(), 0);

    IDirect3DDevice9 *pd3dDevice = pCtx->Get3DDevice();
    D3DTEXTUREFILTERTYPE fhint =
        pCtx->IsTextureFilteringSupported(D3DTEXF_NONE) ?
            D3DTEXF_NONE : D3DTEXF_POINT;
    pd3dDevice->SetSamplerState(0, D3DSAMP_MAGFILTER, fhint);
    pd3dDevice->SetSamplerState(0, D3DSAMP_MINFILTER, fhint);

    return res;
}

HRESULT D3DMaskCache::Disable()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DMaskCache::Disable");

    maskCacheIndex = 0;

    return pCtx->SetTexture(NULL, 0);
}

HRESULT D3DMaskCache::AddMaskQuad(int srcx, int srcy,
                                  int dstx, int dsty,
                                  int width, int height,
                                  int maskscan, void *mask)
{
    HRESULT res;
    float tx1, ty1, tx2, ty2;
    float dx1, dy1, dx2, dy2;

    J2dTraceLn1(J2D_TRACE_INFO, "D3DVertexCacher::AddMaskQuad: %d",
                maskCacheIndex);

    if (maskCacheIndex >= D3D_MASK_CACHE_MAX_INDEX ||
        pCtx->pVCacher->GetFreeVertices() < 6)
    {
        res = pCtx->pVCacher->Render();
        RETURN_STATUS_IF_FAILED(res);
        maskCacheIndex = 0;
    }

    if (mask != NULL) {
        int texx = D3D_MASK_CACHE_TILE_WIDTH *
            (maskCacheIndex % D3D_MASK_CACHE_WIDTH_IN_TILES);
        int texy = D3D_MASK_CACHE_TILE_HEIGHT *
            (maskCacheIndex / D3D_MASK_CACHE_WIDTH_IN_TILES);
        D3DResource *pMaskTexRes;

        res = pCtx->GetResourceManager()->GetMaskTexture(&pMaskTexRes);
        RETURN_STATUS_IF_FAILED(res);

        // copy alpha mask into texture tile
        pCtx->UploadTileToTexture(pMaskTexRes, mask,
                                  texx, texy,
                                  srcx, srcy,
                                  width, height,
                                  maskscan,
                                  TILEFMT_1BYTE_ALPHA);

        tx1 = ((float)texx) / D3D_MASK_CACHE_WIDTH_IN_TEXELS;
        ty1 = ((float)texy) / D3D_MASK_CACHE_HEIGHT_IN_TEXELS;

        maskCacheIndex++;
    } else {
        // use special fully opaque tile
        tx1 = ((float)D3D_MASK_CACHE_SPECIAL_TILE_X) /
            D3D_MASK_CACHE_WIDTH_IN_TEXELS;
        ty1 = ((float)D3D_MASK_CACHE_SPECIAL_TILE_Y) /
            D3D_MASK_CACHE_HEIGHT_IN_TEXELS;
    }

    tx2 = tx1 + (((float)width) / D3D_MASK_CACHE_WIDTH_IN_TEXELS);
    ty2 = ty1 + (((float)height) / D3D_MASK_CACHE_HEIGHT_IN_TEXELS);

    dx1 = (float)dstx;
    dy1 = (float)dsty;
    dx2 = dx1 + width;
    dy2 = dy1 + height;

    return pCtx->pVCacher->DrawTexture(dx1, dy1, dx2, dy2,
                                       tx1, ty1, tx2, ty2);
}
