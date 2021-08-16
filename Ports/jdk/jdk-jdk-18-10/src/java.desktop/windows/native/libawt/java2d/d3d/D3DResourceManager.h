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


#ifndef _D3DRESOURCEMANAGER_H_
#define _D3DRESOURCEMANAGER_H_

#include "D3DContext.h"
#include "D3DSurfaceData.h"

class D3DResourceManager;
class D3DContext;

/**
 * This interface represents a Direct3D resource which is managed by the
 * D3DResourceManager.
 *
 * Subclasses will need to override Release() and the destructor to release
 * the resources held by the object.
 *
 * The subclasses then can be used like this:
 *   class D3DShaderResource : public IManagedResource {
 *               D3DShaderResource(IDirect3DPixelShader9 *pShader) { // ... }
 *      virtual ~D3DShaderResource() { Release(); }
 *      virtual  Release() { SAFE_RELEASE(pShader); }
 *      virtual  IsDefaultPool() { return FALSE; }
 *   private:
 *      IDirect3DPixelShader9 *pShader;
 *   }
 *
 *   pD3DDevice->CreatePixelShader(..., &pShader);
 *   IManagedResource *pShaderRes = new D3DShaderResource(pShader);
 *   pCtx->GetResourceManager()->AddResource(pShaderRes);
 *
 * D3DResourceManager::ReleaseResource() must be used to dispose of the
 * resource:
 *   pCtx->GetResourceManager()->ReleaseResource(pShaderRes);
 *   // pShaderRes is now invalid, it was deleted
 *   shaderRes = NULL;
 *
 * In certain cases the D3DResourceManager may need to release all its
 * resources (like when resetting the device), so the subclasses must be
 * ready to be released at any time, and be able to notify their users.
 * For an example of how this can be achieved see how D3DSDO's
 * pResource field and D3DResource subclass. d3dsdo->pResource is reset when
 * the D3DResource it was pointing to is disposed.
 */
class IManagedResource {
friend class D3DResourceManager;
public:
    // determines whether the resource should be released by the manager
    // when defaul pool resources are to be released
    virtual BOOL IsDefaultPool() = 0;
protected:
                 IManagedResource() { pPrev = pNext = NULL; };
    virtual     ~IManagedResource() { pPrev = pNext = NULL; };
    virtual void Release() = 0;
private:
    // prevents accidental bad things like copying the object
    IManagedResource& operator=(const IManagedResource&);

    IManagedResource* pPrev;
    IManagedResource* pNext;
};

/**
 * This class handles either IDirect3DResource9 or IDirect3DSwapChain9
 * type of resources and provides access to Texture, Surface or SwapChain,
 * as well as the surface description.
 */
class D3DResource : public IManagedResource {
public:
                         D3DResource(IDirect3DResource9 *pRes)
                             { Init(pRes, NULL); }
                         D3DResource(IDirect3DSwapChain9 *pSC)
                             { Init(NULL, pSC); }
    IDirect3DResource9*  GetResource() { return pResource; }
    IDirect3DTexture9*   GetTexture() { return pTexture; }
    IDirect3DSurface9*   GetSurface() { return pSurface; }
    IDirect3DSwapChain9* GetSwapChain() { return pSwapChain; }
    D3DSDOps*            GetSDOps() { return pOps; }
    void                 SetSDOps(D3DSDOps *pOps);
    D3DSURFACE_DESC*     GetDesc() { return &desc; }
    virtual BOOL         IsDefaultPool();

protected:
    // these are protected because we want D3DResource to be only released via
    // ResourceManager
virtual                 ~D3DResource();
virtual void             Release();
    void                 Init(IDirect3DResource9*, IDirect3DSwapChain9*);

private:
    // prevents accidental bad things like copying the object
                         D3DResource() {}
    D3DResource&         operator=(const D3DResource&);

    IDirect3DResource9*  pResource;
    IDirect3DSwapChain9* pSwapChain;
    IDirect3DSurface9*   pSurface;
    IDirect3DTexture9*   pTexture;
    D3DSDOps*            pOps;
    D3DSURFACE_DESC      desc;
};

/**
 * This class maintains a list of d3d resources created by the pipeline or
 * other clients. It is needed because in some cases all resources have to be
 * released in order to reset the device so we must keep track of them.
 *
 * There is one instance of this class per D3DContext. Clients can either
 * use factory methods for creating resources or create their own encapsulated
 * in an IManagedResource interface subclass and add them to the list
 * using the AddResource() method. Resources added to the list must be released
 * via the ReleaseResource() method so that they can be stopped being managed.
 */
class D3DResourceManager {

public:
            ~D3DResourceManager();
    HRESULT Init(D3DContext *pCtx);
    // Releases and deletes all resources managed by this manager.
    void    ReleaseAll();
    // Releases (and deletes) all resources belonging to the default pool.
    // Note: this method may release other resources as well.
    void    ReleaseDefPoolResources();

    // Adds the resource to the list managed by this class.
    HRESULT AddResource(IManagedResource* pResource);
    // Removes the resource from the list of managed resources, and deletes
    // it. The argument pointer is invalid after this method returns.
    HRESULT ReleaseResource(IManagedResource* pResource);

    HRESULT CreateTexture(UINT width, UINT height,
                          BOOL isRTT, BOOL isOpaque,
                          D3DFORMAT *pFormat/*in/out*/,
                          DWORD dwUsage,
                          D3DResource **ppTextureResource/*out*/);

    HRESULT CreateRTSurface(UINT width, UINT height,
                            BOOL isOpaque, BOOL isLockable,
                            D3DFORMAT *pFormat/*in/out*/,
                            D3DResource ** ppSurfaceResource/*out*/);

    HRESULT CreateSwapChain(HWND hWnd, UINT numBuffers, UINT width, UINT height,
                            D3DSWAPEFFECT swapEffect, UINT presentationInterval,
                            D3DResource ** ppSwapChainResource/*out*/);

    HRESULT GetCachedDestTexture(D3DFORMAT format,
                                 D3DResource **ppTextureResource);
    HRESULT GetBlitTexture(D3DResource **ppTextureResource);
    HRESULT GetBlitRTTexture(UINT width, UINT height, D3DFORMAT format,
                             D3DResource **ppTextureResource);
    HRESULT GetBlitOSPSurface(UINT width, UINT height, D3DFORMAT fmt,
                              D3DResource **ppSurfaceResource);
    HRESULT GetMaskTexture(D3DResource **ppTextureResource);
    HRESULT GetGradientTexture(D3DResource **ppTextureResource);
    HRESULT GetMultiGradientTexture(D3DResource **ppTextureResource);
    HRESULT GetLookupOpLutTexture(D3DResource **ppTextureResource);
    HRESULT GetLockableRTSurface(UINT width, UINT height, D3DFORMAT format,
                                 D3DResource **ppSurfaceResource);

static
    HRESULT CreateInstance(D3DContext *pCtx, D3DResourceManager **ppResMgr);

private:
            D3DResourceManager();
    HRESULT GetStockTextureResource(UINT width, UINT height,
                                    BOOL isRTT, BOOL isOpaque,
                                    D3DFORMAT *pFormat/*in/out*/,
                                    DWORD dwUsage,
                                    D3DResource **ppTextureResource/*out*/);

    HRESULT CreateOSPSurface(UINT width, UINT height,
                             D3DFORMAT fmt,
                             D3DResource ** ppSurfaceResource/*out*/);

    D3DResource*      pCachedDestTexture;
    D3DResource*      pBlitTexture;
    D3DResource*      pBlitRTTexture;
    D3DResource*      pBlitOSPSurface;
    D3DResource*      pGradientTexture;
    D3DResource*      pLookupOpLutTexture;
    D3DResource*      pMaskTexture;
    D3DResource*      pMultiGradientTexture;
    D3DResource*      pLockableRTSurface;

    D3DContext*       pCtx;

    IManagedResource* pHead;
};
#endif _D3DRESOURCEMANAGER_H_
