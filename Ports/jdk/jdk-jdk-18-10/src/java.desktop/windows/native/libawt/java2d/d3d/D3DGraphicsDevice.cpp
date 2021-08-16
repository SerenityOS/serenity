/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

#include "sun_java2d_d3d_D3DGraphicsDevice.h"
#include "D3DGraphicsDevice.h"
#include "D3DPipelineManager.h"
#include "D3DRenderQueue.h"
#include "Trace.h"
#include "awt_Toolkit.h"
#include "awt_Window.h"

extern jobject CreateDisplayMode(JNIEnv* env, jint width, jint height,
                                 jint bitDepth, jint refreshRate);
extern void addDisplayMode(JNIEnv* env, jobject arrayList, jint width,
                           jint height, jint bitDepth, jint refreshRate);

extern "C" {
/*
 * Class:     sun_java2d_d3d_D3DGraphicsDevice
 * Method:    initD3D
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_sun_java2d_d3d_D3DGraphicsDevice_initD3D
  (JNIEnv *env, jclass)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DGD_initD3D");

    jboolean result = D3DInitializer::GetInstance().EnsureInited()
                      ? JNI_TRUE : JNI_FALSE;
    J2dTraceLn1(J2D_TRACE_INFO, "D3DGD_initD3D: result=%x", result);
    return result;
}

/*
 * Class:     sun_java2d_d3d_D3DGraphicsDevice
 * Method:    getDeviceIdNative
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_java2d_d3d_D3DGraphicsDevice_getDeviceIdNative
  (JNIEnv *env, jclass d3dsdc, jint gdiScreen)
{
    D3DPipelineManager *pMgr;
    UINT adapter;
    D3DADAPTER_IDENTIFIER9 aid;
    IDirect3D9 *pd3d9;

    J2dTraceLn(J2D_TRACE_INFO, "D3DGD_getDeviceIdNative");

    pMgr = D3DPipelineManager::GetInstance();
    RETURN_STATUS_IF_NULL(pMgr, NULL);
    pd3d9 = pMgr->GetD3DObject();
    RETURN_STATUS_IF_NULL(pd3d9, NULL);

    adapter = pMgr->GetAdapterOrdinalForScreen(gdiScreen);
    if (FAILED(pd3d9->GetAdapterIdentifier(adapter, 0, &aid))) {
        return NULL;
    }

    // ('%d.' will take no more than 6+1 chars since we are printing a WORD)
    //            AAAA&BBBB MAX_DEVICE_IDENTIFIER_STRING (%d.%d.%d.%d)0
    size_t len = (4+1+4  +1+MAX_DEVICE_IDENTIFIER_STRING+1 +1+(6+1)*4+1 +1);
    WCHAR *pAdapterId = new WCHAR[len];
    RETURN_STATUS_IF_NULL(pAdapterId, NULL);

    _snwprintf(pAdapterId, len, L"%x&%x %S (%d.%d.%d.%d)",
               0xffff & aid.VendorId, 0xffff & aid.DeviceId, aid.Description,
               HIWORD(aid.DriverVersion.HighPart),
               LOWORD(aid.DriverVersion.HighPart),
               HIWORD(aid.DriverVersion.LowPart),
               LOWORD(aid.DriverVersion.LowPart));
    // _snwprintf doesn't add 0 at the end if the formatted string didn't fit
    // in the buffer so we have to make sure it is null terminated
    pAdapterId[len-1] = (WCHAR)0;

    J2dTraceLn1(J2D_TRACE_VERBOSE, "  id=%S", pAdapterId);

    jstring ret = JNU_NewStringPlatform(env, pAdapterId);

    delete[] pAdapterId;

    return ret;
}

/*
 * Class:     sun_java2d_d3d_D3DGraphicsDevice
 * Method:    getDeviceCapsNative
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_sun_java2d_d3d_D3DGraphicsDevice_getDeviceCapsNative
  (JNIEnv *env, jclass d3dsdc, jint gdiScreen)
{
    D3DPipelineManager *pMgr;
    D3DContext *pCtx;
    UINT adapter;

    J2dRlsTraceLn(J2D_TRACE_INFO, "D3DGD_getDeviceCapsNative");

    pMgr = D3DPipelineManager::GetInstance();
    RETURN_STATUS_IF_NULL(pMgr, CAPS_EMPTY);
    adapter = pMgr->GetAdapterOrdinalForScreen(gdiScreen);

    if (FAILED(pMgr->GetD3DContext(adapter, &pCtx))) {
        J2dRlsTraceLn1(J2D_TRACE_ERROR,
                      "D3DGD_getDeviceCapsNative: device %d disabled", adapter);
        return CAPS_EMPTY;
    }
    return pCtx->GetContextCaps();
}

/*
 * Class:     sun_java2d_d3d_D3DGraphicsDevice
 * Method:    enterFullScreenExclusiveNative
 * Signature: (IJ)V
 */
JNIEXPORT jboolean JNICALL
Java_sun_java2d_d3d_D3DGraphicsDevice_enterFullScreenExclusiveNative
  (JNIEnv *env, jclass gdc, jint gdiScreen, jlong window)
{
    HRESULT res;
    D3DPipelineManager *pMgr;
    D3DContext *pCtx;
    HWND hWnd;
    AwtWindow *w;
    D3DPRESENT_PARAMETERS newParams, *pCurParams;
    D3DDISPLAYMODE dm;
    UINT adapter;

    J2dTraceLn(J2D_TRACE_INFO, "D3DGD_enterFullScreenExclusiveNative");

    RETURN_STATUS_IF_NULL(pMgr = D3DPipelineManager::GetInstance(), JNI_FALSE);
    adapter = pMgr->GetAdapterOrdinalForScreen(gdiScreen);

    if (FAILED(res = pMgr->GetD3DContext(adapter, &pCtx))) {
        D3DRQ_MarkLostIfNeeded(res, D3DRQ_GetCurrentDestination());
        return JNI_FALSE;
    }

    w = (AwtWindow *)AwtComponent::GetComponent((HWND)window);
    if (w == NULL || !::IsWindow(hWnd = w->GetTopLevelHWnd())) {
        J2dTraceLn(J2D_TRACE_WARNING,
                   "D3DGD_enterFullScreenExclusiveNative: disposed window");
        return JNI_FALSE;
    }

    // REMIND: should we also move the non-topleve window from
    // being on top here (it's moved to front in GraphicsDevice.setFSW())?

    pCtx->Get3DObject()->GetAdapterDisplayMode(adapter, &dm);
    pCurParams = pCtx->GetPresentationParams();

    // let the mananger know that we're entering the fs mode, it will
    // set the proper current focus window for us, which ConfigureContext will
    // use when creating the device
    pMgr->SetFSFocusWindow(adapter, hWnd);

    newParams = *pCurParams;
    newParams.hDeviceWindow = hWnd;
    newParams.Windowed = FALSE;
    newParams.BackBufferCount = 1;
    newParams.BackBufferFormat = dm.Format;
    newParams.FullScreen_RefreshRateInHz = dm.RefreshRate;
    newParams.BackBufferWidth = dm.Width;
    newParams.BackBufferHeight = dm.Height;
    newParams.PresentationInterval = D3DPRESENT_INTERVAL_DEFAULT;
    newParams.SwapEffect = D3DSWAPEFFECT_DISCARD;

    res = pCtx->ConfigureContext(&newParams);
    D3DRQ_MarkLostIfNeeded(res, D3DRQ_GetCurrentDestination());
    return SUCCEEDED(res);
}

/*
 * Class:     sun_java2d_d3d_D3DGraphicsDevice
 * Method:    exitFullScreenExclusiveNative
 * Signature: (I)V
 */
JNIEXPORT jboolean JNICALL
Java_sun_java2d_d3d_D3DGraphicsDevice_exitFullScreenExclusiveNative
  (JNIEnv *env, jclass gdc, jint gdiScreen)
{
    HRESULT res;
    D3DPipelineManager *pMgr;
    D3DContext *pCtx;
    D3DPRESENT_PARAMETERS newParams, *pCurParams;
    UINT adapter;

    J2dTraceLn(J2D_TRACE_INFO, "D3DGD_exitFullScreenExclusiveNative");

    RETURN_STATUS_IF_NULL(pMgr = D3DPipelineManager::GetInstance(), JNI_FALSE);
    adapter = pMgr->GetAdapterOrdinalForScreen(gdiScreen);

    if (FAILED(res = pMgr->GetD3DContext(adapter, &pCtx))) {
        D3DRQ_MarkLostIfNeeded(res, D3DRQ_GetCurrentDestination());
        return JNI_FALSE;
    }

    pCurParams = pCtx->GetPresentationParams();

    newParams = *pCurParams;
    // we're exiting fs, the device window can be 0
    newParams.hDeviceWindow = 0;
    newParams.Windowed = TRUE;
    newParams.BackBufferFormat = D3DFMT_UNKNOWN;
    newParams.BackBufferCount = 1;
    newParams.FullScreen_RefreshRateInHz = 0;
    newParams.BackBufferWidth = 0;
    newParams.BackBufferHeight = 0;
    newParams.PresentationInterval = D3DPRESENT_INTERVAL_IMMEDIATE;
    newParams.SwapEffect = D3DSWAPEFFECT_COPY;

    res = pCtx->ConfigureContext(&newParams);
    D3DRQ_MarkLostIfNeeded(res, D3DRQ_GetCurrentDestination());

    // exited fs, update current focus window
    // note that we call this after this adapter exited fs mode so that
    // the rest of the adapters can be reset
    pMgr->SetFSFocusWindow(adapter, 0);

    return SUCCEEDED(res);
}

/*
 * Class:     sun_java2d_d3d_D3DGraphicsDevice
 * Method:    configDisplayModeNative
 * Signature: (IJIIII)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_d3d_D3DGraphicsDevice_configDisplayModeNative
  (JNIEnv *env, jclass gdc, jint gdiScreen, jlong window,
   jint width, jint height, jint bitDepth, jint refreshRate)
{
    HRESULT res;
    D3DPipelineManager *pMgr;
    D3DContext *pCtx;
    D3DPRESENT_PARAMETERS newParams, *pCurParams;
    UINT adapter;

    J2dTraceLn(J2D_TRACE_INFO, "D3DGD_configDisplayModeNative");

    RETURN_IF_NULL(pMgr = D3DPipelineManager::GetInstance());
    adapter = pMgr->GetAdapterOrdinalForScreen(gdiScreen);

    if (FAILED(res = pMgr->GetD3DContext(adapter, &pCtx))) {
        D3DRQ_MarkLostIfNeeded(res, D3DRQ_GetCurrentDestination());
        return;
    }

    pCurParams = pCtx->GetPresentationParams();

    newParams = *pCurParams;
    newParams.BackBufferWidth = width;
    newParams.BackBufferHeight = height;
    newParams.FullScreen_RefreshRateInHz = refreshRate;
    newParams.PresentationInterval = D3DPRESENT_INTERVAL_DEFAULT;
    // we leave the swap effect so that it's more likely
    // to be the one user selected initially
//    newParams.SwapEffect = D3DSWAPEFFECT_DISCARD;

    if (bitDepth == 32) {
        newParams.BackBufferFormat = D3DFMT_X8R8G8B8;
    } else if (bitDepth == 16) {
        UINT modeNum;
        D3DDISPLAYMODE mode;
        IDirect3D9 *pd3d9;
        UINT modesCount;

        RETURN_IF_NULL(pd3d9 = pMgr->GetD3DObject());

        modesCount = pd3d9->GetAdapterModeCount(adapter, D3DFMT_R5G6B5);
        if (modesCount == 0) {
            modesCount = pd3d9->GetAdapterModeCount(adapter, D3DFMT_X1R5G5B5);
        }

        newParams.BackBufferFormat = D3DFMT_UNKNOWN;
        for (modeNum = 0; modeNum < modesCount; modeNum++) {
            if (SUCCEEDED(pd3d9->EnumAdapterModes(adapter, D3DFMT_R5G6B5,
                                                  modeNum, &mode)))
            {
                if (mode.Width == width && mode.Height == height &&
                    mode.RefreshRate == refreshRate)
                {
                    // prefer 565 over 555
                    if (mode.Format == D3DFMT_R5G6B5) {
                        newParams.BackBufferFormat = D3DFMT_R5G6B5;
                        break;
                    } else if (mode.Format == D3DFMT_X1R5G5B5) {
                        newParams.BackBufferFormat = D3DFMT_X1R5G5B5;
                    }
                }
            }
        }
        if (newParams.BackBufferFormat == D3DFMT_UNKNOWN) {
            J2dRlsTraceLn(J2D_TRACE_ERROR,
                          "D3DGD_configDisplayModeNative: no 16-bit formats");
            return;
        }
    } else {
        J2dRlsTraceLn1(J2D_TRACE_ERROR,
                       "D3DGD_configDisplayModeNative: unsupported depth: %d",
                       bitDepth);
        return;
    }

    J2dTraceLn4(J2D_TRACE_VERBOSE, "  changing to dm: %dx%dx%d@%d",
                newParams.BackBufferWidth, newParams.BackBufferHeight,
                bitDepth, refreshRate);
    J2dTraceLn1(J2D_TRACE_VERBOSE, "  selected backbuffer format: %d",
                newParams.BackBufferFormat);

    res = pCtx->ConfigureContext(&newParams);
    if (SUCCEEDED(res)) {
        // the full screen window doesn't receive WM_SIZE event when
        // the display mode changes (it does get resized though) so we need to
        // generate the event ourselves
        ::SendMessage(newParams.hDeviceWindow, WM_SIZE, width, height);
    }
    D3DRQ_MarkLostIfNeeded(res, D3DRQ_GetCurrentDestination());
}


/*
 * Class:     sun_java2d_d3d_D3DGraphicsDevice
 * Method:    getCurrentDisplayModeNative
 * Signature: (I)Ljava/awt/DisplayMode;
 */
JNIEXPORT jobject JNICALL
Java_sun_java2d_d3d_D3DGraphicsDevice_getCurrentDisplayModeNative
  (JNIEnv *env, jclass gdc, jint gdiScreen)
{
    D3DPipelineManager *pMgr;
    IDirect3D9 *pd3d9;
    jobject ret = NULL;
    D3DDISPLAYMODE mode;
    UINT adapter;

    J2dTraceLn(J2D_TRACE_INFO, "D3DGD_getCurrentDisplayModeNative");

    RETURN_STATUS_IF_NULL(pMgr = D3DPipelineManager::GetInstance(), NULL);
    RETURN_STATUS_IF_NULL(pd3d9 = pMgr->GetD3DObject(), NULL);
    adapter = pMgr->GetAdapterOrdinalForScreen(gdiScreen);

    if (SUCCEEDED(pd3d9->GetAdapterDisplayMode(adapter, &mode))) {
        int bitDepth = -1;
        // these are the only three valid screen formats
        switch (mode.Format) {
            case D3DFMT_X8R8G8B8: bitDepth = 32; break;
            case D3DFMT_R5G6B5:
            case D3DFMT_X1R5G5B5: bitDepth = 16; break;
        }
        J2dTraceLn4(J2D_TRACE_VERBOSE,
                    "  current dm: %dx%dx%d@%d",
                    mode.Width, mode.Height, bitDepth, mode.RefreshRate);
        ret = CreateDisplayMode(env, mode.Width, mode.Height, bitDepth,
                                mode.RefreshRate);
    }
    return ret;
}

/*
 * Class:     sun_java2d_d3d_D3DGraphicsDevice
 * Method:    enumDisplayModesNative
 * Signature: (ILjava/util/ArrayList;)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_d3d_D3DGraphicsDevice_enumDisplayModesNative
  (JNIEnv *env, jclass gdc, jint gdiScreen, jobject arrayList)
{
    D3DPipelineManager *pMgr;
    IDirect3D9 *pd3d9;
    jobject ret = NULL;
    D3DDISPLAYMODE mode;
    UINT formatNum, modeNum, modesCount;
    UINT adapter;
    // EnumAdapterModes treats 555 and 565 formats as equivalents
    static D3DFORMAT formats[] =
      { D3DFMT_X8R8G8B8, D3DFMT_R5G6B5 };

    J2dTraceLn(J2D_TRACE_INFO, "D3DGD_enumDisplayModesNative");

    RETURN_IF_NULL(pMgr = D3DPipelineManager::GetInstance());
    RETURN_IF_NULL(pd3d9 = pMgr->GetD3DObject());
    adapter = pMgr->GetAdapterOrdinalForScreen(gdiScreen);

    for (formatNum = 0; formatNum < (sizeof formats)/(sizeof *formats); formatNum++) {
        modesCount = pd3d9->GetAdapterModeCount(adapter, formats[formatNum]);
        for (modeNum = 0; modeNum < modesCount; modeNum++) {
            if (SUCCEEDED(pd3d9->EnumAdapterModes(adapter, formats[formatNum],
                                                  modeNum, &mode)))
            {
                int bitDepth = -1;
                // these are the only three valid screen formats,
                // 30-bit is returned as X8R8G8B8
                switch (mode.Format) {
                    case D3DFMT_X8R8G8B8: bitDepth = 32; break;
                    case D3DFMT_R5G6B5:
                    case D3DFMT_X1R5G5B5: bitDepth = 16; break;
                }
                J2dTraceLn4(J2D_TRACE_VERBOSE, "  found dm: %dx%dx%d@%d",
                            mode.Width, mode.Height, bitDepth,mode.RefreshRate);
                addDisplayMode(env, arrayList, mode.Width, mode.Height,
                               bitDepth, mode.RefreshRate);
            }
        }
    }
}

/*
 * Class:     sun_java2d_d3d_D3DGraphicsDevice
 * Method:    getAvailableAcceleratedMemoryNative
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL
Java_sun_java2d_d3d_D3DGraphicsDevice_getAvailableAcceleratedMemoryNative
  (JNIEnv *env, jclass gdc, jint gdiScreen)
{
    // REMIND: looks like Direct3D provides information about texture memory
    // only via IDirect3DDevice9::GetAvailableTextureMem, however, it
    // seems to report the same amount as direct draw used to.
    HRESULT res;
    D3DPipelineManager *pMgr;
    D3DContext *pCtx;
    IDirect3DDevice9 *pd3dDevice;
    UINT adapter;

    J2dTraceLn(J2D_TRACE_INFO, "D3DGD_getAvailableAcceleratedMemoryNative");

    RETURN_STATUS_IF_NULL(pMgr = D3DPipelineManager::GetInstance(), 0L);
    adapter = pMgr->GetAdapterOrdinalForScreen(gdiScreen);

    if (FAILED(res = pMgr->GetD3DContext(adapter, &pCtx))) {
        D3DRQ_MarkLostIfNeeded(res, D3DRQ_GetCurrentDestination());
        return 0L;
    }
    RETURN_STATUS_IF_NULL(pd3dDevice = pCtx->Get3DDevice(), 0L);

    UINT mem = pd3dDevice->GetAvailableTextureMem();
    J2dTraceLn1(J2D_TRACE_VERBOSE, "  available memory=%d", mem);
    return mem;
}

/*
 * Class:     sun_java2d_d3d_D3DGraphicsDevice
 * Method:    isD3DAvailableOnDeviceNative
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_java2d_d3d_D3DGraphicsDevice_isD3DAvailableOnDeviceNative
  (JNIEnv *env, jclass gdc, jint gdiScreen)
{
    HRESULT res;
    D3DPipelineManager *pMgr;
    D3DContext *pCtx;
    UINT adapter;

    J2dTraceLn(J2D_TRACE_INFO, "D3DGD_isD3DAvailableOnDeviceNative");

    RETURN_STATUS_IF_NULL(pMgr = D3DPipelineManager::GetInstance(), JNI_FALSE);
    adapter = pMgr->GetAdapterOrdinalForScreen(gdiScreen);

    if (FAILED(res = pMgr->GetD3DContext(adapter, &pCtx))) {
        D3DRQ_MarkLostIfNeeded(res, D3DRQ_GetCurrentDestination());
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

} // extern "C"
