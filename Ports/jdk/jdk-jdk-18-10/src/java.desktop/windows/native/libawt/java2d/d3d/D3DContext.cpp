/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "jlong.h"

#include "GraphicsPrimitiveMgr.h"
#include "D3DContext.h"
#include "D3DSurfaceData.h"
#include "D3DBufImgOps.h"
#include "D3DPaints.h"
#include "D3DRenderQueue.h"
#include "D3DShaders.h"
#include "D3DTextRenderer.h"
#include "D3DPipelineManager.h"
#include "D3DGlyphCache.h"

typedef struct {
    D3DBLEND src;
    D3DBLEND dst;
} D3DBlendRule;

/**
 * This table contains the standard blending rules (or Porter-Duff compositing
 * factors) used in SetRenderState(), indexed by the rule constants from the
 * AlphaComposite class.
 */
D3DBlendRule StdBlendRules[] = {
    { D3DBLEND_ZERO,         D3DBLEND_ZERO        }, /* 0 - Nothing      */
    { D3DBLEND_ZERO,         D3DBLEND_ZERO        }, /* 1 - RULE_Clear   */
    { D3DBLEND_ONE,          D3DBLEND_ZERO        }, /* 2 - RULE_Src     */
    { D3DBLEND_ONE,          D3DBLEND_INVSRCALPHA }, /* 3 - RULE_SrcOver */
    { D3DBLEND_INVDESTALPHA, D3DBLEND_ONE         }, /* 4 - RULE_DstOver */
    { D3DBLEND_DESTALPHA,    D3DBLEND_ZERO        }, /* 5 - RULE_SrcIn   */
    { D3DBLEND_ZERO,         D3DBLEND_SRCALPHA    }, /* 6 - RULE_DstIn   */
    { D3DBLEND_INVDESTALPHA, D3DBLEND_ZERO        }, /* 7 - RULE_SrcOut  */
    { D3DBLEND_ZERO,         D3DBLEND_INVSRCALPHA }, /* 8 - RULE_DstOut  */
    { D3DBLEND_ZERO,         D3DBLEND_ONE         }, /* 9 - RULE_Dst     */
    { D3DBLEND_DESTALPHA,    D3DBLEND_INVSRCALPHA }, /*10 - RULE_SrcAtop */
    { D3DBLEND_INVDESTALPHA, D3DBLEND_SRCALPHA    }, /*11 - RULE_DstAtop */
    { D3DBLEND_INVDESTALPHA, D3DBLEND_INVSRCALPHA }, /*12 - RULE_AlphaXor*/
};

void
D3DUtils_SetOrthoMatrixOffCenterLH(D3DMATRIX *m,
                                   float width, float height)
{
    ZeroMemory(m, sizeof(D3DMATRIX));
    m->_11 =  2.0f/width;
    m->_22 = -2.0f/height;
    m->_33 =  0.5f;
    m->_44 =  1.0f;

    m->_41 = -1.0f;
    m->_42 =  1.0f;
    m->_43 =  0.5f;
}

void
D3DUtils_SetIdentityMatrix(D3DMATRIX *m)
{
    m->_12 = m->_13 = m->_14 = m->_21 = m->_23 = m->_24 = 0.0f;
    m->_31 = m->_32 = m->_34 = m->_41 = m->_42 = m->_43 = 0.0f;
    m->_11 = m->_22 = m->_33 = m->_44 = 1.0f;
}

// the following methods are copies of the AffineTransform's class
// corresponding methods, with these changes to the indexes:
// 00 -> 11
// 11 -> 22
// 01 -> 21
// 10 -> 12
// 02 -> 41
// 12 -> 42

void
D3DUtils_2DConcatenateM(D3DMATRIX *m, D3DMATRIX *m1)
{
    float M0, M1;
    float T00, T10, T01, T11;
    float T02, T12;

    T00 = m1->_11; T01 = m1->_21; T02 = m1->_41;
    T10 = m1->_12; T11 = m1->_22; T12 = m1->_42;

    M0 = m->_11;
    M1 = m->_21;
    m->_11  = T00 * M0 + T10 * M1;
    m->_21  = T01 * M0 + T11 * M1;
    m->_41 += T02 * M0 + T12 * M1;

    M0 = m->_12;
    M1 = m->_22;
    m->_12  = T00 * M0 + T10 * M1;
    m->_22  = T01 * M0 + T11 * M1;
    m->_42 += T02 * M0 + T12 * M1;
}

#ifdef UPDATE_TX

void
D3DUtils_2DScaleM(D3DMATRIX *m, float sx, float sy)
{
    m->_11 *= sx;
    m->_22 *= sy;
}

void
D3DUtils_2DInvertM(D3DMATRIX *m)
{
    float M11, M21, M41;
    float M12, M22, M42;
    float det;

    M11 = m->_11; M21 = m->_21; M41 = m->_41;
    M12 = m->_12; M22 = m->_22; M42 = m->_42;
    det = M11 * M22 - M21 * M12;
    if (fabs(det) <= 0.0000000001f) {
        memset(m, 0, sizeof(D3DMATRIX));
        return;
    }
    m->_11 =  M22 / det;
    m->_12 = -M12 / det;
    m->_21 = -M21 / det;
    m->_22 =  M11 / det;
    m->_41 = (M21 * M42 - M22 * M41) / det;
    m->_42 = (M12 * M41 - M11 * M42) / det;
}

void
D3DUtils_2DTranslateM(D3DMATRIX *m, float tx, float ty)
{
    m->_41 = tx * m->_11 + ty * m->_21 + m->_41;
    m->_42 = tx * m->_12 + ty * m->_22 + m->_42;
}

void
D3DUtils_2DTransformXY(D3DMATRIX *m, float *px, float *py)
{
    float x = *px;
    float y = *py;

    *px = x * m->_11 + y * m->_21 + m->_41;
    *py = x * m->_12 + y * m->_22 + m->_42;
}

void
D3DUtils_2DInverseTransformXY(D3DMATRIX *m, float *px, float *py)
{
    float x = *px, y = *py;

    x -= m->_41;
    y -= m->_42;

    float det = m->_11 * m->_22 - m->_21 * m->_12;
    if (fabs(det) < 0.0000000001f) {
        *px = 0.0f;
        *py = 0.0f;
    } else {
        *px = (x * m->_22 - y * m->_21) / det;
        *py = (y * m->_11 - x * m->_12) / det;
    }
}

#endif // UPDATE_TX

static void
D3DContext_DisposeShader(jlong programID)
{
    IDirect3DPixelShader9 *shader =
        (IDirect3DPixelShader9 *)jlong_to_ptr(programID);

    J2dTraceLn(J2D_TRACE_INFO, "D3DContext_DisposeShader");

    SAFE_RELEASE(shader);
}

// static
HRESULT
D3DContext::CreateInstance(IDirect3D9 *pd3d9, UINT adapter, D3DContext **ppCtx)
{
    HRESULT res;
    *ppCtx = new D3DContext(pd3d9, adapter);
    if (FAILED(res = (*ppCtx)->InitContext())) {
        delete *ppCtx;
        *ppCtx = NULL;
    }
    return res;
}

D3DContext::D3DContext(IDirect3D9 *pd3d, UINT adapter)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::D3DContext");
    J2dTraceLn1(J2D_TRACE_VERBOSE, "  pd3d=0x%x", pd3d);
    pd3dObject = pd3d;
    pd3dDevice = NULL;
    adapterOrdinal = adapter;

    pResourceMgr = NULL;
    pMaskCache = NULL;
    pVCacher = NULL;

    pSyncQuery = NULL;
    pSyncRTRes = NULL;
    pStateBlock = NULL;

    D3DC_INIT_SHADER_LIST(convolvePrograms,   MAX_CONVOLVE);
    D3DC_INIT_SHADER_LIST(rescalePrograms,    MAX_RESCALE);
    D3DC_INIT_SHADER_LIST(lookupPrograms,     MAX_LOOKUP);
    D3DC_INIT_SHADER_LIST(basicGradPrograms,  4);
    D3DC_INIT_SHADER_LIST(linearGradPrograms, 8);
    D3DC_INIT_SHADER_LIST(radialGradPrograms, 8);

    pLCDGlyphCache= NULL;
    pGrayscaleGlyphCache= NULL;
    lcdTextProgram = NULL;
    aaPgramProgram = NULL;

    contextCaps = CAPS_EMPTY;
    bBeginScenePending = FALSE;

    ZeroMemory(&devCaps, sizeof(D3DCAPS9));
    ZeroMemory(&curParams, sizeof(curParams));

    extraAlpha = 1.0f;
}

void D3DContext::ReleaseDefPoolResources()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::ReleaseDefPoolResources");

    EndScene();

    contextCaps = CAPS_EMPTY;

    SAFE_RELEASE(pSyncQuery);
    SAFE_RELEASE(pStateBlock);

    if (pVCacher != NULL) {
        pVCacher->ReleaseDefPoolResources();
    }
    if (pMaskCache != NULL) {
        pMaskCache->ReleaseDefPoolResources();
    }
    if (pLCDGlyphCache != NULL) {
        pLCDGlyphCache->ReleaseDefPoolResources();
    }
    if (pGrayscaleGlyphCache != NULL) {
        pGrayscaleGlyphCache->ReleaseDefPoolResources();
    }
    if (pResourceMgr != NULL) {
        if (pSyncRTRes != NULL) {
            pResourceMgr->ReleaseResource(pSyncRTRes);
            pSyncRTRes = NULL;
        }
        pResourceMgr->ReleaseDefPoolResources();
    }
    ZeroMemory(lastTexture, sizeof(lastTexture));
    ZeroMemory(lastTextureColorState, sizeof(lastTextureColorState));
}

void D3DContext::ReleaseContextResources()
{
    J2dTraceLn1(J2D_TRACE_INFO,
                "D3DContext::ReleaseContextResources: pd3dDevice = 0x%x",
                pd3dDevice);

    ReleaseDefPoolResources();

    // dispose shader lists
    ShaderList_Dispose(&convolvePrograms);
    ShaderList_Dispose(&rescalePrograms);
    ShaderList_Dispose(&lookupPrograms);
    ShaderList_Dispose(&basicGradPrograms);
    ShaderList_Dispose(&linearGradPrograms);
    ShaderList_Dispose(&radialGradPrograms);

    SAFE_DELETE(pLCDGlyphCache);
    SAFE_DELETE(pGrayscaleGlyphCache);

    SAFE_RELEASE(lcdTextProgram);
    SAFE_RELEASE(aaPgramProgram);

    SAFE_DELETE(pVCacher);
    SAFE_DELETE(pMaskCache);
    SAFE_DELETE(pResourceMgr);
}

D3DContext::~D3DContext() {
    J2dTraceLn2(J2D_TRACE_INFO,
                "~D3DContext: pd3dDevice=0x%x, pd3dObject =0x%x",
                pd3dDevice, pd3dObject);
    ReleaseContextResources();
    SAFE_RELEASE(pd3dDevice);
}

HRESULT
D3DContext::InitDevice(IDirect3DDevice9 *pd3dDevice)
{
    HRESULT res = S_OK;

    pd3dDevice->GetDeviceCaps(&devCaps);

    J2dRlsTraceLn1(J2D_TRACE_INFO,
                   "D3DContext::InitDevice: device %d", adapterOrdinal);

    // disable some of the unneeded and costly d3d functionality
    pd3dDevice->SetRenderState(D3DRS_CULLMODE, D3DCULL_NONE);
    pd3dDevice->SetRenderState(D3DRS_SPECULARENABLE, FALSE);
    pd3dDevice->SetRenderState(D3DRS_LIGHTING,  FALSE);
    pd3dDevice->SetRenderState(D3DRS_CLIPPING,  FALSE);
    pd3dDevice->SetRenderState(D3DRS_ZENABLE, D3DZB_FALSE);
    pd3dDevice->SetRenderState(D3DRS_ZWRITEENABLE, D3DZB_FALSE);
    pd3dDevice->SetRenderState(D3DRS_COLORVERTEX, FALSE);
    pd3dDevice->SetRenderState(D3DRS_STENCILENABLE, FALSE);

    // set the default texture addressing mode
    pd3dDevice->SetSamplerState(0, D3DSAMP_ADDRESSU, D3DTADDRESS_CLAMP);
    pd3dDevice->SetSamplerState(0, D3DSAMP_ADDRESSV, D3DTADDRESS_CLAMP);

    // REMIND: check supported filters with
    // IDirect3D9::CheckDeviceFormat with D3DUSAGE_QUERY_FILTER
    pd3dDevice->SetSamplerState(0, D3DSAMP_MAGFILTER, D3DTEXF_POINT);
    pd3dDevice->SetSamplerState(0, D3DSAMP_MINFILTER, D3DTEXF_POINT);

    // these states never change
    pd3dDevice->SetTextureStageState(0, D3DTSS_ALPHAOP, D3DTOP_MODULATE);
    pd3dDevice->SetTextureStageState(0, D3DTSS_COLOROP, D3DTOP_MODULATE);
    pd3dDevice->SetTextureStageState(0, D3DTSS_ALPHAARG2, D3DTA_DIFFUSE);
    pd3dDevice->SetTextureStageState(0, D3DTSS_COLORARG2, D3DTA_DIFFUSE);
    pd3dDevice->SetTextureStageState(1, D3DTSS_ALPHAOP, D3DTOP_MODULATE);
    pd3dDevice->SetTextureStageState(1, D3DTSS_COLOROP, D3DTOP_MODULATE);
    pd3dDevice->SetTextureStageState(1, D3DTSS_ALPHAARG2, D3DTA_CURRENT);
    pd3dDevice->SetTextureStageState(1, D3DTSS_COLORARG2, D3DTA_CURRENT);

    // init the array of latest textures
    ZeroMemory(lastTexture, sizeof(lastTexture));
    ZeroMemory(lastTextureColorState, sizeof(lastTextureColorState));

    opState = STATE_CHANGE;

    if (pResourceMgr == NULL) {
        res = D3DResourceManager::CreateInstance(this, &pResourceMgr);
    } else {
        res = pResourceMgr->Init(this);
    }
    RETURN_STATUS_IF_FAILED(res);

    if (pVCacher == NULL) {
        res = D3DVertexCacher::CreateInstance(this, &pVCacher);
    } else {
        res = pVCacher->Init(this);
    }
    RETURN_STATUS_IF_FAILED(res);

    if (pMaskCache == NULL) {
        res = D3DMaskCache::CreateInstance(this, &pMaskCache);
    } else{
        res = pMaskCache->Init(this);
    }
    RETURN_STATUS_IF_FAILED(res);

    if (pLCDGlyphCache != NULL) {
        if (FAILED(res = pLCDGlyphCache->Init(this))) {
            // we can live without the cache
            SAFE_DELETE(pLCDGlyphCache);
            res = S_OK;
        }
    }

    if (pGrayscaleGlyphCache != NULL) {
        if (FAILED(res = pGrayscaleGlyphCache->Init(this))) {
            // we can live without the cache
            SAFE_DELETE(pGrayscaleGlyphCache);
            res = S_OK;
        }
    }

    D3DMATRIX tx;
    D3DUtils_SetIdentityMatrix(&tx);
    pd3dDevice->SetTransform(D3DTS_WORLD, &tx);
    bIsIdentityTx = TRUE;

    if (pSyncQuery == NULL) {
        // this is allowed to fail, do not propagate the error
        if (FAILED(pd3dDevice->CreateQuery(D3DQUERYTYPE_EVENT, &pSyncQuery))) {
            J2dRlsTraceLn(J2D_TRACE_WARNING,
                          "D3DContext::InitDevice: sync query not available");
            pSyncQuery = NULL;
        }
    }
    if (pSyncRTRes == NULL) {
        D3DFORMAT format;
        if (FAILED(GetResourceManager()->
                   CreateRTSurface(32, 32, TRUE, TRUE, &format, &pSyncRTRes))) {
            J2dRlsTraceLn(J2D_TRACE_WARNING,
                          "D3DContext::InitDevice: "
                          "error creating sync surface");
        }
    }

    bBeginScenePending = FALSE;

    J2dRlsTraceLn1(J2D_TRACE_INFO,
                   "D3DContext::InitDefice: successfully initialized device %d",
                   adapterOrdinal);

    return res;
}

HRESULT
D3DContext::CheckAndResetDevice()
{
    HRESULT res = E_FAIL;

    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::CheckAndResetDevice");

    if (pd3dDevice != NULL) {
        if (FAILED(res = pd3dDevice->TestCooperativeLevel())) {
            if (res == D3DERR_DEVICELOST) {
                J2dTraceLn1(J2D_TRACE_VERBOSE, "  device %d is still lost",
                            adapterOrdinal);
                // nothing to be done here, wait for D3DERR_DEVICENOTRESET
                return res;
            } else if (res == D3DERR_DEVICENOTRESET) {
                J2dTraceLn1(J2D_TRACE_VERBOSE, "  device %d needs to be reset",
                            adapterOrdinal);
                res = ResetContext();
            } else {
                // some unexpected error
                DebugPrintD3DError(res, "D3DContext::CheckAndResetDevice: "\
                                   "unknown error %x from TestCooperativeLevel");
            }
        } else {
            J2dTraceLn1(J2D_TRACE_VERBOSE, "  device %d is not lost",
                        adapterOrdinal);
        }
    } else {
        J2dTraceLn(J2D_TRACE_VERBOSE, "  null device");
    }
    return res;
}

HRESULT
D3DContext::ResetContext()
{
    HRESULT res = E_FAIL;

    J2dRlsTraceLn(J2D_TRACE_INFO, "D3DContext::ResetContext");
    if (pd3dDevice != NULL) {
        D3DPRESENT_PARAMETERS newParams;

        newParams = curParams;

        if (newParams.Windowed) {
            // reset to the current display mode if we're windowed,
            // otherwise to the display mode we were in when the device
            // was lost
            newParams.BackBufferFormat = D3DFMT_UNKNOWN;
            newParams.FullScreen_RefreshRateInHz = 0;
            newParams.BackBufferWidth = 0;
            newParams.BackBufferHeight = 0;
        }
        res = ConfigureContext(&newParams);
    }
    return res;
}

HRESULT
D3DContext::ConfigureContext(D3DPRESENT_PARAMETERS *pNewParams)
{
    J2dRlsTraceLn1(J2D_TRACE_INFO, "D3DContext::ConfigureContext device %d",
                   adapterOrdinal);
    HRESULT res = S_OK;
    D3DFORMAT stencilFormat;
    HWND focusHWND = D3DPipelineManager::GetInstance()->GetCurrentFocusWindow();
    D3DDEVTYPE devType = D3DPipelineManager::GetInstance()->GetDeviceType();
    // this is needed so that we can find the stencil buffer format
    if (pNewParams->BackBufferFormat == D3DFMT_UNKNOWN) {
        D3DDISPLAYMODE dm;

        pd3dObject->GetAdapterDisplayMode(adapterOrdinal, &dm);
        pNewParams->BackBufferFormat = dm.Format;
    }

    stencilFormat =
        D3DPipelineManager::GetInstance()->GetMatchingDepthStencilFormat(
            adapterOrdinal,
            pNewParams->BackBufferFormat, pNewParams->BackBufferFormat);

    pNewParams->EnableAutoDepthStencil = TRUE;
    pNewParams->AutoDepthStencilFormat = stencilFormat;

    // do not set device window in the windowed mode, we use additional
    // swap chains for rendering, the default chain is not used. otherwise
    // our scratch focus window will be made visible
    J2dTraceLn1(J2D_TRACE_VERBOSE, "  windowed=%d",pNewParams->Windowed);
    if (pNewParams->Windowed) {
        pNewParams->hDeviceWindow = (HWND)0;
    }

    // The focus window may change when we're entering/exiting the full-screen
    // mode. It may either be set to the default focus window (when there are
    // no more devices in fs mode), or to fs window for another device
    // in fs mode. See D3DPipelineManager::GetCurrentFocusWindow.
    if (pd3dDevice != NULL) {
        D3DDEVICE_CREATION_PARAMETERS cParams;
        pd3dDevice->GetCreationParameters(&cParams);
        if (cParams.hFocusWindow != focusHWND) {
            J2dTraceLn(J2D_TRACE_VERBOSE,
                       "  focus window changed, need to recreate the device");

            // if fs -> windowed, first exit fs, then recreate, otherwise
            // the screen might be left in a different display mode
            if (pNewParams->Windowed && !curParams.Windowed) {
                J2dTraceLn(J2D_TRACE_VERBOSE,
                            "  exiting full-screen mode, reset the device");
                curParams.Windowed = FALSE;
                ReleaseDefPoolResources();
                res = pd3dDevice->Reset(&curParams);

                if (FAILED(res)) {
                    DebugPrintD3DError(res, "D3DContext::ConfigureContext: "\
                                       "cound not reset the device");
                }
            }

            // note that here we should release all device resources, not only
            // thos in the default pool since the device is released
            ReleaseContextResources();
            SAFE_RELEASE(pd3dDevice);
        }
    }

    if (pd3dDevice != NULL) {
        J2dTraceLn(J2D_TRACE_VERBOSE, "  resetting the device");

        ReleaseDefPoolResources();

        if (pNewParams->PresentationInterval == D3DPRESENT_INTERVAL_IMMEDIATE &&
            !IsImmediateIntervalSupported())
        {
            pNewParams->PresentationInterval = D3DPRESENT_INTERVAL_DEFAULT;
        }

        res = pd3dDevice->Reset(pNewParams);
        if (FAILED(res)) {
            DebugPrintD3DError(res,
                "D3DContext::ConfigureContext: cound not reset the device");
            return res;
        }
        J2dRlsTraceLn1(J2D_TRACE_INFO,
            "D3DContext::ConfigureContext: successfully reset device: %d",
            adapterOrdinal);
    } else {
        D3DCAPS9 d3dCaps;
        DWORD dwBehaviorFlags;

        J2dTraceLn(J2D_TRACE_VERBOSE, "  creating a new device");

        if (FAILED(res = pd3dObject->GetDeviceCaps(adapterOrdinal,
                                                   devType, &d3dCaps)))
        {
            DebugPrintD3DError(res,
                "D3DContext::ConfigureContext: failed to get caps");
            return res;
        }

        if (pNewParams->PresentationInterval == D3DPRESENT_INTERVAL_IMMEDIATE &&
            !(d3dCaps.PresentationIntervals & D3DPRESENT_INTERVAL_IMMEDIATE))
        {
            pNewParams->PresentationInterval = D3DPRESENT_INTERVAL_DEFAULT;
        }

        // not preserving fpu control word could cause issues (4860749)
        dwBehaviorFlags = D3DCREATE_FPU_PRESERVE;

        J2dRlsTrace(J2D_TRACE_VERBOSE,
                    "[V] dwBehaviorFlags=D3DCREATE_FPU_PRESERVE|");
        if (d3dCaps.DevCaps & D3DDEVCAPS_HWTRANSFORMANDLIGHT) {
            J2dRlsTrace(J2D_TRACE_VERBOSE,
                        "D3DCREATE_HARDWARE_VERTEXPROCESSING");
            dwBehaviorFlags |= D3DCREATE_HARDWARE_VERTEXPROCESSING;
        } else {
            J2dRlsTrace(J2D_TRACE_VERBOSE,
                        "D3DCREATE_SOFTWARE_VERTEXPROCESSING");
            dwBehaviorFlags |= D3DCREATE_SOFTWARE_VERTEXPROCESSING;
        }
        // Handling focus changes by ourselves proved to be problematic,
        // so we're reverting back to D3D handling
        // dwBehaviorFlags |= D3DCREATE_NOWINDOWCHANGES;
        J2dRlsTrace(J2D_TRACE_VERBOSE,"\n");

        if (FAILED(res = pd3dObject->CreateDevice(adapterOrdinal, devType,
                                                  focusHWND,
                                                  dwBehaviorFlags,
                                                  pNewParams, &pd3dDevice)))
        {
            DebugPrintD3DError(res,
                "D3DContext::ConfigureContext: error creating d3d device");
            return res;
        }
        J2dRlsTraceLn1(J2D_TRACE_INFO,
            "D3DContext::ConfigureContext: successfully created device: %d",
            adapterOrdinal);
        bIsHWRasterizer = (devType == D3DDEVTYPE_HAL);
    }

    curParams = *pNewParams;
    // during the creation of the device d3d modifies this field, we reset
    // it back to 0
    curParams.Flags = 0;

    if (FAILED(res = InitDevice(pd3dDevice))) {
        ReleaseContextResources();
        return res;
    }

    res = InitContextCaps();

    return res;
}

HRESULT
D3DContext::InitContext()
{
    J2dRlsTraceLn1(J2D_TRACE_INFO, "D3DContext::InitContext device %d",
                   adapterOrdinal);

    D3DPRESENT_PARAMETERS params;
    ZeroMemory(&params, sizeof(D3DPRESENT_PARAMETERS));

    params.hDeviceWindow = 0;
    params.Windowed = TRUE;
    params.BackBufferCount = 1;
    params.BackBufferFormat = D3DFMT_UNKNOWN;
    params.SwapEffect = D3DSWAPEFFECT_DISCARD;
    params.PresentationInterval = D3DPRESENT_INTERVAL_DEFAULT;

    return ConfigureContext(&params);
}

HRESULT
D3DContext::Sync()
{
    HRESULT res = S_OK;

    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::Sync");

    if (pSyncQuery != NULL) {
        J2dTrace(J2D_TRACE_VERBOSE, "  flushing the device queue..");
        while (S_FALSE ==
               (res = pSyncQuery->GetData(NULL, 0, D3DGETDATA_FLUSH))) ;
        J2dTrace(J2D_TRACE_VERBOSE, ".. done\n");
    }
    if (pSyncRTRes != NULL) {
        D3DLOCKED_RECT lr;
        IDirect3DSurface9 *pSurface = pSyncRTRes->GetSurface();
        if (SUCCEEDED(pSurface->LockRect(&lr, NULL, D3DLOCK_NOSYSLOCK))) {
            pSurface->UnlockRect();
        }
    }
    return res;
}

#define POINT_FILTER_CAP (D3DPTFILTERCAPS_MAGFPOINT|D3DPTFILTERCAPS_MINFPOINT)
#define LINEAR_FILTER_CAP (D3DPTFILTERCAPS_MAGFLINEAR|D3DPTFILTERCAPS_MINFLINEAR)

BOOL
D3DContext::IsStretchRectFilteringSupported(D3DTEXTUREFILTERTYPE fType)
{
    if (fType == D3DTEXF_POINT) {
        return ((devCaps.StretchRectFilterCaps & POINT_FILTER_CAP) != 0);
    }
    if (fType == D3DTEXF_LINEAR) {
        return ((devCaps.StretchRectFilterCaps & LINEAR_FILTER_CAP) != 0);
    }
    return FALSE;
}

BOOL
D3DContext::IsTextureFilteringSupported(D3DTEXTUREFILTERTYPE fType)
{
    if (fType == D3DTEXF_POINT) {
        return ((devCaps.TextureFilterCaps & POINT_FILTER_CAP) != 0);
    }
    if (fType == D3DTEXF_LINEAR) {
        return ((devCaps.TextureFilterCaps & LINEAR_FILTER_CAP) != 0);
    }
    return FALSE;
}

BOOL
D3DContext::IsTextureFormatSupported(D3DFORMAT format, DWORD usage)
{
    HRESULT hr = pd3dObject->CheckDeviceFormat(adapterOrdinal,
                                               devCaps.DeviceType,
                                               curParams.BackBufferFormat,
                                               usage,
                                               D3DRTYPE_TEXTURE,
                                               format);
    return SUCCEEDED( hr );
}

BOOL
D3DContext::IsDepthStencilBufferOk(D3DSURFACE_DESC *pTargetDesc)
{
    IDirect3DSurface9 *pStencil;
    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::IsDepthStencilBufferOk");

    if (SUCCEEDED(pd3dDevice->GetDepthStencilSurface(&pStencil))) {
        D3DSURFACE_DESC descStencil;
        pStencil->GetDesc(&descStencil);
        pStencil->Release();

        D3DDISPLAYMODE dm;
        return
            (SUCCEEDED(pd3dDevice->GetDisplayMode(0, &dm)) &&
             pTargetDesc->Width <= descStencil.Width &&
             pTargetDesc->Height <= descStencil.Height &&
             SUCCEEDED(pd3dObject->CheckDepthStencilMatch(
                   adapterOrdinal,
                   devCaps.DeviceType,
                   dm.Format, pTargetDesc->Format,
                   descStencil.Format)));
    }
    J2dTraceLn(J2D_TRACE_VERBOSE,
        "  current stencil buffer is not compatible with new Render Target");

    return false;
}



HRESULT
D3DContext::InitDepthStencilBuffer(D3DSURFACE_DESC *pTargetDesc)
{
    HRESULT res;
    IDirect3DSurface9 *pBB;
    D3DDISPLAYMODE dm;

    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::InitDepthStencilBuffer");

    if (FAILED(res = pd3dDevice->GetDisplayMode(0, &dm))) {
        return res;
    }

    D3DFORMAT newFormat =
        D3DPipelineManager::GetInstance()->GetMatchingDepthStencilFormat(
            adapterOrdinal, dm.Format, pTargetDesc->Format);

    res = pd3dDevice->CreateDepthStencilSurface(
        pTargetDesc->Width, pTargetDesc->Height,
        newFormat, D3DMULTISAMPLE_NONE, 0, false, &pBB, 0);
    if (SUCCEEDED(res)) {
        res = pd3dDevice->SetDepthStencilSurface(pBB);
        pBB->Release();
    }

    return res;
}


HRESULT
D3DContext::SetRenderTarget(IDirect3DSurface9 *pSurface)
{
    static D3DMATRIX tx;
    HRESULT res;
    D3DSURFACE_DESC descNew;
    IDirect3DSurface9 *pCurrentTarget;

    J2dTraceLn1(J2D_TRACE_INFO,
                "D3DContext::SetRenderTarget: pSurface=0x%x",
                pSurface);

    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);
    RETURN_STATUS_IF_NULL(pSurface, E_FAIL);

    pSurface->GetDesc(&descNew);

    if (SUCCEEDED(res = pd3dDevice->GetRenderTarget(0, &pCurrentTarget))) {
        if (pCurrentTarget != pSurface) {
            FlushVertexQueue();
            if (FAILED(res = pd3dDevice->SetRenderTarget(0, pSurface))) {
                DebugPrintD3DError(res, "D3DContext::SetRenderTarget: "\
                                        "error setting render target");
                SAFE_RELEASE(pCurrentTarget);
                return res;
            }

            if (!IsDepthStencilBufferOk(&descNew)) {
                if (FAILED(res = InitDepthStencilBuffer(&descNew))) {
                    SAFE_RELEASE(pCurrentTarget);
                    return res;
                }
            }
        }
        SAFE_RELEASE(pCurrentTarget);
    }
    // we set the transform even if the render target didn't change;
    // this is because in some cases (fs mode) we use the default SwapChain of
    // the device, and its render target will be the same as the device's, and
    // we have to set the matrix correctly. This shouldn't be a performance
    // issue as render target changes are relatively rare
    D3DUtils_SetOrthoMatrixOffCenterLH(&tx,
                       (float)descNew.Width,
                       (float)descNew.Height);
    pd3dDevice->SetTransform(D3DTS_PROJECTION, &tx);

    J2dTraceLn1(J2D_TRACE_VERBOSE, "  current render target=0x%x", pSurface);
    return res;
}

HRESULT
D3DContext::ResetTransform()
{
    HRESULT res = S_OK;
    D3DMATRIX tx;

    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::ResetTransform");
    if (pd3dDevice == NULL) {
        return E_FAIL;
    }

    // no need for state change, just flush the queue
    FlushVertexQueue();

    D3DUtils_SetIdentityMatrix(&tx);
    if (FAILED(res = pd3dDevice->SetTransform(D3DTS_WORLD, &tx))) {
        DebugPrintD3DError(res, "D3DContext::SetTransform failed");
    }
    bIsIdentityTx = TRUE;
    return res;
}

HRESULT
D3DContext::SetTransform(jdouble m00, jdouble m10,
                         jdouble m01, jdouble m11,
                         jdouble m02, jdouble m12)
{
    HRESULT res = S_OK;
    D3DMATRIX tx, tx1;

    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::SetTransform");
    if (pd3dDevice == NULL) {
        return E_FAIL;
    }

    // no need for state change, just flush the queue
    FlushVertexQueue();

    // In order to correctly map texels to pixels we need to
    // adjust geometry by -0.5f in the transformed space.
    // In order to do that we first create a translated matrix
    // and then concatenate it with the world transform.
    //
    // Note that we only use non-id transform with DrawTexture,
    // the rest is rendered pre-transformed.
    //
    // The identity transform for textures is handled in
    // D3DVertexCacher::DrawTexture() because shifting by -0.5 for id
    // transform breaks lines rendering.

    ZeroMemory(&tx1, sizeof(D3DMATRIX));

    tx1._11 = (float)m00;
    tx1._12 = (float)m10;
    tx1._21 = (float)m01;
    tx1._22 = (float)m11;
    tx1._41 = (float)m02;
    tx1._42 = (float)m12;

    tx1._33 = 1.0f;
    tx1._44 = 1.0f;

    D3DUtils_SetIdentityMatrix(&tx);
    tx._41 = -0.5f;
    tx._42 = -0.5f;
    D3DUtils_2DConcatenateM(&tx, &tx1);

    J2dTraceLn4(J2D_TRACE_VERBOSE,
                "  %5f %5f %5f %5f", tx._11, tx._12, tx._13, tx._14);
    J2dTraceLn4(J2D_TRACE_VERBOSE,
                "  %5f %5f %5f %5f", tx._21, tx._22, tx._23, tx._24);
    J2dTraceLn4(J2D_TRACE_VERBOSE,
                "  %5f %5f %5f %5f", tx._31, tx._32, tx._33, tx._34);
    J2dTraceLn4(J2D_TRACE_VERBOSE,
                "  %5f %5f %5f %5f", tx._41, tx._42, tx._43, tx._44);
    if (FAILED(res = pd3dDevice->SetTransform(D3DTS_WORLD, &tx))) {
        DebugPrintD3DError(res, "D3DContext::SetTransform failed");
    }
    bIsIdentityTx = FALSE;

    return res;
}

HRESULT
D3DContext::SetRectClip(int x1, int y1, int x2, int y2)
{
    HRESULT res = S_OK;
    D3DSURFACE_DESC desc;
    IDirect3DSurface9 *pCurrentTarget;

    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::SetRectClip");
    J2dTraceLn4(J2D_TRACE_VERBOSE,
                "  x1=%-4d y1=%-4d x2=%-4d y2=%-4d",
                x1, y1, x2, y2);

    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    // no need for state change, just flush the queue
    FlushVertexQueue();

    pd3dDevice->SetRenderState(D3DRS_ZENABLE, D3DZB_FALSE);

    res = pd3dDevice->GetRenderTarget(0, &pCurrentTarget);
    RETURN_STATUS_IF_FAILED(res);

    pCurrentTarget->GetDesc(&desc);
    SAFE_RELEASE(pCurrentTarget);

    if (x1 <= 0 && y1 <= 0 &&
        (UINT)x2 >= desc.Width && (UINT)y2 >= desc.Height)
    {
        J2dTraceLn(J2D_TRACE_VERBOSE,
                   "  disabling clip (== render target dimensions)");
        return pd3dDevice->SetRenderState(D3DRS_SCISSORTESTENABLE, FALSE);
    }

    // clip to the dimensions of the target surface, otherwise
    // SetScissorRect will fail
    if (x1 < 0)                 x1 = 0;
    if (y1 < 0)                 y1 = 0;
    if ((UINT)x2 > desc.Width)  x2 = desc.Width;
    if ((UINT)y2 > desc.Height) y2 = desc.Height;
    if (x1 > x2)                x2 = x1 = 0;
    if (y1 > y2)                y2 = y1 = 0;
    RECT newRect = { x1, y1, x2, y2 };
    if (SUCCEEDED(res = pd3dDevice->SetScissorRect(&newRect))) {
        res = pd3dDevice->SetRenderState(D3DRS_SCISSORTESTENABLE, TRUE);
    } else {
        DebugPrintD3DError(res, "Error setting scissor rect");
        J2dRlsTraceLn4(J2D_TRACE_ERROR,
                       "  x1=%-4d y1=%-4d x2=%-4d y2=%-4d",
                       x1, y1, x2, y2);
    }

    return res;
}

HRESULT
D3DContext::ResetClip()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::ResetClip");
    // no need for state change, just flush the queue
    FlushVertexQueue();
    pd3dDevice->SetRenderState(D3DRS_SCISSORTESTENABLE, FALSE);
    return pd3dDevice->SetRenderState(D3DRS_ZENABLE, D3DZB_FALSE);
}

ClipType
D3DContext::GetClipType()
{
    // REMIND: this method could be optimized: we could keep the
    // clip state around when re/setting the clip instead of asking
    // every time.
    DWORD zEnabled = 0;
    DWORD stEnabled = 0;

    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::GetClipType");
    pd3dDevice->GetRenderState(D3DRS_SCISSORTESTENABLE, &stEnabled);
    if (stEnabled) {
        return CLIP_RECT;
    }
    pd3dDevice->GetRenderState(D3DRS_ZENABLE, &zEnabled);
    if (zEnabled) {
        return CLIP_SHAPE;
    }
    return CLIP_NONE;
}


/**
 * This method assumes that ::SetRenderTarget has already
 * been called. SetRenderTarget creates and attaches a
 * depth buffer to the target surface prior to setting it
 * as target surface to the device.
 */
DWORD dwAlphaSt, dwSrcBlendSt, dwDestBlendSt;
D3DMATRIX tx, idTx;

HRESULT
D3DContext::BeginShapeClip()
{
    HRESULT res = S_OK;
    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::BeginShapeClip");

    UpdateState(STATE_CHANGE);

    pd3dDevice->SetRenderState(D3DRS_SCISSORTESTENABLE, FALSE);

    // save alpha blending state
    pd3dDevice->GetRenderState(D3DRS_ALPHABLENDENABLE, &dwAlphaSt);
    pd3dDevice->GetRenderState(D3DRS_SRCBLEND, &dwSrcBlendSt);
    pd3dDevice->GetRenderState(D3DRS_DESTBLEND, &dwDestBlendSt);

    pd3dDevice->SetRenderState(D3DRS_ALPHABLENDENABLE, TRUE);
    pd3dDevice->SetRenderState(D3DRS_SRCBLEND, D3DBLEND_ZERO);
    pd3dDevice->SetRenderState(D3DRS_DESTBLEND, D3DBLEND_ONE);

    pd3dDevice->GetTransform(D3DTS_WORLD, &tx);
    D3DUtils_SetIdentityMatrix(&idTx);
    // translate the clip spans by 1.0f in z direction so that the
    // clip spans are rendered to the z buffer
    idTx._43 = 1.0f;
    pd3dDevice->SetTransform(D3DTS_WORLD, &idTx);

    // The depth buffer is first cleared with zeroes, which is the farthest
    // plane from the viewer (our projection matrix is an inversed orthogonal
    // transform).
    // To set the clip we'll render the clip spans with Z coordinates of 1.0f
    // (the closest to the viewer). Since all rendering primitives
    // have their vertices' Z coordinate set to 0.0, they will effectively be
    // clipped because the Z depth test for them will fail (vertex with 1.0
    // depth is closer than the one with 0.0f)
    pd3dDevice->SetRenderState(D3DRS_ZENABLE, D3DZB_TRUE);
    pd3dDevice->SetRenderState(D3DRS_ZWRITEENABLE, TRUE);
    pd3dDevice->SetRenderState(D3DRS_ZFUNC, D3DCMP_ALWAYS);
    pd3dDevice->Clear(0, NULL, D3DCLEAR_ZBUFFER, 0L, 0.0f, 0x0L);

    //res = BeginScene(STATE_SHAPE_CLIPOP);

    return res;
}

HRESULT
D3DContext::EndShapeClip()
{
    HRESULT res;

    // no need for state change, just flush the queue
    res = FlushVertexQueue();

    // restore alpha blending state
    pd3dDevice->SetRenderState(D3DRS_ALPHABLENDENABLE, dwAlphaSt);
    pd3dDevice->SetRenderState(D3DRS_SRCBLEND, dwSrcBlendSt);
    pd3dDevice->SetRenderState(D3DRS_DESTBLEND, dwDestBlendSt);

    // resore the transform
    pd3dDevice->SetTransform(D3DTS_WORLD, &tx);

    // Enable the depth buffer.
    // We disable further updates to the depth buffer: it should only
    // be updated in SetClip method.
    pd3dDevice->SetRenderState(D3DRS_ZWRITEENABLE, FALSE);
    pd3dDevice->SetRenderState(D3DRS_ZFUNC, D3DCMP_LESS);

    return res;
}

HRESULT
D3DContext::UploadTileToTexture(D3DResource *pTextureRes, void *pixels,
                                jint dstx, jint dsty,
                                jint srcx, jint srcy,
                                jint srcWidth, jint srcHeight,
                                jint srcStride,
                                TileFormat srcFormat,
                                jint *pPixelsTouchedL,
                                jint* pPixelsTouchedR)
{
#ifndef PtrAddBytes
#define PtrAddBytes(p, b)               ((void *) (((intptr_t) (p)) + (b)))
#define PtrCoord(p, x, xinc, y, yinc)   PtrAddBytes(p, \
                                                    ((ptrdiff_t)(y))*(yinc) + \
                                                    ((ptrdiff_t)(x))*(xinc))
#endif // PtrAddBytes

    HRESULT res = S_OK;
    IDirect3DTexture9 *pTexture = pTextureRes->GetTexture();
    D3DSURFACE_DESC *pDesc = pTextureRes->GetDesc();
    RECT r = { dstx, dsty, dstx+srcWidth, dsty+srcHeight };
    RECT *pR = &r;
    D3DLOCKED_RECT lockedRect;
    DWORD dwLockFlags = D3DLOCK_NOSYSLOCK;
    // these are only counted for LCD glyph uploads
    jint pixelsTouchedL = 0, pixelsTouchedR = 0;

    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::UploadTileToTexture");
    J2dTraceLn4(J2D_TRACE_VERBOSE,
        " rect={%-4d, %-4d, %-4d, %-4d}",
        r.left, r.top, r.right, r.bottom);

    if (pDesc->Usage == D3DUSAGE_DYNAMIC) {
        // it is safe to lock with discard because we don't care about the
        // contents of dynamic textures and dstx,dsty for this case is
        // always 0,0 because we are uploading into a tile texture
        dwLockFlags |= D3DLOCK_DISCARD;
        pR = NULL;
    }

    if (FAILED(res = pTexture->LockRect(0, &lockedRect, pR, dwLockFlags))) {
        DebugPrintD3DError(res,
            "D3DContext::UploadImageToTexture: could "\
            "not lock texture");
        return res;
    }

    if (srcFormat == TILEFMT_1BYTE_ALPHA) {
        // either a MaskFill tile, or a grayscale glyph
        if (pDesc->Format == D3DFMT_A8) {
            void *pSrcPixels = PtrCoord(pixels, srcx, 1, srcy, srcStride);
            void *pDstPixels = lockedRect.pBits;
            do {
                memcpy(pDstPixels, pSrcPixels, srcWidth);
                pSrcPixels = PtrAddBytes(pSrcPixels, srcStride);
                pDstPixels = PtrAddBytes(pDstPixels, lockedRect.Pitch);
            } while (--srcHeight > 0);
        }
        else if (pDesc->Format == D3DFMT_A8R8G8B8) {
            jubyte *pSrcPixels = (jubyte*)
                PtrCoord(pixels, srcx, 1, srcy, srcStride);
            jint *pDstPixels = (jint*)lockedRect.pBits;
            for (int yy = 0; yy < srcHeight; yy++) {
                for (int xx = 0; xx < srcWidth; xx++) {
                    // only need to set the alpha channel (the D3D texture
                    // state will be setup in this case to replicate the
                    // alpha channel as needed)
                    pDstPixels[xx] = pSrcPixels[xx] << 24;
                }
                pSrcPixels = (jubyte*)PtrAddBytes(pSrcPixels, srcStride);
                pDstPixels = (jint*)PtrAddBytes(pDstPixels, lockedRect.Pitch);
            }
        }
    } else if (srcFormat == TILEFMT_3BYTE_RGB) {
        // LCD glyph with RGB order
        if (pDesc->Format == D3DFMT_R8G8B8) {
            jubyte *pSrcPixels = (jubyte*)
                PtrCoord(pixels, srcx, 3, srcy, srcStride);
            jubyte *pDstPixels = (jubyte*)lockedRect.pBits;
            for (int yy = 0; yy < srcHeight; yy++) {
                for (int xx = 0; xx < srcWidth*3; xx+=3) {
                    // alpha channel is ignored in this case
                    // (note that this is backwards from what one might
                    // expect; it appears that D3DFMT_R8G8B8 is actually
                    // laid out in BGR order in memory)
                    pDstPixels[xx+0] = pSrcPixels[xx+2];
                    pDstPixels[xx+1] = pSrcPixels[xx+1];
                    pDstPixels[xx+2] = pSrcPixels[xx+0];
                }
                pixelsTouchedL +=
                    (pDstPixels[0+0]|pDstPixels[0+1]|pDstPixels[0+2]) ? 1 : 0;
                jint i = 3*(srcWidth-1);
                pixelsTouchedR +=
                    (pDstPixels[i+0]|pDstPixels[i+1]|pDstPixels[i+2]) ? 1 : 0;

                pSrcPixels = (jubyte*)PtrAddBytes(pSrcPixels, srcStride);
                pDstPixels = (jubyte*)PtrAddBytes(pDstPixels, lockedRect.Pitch);
            }
        }
        else if (pDesc->Format == D3DFMT_A8R8G8B8) {
            jubyte *pSrcPixels = (jubyte*)
                PtrCoord(pixels, srcx, 3, srcy, srcStride);
            jint *pDstPixels = (jint*)lockedRect.pBits;
            for (int yy = 0; yy < srcHeight; yy++) {
                for (int dx = 0, sx = 0; dx < srcWidth; dx++, sx+=3) {
                    // alpha channel is ignored in this case
                    jubyte r = pSrcPixels[sx+0];
                    jubyte g = pSrcPixels[sx+1];
                    jubyte b = pSrcPixels[sx+2];
                    pDstPixels[dx] = (r << 16) | (g << 8) | (b);
                }
                pixelsTouchedL += (pDstPixels[0]          ? 1 : 0);
                pixelsTouchedR += (pDstPixels[srcWidth-1] ? 1 : 0);

                pSrcPixels = (jubyte*)PtrAddBytes(pSrcPixels, srcStride);
                pDstPixels = (jint*)PtrAddBytes(pDstPixels, lockedRect.Pitch);
            }
        }
    } else if (srcFormat == TILEFMT_3BYTE_BGR) {
        // LCD glyph with BGR order
        if (pDesc->Format == D3DFMT_R8G8B8) {
            void *pSrcPixels = PtrCoord(pixels, srcx, 3, srcy, srcStride);
            void *pDstPixels = lockedRect.pBits;
            jubyte *pbDst;
            do {
                // alpha channel is ignored in this case
                // (note that this is backwards from what one might
                // expect; it appears that D3DFMT_R8G8B8 is actually
                // laid out in BGR order in memory)
                memcpy(pDstPixels, pSrcPixels, srcWidth * 3);

                pbDst = (jubyte*)pDstPixels;
                pixelsTouchedL +=(pbDst[0+0]|pbDst[0+1]|pbDst[0+2]) ? 1 : 0;
                jint i = 3*(srcWidth-1);
                pixelsTouchedR +=(pbDst[i+0]|pbDst[i+1]|pbDst[i+2]) ? 1 : 0;

                pSrcPixels = PtrAddBytes(pSrcPixels, srcStride);
                pDstPixels = PtrAddBytes(pDstPixels, lockedRect.Pitch);
            } while (--srcHeight > 0);
        }
        else if (pDesc->Format == D3DFMT_A8R8G8B8) {
            jubyte *pSrcPixels = (jubyte*)
                PtrCoord(pixels, srcx, 3, srcy, srcStride);
            jint *pDstPixels = (jint*)lockedRect.pBits;
            for (int yy = 0; yy < srcHeight; yy++) {
                for (int dx = 0, sx = 0; dx < srcWidth; dx++, sx+=3) {
                    // alpha channel is ignored in this case
                    jubyte b = pSrcPixels[sx+0];
                    jubyte g = pSrcPixels[sx+1];
                    jubyte r = pSrcPixels[sx+2];
                    pDstPixels[dx] = (r << 16) | (g << 8) | (b);
                }
                pixelsTouchedL += (pDstPixels[0]          ? 1 : 0);
                pixelsTouchedR += (pDstPixels[srcWidth-1] ? 1 : 0);

                pSrcPixels = (jubyte*)PtrAddBytes(pSrcPixels, srcStride);
                pDstPixels = (jint*)PtrAddBytes(pDstPixels, lockedRect.Pitch);
            }
        }
    } else if (srcFormat == TILEFMT_4BYTE_ARGB_PRE) {
        // MaskBlit tile
        if (pDesc->Format == D3DFMT_A8R8G8B8) {
            void *pSrcPixels = PtrCoord(pixels, srcx, 4, srcy, srcStride);
            void *pDstPixels = lockedRect.pBits;
            do {
                memcpy(pDstPixels, pSrcPixels, srcWidth * 4);
                pSrcPixels = PtrAddBytes(pSrcPixels, srcStride);
                pDstPixels = PtrAddBytes(pDstPixels, lockedRect.Pitch);
            } while (--srcHeight > 0);
        }
    } else {
        // should not happen, no-op just in case...
    }

    if (pPixelsTouchedL) {
        *pPixelsTouchedL  = pixelsTouchedL;
    }
    if (pPixelsTouchedR) {
        *pPixelsTouchedR = pixelsTouchedR;
    }

    return pTexture->UnlockRect(0);
}

HRESULT
D3DContext::InitLCDGlyphCache()
{
    if (pLCDGlyphCache == NULL) {
        return D3DGlyphCache::CreateInstance(this, CACHE_LCD, &pLCDGlyphCache);
    }
    return S_OK;
}

HRESULT
D3DContext::InitGrayscaleGlyphCache()
{
    if (pGrayscaleGlyphCache == NULL) {
        return D3DGlyphCache::CreateInstance(this, CACHE_GRAY,
                                             &pGrayscaleGlyphCache);
    }
    return S_OK;
}

HRESULT
D3DContext::ResetComposite()
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::ResetComposite");

    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    HRESULT res = UpdateState(STATE_CHANGE);
    pd3dDevice->SetRenderState(D3DRS_ALPHABLENDENABLE, FALSE);
    extraAlpha = 1.0f;
    return res;
}

HRESULT
D3DContext::SetAlphaComposite(jint rule, jfloat ea, jint flags)
{
    HRESULT res;
    J2dTraceLn3(J2D_TRACE_INFO,
                "D3DContext::SetAlphaComposite: rule=%-1d ea=%f flags=%d",
                rule, ea, flags);

    RETURN_STATUS_IF_NULL(pd3dDevice, E_FAIL);

    res = UpdateState(STATE_CHANGE);

    // we can safely disable blending when:
    //   - comp is SrcNoEa or SrcOverNoEa, and
    //   - the source is opaque
    // (turning off blending can have a large positive impact on performance)
    if ((rule == RULE_Src || rule == RULE_SrcOver) &&
        (ea == 1.0f) &&
        (flags & D3DC_SRC_IS_OPAQUE))
    {
        J2dTraceLn1(J2D_TRACE_VERBOSE,
                    "  disabling alpha comp rule=%-1d ea=1.0 src=opq)", rule);
        pd3dDevice->SetRenderState(D3DRS_ALPHABLENDENABLE, FALSE);
    } else {
        J2dTraceLn2(J2D_TRACE_VERBOSE,
                    "  enabling alpha comp (rule=%-1d ea=%f)", rule, ea);
        pd3dDevice->SetRenderState(D3DRS_ALPHABLENDENABLE, TRUE);

        pd3dDevice->SetRenderState(D3DRS_SRCBLEND,
                                   StdBlendRules[rule].src);
        pd3dDevice->SetRenderState(D3DRS_DESTBLEND,
                                   StdBlendRules[rule].dst);
    }

    extraAlpha = ea;
    return res;
}

#ifdef UPDATE_TX

// Note: this method of adjusting pixel to texel mapping proved to be
// difficult to perfect. The current variation works great for id,
// scale (including all kinds of flips) transforms, but not still not
// for generic transforms.
//
// Since we currently only do DrawTexture with non-id transform we instead
// adjust the geometry (see D3DVertexCacher::DrawTexture(), SetTransform())
//
// In order to enable this code path UpdateTextureTransforms needs to
// be called in SetTexture(), SetTransform() and ResetTranform().
HRESULT
D3DContext::UpdateTextureTransforms(DWORD dwSamplerToUpdate)
{
    HRESULT res = S_OK;
    DWORD dwSampler, dwMaxSampler;

    if (dwSamplerToUpdate == -1) {
        // update all used samplers, dwMaxSampler will be set to max
        dwSampler = 0;
        dwSampler = MAX_USED_TEXTURE_SAMPLER;
        J2dTraceLn(J2D_TRACE_INFO, "D3DContext::UpdateTextureTransforms: "\
                                   "updating all samplers");
    } else {
        // update only given sampler, dwMaxSampler will be set to it as well
        dwSampler = dwSamplerToUpdate;
        dwMaxSampler = dwSamplerToUpdate;
        J2dTraceLn1(J2D_TRACE_INFO, "D3DContext::UpdateTextureTransforms: "\
                                    "updating sampler %d", dwSampler);
    }

    do {
        D3DTRANSFORMSTATETYPE state =
            (D3DTRANSFORMSTATETYPE)(D3DTS_TEXTURE0 + dwSampler);
        IDirect3DTexture9 *pTexture = lastTexture[dwSampler];

        if (pTexture != NULL) {
            D3DMATRIX mt, tx;
            D3DSURFACE_DESC texDesc;

            pd3dDevice->GetTransform(D3DTS_WORLD, &tx);
            J2dTraceLn4(10,
                        "  %5f %5f %5f %5f", tx._11, tx._12, tx._13, tx._14);
            J2dTraceLn4(10,
                        "  %5f %5f %5f %5f", tx._21, tx._22, tx._23, tx._24);
            J2dTraceLn4(10,
                        "  %5f %5f %5f %5f", tx._31, tx._32, tx._33, tx._34);
            J2dTraceLn4(10,
                        "  %5f %5f %5f %5f", tx._41, tx._42, tx._43, tx._44);

            // this formula works for scales and flips
            if (tx._11 == 0.0f) {
                tx._11 = tx._12;
            }
            if (tx._22 == 0.0f) {
                tx._22 = tx._21;
            }

            pTexture->GetLevelDesc(0, &texDesc);

            // shift by .5 texel, but take into account
            // the scale factor of the device transform

            // REMIND: this approach is not entirely correct,
            // as it only takes into account the scale of the device
            // transform.
            mt._31 = (1.0f / (2.0f * texDesc.Width  * tx._11));
            mt._32 = (1.0f / (2.0f * texDesc.Height * tx._22));
            J2dTraceLn2(J2D_TRACE_VERBOSE, "  offsets: tx=%f ty=%f",
                        mt._31, mt._32);

            pd3dDevice->SetTextureStageState(dwSampler,
                                             D3DTSS_TEXTURETRANSFORMFLAGS,
                                             D3DTTFF_COUNT2);
            res = pd3dDevice->SetTransform(state, &mt);
        } else {
            res = pd3dDevice->SetTextureStageState(dwSampler,
                                                   D3DTSS_TEXTURETRANSFORMFLAGS,
                                                   D3DTTFF_DISABLE);
        }
        dwSampler++;
    } while (dwSampler <= dwMaxSampler);

    return res;
}
#endif // UPDATE_TX

/**
 * We go into the pains of maintaining the list of set textures
 * instead of just calling GetTexture() and comparing the old one
 * with the new one because it's actually noticeably slower to call
 * GetTexture() (note that we'd have to then call Release() on the
 * texture since GetTexture() increases texture's ref. count).
 */
HRESULT
D3DContext::SetTexture(IDirect3DTexture9 *pTexture, DWORD dwSampler)
{
    HRESULT res = S_OK;
    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::SetTexture");

    if (dwSampler < 0 || dwSampler > MAX_USED_TEXTURE_SAMPLER) {
        J2dTraceLn1(J2D_TRACE_ERROR,
                    "D3DContext::SetTexture: incorrect sampler: %d", dwSampler);
        return E_FAIL;
    }
    if (lastTexture[dwSampler] != pTexture) {
        if (FAILED(res = FlushVertexQueue())) {
            return res;
        }
        J2dTraceLn2(J2D_TRACE_VERBOSE,
                    "  new texture=0x%x on sampler %d", pTexture, dwSampler);
        res = pd3dDevice->SetTexture(dwSampler, pTexture);
        if (SUCCEEDED(res)) {
            lastTexture[dwSampler] = pTexture;
            // REMIND: see comment at UpdateTextureTransforms
#ifdef UPDATE_TX
            res = UpdateTextureTransforms(dwSampler);
#endif
        }  else {
            lastTexture[dwSampler] = NULL;
        }
    }
    return res;
}

HRESULT
D3DContext::UpdateTextureColorState(DWORD dwState, DWORD dwSampler)
{
    HRESULT res = S_OK;

    if (dwState != lastTextureColorState[dwSampler]) {
        res = pd3dDevice->SetTextureStageState(dwSampler,
                                               D3DTSS_ALPHAARG1, dwState);
        res = pd3dDevice->SetTextureStageState(dwSampler,
                                               D3DTSS_COLORARG1, dwState);
        lastTextureColorState[dwSampler] = dwState;
    }

    return res;
}

HRESULT /*NOLOCK*/
D3DContext::UpdateState(jbyte newState)
{
    HRESULT res = S_OK;

    if (opState == newState) {
        // The op is the same as last time, so we can return immediately.
        return res;
    } else if (opState != STATE_CHANGE) {
        res = FlushVertexQueue();
    }

    switch (opState) {
    case STATE_MASKOP:
        pMaskCache->Disable();
        break;
    case STATE_GLYPHOP:
        D3DTR_DisableGlyphVertexCache(this);
        break;
    case STATE_TEXTUREOP:
        // optimization: certain state changes (those marked STATE_CHANGE)
        // are allowed while texturing is enabled.
        // In this case, we can allow previousOp to remain as it is and
        // then return early.
        if (newState == STATE_CHANGE) {
            return res;
        }
        // REMIND: not necessary if we are switching to MASKOP or GLYPHOP
        // (or a complex paint, for that matter), but would that be a
        // worthwhile optimization?
        SetTexture(NULL);
        break;
    case STATE_AAPGRAMOP:
        res = DisableAAParallelogramProgram();
        break;
    default:
        break;
    }

    switch (newState) {
    case STATE_MASKOP:
        pMaskCache->Enable();
        UpdateTextureColorState(D3DTA_TEXTURE | D3DTA_ALPHAREPLICATE);
        break;
    case STATE_GLYPHOP:
        D3DTR_EnableGlyphVertexCache(this);
        UpdateTextureColorState(D3DTA_TEXTURE | D3DTA_ALPHAREPLICATE);
        break;
    case STATE_TEXTUREOP:
        UpdateTextureColorState(D3DTA_TEXTURE);
        break;
    case STATE_AAPGRAMOP:
        res = EnableAAParallelogramProgram();
        break;
    default:
        break;
    }

    opState = newState;

    return res;
}

HRESULT D3DContext::FlushVertexQueue()
{
    if (pVCacher != NULL) {
        return pVCacher->Render();
    }
    return E_FAIL;
}

HRESULT D3DContext::BeginScene(jbyte newState)
{
    if (!pd3dDevice) {
        return E_FAIL;
    } else {
        UpdateState(newState);
        if (!bBeginScenePending) {
            bBeginScenePending = TRUE;
            HRESULT res = pd3dDevice->BeginScene();
            J2dTraceLn(J2D_TRACE_INFO, "D3DContext::BeginScene");
            if (FAILED(res)) {
                // this will cause context reinitialization
                opState = STATE_CHANGE;
            }
            return res;
        }
        return S_OK;
    }
}

HRESULT D3DContext::EndScene() {
    if (bBeginScenePending) {
        FlushVertexQueue();
        bBeginScenePending = FALSE;
        J2dTraceLn(J2D_TRACE_INFO, "D3DContext::EndScene");
        return pd3dDevice->EndScene();
    }
    return S_OK;
}

/**
 * Compiles and links the given fragment shader program.  If
 * successful, this function returns a handle to the newly created shader
 * program; otherwise returns 0.
 */
IDirect3DPixelShader9 *D3DContext::CreateFragmentProgram(DWORD **shaders,
                                                       ShaderList *programs,
                                                       jint flags)
{
    DWORD *sourceCode;
    IDirect3DPixelShader9 *pProgram;

    J2dTraceLn1(J2D_TRACE_INFO,
                "D3DContext::CreateFragmentProgram: flags=%d",
                flags);

    sourceCode = shaders[flags];
    if (FAILED(pd3dDevice->CreatePixelShader(sourceCode, &pProgram))) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "D3DContext::CreateFragmentProgram: error creating program");
        return NULL;
    }

    // add it to the cache
    ShaderList_AddProgram(programs, ptr_to_jlong(pProgram),
                          0 /*unused*/, 0 /*unused*/, flags);

    return pProgram;
}

/**
 * Locates and enables a fragment program given a list of shader programs
 * (ShaderInfos), using this context's state and flags as search
 * parameters.  The "flags" parameter is a bitwise-or'd value that helps
 * differentiate one program for another; the interpretation of this value
 * varies depending on the type of shader (BufImgOp, Paint, etc) but here
 * it is only used to find another ShaderInfo with that same "flags" value.
 */
HRESULT D3DContext::EnableFragmentProgram(DWORD **shaders,
                                          ShaderList *programList,
                                          jint flags)
{
    HRESULT res;
    jlong programID;
    IDirect3DPixelShader9 *pProgram;

    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::EnableFragmentProgram");

    programID =
        ShaderList_FindProgram(programList,
                               0 /*unused*/, 0 /*unused*/, flags);

    pProgram = (IDirect3DPixelShader9 *)jlong_to_ptr(programID);
    if (pProgram == NULL) {
        pProgram = CreateFragmentProgram(shaders, programList, flags);
        if (pProgram == NULL) {
            return E_FAIL;
        }
    }

    if (FAILED(res = pd3dDevice->SetPixelShader(pProgram))) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "D3DContext::EnableFragmentProgram: error setting pixel shader");
        return res;
    }

    return S_OK;
}

HRESULT D3DContext::EnableBasicGradientProgram(jint flags)
{
    return EnableFragmentProgram((DWORD **)gradShaders,
                                 &basicGradPrograms, flags);
}

HRESULT D3DContext::EnableLinearGradientProgram(jint flags)
{
    return EnableFragmentProgram((DWORD **)linearShaders,
                                 &linearGradPrograms, flags);
}

HRESULT D3DContext::EnableRadialGradientProgram(jint flags)
{
    return EnableFragmentProgram((DWORD **)radialShaders,
                                 &radialGradPrograms, flags);
}

HRESULT D3DContext::EnableConvolveProgram(jint flags)
{
    return EnableFragmentProgram((DWORD **)convolveShaders,
                                 &convolvePrograms, flags);
}

HRESULT D3DContext::EnableRescaleProgram(jint flags)
{
    return EnableFragmentProgram((DWORD **)rescaleShaders,
                                 &rescalePrograms, flags);
}

HRESULT D3DContext::EnableLookupProgram(jint flags)
{
    return EnableFragmentProgram((DWORD **)lookupShaders,
                                 &lookupPrograms, flags);
}

HRESULT D3DContext::EnableLCDTextProgram()
{
    HRESULT res;

    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::EnableLCDTextProgram");

    if (lcdTextProgram == NULL) {
        if (FAILED(res = pd3dDevice->CreatePixelShader(lcdtext0,
                                                       &lcdTextProgram)))
        {
            return res;
        }
    }

    if (FAILED(res = pd3dDevice->SetPixelShader(lcdTextProgram))) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "D3DContext::EnableLCDTextProgram: error setting pixel shader");
        return res;
    }

    return S_OK;
}

HRESULT D3DContext::EnableAAParallelogramProgram()
{
    HRESULT res;

    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::EnableAAParallelogramProgram");

    if (aaPgramProgram == NULL) {
        if (FAILED(res = pd3dDevice->CreatePixelShader(aapgram0,
                                                       &aaPgramProgram))) {
            DebugPrintD3DError(res, "D3DContext::EnableAAParallelogramProgram: "
                               "error creating pixel shader");
            return res;
        }
    }

    if (FAILED(res = pd3dDevice->SetPixelShader(aaPgramProgram))) {
        DebugPrintD3DError(res, "D3DContext::EnableAAParallelogramProgram: "
                           "error setting pixel shader");
        return res;
    }

    return S_OK;
}

HRESULT D3DContext::DisableAAParallelogramProgram()
{
    HRESULT res;

    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::DisableAAParallelogramProgram");

    if (aaPgramProgram != NULL) {
        if (FAILED(res = pd3dDevice->SetPixelShader(NULL))) {
            DebugPrintD3DError(res,
                               "D3DContext::DisableAAParallelogramProgram: "
                               "error clearing pixel shader");
            return res;
        }
    }

    return S_OK;
}

BOOL D3DContext::IsAlphaRTSurfaceSupported()
{
    HRESULT res = pd3dObject->CheckDeviceFormat(adapterOrdinal,
            devCaps.DeviceType,
            curParams.BackBufferFormat,
            D3DUSAGE_RENDERTARGET,
            D3DRTYPE_SURFACE,
            D3DFMT_A8R8G8B8);
    return SUCCEEDED(res);
}

BOOL D3DContext::IsAlphaRTTSupported()
{
    HRESULT res = pd3dObject->CheckDeviceFormat(adapterOrdinal,
            devCaps.DeviceType,
            curParams.BackBufferFormat,
            D3DUSAGE_RENDERTARGET,
            D3DRTYPE_TEXTURE,
            D3DFMT_A8R8G8B8);
    return SUCCEEDED(res);
}

BOOL D3DContext::IsOpaqueRTTSupported()
{
    HRESULT res = pd3dObject->CheckDeviceFormat(adapterOrdinal,
            devCaps.DeviceType,
            curParams.BackBufferFormat,
            D3DUSAGE_RENDERTARGET,
            D3DRTYPE_TEXTURE,
            curParams.BackBufferFormat);
    return SUCCEEDED(res);
}

HRESULT D3DContext::InitContextCaps() {
    J2dTraceLn(J2D_TRACE_INFO, "D3DContext::InitContextCaps");
    J2dTraceLn1(J2D_TRACE_VERBOSE, "  caps for adapter %d :", adapterOrdinal);

    if (pd3dDevice == NULL || pd3dObject == NULL) {
        contextCaps = CAPS_EMPTY;
        J2dRlsTraceLn(J2D_TRACE_VERBOSE, "  | CAPS_EMPTY");
        return E_FAIL;
    }

    contextCaps = CAPS_DEVICE_OK;
    J2dRlsTraceLn(J2D_TRACE_VERBOSE, "  | CAPS_DEVICE_OK");

    if (IsAlphaRTSurfaceSupported()) {
        contextCaps |= CAPS_RT_PLAIN_ALPHA;
        J2dRlsTraceLn(J2D_TRACE_VERBOSE, "  | CAPS_RT_PLAIN_ALPHA");
    }
    if (IsAlphaRTTSupported()) {
        contextCaps |= CAPS_RT_TEXTURE_ALPHA;
        J2dRlsTraceLn(J2D_TRACE_VERBOSE, "  | CAPS_RT_TEXTURE_ALPHA");
    }
    if (IsOpaqueRTTSupported()) {
        contextCaps |= CAPS_RT_TEXTURE_OPAQUE;
        J2dRlsTraceLn(J2D_TRACE_VERBOSE, "  | CAPS_RT_TEXTURE_OPAQUE");
    }
    if (IsPixelShader20Supported()) {
        contextCaps |= CAPS_LCD_SHADER | CAPS_BIOP_SHADER | CAPS_PS20;
        J2dRlsTraceLn(J2D_TRACE_VERBOSE,
                      "  | CAPS_LCD_SHADER | CAPS_BIOP_SHADER | CAPS_PS20");
        // Pre-PS3.0 video boards are very slow with the AA shader, so
        // we will require PS30 hw even though the shader is compiled for 2.0a
//        if (IsGradientInstructionExtensionSupported()) {
//            contextCaps |= CAPS_AA_SHADER;
//            J2dRlsTraceLn(J2D_TRACE_VERBOSE, "  | CAPS_AA_SHADER");
//        }
    }
    if (IsPixelShader30Supported()) {
        if ((contextCaps & CAPS_AA_SHADER) == 0) {
            // This flag was not already mentioned above...
            J2dRlsTraceLn(J2D_TRACE_VERBOSE, "  | CAPS_AA_SHADER");
        }
        contextCaps |= CAPS_PS30 | CAPS_AA_SHADER;
        J2dRlsTraceLn(J2D_TRACE_VERBOSE, "  | CAPS_PS30");
    }
    if (IsMultiTexturingSupported()) {
        contextCaps |= CAPS_MULTITEXTURE;
        J2dRlsTraceLn(J2D_TRACE_VERBOSE, "  | CAPS_MULTITEXTURE");
    }
    if (!IsPow2TexturesOnly()) {
        contextCaps |= CAPS_TEXNONPOW2;
        J2dRlsTraceLn(J2D_TRACE_VERBOSE, "  | CAPS_TEXNONPOW2");
    }
    if (!IsSquareTexturesOnly()) {
        contextCaps |= CAPS_TEXNONSQUARE;
        J2dRlsTraceLn(J2D_TRACE_VERBOSE, "  | CAPS_TEXNONSQUARE");
    }
    return S_OK;
}
