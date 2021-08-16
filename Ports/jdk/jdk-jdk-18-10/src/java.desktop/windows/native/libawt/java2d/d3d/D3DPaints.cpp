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

#include <jlong.h>
#include <string.h>

#include "sun_java2d_d3d_D3DPaints_MultiGradient.h"

#include "D3DPaints.h"
#include "D3DContext.h"
#include "D3DRenderQueue.h"
#include "D3DSurfaceData.h"

HRESULT
D3DPaints_ResetPaint(D3DContext *d3dc)
{
    jint pixel, paintState;
    jubyte ea;
    HRESULT res;

    J2dTraceLn(J2D_TRACE_INFO, "D3DPaints_ResetPaint");

    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);

    paintState = d3dc->GetPaintState();
    J2dTraceLn1(J2D_TRACE_VERBOSE, "  state=%d", paintState);

    res = d3dc->UpdateState(STATE_OTHEROP);

    // disable current complex paint state, if necessary
    if (paintState > PAINT_ALPHACOLOR) {
        IDirect3DDevice9 *pd3dDevice = d3dc->Get3DDevice();
        DWORD sampler = d3dc->useMask ? 1 : 0;

        d3dc->SetTexture(NULL, sampler);
        pd3dDevice->SetSamplerState(sampler,
                                    D3DSAMP_ADDRESSU, D3DTADDRESS_CLAMP);
        pd3dDevice->SetSamplerState(sampler,
                                    D3DSAMP_ADDRESSV, D3DTADDRESS_CLAMP);
        pd3dDevice->SetTextureStageState(sampler,
                                         D3DTSS_TEXCOORDINDEX, sampler);
        res = pd3dDevice->SetTextureStageState(sampler,
                                               D3DTSS_TEXTURETRANSFORMFLAGS,
                                               D3DTTFF_DISABLE);

        if (paintState == PAINT_GRADIENT     ||
            paintState == PAINT_LIN_GRADIENT ||
            paintState == PAINT_RAD_GRADIENT)
        {
            res = pd3dDevice->SetPixelShader(NULL);
        }
    }

    // set each component of the current color state to the extra alpha
    // value, which will effectively apply the extra alpha to each fragment
    // in paint/texturing operations
    ea = (jubyte)(d3dc->extraAlpha * 0xff + 0.5f);
    pixel = (ea << 24) | (ea << 16) | (ea << 8) | (ea << 0);
    d3dc->pVCacher->SetColor(pixel);
    d3dc->useMask = JNI_FALSE;
    d3dc->SetPaintState(-1);
    return res;
}

HRESULT
D3DPaints_SetColor(D3DContext *d3dc, jint pixel)
{
    HRESULT res = S_OK;

    J2dTraceLn1(J2D_TRACE_INFO, "D3DPaints_SetColor: pixel=%08x", pixel);

    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);

    // no need to reset the current op state here unless the paint
    // state really needs to be changed
    if (d3dc->GetPaintState() > PAINT_ALPHACOLOR) {
        res = D3DPaints_ResetPaint(d3dc);
    }

    d3dc->pVCacher->SetColor(pixel);
    d3dc->useMask = JNI_FALSE;
    d3dc->SetPaintState(PAINT_ALPHACOLOR);
    return res;
}

/************************* GradientPaint support ****************************/

HRESULT
D3DPaints_SetGradientPaint(D3DContext *d3dc,
                           jboolean useMask, jboolean cyclic,
                           jdouble p0, jdouble p1, jdouble p3,
                           jint pixel1, jint pixel2)
{
    IDirect3DDevice9 *pd3dDevice;
    HRESULT res;

    J2dTraceLn(J2D_TRACE_INFO, "D3DPaints_SetGradientPaint");

    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);
    D3DPaints_ResetPaint(d3dc);

#if 0
    /*
     * REMIND: The following code represents the original fast gradient
     *         implementation.  The problem is that it relies on LINEAR
     *         texture filtering, which does not provide sufficient
     *         precision on certain hardware (from ATI, notably), which
     *         will cause visible banding (e.g. 64 shades of gray between
     *         black and white, instead of the expected 256 shades.  For
     *         correctness on such hardware, it is necessary to use a
     *         shader-based approach that does not suffer from these
     *         precision issues (see below).  This original implementation
     *         is about 16x faster than software, whereas the shader-based
     *         implementation is only about 4x faster than software (still
     *         impressive).  For simplicity, we will always use the
     *         shader-based version for now, but in the future we could
     *         consider using the fast path for certain hardware (that does
     *         not exhibit the problem) or provide a flag to allow developers
     *         to control which path we take (for those that are less
     *         concerned about quality).  Therefore, I'll leave this code
     *         here (currently disabled) for future use.
     */
    D3DResource *pGradientTexRes;
    IDirect3DTexture9 *pGradientTex;

    // this will initialize the gradient texture, if necessary
    res = d3dc->GetResourceManager()->GetGradientTexture(&pGradientTexRes);
    RETURN_STATUS_IF_FAILED(res);

    pGradientTex = pGradientTexRes->GetTexture();

    // update the texture containing the gradient colors
    {
        D3DLOCKED_RECT lockedRect;
        res = pGradientTex->LockRect(0, &lockedRect, NULL, D3DLOCK_NOSYSLOCK);
        RETURN_STATUS_IF_FAILED(res);
        jint *pPix = (jint*)lockedRect.pBits;
        pPix[0] = pixel1;
        pPix[1] = pixel2;
        pGradientTex->UnlockRect(0);
    }

    DWORD sampler = useMask ? 1 : 0;
    DWORD wrapMode = cyclic ? D3DTADDRESS_WRAP : D3DTADDRESS_CLAMP;
    d3dc->SetTexture(pGradientTex, sampler);
    d3dc->UpdateTextureColorState(D3DTA_TEXTURE, sampler);

    pd3dDevice = d3dc->Get3DDevice();
    pd3dDevice->SetSamplerState(sampler, D3DSAMP_ADDRESSU, wrapMode);
    pd3dDevice->SetSamplerState(sampler, D3DSAMP_ADDRESSV, wrapMode);
    pd3dDevice->SetSamplerState(sampler, D3DSAMP_MAGFILTER, D3DTEXF_LINEAR);
    pd3dDevice->SetSamplerState(sampler, D3DSAMP_MINFILTER, D3DTEXF_LINEAR);

    D3DMATRIX mt;
    ZeroMemory(&mt, sizeof(mt));
    mt._11 = (float)p0;
    mt._21 = (float)p1;
    mt._31 = (float)0.0;
    mt._41 = (float)p3;
    mt._12 = 0.0f;
    mt._22 = 1.0f;
    mt._32 = 0.0f;
    mt._42 = 0.0f;
    pd3dDevice->SetTransform(useMask ? D3DTS_TEXTURE1 : D3DTS_TEXTURE0, &mt);
    pd3dDevice->SetTextureStageState(sampler, D3DTSS_TEXCOORDINDEX,
                                     D3DTSS_TCI_CAMERASPACEPOSITION);
    res = pd3dDevice->SetTextureStageState(sampler,
                                     D3DTSS_TEXTURETRANSFORMFLAGS,
                                     D3DTTFF_COUNT2);
#else
    jfloat params[4];
    jfloat color[4];
    jint flags = 0;

    if (cyclic)  flags |= BASIC_GRAD_IS_CYCLIC;
    if (useMask) flags |= BASIC_GRAD_USE_MASK;

    // locate/enable the shader program for the given flags
    res = d3dc->EnableBasicGradientProgram(flags);
    RETURN_STATUS_IF_FAILED(res);

    // update the "uniform" values
    params[0] = (jfloat)p0;
    params[1] = (jfloat)p1;
    params[2] = (jfloat)p3;
    params[3] = 0.0f; // unused
    pd3dDevice = d3dc->Get3DDevice();
    res = pd3dDevice->SetPixelShaderConstantF(0, params, 1);

    color[0] = ((pixel1 >> 16) & 0xff) / 255.0f; // r
    color[1] = ((pixel1 >>  8) & 0xff) / 255.0f; // g
    color[2] = ((pixel1 >>  0) & 0xff) / 255.0f; // b
    color[3] = ((pixel1 >> 24) & 0xff) / 255.0f; // a
    res = pd3dDevice->SetPixelShaderConstantF(1, color, 1);

    color[0] = ((pixel2 >> 16) & 0xff) / 255.0f; // r
    color[1] = ((pixel2 >>  8) & 0xff) / 255.0f; // g
    color[2] = ((pixel2 >>  0) & 0xff) / 255.0f; // b
    color[3] = ((pixel2 >> 24) & 0xff) / 255.0f; // a
    res = pd3dDevice->SetPixelShaderConstantF(2, color, 1);

    // set up texture coordinate transform with identity matrix, which
    // will have the effect of passing the current window-space coordinates
    // through to the TEXCOORD0/1 register used by the basic gradient
    // pixel shader
    DWORD sampler = useMask ? 1 : 0;
    D3DMATRIX mt;
    ZeroMemory(&mt, sizeof(mt));
    mt._11 = 1.0f;
    mt._21 = 0.0f;
    mt._31 = 0.0f;
    mt._41 = 0.0f;
    mt._12 = 0.0f;
    mt._22 = 1.0f;
    mt._32 = 0.0f;
    mt._42 = 0.0f;
    pd3dDevice->SetTransform(useMask ? D3DTS_TEXTURE1 : D3DTS_TEXTURE0, &mt);
    pd3dDevice->SetTextureStageState(sampler, D3DTSS_TEXCOORDINDEX,
                                     D3DTSS_TCI_CAMERASPACEPOSITION);
    pd3dDevice->SetTextureStageState(sampler, D3DTSS_TEXTURETRANSFORMFLAGS,
                                     D3DTTFF_COUNT2);
#endif

    // pixel state has been set appropriately in D3DPaints_ResetPaint()
    d3dc->useMask = useMask;
    d3dc->SetPaintState(PAINT_GRADIENT);
    return res;
}

/************************** TexturePaint support ****************************/

HRESULT
D3DPaints_SetTexturePaint(D3DContext *d3dc,
                          jboolean useMask,
                          jlong pSrcOps, jboolean filter,
                          jdouble xp0, jdouble xp1, jdouble xp3,
                          jdouble yp0, jdouble yp1, jdouble yp3)
{
    D3DSDOps *srcOps = (D3DSDOps *)jlong_to_ptr(pSrcOps);
    IDirect3DDevice9 *pd3dDevice;
    HRESULT res;

    J2dTraceLn(J2D_TRACE_INFO, "D3DPaints_SetTexturePaint");

    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);
    RETURN_STATUS_IF_NULL(srcOps, E_FAIL);
    RETURN_STATUS_IF_NULL(srcOps->pResource, E_FAIL);
    D3DPaints_ResetPaint(d3dc);

    DWORD sampler = useMask ? 1 : 0;
    DWORD dwFilter = filter ? D3DTEXF_LINEAR : D3DTEXF_POINT;
    res = d3dc->SetTexture(srcOps->pResource->GetTexture(), sampler);
    d3dc->UpdateTextureColorState(D3DTA_TEXTURE, sampler);
    pd3dDevice = d3dc->Get3DDevice();
    pd3dDevice->SetSamplerState(sampler, D3DSAMP_ADDRESSU, D3DTADDRESS_WRAP);
    pd3dDevice->SetSamplerState(sampler, D3DSAMP_ADDRESSV, D3DTADDRESS_WRAP);
    pd3dDevice->SetSamplerState(sampler, D3DSAMP_MAGFILTER, dwFilter);
    pd3dDevice->SetSamplerState(sampler, D3DSAMP_MINFILTER, dwFilter);

    D3DMATRIX mt;
    ZeroMemory(&mt, sizeof(mt));

    // offset by a half texel to correctly map texels to pixels
    //  m02 = tx * m00 + ty * m01 + m02;
    //  m12 = tx * m10 + ty * m11 + m12;
    jdouble tx = (1 / (2.0f * srcOps->pResource->GetDesc()->Width));
    jdouble ty = (1 / (2.0f * srcOps->pResource->GetDesc()->Height));
    xp3 = tx * xp0 + ty * xp1 + xp3;
    yp3 = tx * yp0 + ty * yp1 + yp3;

    mt._11 = (float)xp0;
    mt._21 = (float)xp1;
    mt._31 = (float)0.0;
    mt._41 = (float)xp3;
    mt._12 = (float)yp0;
    mt._22 = (float)yp1;
    mt._32 = (float)0.0;
    mt._42 = (float)yp3;
    pd3dDevice->SetTransform(useMask ? D3DTS_TEXTURE1 : D3DTS_TEXTURE0, &mt);
    pd3dDevice->SetTextureStageState(sampler, D3DTSS_TEXCOORDINDEX,
                                     D3DTSS_TCI_CAMERASPACEPOSITION);
    pd3dDevice->SetTextureStageState(sampler, D3DTSS_TEXTURETRANSFORMFLAGS,
                                     D3DTTFF_COUNT2);

    // pixel state has been set appropriately in D3DPaints_ResetPaint()
    d3dc->useMask = useMask;
    d3dc->SetPaintState(PAINT_TEXTURE);
    return res;
}

/****************** Shared MultipleGradientPaint support ********************/

/** Composes the given parameters as flags into the given flags variable.*/
#define COMPOSE_FLAGS(flags, cycleMethod, large, useMask, linear) \
    do {                                                        \
        flags |= ((cycleMethod) & MULTI_GRAD_CYCLE_METHOD);     \
        if (large)   flags |= MULTI_GRAD_LARGE;                 \
        if (useMask) flags |= MULTI_GRAD_USE_MASK;              \
        if (linear)  flags |= MULTI_GRAD_LINEAR_RGB;            \
    } while (0)

/**
 * The maximum number of gradient "stops" supported by the fragment shader
 * and related code.  When the MULTI_GRAD_LARGE flag is set, we will use
 * MAX_FRACTIONS_LARGE; otherwise, we use MAX_FRACTIONS_SMALL.  By having
 * two separate values, we can have one highly optimized shader (SMALL) that
 * supports only a few fractions/colors, and then another, less optimal
 * shader that supports more stops.
 */
#define MAX_FRACTIONS \
    sun_java2d_d3d_D3DPaints_MultiGradient_MULTI_MAX_FRACTIONS_D3D
#define MAX_FRACTIONS_LARGE MAX_FRACTIONS
#define MAX_FRACTIONS_SMALL 4

/**
 * Called from the D3DPaints_SetLinear/RadialGradientPaint() methods
 * in order to setup the fraction/color values that are common to both.
 */
static HRESULT
D3DPaints_SetMultiGradientPaint(D3DContext *d3dc,
                                jboolean useMask, jint numStops,
                                void *pFractions, void *pPixels)
{
    HRESULT res;
    IDirect3DDevice9 *pd3dDevice;
    IDirect3DTexture9 *pMultiGradientTex;
    D3DResource *pMultiGradientTexRes;
    jint maxFractions = (numStops > MAX_FRACTIONS_SMALL) ?
        MAX_FRACTIONS_LARGE : MAX_FRACTIONS_SMALL;
    jfloat stopVals[MAX_FRACTIONS * 4];
    jfloat *fractions = (jfloat *)pFractions;
    juint *pixels = (juint *)pPixels;
    int i;
    int fIndex = 0;

    pd3dDevice = d3dc->Get3DDevice();

    // update the "uniform" fractions and scale factors
    for (i = 0; i < maxFractions; i++) {
        stopVals[fIndex+0] = (i < numStops)   ?
            fractions[i] : 0.0f;
        stopVals[fIndex+1] = (i < numStops-1) ?
            1.0f / (fractions[i+1] - fractions[i]) : 0.0f;
        stopVals[fIndex+2] = 0.0f; // unused
        stopVals[fIndex+3] = 0.0f; // unused
        fIndex += 4;
    }
    pd3dDevice->SetPixelShaderConstantF(0, stopVals, maxFractions);

    // this will initialize the multi-gradient texture, if necessary
    res = d3dc->GetResourceManager()->
        GetMultiGradientTexture(&pMultiGradientTexRes);
    RETURN_STATUS_IF_FAILED(res);

    pMultiGradientTex = pMultiGradientTexRes->GetTexture();

    // update the texture containing the gradient colors
    D3DLOCKED_RECT lockedRect;
    res = pMultiGradientTex->LockRect(0, &lockedRect, NULL, D3DLOCK_NOSYSLOCK);
    RETURN_STATUS_IF_FAILED(res);

    juint *pPix = (juint*)lockedRect.pBits;
    memcpy(pPix, pixels, numStops*sizeof(juint));
    if (numStops < MAX_MULTI_GRADIENT_COLORS) {
        // when we don't have enough colors to fill the entire
        // color gradient, we have to replicate the last color
        // in the right-most texel for the NO_CYCLE case where the
        // texcoord is sometimes forced to 1.0
        pPix[MAX_MULTI_GRADIENT_COLORS-1] = pixels[numStops-1];
    }
    pMultiGradientTex->UnlockRect(0);

    // set the gradient texture and update relevant state
    DWORD sampler = useMask ? 1 : 0;
    res = d3dc->SetTexture(pMultiGradientTex, sampler);
    d3dc->UpdateTextureColorState(D3DTA_TEXTURE, sampler);
    pd3dDevice->SetSamplerState(sampler, D3DSAMP_ADDRESSU, D3DTADDRESS_CLAMP);
    pd3dDevice->SetSamplerState(sampler, D3DSAMP_ADDRESSV, D3DTADDRESS_CLAMP);
    pd3dDevice->SetSamplerState(sampler, D3DSAMP_MAGFILTER, D3DTEXF_LINEAR);
    pd3dDevice->SetSamplerState(sampler, D3DSAMP_MINFILTER, D3DTEXF_LINEAR);

    // set up texture coordinate transform with identity matrix, which
    // will have the effect of passing the current window-space coordinates
    // through to the TEXCOORD0/1 register used by the multi-stop
    // gradient pixel shader
    D3DMATRIX mt;
    ZeroMemory(&mt, sizeof(mt));
    mt._11 = 1.0f;
    mt._21 = 0.0f;
    mt._31 = 0.0f;
    mt._41 = 0.0f;
    mt._12 = 0.0f;
    mt._22 = 1.0f;
    mt._32 = 0.0f;
    mt._42 = 0.0f;
    pd3dDevice->SetTransform(useMask ? D3DTS_TEXTURE1 : D3DTS_TEXTURE0, &mt);
    pd3dDevice->SetTextureStageState(sampler, D3DTSS_TEXCOORDINDEX,
                                     D3DTSS_TCI_CAMERASPACEPOSITION);
    pd3dDevice->SetTextureStageState(sampler, D3DTSS_TEXTURETRANSFORMFLAGS,
                                     D3DTTFF_COUNT2);
    return res;
}

/********************** LinearGradientPaint support *************************/

HRESULT
D3DPaints_SetLinearGradientPaint(D3DContext *d3dc, D3DSDOps *dstOps,
                                 jboolean useMask, jboolean linear,
                                 jint cycleMethod, jint numStops,
                                 jfloat p0, jfloat p1, jfloat p3,
                                 void *fractions, void *pixels)
{
    HRESULT res;
    IDirect3DDevice9 *pd3dDevice;
    jfloat params[4];
    jboolean large = (numStops > MAX_FRACTIONS_SMALL);
    jint flags = 0;

    J2dTraceLn(J2D_TRACE_INFO, "D3DPaints_SetLinearGradientPaint");

    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);
    RETURN_STATUS_IF_NULL(dstOps, E_FAIL);
    D3DPaints_ResetPaint(d3dc);

    COMPOSE_FLAGS(flags, cycleMethod, large, useMask, linear);

    // locate/enable the shader program for the given flags
    res = d3dc->EnableLinearGradientProgram(flags);
    RETURN_STATUS_IF_FAILED(res);

    // update the common "uniform" values (fractions and colors)
    D3DPaints_SetMultiGradientPaint(d3dc, useMask,
                                    numStops, fractions, pixels);

    // update the other "uniform" values
    params[0] = p0;
    params[1] = p1;
    params[2] = p3;
    params[3] = 0.0f; // unused
    pd3dDevice = d3dc->Get3DDevice();
    res = pd3dDevice->SetPixelShaderConstantF(16, params, 1);

    // pixel state has been set appropriately in D3DPaints_ResetPaint()
    d3dc->useMask = useMask;
    d3dc->SetPaintState(PAINT_LIN_GRADIENT);
    return res;
}

/********************** RadialGradientPaint support *************************/

HRESULT
D3DPaints_SetRadialGradientPaint(D3DContext *d3dc, D3DSDOps *dstOps,
                                 jboolean useMask, jboolean linear,
                                 jint cycleMethod, jint numStops,
                                 jfloat m00, jfloat m01, jfloat m02,
                                 jfloat m10, jfloat m11, jfloat m12,
                                 jfloat focusX,
                                 void *fractions, void *pixels)
{
    HRESULT res;
    IDirect3DDevice9 *pd3dDevice;
    jfloat denom, inv_denom;
    jfloat params[4];
    jboolean large = (numStops > MAX_FRACTIONS_SMALL);
    jint flags = 0;

    J2dTraceLn(J2D_TRACE_INFO, "D3DPaints_SetRadialGradientPaint");

    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);
    RETURN_STATUS_IF_NULL(dstOps, E_FAIL);
    D3DPaints_ResetPaint(d3dc);

    COMPOSE_FLAGS(flags, cycleMethod, large, useMask, linear);

    // locate/enable the shader program for the given flags
    res = d3dc->EnableRadialGradientProgram(flags);
    RETURN_STATUS_IF_FAILED(res);

    // update the common "uniform" values (fractions and colors)
    D3DPaints_SetMultiGradientPaint(d3dc, useMask,
                                    numStops, fractions, pixels);

    // update the other "uniform" values
    params[0] = m00;
    params[1] = m01;
    params[2] = m02;
    params[3] = 0.0f; // unused
    pd3dDevice = d3dc->Get3DDevice();
    pd3dDevice->SetPixelShaderConstantF(16, params, 1);

    params[0] = m10;
    params[1] = m11;
    params[2] = m12;
    params[3] = 0.0f; // unused
    pd3dDevice->SetPixelShaderConstantF(17, params, 1);

    // pack a few unrelated, precalculated values into a single float4
    denom = 1.0f - (focusX * focusX);
    inv_denom = 1.0f / denom;
    params[0] = focusX;
    params[1] = denom;
    params[2] = inv_denom;
    params[3] = 0.0f; // unused
    res = pd3dDevice->SetPixelShaderConstantF(18, params, 1);

    // pixel state has been set appropriately in D3DPaints_ResetPaint()
    d3dc->useMask = useMask;
    d3dc->SetPaintState(PAINT_RAD_GRADIENT);
    return res;
}
