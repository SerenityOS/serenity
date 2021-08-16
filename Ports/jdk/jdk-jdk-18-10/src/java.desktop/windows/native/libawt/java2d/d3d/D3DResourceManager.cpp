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

#include "D3DResourceManager.h"
#include "awt.h"
#include "D3DPaints.h"
#include "D3DTextRenderer.h"

void
D3DResource::Init(IDirect3DResource9 *pRes, IDirect3DSwapChain9 *pSC)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DResource::Init");

    pResource  = NULL;
    pSwapChain = pSC;
    pSurface   = NULL;
    pTexture   = NULL;
    pOps       = NULL;
    ZeroMemory(&desc, sizeof(desc));
    desc.Format = D3DFMT_UNKNOWN;

    if (pRes != NULL) {
        pResource = pRes;

        D3DRESOURCETYPE type = pResource->GetType();
        switch (type) {
        case D3DRTYPE_TEXTURE:
            // addRef is needed because both pResource and pTexture will be
            // Release()d, and they point to the same object
            pResource->AddRef();
            pTexture = (IDirect3DTexture9*)pResource;
            pTexture->GetSurfaceLevel(0, &pSurface);
            break;
        case D3DRTYPE_SURFACE:
            pResource->AddRef();
            pSurface = (IDirect3DSurface9*)pResource;
            break;
        case D3DRTYPE_CUBETEXTURE:
            ((IDirect3DCubeTexture9*)pResource)->GetLevelDesc(0, &desc);
            break;
        default:
            J2dTraceLn1(J2D_TRACE_VERBOSE, "  resource type=%d", type);
        }
    } else if (pSwapChain != NULL) {
        pSwapChain->GetBackBuffer(0, D3DBACKBUFFER_TYPE_MONO, &pSurface);
    } else {
        J2dTraceLn(J2D_TRACE_VERBOSE, "  pResource == pSwapChain == NULL");
    }

    if (pSurface != NULL) {
        pSurface->GetDesc(&desc);
    }

    SAFE_PRINTLN(pResource);
    SAFE_PRINTLN(pSurface);
    SAFE_PRINTLN(pTexture);
    SAFE_PRINTLN(pSwapChain);
}

D3DResource::~D3DResource()
{
    Release();
}

void
D3DResource::SetSDOps(D3DSDOps *pOps)
{
    if (pOps != NULL && this->pOps != NULL) {
        // something's wrong, we're overwriting
        // a non-null field (setting it to null is allowed)
        J2dTraceLn2(J2D_TRACE_WARNING,
                    "D3DResource::SetSDOps: overwriting "\
                    "this->pOps=0x%x with pOps=0x%x", this->pOps, pOps);
    }
    this->pOps = pOps;
}

BOOL
D3DResource::IsDefaultPool()
{
    if (desc.Format != D3DFMT_UNKNOWN) {
        return (desc.Pool == D3DPOOL_DEFAULT);
    }
    return TRUE;
}

void
D3DResource::Release()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DResource::Release");

    SAFE_PRINTLN(pResource);
    SAFE_PRINTLN(pSurface);
    SAFE_PRINTLN(pTexture);
    SAFE_PRINTLN(pSwapChain);

    SAFE_RELEASE(pSurface);
    SAFE_RELEASE(pTexture);
    SAFE_RELEASE(pResource);
    SAFE_RELEASE(pSwapChain);

    if (pOps != NULL) {
        // if sdOps is not NULL it means that the release was initiated
        // from the native level, and is caused by a surface loss
        D3DSD_MarkLost(pOps);
        pOps->pResource = NULL;
        pOps = NULL;
    }
}

HRESULT
D3DResourceManager::CreateInstance(D3DContext *pCtx,
                                   D3DResourceManager** ppResourceMgr)
{
    HRESULT res;

    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::CreateInstance");

    *ppResourceMgr = new D3DResourceManager();
    if (FAILED(res = (*ppResourceMgr)->Init(pCtx))) {
        delete *ppResourceMgr;
        *ppResourceMgr = NULL;
    }
    return res;
}

D3DResourceManager::D3DResourceManager()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::D3DRM");

    this->pCtx = NULL;
    this->pHead = NULL;
}

HRESULT
D3DResourceManager::Init(D3DContext *pCtx)
{
    J2dTraceLn1(J2D_TRACE_INFO, "D3DRM::Init pCtx=%x", pCtx);
    if (this->pCtx != pCtx ||
        (this->pCtx != NULL &&
         this->pCtx->Get3DDevice() != pCtx->Get3DDevice()))
    {
        ReleaseAll();
    }
    this->pCtx = pCtx;
    return S_OK;
}

D3DResourceManager::~D3DResourceManager()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::~D3DRM");
    ReleaseAll();
    pCtx = NULL;
    pHead = NULL;
}

void
D3DResourceManager::ReleaseAll()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::ReleaseAll");
    IManagedResource* pCurrent;
    while (pHead != NULL) {
        pCurrent = pHead;
        pHead = pHead->pNext;
        delete pCurrent;
    }
    pCachedDestTexture    = NULL;
    pBlitTexture          = NULL;
    pBlitRTTexture        = NULL;
    pBlitOSPSurface       = NULL;
    pGradientTexture      = NULL;
    pLookupOpLutTexture   = NULL;
    pMaskTexture          = NULL;
    pMultiGradientTexture = NULL;
    pLockableRTSurface    = NULL;
}

void
D3DResourceManager::ReleaseDefPoolResources()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::ReleaseDefPoolResources");
    // REMIND: for now, release all resources
    ReleaseAll();
}

HRESULT
D3DResourceManager::ReleaseResource(IManagedResource* pResource)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::ReleaseResource");

    if (pResource != NULL) {
        J2dTraceLn1(J2D_TRACE_VERBOSE, "  releasing pResource=%x", pResource);
        if (pResource->pPrev != NULL) {
            pResource->pPrev->pNext = pResource->pNext;
        } else {
            // it's the head
            pHead = pResource->pNext;
            if (pHead != NULL) {
                pHead->pPrev = NULL;
            }
        }
        if (pResource->pNext != NULL) {
            pResource->pNext->pPrev = pResource->pPrev;
        }
        delete pResource;
    }
    return S_OK;
}

HRESULT
D3DResourceManager::AddResource(IManagedResource* pResource)
{
    HRESULT res = S_OK;

    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::AddResource");

    if (pResource != NULL) {
        J2dTraceLn1(J2D_TRACE_VERBOSE, "  pResource=%x", pResource);
        pResource->pPrev = NULL;
        pResource->pNext = pHead;
        if (pHead != NULL) {
            pHead->pPrev = pResource;
        }
        pHead = pResource;
    }

    return S_OK;
}

HRESULT
D3DResourceManager::CreateTexture(UINT width, UINT height,
                                  BOOL isRTT, BOOL isOpaque,
                                  D3DFORMAT *pFormat, DWORD dwUsage,
                                  D3DResource **ppTextureResource)
{
    D3DPOOL pool;
    D3DFORMAT format;
    HRESULT res;
    IDirect3DDevice9 *pd3dDevice;

    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::CreateTexture");
    J2dTraceLn4(J2D_TRACE_VERBOSE, "  w=%d h=%d isRTT=%d isOpaque=%d",
                width, height, isRTT, isOpaque);

    if (ppTextureResource == NULL || pCtx == NULL ||
        (pd3dDevice = pCtx->Get3DDevice()) == NULL)
    {
        return E_FAIL;
    }
    if (FAILED(res = pd3dDevice->TestCooperativeLevel())) {
        return res;
    }

    if (pFormat != NULL && *pFormat != D3DFMT_UNKNOWN) {
        format = *pFormat;
    } else {
        if (isOpaque) {
            format = D3DFMT_X8R8G8B8;
        } else {
            format = D3DFMT_A8R8G8B8;
        }
    }

    if (isRTT) {
        dwUsage = D3DUSAGE_RENDERTARGET;
        pool = D3DPOOL_DEFAULT;
    } else {
        if (dwUsage == D3DUSAGE_DYNAMIC && !pCtx->IsDynamicTextureSupported()) {
            dwUsage = 0;
        }
        if (dwUsage == D3DUSAGE_DYNAMIC) {
            pool = D3DPOOL_DEFAULT;
        } else {
            pool = pCtx->IsHWRasterizer() ?
                D3DPOOL_MANAGED : D3DPOOL_SYSTEMMEM;
        }
    }

    if (pCtx->IsPow2TexturesOnly()) {
          UINT w, h;
          for (w = 1; width  > w; w <<= 1);
          for (h = 1; height > h; h <<= 1);
          width = w;
          height = h;
    }
    if (pCtx->IsSquareTexturesOnly()) {
        if (width > height) {
            height = width;
        } else {
            width = height;
        }
    }

    IDirect3DTexture9 *pTexture = NULL;
    res = pd3dDevice->CreateTexture(width, height, 1/*levels*/, dwUsage,
                                    format, pool, &pTexture, 0);
    if (SUCCEEDED(res)) {
        J2dTraceLn1(J2D_TRACE_VERBOSE, "  created texture: 0x%x", pTexture);
        *ppTextureResource = new D3DResource((IDirect3DResource9*)pTexture);
        res = AddResource(*ppTextureResource);
    } else {
        DebugPrintD3DError(res, "D3DRM::CreateTexture failed");
        *ppTextureResource = NULL;
        format = D3DFMT_UNKNOWN;
    }

    if (pFormat != NULL) {
        *pFormat = format;
    }

    return res;
}

HRESULT D3DResourceManager::CreateRTSurface(UINT width, UINT height,
                                         BOOL isOpaque, BOOL isLockable,
                                         D3DFORMAT *pFormat/*out*/,
                                         D3DResource** ppSurfaceResource/*out*/)
{
    HRESULT res;
    IDirect3DDevice9 *pd3dDevice;

    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::CreateRTSurface");
    J2dTraceLn3(J2D_TRACE_VERBOSE, "  w=%d h=%d isOpaque=%d",
                width, height, isOpaque);

    if (pCtx == NULL || ppSurfaceResource == NULL ||
        (pd3dDevice = pCtx->Get3DDevice()) == NULL)
    {
        return E_FAIL;
    }
    if (FAILED(res = pd3dDevice->TestCooperativeLevel())) {
        return res;
    }

    D3DPRESENT_PARAMETERS *curParams = pCtx->GetPresentationParams();
    D3DFORMAT format = isOpaque ? curParams->BackBufferFormat : D3DFMT_A8R8G8B8;
    IDirect3DSurface9 *pSurface = NULL;

    res = pd3dDevice->CreateRenderTarget(width, height, format,
                                         D3DMULTISAMPLE_NONE, 0,
                                         isLockable,
                                         &pSurface, NULL);
    if (SUCCEEDED(res)) {
        J2dTraceLn1(J2D_TRACE_VERBOSE, "  created RT Surface: 0x%x ", pSurface);
        if (pFormat != NULL) {
            *pFormat = format;
        }
        *ppSurfaceResource = new D3DResource((IDirect3DResource9*)pSurface);
        res = AddResource(*ppSurfaceResource);
    } else {
        DebugPrintD3DError(res, "D3DRM::CreateRTSurface failed");
        ppSurfaceResource = NULL;
    }
    return res;
}

// REMIND: this method is currently unused; consider removing it later...
HRESULT D3DResourceManager::CreateOSPSurface(UINT width, UINT height,
                                         D3DFORMAT fmt,
                                         D3DResource** ppSurfaceResource/*out*/)
{
    HRESULT res;
    IDirect3DDevice9 *pd3dDevice;

    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::CreateOSPSurface");
    J2dTraceLn2(J2D_TRACE_VERBOSE, "  w=%d h=%d", width, height);

    if (pCtx == NULL || ppSurfaceResource == NULL ||
        (pd3dDevice = pCtx->Get3DDevice()) == NULL)
    {
        return E_FAIL;
    }
    if (FAILED(res = pd3dDevice->TestCooperativeLevel())) {
        return res;
    }

    // since the off-screen plain surface is intended to be used with
    // the UpdateSurface() method, it is essential that it be created
    // in the same format as the destination and allocated in the
    // SYSTEMMEM pool (otherwise UpdateSurface() will fail)
    D3DFORMAT format;
    if (fmt == D3DFMT_UNKNOWN) {
        format = pCtx->GetPresentationParams()->BackBufferFormat;
    } else {
        format = fmt;
    }
    D3DPOOL pool = D3DPOOL_SYSTEMMEM;
    IDirect3DSurface9 *pSurface = NULL;

    res = pd3dDevice->CreateOffscreenPlainSurface(width, height,
                                                  format, pool,
                                                  &pSurface, NULL);
    if (SUCCEEDED(res)) {
        J2dTraceLn1(J2D_TRACE_VERBOSE, "  created OSP Surface: 0x%x ",pSurface);
        *ppSurfaceResource = new D3DResource((IDirect3DResource9*)pSurface);
        res = AddResource(*ppSurfaceResource);
    } else {
        DebugPrintD3DError(res, "D3DRM::CreateOSPSurface failed");
        ppSurfaceResource = NULL;
    }
    return res;
}

HRESULT
D3DResourceManager::CreateSwapChain(HWND hWnd, UINT numBuffers,
                                    UINT width, UINT height,
                                    D3DSWAPEFFECT swapEffect,
                                    UINT presentationInterval,
                                    D3DResource ** ppSwapChainResource)
{
    HRESULT res;
    IDirect3DDevice9 *pd3dDevice;
    IDirect3DSwapChain9 *pSwapChain = NULL;
    D3DPRESENT_PARAMETERS newParams, *curParams;

    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::CreateSwapChain");
    J2dTraceLn4(J2D_TRACE_VERBOSE, "  w=%d h=%d hwnd=%x numBuffers=%d",
                width, height, hWnd, numBuffers);

    if (pCtx == NULL || ppSwapChainResource == NULL ||
        (pd3dDevice = pCtx->Get3DDevice()) == NULL)
    {
        return E_FAIL;
    }
    RETURN_STATUS_IF_FAILED(res = pd3dDevice->TestCooperativeLevel());

    curParams = pCtx->GetPresentationParams();

    if (curParams->Windowed == FALSE) {
        // there's a single swap chain in full-screen mode, use it if
        // it fits our parameters, reset the device otherwise
        if (curParams->BackBufferCount != numBuffers ||
            curParams->SwapEffect != swapEffect ||
            curParams->PresentationInterval != presentationInterval)
        {
            newParams = *curParams;
            newParams.BackBufferCount = numBuffers;
            newParams.SwapEffect = swapEffect;
            newParams.PresentationInterval = presentationInterval;

            res = pCtx->ConfigureContext(&newParams);
            RETURN_STATUS_IF_FAILED(res);
            // this reset will not have released the device, so our pd3dDevice
            // is still valid, but to be on a safe side, reset it
            pd3dDevice = pCtx->Get3DDevice();
        }
        res = pd3dDevice->GetSwapChain(0, &pSwapChain);
    } else {
        ZeroMemory(&newParams, sizeof(D3DPRESENT_PARAMETERS));
        newParams.BackBufferWidth = width;
        newParams.BackBufferHeight = height;
        newParams.hDeviceWindow = hWnd;
        newParams.Windowed = TRUE;
        newParams.BackBufferCount = numBuffers;
        newParams.SwapEffect = swapEffect;
        newParams.PresentationInterval = presentationInterval;

        res = pd3dDevice->CreateAdditionalSwapChain(&newParams, &pSwapChain);
    }

    if (SUCCEEDED(res)) {
        J2dTraceLn1(J2D_TRACE_VERBOSE,"  created swap chain: 0x%x ",pSwapChain);
        *ppSwapChainResource = new D3DResource(pSwapChain);
        res = AddResource(*ppSwapChainResource);
    } else {
        DebugPrintD3DError(res, "D3DRM::CreateSwapChain failed");
        *ppSwapChainResource = NULL;
    }
    return res;
}

HRESULT
D3DResourceManager::GetMaskTexture(D3DResource **ppTextureResource)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::GetMaskTexture");

    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    RETURN_STATUS_IF_NULL(ppTextureResource, E_FAIL);

    D3DFORMAT format = pCtx->IsTextureFormatSupported(D3DFMT_A8) ?
        D3DFMT_A8 : D3DFMT_A8R8G8B8;

    jboolean needsInit = (pMaskTexture == NULL);
    HRESULT res;
    if (FAILED(res =
        GetStockTextureResource(D3D_MASK_CACHE_WIDTH_IN_TEXELS,
                                D3D_MASK_CACHE_HEIGHT_IN_TEXELS,
                                FALSE/*isRTT*/, FALSE/*isOpaque*/, &format, 0,
                                &pMaskTexture)))
    {
        return res;
    }

    if (needsInit) {
        // init special fully opaque tile in the upper-right corner of
        // the mask cache texture
        jubyte allOnes[D3D_MASK_CACHE_TILE_SIZE];
        memset(allOnes, 0xff, D3D_MASK_CACHE_TILE_SIZE);
        if (FAILED(res = pCtx->UploadTileToTexture(
                                         pMaskTexture,
                                         allOnes,
                                         D3D_MASK_CACHE_SPECIAL_TILE_X,
                                         D3D_MASK_CACHE_SPECIAL_TILE_Y,
                                         0, 0,
                                         D3D_MASK_CACHE_TILE_WIDTH,
                                         D3D_MASK_CACHE_TILE_HEIGHT,
                                         D3D_MASK_CACHE_TILE_WIDTH,
                                         TILEFMT_1BYTE_ALPHA)))
        {
            return res;
        }
    }

    *ppTextureResource = pMaskTexture;

    return res;
}

HRESULT
D3DResourceManager::GetBlitTexture(D3DResource **ppTextureResource)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::GetBlitTexture");

    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    RETURN_STATUS_IF_NULL(ppTextureResource, E_FAIL);

    HRESULT res =
        GetStockTextureResource(D3DC_BLIT_TILE_SIZE, D3DC_BLIT_TILE_SIZE,
                                FALSE/*isRTT*/, FALSE/*isOpaque*/, NULL,
                                D3DUSAGE_DYNAMIC,
                                &pBlitTexture);
    *ppTextureResource = pBlitTexture;

    return res;
}

HRESULT
D3DResourceManager::GetGradientTexture(D3DResource **ppTextureResource)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::GetGradientTexture");

    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    RETURN_STATUS_IF_NULL(ppTextureResource, E_FAIL);

    HRESULT res =
        GetStockTextureResource(2, 1,
                                FALSE/*isRTT*/, FALSE/*isOpaque*/, NULL, 0,
                                &pGradientTexture);
    *ppTextureResource = pGradientTexture;

    return res;
}

HRESULT
D3DResourceManager::GetMultiGradientTexture(D3DResource **ppTextureResource)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::GetMultiGradientTexture");

    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    RETURN_STATUS_IF_NULL(ppTextureResource, E_FAIL);

    HRESULT res =
        GetStockTextureResource(MAX_MULTI_GRADIENT_COLORS, 1,
                                FALSE/*isRTT*/, FALSE/*isOpaque*/, NULL, 0,
                                &pMultiGradientTexture);
    *ppTextureResource = pMultiGradientTexture;

    return res;
}

HRESULT
D3DResourceManager::GetLookupOpLutTexture(D3DResource **ppTextureResource)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::GetLookupOpTexture");

    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    RETURN_STATUS_IF_NULL(ppTextureResource, E_FAIL);

    D3DFORMAT format = D3DFMT_L16;
    HRESULT res =
        GetStockTextureResource(256, 4,
                                FALSE/*isRTT*/, FALSE/*isOpaque*/, &format, 0,
                                &pLookupOpLutTexture);
    *ppTextureResource = pLookupOpLutTexture;

    return res;
}

HRESULT
D3DResourceManager::GetBlitRTTexture(UINT width, UINT height, D3DFORMAT format,
                                     D3DResource **ppTextureResource)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::GetBlitRTTexture");
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    RETURN_STATUS_IF_NULL(ppTextureResource, E_FAIL);

    HRESULT res = GetStockTextureResource(width, height,
                                          TRUE/*isRTT*/, FALSE/*isOpaque*/,
                                          &format, 0,
                                          &pBlitRTTexture);
    if (SUCCEEDED(res)) {
        D3DSURFACE_DESC *pDesc = pBlitRTTexture->GetDesc();
        D3DCAPS9 *pDevCaps = pCtx->GetDeviceCaps();
        if ((width <= pDesc->Width && height <= pDesc->Height) &&
            (format == pDesc->Format ||
             SUCCEEDED(pCtx->Get3DObject()->CheckDeviceFormatConversion(
                       pDevCaps->AdapterOrdinal,
                       pDevCaps->DeviceType, format, pDesc->Format))))
        {
            *ppTextureResource = pBlitRTTexture;
            return res;
        }
        // current texture doesn't fit, release and allocate a new one
        ReleaseResource(pBlitRTTexture);
        pBlitRTTexture = NULL;
    }
    if (width  < D3DC_BLIT_TILE_SIZE) width  = D3DC_BLIT_TILE_SIZE;
    if (height < D3DC_BLIT_TILE_SIZE) height = D3DC_BLIT_TILE_SIZE;

    res = CreateTexture(width, height, TRUE, FALSE, &format, 0,&pBlitRTTexture);
    *ppTextureResource = pBlitRTTexture;

    return res;
}

HRESULT
D3DResourceManager::GetBlitOSPSurface(UINT width, UINT height, D3DFORMAT fmt,
                                      D3DResource **ppSurfaceResource)
{
    HRESULT res = S_OK;

    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::GetBlitOSPSurface");
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    RETURN_STATUS_IF_NULL(ppSurfaceResource, E_FAIL);

    if (pBlitOSPSurface != NULL) {
        D3DSURFACE_DESC *pDesc = pBlitOSPSurface->GetDesc();
        if (width == pDesc->Width && height == pDesc->Height &&
            (fmt == pDesc->Format || fmt == D3DFMT_UNKNOWN))
        {
            *ppSurfaceResource = pBlitOSPSurface;
            return res;
        }
        // current surface doesn't fit, release and allocate a new one
        ReleaseResource(pBlitOSPSurface);
        pBlitOSPSurface = NULL;
    }

    res = CreateOSPSurface(width, height, fmt, &pBlitOSPSurface);
    *ppSurfaceResource = pBlitOSPSurface;

    return res;
}

HRESULT
D3DResourceManager::GetLockableRTSurface(UINT width, UINT height,
                                         D3DFORMAT format,
                                         D3DResource **ppSurfaceResource)
{
    HRESULT res = S_OK;

    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::GetLockableRTSurface");
    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    RETURN_STATUS_IF_NULL(ppSurfaceResource, E_FAIL);

    if (pLockableRTSurface != NULL) {
        D3DSURFACE_DESC *pDesc = pLockableRTSurface->GetDesc();
        if (width <= pDesc->Width && height <= pDesc->Height &&
            format == pDesc->Format)
        {
            *ppSurfaceResource = pLockableRTSurface;
            return res;
        }
        // current surface doesn't fit, release and allocate a new one
        ReleaseResource(pLockableRTSurface);
        pLockableRTSurface = NULL;
    }
    if (width  < D3DC_BLIT_TILE_SIZE) width  = D3DC_BLIT_TILE_SIZE;
    if (height < D3DC_BLIT_TILE_SIZE) height = D3DC_BLIT_TILE_SIZE;

    res = CreateRTSurface(width,height,
                          (format != D3DFMT_A8R8G8B8), TRUE /*lockable*/,
                          &format, &pLockableRTSurface);
    *ppSurfaceResource = pLockableRTSurface;

    return res;
}

HRESULT
D3DResourceManager::GetCachedDestTexture(D3DFORMAT format,
                                         D3DResource **ppTextureResource)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DRM::GetCachedDestTexture");

    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);
    RETURN_STATUS_IF_NULL(ppTextureResource, E_FAIL);

    HRESULT res =
        GetStockTextureResource(D3DTR_CACHED_DEST_WIDTH,
                                D3DTR_CACHED_DEST_HEIGHT,
                                TRUE/*isRTT*/, FALSE/*isOpaque*/,
                                &format, 0, &pCachedDestTexture);
    if (SUCCEEDED(res)) {
        D3DSURFACE_DESC *pDesc = pCachedDestTexture->GetDesc();
        D3DCAPS9 *pDevCaps = pCtx->GetDeviceCaps();
        if ((format == pDesc->Format ||
             SUCCEEDED(pCtx->Get3DObject()->CheckDeviceFormatConversion(
                           pDevCaps->AdapterOrdinal,
                           pDevCaps->DeviceType, format, pDesc->Format))))
        {
            *ppTextureResource = pCachedDestTexture;
            return res;
        }
        // current texture doesn't fit, release and allocate a new one
        ReleaseResource(pCachedDestTexture);
        pCachedDestTexture = NULL;
    }
    res = CreateTexture(D3DTR_CACHED_DEST_WIDTH, D3DTR_CACHED_DEST_HEIGHT,
                        TRUE, FALSE, &format, 0,
                        &pCachedDestTexture);
    *ppTextureResource = pCachedDestTexture;
    return res;
}

HRESULT
D3DResourceManager::GetStockTextureResource(UINT width, UINT height,
                                            BOOL isRTT, BOOL isOpaque,
                                            D3DFORMAT *pFormat/*in/out*/,
                                            DWORD dwUsage,
                                            D3DResource **ppTextureResource)
{
    D3DResource *pResource = *ppTextureResource;
    if (pResource != NULL) {
        if (pResource->GetTexture() != NULL) {
            return S_OK;
        }
        ReleaseResource(pResource);
        *ppTextureResource = NULL;
    }

    return CreateTexture(width, height, isRTT, isOpaque, pFormat, dwUsage,
                         ppTextureResource);
}
