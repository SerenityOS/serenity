/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

#include "D3DPipeline.h"
#include <jlong.h>
#include "D3DSurfaceData.h"
#include "D3DPipelineManager.h"
#include "Trace.h"
#include "awt_Toolkit.h"
#include "awt_Window.h"
#include "awt_BitmapUtil.h"
#include "D3DRenderQueue.h"


// REMIND: move to awt_Component.h
extern "C" HWND AwtComponent_GetHWnd(JNIEnv *env, jlong pData);

/* This looks weird. but since some AWT headers need to be included,
 * we end up with AWT's alloc.h macro definition of ExceptionOccurred.
 * The reasons for that re-defintion do not apply to this code, so undef it.
 */
#undef ExceptionOccurred

/**
 * Initializes nativeWidth/Height fields of the SurfaceData object with
 * dimensions on the native surface.
 */
void D3DSD_SetNativeDimensions(JNIEnv *env, D3DSDOps *d3dsdo) {
    jobject sdObject;
    jint width, height;

    RETURN_IF_NULL(sdObject = env->NewLocalRef(d3dsdo->sdOps.sdObject));

    if (d3dsdo->pResource != NULL) {
        width = d3dsdo->pResource->GetDesc()->Width;
        height = d3dsdo->pResource->GetDesc()->Height;
    } else {
        width = d3dsdo->width;
        height = d3dsdo->height;
    }

    JNU_SetFieldByName(env, NULL, sdObject, "nativeWidth", "I", width);
    if (!(env->ExceptionOccurred())) {
        JNU_SetFieldByName(env, NULL, sdObject, "nativeHeight", "I", height);
    }

    env->DeleteLocalRef(sdObject);
}

void D3DSD_Flush(void *pData)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DSD_Flush");
    RETURN_IF_NULL(pData);

    D3DSDOps *d3dsdo = (D3DSDOps*)pData;
    if (d3dsdo->pResource != NULL) {
        D3DContext *pCtx;
        D3DPipelineManager *pMgr;

        d3dsdo->pResource->SetSDOps(NULL);

        if ((pMgr = D3DPipelineManager::GetInstance()) != NULL &&
            SUCCEEDED(pMgr->GetD3DContext(d3dsdo->adapter, &pCtx)))
        {
            if (pCtx->GetResourceManager()) {
                pCtx->GetResourceManager()->ReleaseResource(d3dsdo->pResource);
            }
        }
        d3dsdo->pResource = NULL;
    }
}

void
D3DSD_MarkLost(void *pData)
{
    D3DSDOps *d3dsdo;
    jobject sdObject;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    J2dTraceLn(J2D_TRACE_INFO, "D3DSD_MarkLost");

    RETURN_IF_NULL(pData);

    d3dsdo = (D3DSDOps*)pData;
    RETURN_IF_NULL(sdObject = env->NewLocalRef(d3dsdo->sdOps.sdObject));

    JNU_CallMethodByName(env, NULL, sdObject,
                         "setSurfaceLost", "(Z)V", JNI_TRUE);

    env->DeleteLocalRef(sdObject);
}

// ------------ generic SurfaceData.h functions ----------------

void
D3DSD_Dispose(JNIEnv *env, SurfaceDataOps *ops)
{
    D3DSDOps *d3dsdo = (D3DSDOps *)ops;
    RETURN_IF_NULL(d3dsdo);

    JNU_CallStaticMethodByName(env, NULL, "sun/java2d/d3d/D3DSurfaceData",
                               "dispose", "(J)V",
                               ptr_to_jlong(ops));
}

/**
 * This is the implementation of the general surface LockFunc defined in
 * SurfaceData.h.
 */
jint
D3DSD_Lock(JNIEnv *env,
           SurfaceDataOps *ops,
           SurfaceDataRasInfo *pRasInfo,
           jint lockflags)
{
    JNU_ThrowInternalError(env, "D3DSD_Lock not implemented!");
    return SD_FAILURE;
}

/**
 * This is the implementation of the general GetRasInfoFunc defined in
 * SurfaceData.h.
 */
void
D3DSD_GetRasInfo(JNIEnv *env,
                 SurfaceDataOps *ops,
                 SurfaceDataRasInfo *pRasInfo)
{
    JNU_ThrowInternalError(env, "D3DSD_GetRasInfo not implemented!");
}

/**
 * This is the implementation of the general surface UnlockFunc defined in
 * SurfaceData.h.
 */
void
D3DSD_Unlock(JNIEnv *env,
             SurfaceDataOps *ops,
             SurfaceDataRasInfo *pRasInfo)
{
    JNU_ThrowInternalError(env, "D3DSD_Unlock not implemented!");
}

// ------------ D3DSurfaceData's JNI methods ----------------


extern "C" {

/*
 * Class:     sun_java2d_d3d_D3DSurfaceData
 * Method:    initOps
 * Signature: (III)V
 */
JNIEXPORT void
JNICALL Java_sun_java2d_d3d_D3DSurfaceData_initOps
  (JNIEnv *env, jobject d3dsd, jint gdiScreen, jint width, jint height)
{
    D3DPipelineManager *pMgr;
    D3DSDOps *d3dsdo = (D3DSDOps *)SurfaceData_InitOps(env, d3dsd,
                                                       sizeof(D3DSDOps));

    J2dTraceLn(J2D_TRACE_INFO, "D3DSurfaceData_initOps");

    if (d3dsdo == NULL) {
        JNU_ThrowOutOfMemoryError(env, "creating native d3d ops");
        return;
    }

    d3dsdo->sdOps.Lock       = D3DSD_Lock;
    d3dsdo->sdOps.GetRasInfo = D3DSD_GetRasInfo;
    d3dsdo->sdOps.Unlock     = D3DSD_Unlock;
    d3dsdo->sdOps.Dispose    = D3DSD_Dispose;

    d3dsdo->xoff = 0;
    d3dsdo->yoff = 0;
    d3dsdo->width = width;
    d3dsdo->height = height;

    d3dsdo->pResource = NULL;

    d3dsdo->adapter =
        (pMgr = D3DPipelineManager::GetInstance()) == NULL ?
            D3DADAPTER_DEFAULT :
            pMgr->GetAdapterOrdinalForScreen(gdiScreen);
}


/*
 * Class:     sun_java2d_d3d_D3DSurfaceData
 * Method:    initTexture
 * Signature: (JZZ)Z
 */
JNIEXPORT jboolean
JNICALL Java_sun_java2d_d3d_D3DSurfaceData_initTexture
  (JNIEnv *env, jobject d3dsd,
  jlong pData, jboolean isRTT, jboolean isOpaque)
{
    HRESULT res;
    D3DSDOps *d3dsdo;
    D3DContext *pCtx;
    D3DPipelineManager *pMgr;
    D3DFORMAT format;

    J2dTraceLn(J2D_TRACE_INFO, "D3DSurfaceData_initTexture");

    RETURN_STATUS_IF_NULL(d3dsdo = (D3DSDOps *)jlong_to_ptr(pData), JNI_FALSE);
    RETURN_STATUS_IF_NULL(pMgr = D3DPipelineManager::GetInstance(), JNI_FALSE);

    if (FAILED(res = pMgr->GetD3DContext(d3dsdo->adapter, &pCtx))) {
        D3DRQ_MarkLostIfNeeded(res, d3dsdo);
        return JNI_FALSE;
    }
    RETURN_STATUS_IF_NULL(pCtx->GetResourceManager(), JNI_FALSE);

    pCtx->GetResourceManager()->ReleaseResource(d3dsdo->pResource);
    d3dsdo->pResource = NULL;

    if (isRTT && isOpaque) {
        format = pCtx->GetPresentationParams()->BackBufferFormat;
    } else {
        format = D3DFMT_UNKNOWN;
    }

    res = pCtx->GetResourceManager()->
        CreateTexture(d3dsdo->width, d3dsdo->height,
                      isRTT, isOpaque,
                      &format, 0/*usage*/, &d3dsdo->pResource);
    if (SUCCEEDED(res)) {
        J2dTraceLn1(J2D_TRACE_VERBOSE,
                    "  created texture pResource=%x", d3dsdo->pResource);
        d3dsdo->pResource->SetSDOps(d3dsdo);
    } else {
        D3DRQ_MarkLostIfNeeded(res, d3dsdo);
    }
    D3DSD_SetNativeDimensions(env, d3dsdo);

    return SUCCEEDED(res);
}

/*
 * Class:     sun_java2d_d3d_D3DSurfaceData
 * Method:    initPlain
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_java2d_d3d_D3DSurfaceData_initRTSurface
  (JNIEnv *env, jobject d3dsd, jlong pData, jboolean isOpaque)
{
    HRESULT res;
    D3DSDOps *d3dsdo;
    D3DContext *pCtx;
    D3DPipelineManager *pMgr;
    D3DFORMAT format = D3DFMT_UNKNOWN;

    J2dTraceLn(J2D_TRACE_INFO, "D3DSurfaceData_initRTSurface");

    RETURN_STATUS_IF_NULL(d3dsdo = (D3DSDOps *)jlong_to_ptr(pData), JNI_FALSE);
    RETURN_STATUS_IF_NULL(pMgr = D3DPipelineManager::GetInstance(), JNI_FALSE);

    if (FAILED(res = pMgr->GetD3DContext(d3dsdo->adapter, &pCtx))) {
        D3DRQ_MarkLostIfNeeded(res, d3dsdo);
        return JNI_FALSE;
    }
    RETURN_STATUS_IF_NULL(pCtx->GetResourceManager(), JNI_FALSE);

    pCtx->GetResourceManager()->ReleaseResource(d3dsdo->pResource);
    d3dsdo->pResource = NULL;

    res = pCtx->GetResourceManager()->
            CreateRTSurface(d3dsdo->width, d3dsdo->height,
                            isOpaque, FALSE /*lockable*/,
                            &format, &d3dsdo->pResource);
    if (SUCCEEDED(res)) {
        J2dTraceLn1(J2D_TRACE_VERBOSE, "  created RT surface pResource=0x%x",
                    d3dsdo->pResource);
        d3dsdo->pResource->SetSDOps(d3dsdo);
    } else {
        D3DRQ_MarkLostIfNeeded(res, d3dsdo);
    }
    D3DSD_SetNativeDimensions(env, d3dsdo);

    return SUCCEEDED(res);
}

/*
 * Class:     sun_java2d_d3d_D3DSurfaceData
 * Method:    initFlipBackbuffer
 * Signature: (JJIZ)Z
 */
JNIEXPORT jboolean
JNICALL Java_sun_java2d_d3d_D3DSurfaceData_initFlipBackbuffer
  (JNIEnv *env, jobject d3dsd, jlong pData, jlong pPeerData,
  jint numBuffers, jint swapEffect,
  jint vSyncType)
{
    HRESULT res;
    D3DSDOps *d3dsdo;
    D3DContext *pCtx;
    D3DPipelineManager *pMgr;
    HWND hWnd;
    UINT presentationInterval;
    AwtComponent *pPeer;
    RECT r = { 0, 0, 0, 0 };

    J2dTraceLn(J2D_TRACE_INFO, "D3DSurfaceData_initFlipBackbuffer");

    RETURN_STATUS_IF_NULL(d3dsdo = (D3DSDOps *)jlong_to_ptr(pData), JNI_FALSE);
    RETURN_STATUS_IF_NULL(pMgr = D3DPipelineManager::GetInstance(), JNI_FALSE);
    RETURN_STATUS_IF_NULL(pPeer = (AwtComponent *)jlong_to_ptr(pPeerData),
                          JNI_FALSE);

    hWnd = pPeer->GetHWnd();
    if (!IsWindow(hWnd)) {
        J2dTraceLn(J2D_TRACE_WARNING,
                   "D3DSurfaceData_initFlipBackbuffer: disposed component");
        return JNI_FALSE;
    }

    pPeer->GetInsets(&r);
    d3dsdo->xoff = -r.left;
    d3dsdo->yoff = -r.top;

    if (FAILED(res = pMgr->GetD3DContext(d3dsdo->adapter, &pCtx))) {
        D3DRQ_MarkLostIfNeeded(res, d3dsdo);
        return JNI_FALSE;
    }
    RETURN_STATUS_IF_NULL(pCtx->GetResourceManager(), JNI_FALSE);

    pCtx->GetResourceManager()->ReleaseResource(d3dsdo->pResource);
    d3dsdo->pResource = NULL;

    d3dsdo->swapEffect = (D3DSWAPEFFECT)swapEffect;

    // in full-screen mode we should v-sync
    if (pCtx->GetPresentationParams()->Windowed) {
        if (vSyncType == VSYNC_ON) {
            presentationInterval = D3DPRESENT_INTERVAL_ONE;
            J2dTraceLn(J2D_TRACE_VERBOSE,
                       "  windowed, forced interval: ONE");
        } else {
            presentationInterval = D3DPRESENT_INTERVAL_IMMEDIATE;
            J2dTraceLn(J2D_TRACE_VERBOSE,
                       "  windowed, default interval: IMMEDIATE");
        }

        // REMIND: this is a workaround for the current issue
        // we have with non-copy flip chains: since we can not specify
        // the dest rectangle for Present for these modes, the result of
        // Present(NULL, NULL) is scaled to the client area.
        if (d3dsdo->xoff != 0 || d3dsdo->yoff != 0) {
            d3dsdo->swapEffect = D3DSWAPEFFECT_COPY;
        }
    } else {
        if (vSyncType == VSYNC_OFF) {
            presentationInterval = D3DPRESENT_INTERVAL_IMMEDIATE;
            J2dTraceLn(J2D_TRACE_VERBOSE,
                       "  full-screen, forced interval: IMMEDIATE");
        } else {
            presentationInterval = D3DPRESENT_INTERVAL_ONE;
            J2dTraceLn(J2D_TRACE_VERBOSE,
                       "  full-screen, default interval: ONE");
        }
    }

    res = pCtx->GetResourceManager()->
        CreateSwapChain(hWnd, numBuffers,
                        d3dsdo->width, d3dsdo->height,
                        d3dsdo->swapEffect, presentationInterval,
                        &d3dsdo->pResource);
    if (SUCCEEDED(res)) {
        J2dTraceLn1(J2D_TRACE_VERBOSE, "  created swap chain pResource=0x%x",
                    d3dsdo->pResource);
        d3dsdo->pResource->SetSDOps(d3dsdo);
    } else {
        D3DRQ_MarkLostIfNeeded(res, d3dsdo);
    }
    D3DSD_SetNativeDimensions(env, d3dsdo);

    return SUCCEEDED(res);
}

/*
 * Class:     sun_java2d_d3d_D3DSurfaceData
 * Method:    dbGetPixelNative
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL Java_sun_java2d_d3d_D3DSurfaceData_dbGetPixelNative
  (JNIEnv *env, jclass clazz, jlong pData, jint x, jint y)
{
    HRESULT res;
    D3DSDOps *d3dsdo;
    D3DContext *pCtx;
    D3DPipelineManager *pMgr;
    D3DResource *pLockableRes;
    jint pixel = 0;

    J2dTraceLn(J2D_TRACE_INFO, "D3DSurfaceData_dbGetPixelNative");

    RETURN_STATUS_IF_NULL(d3dsdo = (D3DSDOps *)jlong_to_ptr(pData), pixel);
    RETURN_STATUS_IF_NULL(d3dsdo->pResource, pixel);
    RETURN_STATUS_IF_NULL(pMgr = D3DPipelineManager::GetInstance(), pixel);

    if (FAILED(res = pMgr->GetD3DContext(d3dsdo->adapter, &pCtx))) {
        D3DRQ_MarkLostIfNeeded(res, d3dsdo);
        return pixel;
    }
    RETURN_STATUS_IF_NULL(pCtx->GetResourceManager(), 0);

    IDirect3DDevice9 *pd3dDevice = pCtx->Get3DDevice();
    IDirect3DSurface9 *pSrc = d3dsdo->pResource->GetSurface();
    D3DFORMAT srcFmt = d3dsdo->pResource->GetDesc()->Format;

    pCtx->UpdateState(STATE_OTHEROP);

    res = pCtx->GetResourceManager()->
            GetLockableRTSurface(1, 1, srcFmt, &pLockableRes);
    if (SUCCEEDED(res)) {
        IDirect3DSurface9 *pTmpSurface;
        RECT srcRect = { x, y, x+1, y+1};
        RECT dstRect = { 0l, 0l, 1, 1 };

        pTmpSurface = pLockableRes->GetSurface();
        res = pd3dDevice->StretchRect(pSrc, &srcRect, pTmpSurface, &dstRect,
                                      D3DTEXF_NONE);
        if (SUCCEEDED(res)) {
            D3DLOCKED_RECT lRect;

            res = pTmpSurface->LockRect(&lRect, &dstRect, D3DLOCK_NOSYSLOCK);
            if (SUCCEEDED(res)) {
                if (srcFmt == D3DFMT_X8R8G8B8) {
                    pixel = *(jint*)lRect.pBits;
                } else {
                    pixel = *(unsigned short*)lRect.pBits;
                }
                pTmpSurface->UnlockRect();
            }
        }
    }
    D3DRQ_MarkLostIfNeeded(res, d3dsdo);

    return pixel;
}

/*
 * Class:     sun_java2d_d3d_D3DSurfaceData
 * Method:    dbSetPixelNative
 * Signature: (JIII)V
 */
JNIEXPORT void JNICALL Java_sun_java2d_d3d_D3DSurfaceData_dbSetPixelNative
  (JNIEnv *env, jclass clazz, jlong pData, jint x, jint y, jint pixel)
{
    HRESULT res;
    D3DSDOps *d3dsdo;
    D3DResource *pLockableRes;
    D3DContext *pCtx;
    D3DPipelineManager *pMgr;

    J2dTraceLn(J2D_TRACE_INFO, "D3DSurfaceData_dbSetPixelNative");

    RETURN_IF_NULL(d3dsdo = (D3DSDOps *)jlong_to_ptr(pData));
    RETURN_IF_NULL(d3dsdo->pResource);
    RETURN_IF_NULL(pMgr = D3DPipelineManager::GetInstance());

    if (FAILED(res = pMgr->GetD3DContext(d3dsdo->adapter, &pCtx))) {
        D3DRQ_MarkLostIfNeeded(res, d3dsdo);
        return;
    }
    RETURN_IF_NULL(pCtx->GetResourceManager());

    IDirect3DDevice9 *pd3dDevice = pCtx->Get3DDevice();
    IDirect3DSurface9 *pSrc = d3dsdo->pResource->GetSurface();
    D3DFORMAT srcFmt = d3dsdo->pResource->GetDesc()->Format;

    pCtx->UpdateState(STATE_OTHEROP);

    res = pCtx->GetResourceManager()->
            GetLockableRTSurface(1, 1, srcFmt, &pLockableRes);
    if (SUCCEEDED(res)) {
        IDirect3DSurface9 *pTmpSurface;
        D3DLOCKED_RECT lRect;
        RECT srcRect = { 0l, 0l, 1, 1 };
        RECT dstRect = { x, y, x+1, y+1};

        pTmpSurface = pLockableRes->GetSurface();
        res = pTmpSurface->LockRect(&lRect, &srcRect, D3DLOCK_NOSYSLOCK);
        if (SUCCEEDED(res)) {
            if (srcFmt == D3DFMT_X8R8G8B8) {
                *(jint*)lRect.pBits = pixel;
            } else {
                *(unsigned short*)lRect.pBits = (unsigned short)pixel;
            }
            pTmpSurface->UnlockRect();

            res = pd3dDevice->StretchRect(pTmpSurface, &srcRect, pSrc, &dstRect,
                                          D3DTEXF_NONE);
        }
    }
    D3DRQ_MarkLostIfNeeded(res, d3dsdo);
}

/*
 * Class:     sun_java2d_d3d_D3DSurfaceData
 * Method:    getNativeResourceNative
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL
    Java_sun_java2d_d3d_D3DSurfaceData_getNativeResourceNative
        (JNIEnv *env, jclass d3sdc, jlong pData, jint resType)
{
    D3DSDOps *d3dsdo;

    J2dTraceLn(J2D_TRACE_INFO, "D3DSurfaceData_getNativeResourceNative")

    RETURN_STATUS_IF_NULL(d3dsdo = (D3DSDOps *)jlong_to_ptr(pData), 0L);

    if (resType == D3D_DEVICE_RESOURCE) {
        HRESULT res;
        D3DPipelineManager *pMgr;
        D3DContext *pCtx;

        RETURN_STATUS_IF_NULL(pMgr = D3DPipelineManager::GetInstance(), 0L);
        if (FAILED(res = pMgr->GetD3DContext(d3dsdo->adapter, &pCtx))) {
            D3DRQ_MarkLostIfNeeded(res, d3dsdo);
            return 0L;
        }
        return ptr_to_jlong(pCtx->Get3DDevice());
    }

    RETURN_STATUS_IF_NULL(d3dsdo->pResource, 0L);

    if (resType == RT_PLAIN || resType == RT_TEXTURE) {
        return ptr_to_jlong(d3dsdo->pResource->GetSurface());
    }
    if (resType == TEXTURE) {
        return ptr_to_jlong(d3dsdo->pResource->GetTexture());
    }
    if (resType == FLIP_BACKBUFFER) {
        return ptr_to_jlong(d3dsdo->pResource->GetSwapChain());
    }

    return 0L;
}

/*
 * Class:     sun_java2d_d3d_D3DSurfaceData
 * Method:    updateWindowAccelImpl
 * Signature: (JJII)Z
 */
JNIEXPORT jboolean
JNICALL Java_sun_java2d_d3d_D3DSurfaceData_updateWindowAccelImpl
  (JNIEnv *env, jclass clazz, jlong pd3dsd, jlong pData, jint w, jint h)
{
    HRESULT res;
    AwtWindow *window;
    HBITMAP hBitmap = NULL;
    D3DSDOps *d3dsdo;
    D3DResource *pSrcRes;
    D3DContext *pCtx;
    D3DPipelineManager *pMgr;
    D3DResource *pLockableRes = NULL;
    IDirect3DSurface9 *pTmpSurface = NULL;
    IDirect3DDevice9 *pd3dDevice = NULL;
    D3DLOCKED_RECT lockedRect;

    J2dTraceLn(J2D_TRACE_ERROR, "D3DSurfaceData_updateWindowAccelImpl");

    if (w <= 0 || h <= 0) {
        return JNI_TRUE;
    }

    RETURN_STATUS_IF_NULL(window = (AwtWindow *)jlong_to_ptr(pData), JNI_FALSE);
    RETURN_STATUS_IF_NULL(d3dsdo = (D3DSDOps *)jlong_to_ptr(pd3dsd), JNI_FALSE);
    RETURN_STATUS_IF_NULL(pMgr = D3DPipelineManager::GetInstance(), JNI_FALSE);
    RETURN_STATUS_IF_NULL(pSrcRes = d3dsdo->pResource, JNI_FALSE);

    if (FAILED(res = pMgr->GetD3DContext(d3dsdo->adapter, &pCtx))) {
        D3DRQ_MarkLostIfNeeded(res, d3dsdo);
        return JNI_FALSE;
    }

    RETURN_STATUS_IF_NULL(pd3dDevice = pCtx->Get3DDevice(), JNI_FALSE);
    pCtx->UpdateState(STATE_OTHEROP);

    res = pCtx->GetResourceManager()->
            GetBlitOSPSurface(pSrcRes->GetDesc()->Width,
                              pSrcRes->GetDesc()->Height,
                              pSrcRes->GetDesc()->Format,
                              &pLockableRes);
    if (FAILED(res)) {
        D3DRQ_MarkLostIfNeeded(res, d3dsdo);
        return JNI_FALSE;
    }
    pTmpSurface = pLockableRes->GetSurface();

    res = pd3dDevice->GetRenderTargetData(pSrcRes->GetSurface(), pTmpSurface);
    if (FAILED(res)) {
        D3DRQ_MarkLostIfNeeded(res, d3dsdo);
        return JNI_FALSE;
    }

    res = pTmpSurface->LockRect(&lockedRect, NULL, D3DLOCK_NOSYSLOCK);
    if (SUCCEEDED(res)) {
        hBitmap =
            BitmapUtil::CreateBitmapFromARGBPre(w, h,
                                                lockedRect.Pitch,
                                                (int*)lockedRect.pBits);
        pTmpSurface->UnlockRect();
    }
    RETURN_STATUS_IF_NULL(hBitmap, JNI_FALSE);

    window->UpdateWindow(env, NULL, w, h, hBitmap);

    // hBitmap is released in UpdateWindow

    return JNI_TRUE;
}
}
