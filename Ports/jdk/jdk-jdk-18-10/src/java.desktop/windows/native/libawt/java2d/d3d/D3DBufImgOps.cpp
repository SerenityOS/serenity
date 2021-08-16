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

#include "D3DBufImgOps.h"
#include "D3DContext.h"
#include "D3DRenderQueue.h"
#include "D3DSurfaceData.h"
#include "GraphicsPrimitiveMgr.h"

/**************************** ConvolveOp support ****************************/

/**
 * The maximum kernel size supported by the ConvolveOp shader.
 */
#define MAX_KERNEL_SIZE 25

HRESULT
D3DBufImgOps_EnableConvolveOp(D3DContext *d3dc, jlong pSrcOps,
                              jboolean edgeZeroFill,
                              jint kernelWidth, jint kernelHeight,
                              unsigned char *kernel)
{
    HRESULT res;
    IDirect3DDevice9 *pd3dDevice;
    D3DSDOps *srcOps = (D3DSDOps *)jlong_to_ptr(pSrcOps);
    jint kernelSize = kernelWidth * kernelHeight;
    jint texW, texH;
    jfloat xoff, yoff;
    jfloat edgeX, edgeY;
    jfloat imgEdge[4];
    jfloat kernelVals[MAX_KERNEL_SIZE*4];
    jint i, j, kIndex;
    jint flags = 0;

    J2dTraceLn2(J2D_TRACE_INFO,
                "D3DBufImgOps_EnableConvolveOp: kernelW=%d kernelH=%d",
                kernelWidth, kernelHeight);

    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);
    RETURN_STATUS_IF_NULL(srcOps, E_FAIL);

    d3dc->UpdateState(STATE_CHANGE);

    // texcoords are specified in the range [0,1], so to achieve an
    // x/y offset of approximately one pixel we have to normalize
    // to that range here
    texW = srcOps->pResource->GetDesc()->Width;
    texH = srcOps->pResource->GetDesc()->Height;
    xoff = 1.0f / texW;
    yoff = 1.0f / texH;

    if (edgeZeroFill) {
        flags |= CONVOLVE_EDGE_ZERO_FILL;
    }
    if (kernelWidth == 5 && kernelHeight == 5) {
        flags |= CONVOLVE_5X5;
    }

    // locate/enable the shader program for the given flags
    res = d3dc->EnableConvolveProgram(flags);
    RETURN_STATUS_IF_FAILED(res);

    // update the "uniform" image min/max values
    // (texcoords are in the range [0,1])
    //   imgEdge[0] = imgMin.x
    //   imgEdge[1] = imgMin.y
    //   imgEdge[2] = imgMax.x
    //   imgEdge[3] = imgMax.y
    edgeX = (kernelWidth/2) * xoff;
    edgeY = (kernelHeight/2) * yoff;
    imgEdge[0] = edgeX;
    imgEdge[1] = edgeY;
    imgEdge[2] = (((jfloat)srcOps->width)  / texW) - edgeX;
    imgEdge[3] = (((jfloat)srcOps->height) / texH) - edgeY;
    pd3dDevice = d3dc->Get3DDevice();
    pd3dDevice->SetPixelShaderConstantF(0, imgEdge, 1);

    // update the "uniform" kernel offsets and values
    kIndex = 0;
    for (i = -kernelHeight/2; i < kernelHeight/2+1; i++) {
        for (j = -kernelWidth/2; j < kernelWidth/2+1; j++) {
            kernelVals[kIndex+0] = j*xoff;
            kernelVals[kIndex+1] = i*yoff;
            kernelVals[kIndex+2] = NEXT_FLOAT(kernel);
            kernelVals[kIndex+3] = 0.0f; // unused
            kIndex += 4;
        }
    }
    return pd3dDevice->SetPixelShaderConstantF(1, kernelVals, kernelSize);
}

HRESULT
D3DBufImgOps_DisableConvolveOp(D3DContext *d3dc)
{
    IDirect3DDevice9 *pd3dDevice;

    J2dTraceLn(J2D_TRACE_INFO, "D3DBufImgOps_DisableConvolveOp");

    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);
    d3dc->UpdateState(STATE_CHANGE);

    // disable the ConvolveOp shader
    pd3dDevice = d3dc->Get3DDevice();
    return pd3dDevice->SetPixelShader(NULL);
}

/**************************** RescaleOp support *****************************/

HRESULT
D3DBufImgOps_EnableRescaleOp(D3DContext *d3dc,
                             jboolean nonPremult,
                             unsigned char *scaleFactors,
                             unsigned char *offsets)
{
    HRESULT res;
    IDirect3DDevice9 *pd3dDevice;
    jint flags = 0;

    J2dTraceLn(J2D_TRACE_INFO, "D3DBufImgOps_EnableRescaleOp");

    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);

    d3dc->UpdateState(STATE_CHANGE);

    // choose the appropriate shader, depending on the source image
    if (nonPremult) {
        flags |= RESCALE_NON_PREMULT;
    }

    // locate/enable the shader program for the given flags
    res = d3dc->EnableRescaleProgram(flags);
    RETURN_STATUS_IF_FAILED(res);

    // update the "uniform" scale factor values (note that the Java-level
    // dispatching code always passes down 4 values here, regardless of
    // the original source image type)
    pd3dDevice = d3dc->Get3DDevice();
    pd3dDevice->SetPixelShaderConstantF(0, (float *)scaleFactors, 1);

    // update the "uniform" offset values (note that the Java-level
    // dispatching code always passes down 4 values here, and that the
    // offsets will have already been normalized to the range [0,1])
    return pd3dDevice->SetPixelShaderConstantF(1, (float *)offsets, 1);
}

HRESULT
D3DBufImgOps_DisableRescaleOp(D3DContext *d3dc)
{
    IDirect3DDevice9 *pd3dDevice;

    J2dTraceLn(J2D_TRACE_INFO, "D3DBufImgOps_DisableRescaleOp");

    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);

    d3dc->UpdateState(STATE_CHANGE);

    // disable the RescaleOp shader
    pd3dDevice = d3dc->Get3DDevice();
    return pd3dDevice->SetPixelShader(NULL);
}

/**************************** LookupOp support ******************************/

HRESULT
D3DBufImgOps_EnableLookupOp(D3DContext *d3dc,
                            jboolean nonPremult, jboolean shortData,
                            jint numBands, jint bandLength, jint offset,
                            void *tableValues)
{
    HRESULT res;
    IDirect3DDevice9 *pd3dDevice;
    D3DResource *pLutTexRes;
    IDirect3DTexture9 *pLutTex;
    int bytesPerElem = (shortData ? 2 : 1);
    jfloat foffsets[4];
    void *bands[4];
    int i;
    jint flags = 0;

    for (i = 0; i < 4; i++) {
        bands[i] = NULL;
    }
    J2dTraceLn4(J2D_TRACE_INFO,
                "D3DBufImgOps_EnableLookupOp: short=%d num=%d len=%d off=%d",
                shortData, numBands, bandLength, offset);

    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);

    d3dc->UpdateState(STATE_CHANGE);

    // choose the appropriate shader, depending on the source image
    // and the number of bands involved
    if (numBands != 4) {
        flags |= LOOKUP_USE_SRC_ALPHA;
    }
    if (nonPremult) {
        flags |= LOOKUP_NON_PREMULT;
    }

    // locate/enable the shader program for the given flags
    res = d3dc->EnableLookupProgram(flags);
    RETURN_STATUS_IF_FAILED(res);

    // update the "uniform" offset value
    for (i = 0; i < 4; i++) {
        foffsets[i] = offset / 255.0f;
    }
    pd3dDevice = d3dc->Get3DDevice();
    pd3dDevice->SetPixelShaderConstantF(0, foffsets, 1);

    res = d3dc->GetResourceManager()->GetLookupOpLutTexture(&pLutTexRes);
    RETURN_STATUS_IF_FAILED(res);
    pLutTex = pLutTexRes->GetTexture();

    // update the lookup table with the user-provided values
    if (numBands == 1) {
        // replicate the single band for R/G/B; alpha band is unused
        for (i = 0; i < 3; i++) {
            bands[i] = tableValues;
        }
        bands[3] = NULL;
    } else if (numBands == 3) {
        // user supplied band for each of R/G/B; alpha band is unused
        for (i = 0; i < 3; i++) {
            bands[i] = PtrAddBytes(tableValues, i*bandLength*bytesPerElem);
        }
        bands[3] = NULL;
    } else if (numBands == 4) {
        // user supplied band for each of R/G/B/A
        for (i = 0; i < 4; i++) {
            bands[i] = PtrAddBytes(tableValues, i*bandLength*bytesPerElem);
        }
    }

    // upload the bands one row at a time into our lookup table texture
    D3DLOCKED_RECT lockedRect;
    res = pLutTex->LockRect(0, &lockedRect, NULL, D3DLOCK_NOSYSLOCK);
    RETURN_STATUS_IF_FAILED(res);

    jushort *pBase = (jushort*)lockedRect.pBits;
    for (i = 0; i < 4; i++) {
        jushort *pDst;
        if (bands[i] == NULL) {
            continue;
        }
        pDst = pBase + (i * 256);
        if (shortData) {
            memcpy(pDst, bands[i], bandLength*sizeof(jushort));
        } else {
            int j;
            jubyte *pSrc = (jubyte *)bands[i];
            for (j = 0; j < bandLength; j++) {
                pDst[j] = (jushort)(pSrc[j] << 8);
            }
        }
    }
    pLutTex->UnlockRect(0);

    // bind the lookup table to texture unit 1 and enable texturing
    res = d3dc->SetTexture(pLutTex, 1);
    pd3dDevice->SetSamplerState(1, D3DSAMP_ADDRESSU, D3DTADDRESS_CLAMP);
    pd3dDevice->SetSamplerState(1, D3DSAMP_ADDRESSV, D3DTADDRESS_CLAMP);
    pd3dDevice->SetSamplerState(1, D3DSAMP_MAGFILTER, D3DTEXF_LINEAR);
    pd3dDevice->SetSamplerState(1, D3DSAMP_MINFILTER, D3DTEXF_LINEAR);
    return res;
}

HRESULT
D3DBufImgOps_DisableLookupOp(D3DContext *d3dc)
{
    IDirect3DDevice9 *pd3dDevice;

    J2dTraceLn(J2D_TRACE_INFO, "D3DBufImgOps_DisableLookupOp");

    RETURN_STATUS_IF_NULL(d3dc, E_FAIL);

    d3dc->UpdateState(STATE_CHANGE);

    // disable the LookupOp shader
    pd3dDevice = d3dc->Get3DDevice();
    pd3dDevice->SetPixelShader(NULL);

    // disable the lookup table on texture unit 1
    return d3dc->SetTexture(NULL, 1);
}
