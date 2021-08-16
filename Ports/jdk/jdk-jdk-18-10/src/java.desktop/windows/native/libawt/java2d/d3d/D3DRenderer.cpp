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

#include "D3DPipeline.h"

#include "sun_java2d_d3d_D3DRenderer.h"

#include "D3DContext.h"
#include "D3DRenderer.h"
#include "D3DRenderQueue.h"

HRESULT D3DPIPELINE_API
D3DRenderer_DrawLine(D3DContext *d3dc,
                     jint x1, jint y1, jint x2, jint y2)
{
    J2dTraceLn4(J2D_TRACE_INFO,
                "D3DRenderer_doDrawLineD3D x1=%-4d y1=%-4d x2=%-4d y2=%-4d",
                x1, y1, x2, y2);
    d3dc->BeginScene(STATE_RENDEROP);
    return d3dc->pVCacher->DrawLine(x1, y1, x2, y2);
}

HRESULT D3DPIPELINE_API
D3DRenderer_DrawRect(D3DContext *d3dc,
                     jint x, jint y, jint w, jint h)
{
    J2dTraceLn4(J2D_TRACE_INFO,
                "D3DRenderer_DrawRect x=%-4d y=%-4d w=%-4d h=%-4d",
                x, y, w, h);

    d3dc->BeginScene(STATE_RENDEROP);
    return d3dc->pVCacher->DrawRect(x, y, x + w, y + h);
}

HRESULT D3DPIPELINE_API
D3DRenderer_FillRect(D3DContext *d3dc,
                     jint x, jint y, jint w, jint h)
{
    J2dTraceLn4(J2D_TRACE_INFO,
               "D3DRenderer_FillRect x=%-4d y=%-4d w=%-4d h=%-4d",
                x, y, w, h);

    d3dc->BeginScene(STATE_RENDEROP);
    return d3dc->pVCacher->FillRect(x, y, x + w, y + h);
}

HRESULT D3DPIPELINE_API
D3DRenderer_DrawPoly(D3DContext *d3dc,
                     jint nPoints, jboolean isClosed,
                     jint transX, jint transY,
                     jint *xPoints, jint *yPoints)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DRenderer_DrawPoly");

    if (d3dc == NULL || xPoints == NULL || yPoints == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "D3DRenderer_DrawPoly: d3dc, xPoints or yPoints is NULL");
        return E_FAIL;
    }

    d3dc->BeginScene(STATE_RENDEROP);
    return d3dc->pVCacher->DrawPoly(nPoints, isClosed, transX, transY,
                                    xPoints, yPoints);
}

HRESULT D3DPIPELINE_API
D3DRenderer_DrawScanlines(D3DContext *d3dc,
                          jint scanlineCount, jint *scanlines)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DRenderer_DrawScanlines");

    if (d3dc == NULL) {
       return E_FAIL;
    }
    if (scanlines == NULL || scanlineCount <= 0) {
        return D3D_OK;
    }

    d3dc->BeginScene(STATE_RENDEROP);
    return d3dc->pVCacher->DrawScanlines(scanlineCount, scanlines);
}

HRESULT D3DPIPELINE_API
D3DRenderer_FillSpans(D3DContext *d3dc, jint spanCount, jint *spans)
{
    J2dTraceLn(J2D_TRACE_INFO, "D3DRenderer_FillSpans");
    if (d3dc == NULL) {
        return E_FAIL;
    }

    d3dc->BeginScene(STATE_RENDEROP);
    return d3dc->pVCacher->FillSpans(spanCount, spans);
}

HRESULT D3DPIPELINE_API
D3DRenderer_FillParallelogram(D3DContext *d3dc,
                              jfloat fx11, jfloat fy11,
                              jfloat dx21, jfloat dy21,
                              jfloat dx12, jfloat dy12)
{
    J2dTraceLn6(J2D_TRACE_INFO,
                "D3DRenderer_FillParallelogram "
                "x=%6.2f y=%6.2f "
                "dx1=%6.2f dy1=%6.2f "
                "dx2=%6.2f dy2=%6.2f ",
                fx11, fy11,
                dx21, dy21,
                dx12, dy12);

    d3dc->BeginScene(STATE_RENDEROP);
    return d3dc->pVCacher->FillParallelogram(fx11, fy11,
                                             dx21, dy21,
                                             dx12, dy12);
}

HRESULT D3DPIPELINE_API
D3DRenderer_DrawParallelogram(D3DContext *d3dc,
                              jfloat fx11, jfloat fy11,
                              jfloat dx21, jfloat dy21,
                              jfloat dx12, jfloat dy12,
                              jfloat lwr21, jfloat lwr12)
{
    HRESULT res;

    J2dTraceLn8(J2D_TRACE_INFO,
                "D3DRenderer_DrawParallelogram "
                "x=%6.2f y=%6.2f "
                "dx1=%6.2f dy1=%6.2f lwr1=%6.2f "
                "dx2=%6.2f dy2=%6.2f lwr2=%6.2f ",
                fx11, fy11,
                dx21, dy21, lwr21,
                dx12, dy12, lwr12);

    // dx,dy for line width in the "21" and "12" directions.
    jfloat ldx21 = dx21 * lwr21;
    jfloat ldy21 = dy21 * lwr21;
    jfloat ldx12 = dx12 * lwr12;
    jfloat ldy12 = dy12 * lwr12;

    // calculate origin of the outer parallelogram
    jfloat ox11 = fx11 - (ldx21 + ldx12) / 2.0f;
    jfloat oy11 = fy11 - (ldy21 + ldy12) / 2.0f;

    res = d3dc->BeginScene(STATE_RENDEROP);
    RETURN_STATUS_IF_FAILED(res);

    // Only need to generate 4 quads if the interior still
    // has a hole in it (i.e. if the line width ratio was
    // less than 1.0)
    if (lwr21 < 1.0f && lwr12 < 1.0f) {
        // Note: "TOP", "BOTTOM", "LEFT" and "RIGHT" here are
        // relative to whether the dxNN variables are positive
        // and negative.  The math works fine regardless of
        // their signs, but for conceptual simplicity the
        // comments will refer to the sides as if the dxNN
        // were all positive.  "TOP" and "BOTTOM" segments
        // are defined by the dxy21 deltas.  "LEFT" and "RIGHT"
        // segments are defined by the dxy12 deltas.

        // Each segment includes its starting corner and comes
        // to just short of the following corner.  Thus, each
        // corner is included just once and the only lengths
        // needed are the original parallelogram delta lengths
        // and the "line width deltas".  The sides will cover
        // the following relative territories:
        //
        //     T T T T T R
        //      L         R
        //       L         R
        //        L         R
        //         L         R
        //          L B B B B B

        // TOP segment, to left side of RIGHT edge
        // "width" of original pgram, "height" of hor. line size
        fx11 = ox11;
        fy11 = oy11;
        res = d3dc->pVCacher->FillParallelogram(fx11, fy11,
                                                dx21, dy21,
                                                ldx12, ldy12);

        // RIGHT segment, to top of BOTTOM edge
        // "width" of vert. line size , "height" of original pgram
        fx11 = ox11 + dx21;
        fy11 = oy11 + dy21;
        res = d3dc->pVCacher->FillParallelogram(fx11, fy11,
                                                ldx21, ldy21,
                                                dx12, dy12);

        // BOTTOM segment, from right side of LEFT edge
        // "width" of original pgram, "height" of hor. line size
        fx11 = ox11 + dx12 + ldx21;
        fy11 = oy11 + dy12 + ldy21;
        res = d3dc->pVCacher->FillParallelogram(fx11, fy11,
                                                dx21, dy21,
                                                ldx12, ldy12);

        // LEFT segment, from bottom of TOP edge
        // "width" of vert. line size , "height" of inner pgram
        fx11 = ox11 + ldx12;
        fy11 = oy11 + ldy12;
        res = d3dc->pVCacher->FillParallelogram(fx11, fy11,
                                                ldx21, ldy21,
                                                dx12, dy12);
    } else {
        // The line width ratios were large enough to consume
        // the entire hole in the middle of the parallelogram
        // so we can just issue one large quad for the outer
        // parallelogram.
        dx21 += ldx21;
        dy21 += ldy21;
        dx12 += ldx12;
        dy12 += ldy12;

        res = d3dc->pVCacher->FillParallelogram(ox11, oy11,
                                                dx21, dy21,
                                                dx12, dy12);
    }

    return res;
}

HRESULT D3DPIPELINE_API
D3DRenderer_FillAAParallelogram(D3DContext *d3dc,
                                jfloat fx11, jfloat fy11,
                                jfloat dx21, jfloat dy21,
                                jfloat dx12, jfloat dy12)
{
    IDirect3DDevice9 *pd3dDevice;
    HRESULT res;

    J2dTraceLn6(J2D_TRACE_INFO,
                "D3DRenderer_FillAAParallelogram "
                "x=%6.2f y=%6.2f "
                "dx1=%6.2f dy1=%6.2f "
                "dx2=%6.2f dy2=%6.2f ",
                fx11, fy11,
                dx21, dy21,
                dx12, dy12);

    res = d3dc->BeginScene(STATE_AAPGRAMOP);
    RETURN_STATUS_IF_FAILED(res);

    pd3dDevice = d3dc->Get3DDevice();
    if (pd3dDevice == NULL) {
        return E_FAIL;
    }

    res = d3dc->pVCacher->FillParallelogramAA(fx11, fy11,
                                              dx21, dy21,
                                              dx12, dy12);
    return res;
}

HRESULT D3DPIPELINE_API
D3DRenderer_DrawAAParallelogram(D3DContext *d3dc,
                                jfloat fx11, jfloat fy11,
                                jfloat dx21, jfloat dy21,
                                jfloat dx12, jfloat dy12,
                                jfloat lwr21, jfloat lwr12)
{
    IDirect3DDevice9 *pd3dDevice;
    // dx,dy for line width in the "21" and "12" directions.
    jfloat ldx21, ldy21, ldx12, ldy12;
    // parameters for "outer" parallelogram
    jfloat ofx11, ofy11, odx21, ody21, odx12, ody12;
    // parameters for "inner" parallelogram
    jfloat ifx11, ify11, idx21, idy21, idx12, idy12;
    HRESULT res;

    J2dTraceLn8(J2D_TRACE_INFO,
                "D3DRenderer_DrawAAParallelogram "
                "x=%6.2f y=%6.2f "
                "dx1=%6.2f dy1=%6.2f lwr1=%6.2f "
                "dx2=%6.2f dy2=%6.2f lwr2=%6.2f ",
                fx11, fy11,
                dx21, dy21, lwr21,
                dx12, dy12, lwr12);

    res = d3dc->BeginScene(STATE_AAPGRAMOP);
    RETURN_STATUS_IF_FAILED(res);

    pd3dDevice = d3dc->Get3DDevice();
    if (pd3dDevice == NULL) {
        return E_FAIL;
    }

    // calculate true dx,dy for line widths from the "line width ratios"
    ldx21 = dx21 * lwr21;
    ldy21 = dy21 * lwr21;
    ldx12 = dx12 * lwr12;
    ldy12 = dy12 * lwr12;

    // calculate coordinates of the outer parallelogram
    ofx11 = fx11 - (ldx21 + ldx12) / 2.0f;
    ofy11 = fy11 - (ldy21 + ldy12) / 2.0f;
    odx21 = dx21 + ldx21;
    ody21 = dy21 + ldy21;
    odx12 = dx12 + ldx12;
    ody12 = dy12 + ldy12;

    // Only process the inner parallelogram if the line width ratio
    // did not consume the entire interior of the parallelogram
    // (i.e. if the width ratio was less than 1.0)
    if (lwr21 < 1.0f && lwr12 < 1.0f) {
        // calculate coordinates of the inner parallelogram
        ifx11 = fx11 + (ldx21 + ldx12) / 2.0f;
        ify11 = fy11 + (ldy21 + ldy12) / 2.0f;
        idx21 = dx21 - ldx21;
        idy21 = dy21 - ldy21;
        idx12 = dx12 - ldx12;
        idy12 = dy12 - ldy12;

        res = d3dc->pVCacher->DrawParallelogramAA(ofx11, ofy11,
                                                  odx21, ody21,
                                                  odx12, ody12,
                                                  ifx11, ify11,
                                                  idx21, idy21,
                                                  idx12, idy12);
    } else {
        // Just invoke a regular fill on the outer parallelogram
        res = d3dc->pVCacher->FillParallelogramAA(ofx11, ofy11,
                                                  odx21, ody21,
                                                  odx12, ody12);
    }

    return res;
}

#ifndef D3D_PPL_DLL

extern "C"
{

JNIEXPORT void JNICALL
Java_sun_java2d_d3d_D3DRenderer_drawPoly
    (JNIEnv *env, jobject d3dr,
     jintArray xpointsArray, jintArray ypointsArray,
     jint nPoints, jboolean isClosed,
     jint transX, jint transY)
{
    jint *xPoints, *yPoints;

    J2dTraceLn(J2D_TRACE_INFO, "D3DRenderer_drawPoly");

    xPoints = (jint *)env->GetPrimitiveArrayCritical(xpointsArray, NULL);
    if (xPoints != NULL) {
        yPoints = (jint *)env->GetPrimitiveArrayCritical(ypointsArray, NULL);
        if (yPoints != NULL) {
            D3DContext *d3dc = D3DRQ_GetCurrentContext();

            D3DRenderer_DrawPoly(d3dc,
                                 nPoints, isClosed,
                                 transX, transY,
                                 xPoints, yPoints);

            if (d3dc != NULL) {
                HRESULT res = d3dc->EndScene();
                D3DRQ_MarkLostIfNeeded(res,
                    D3DRQ_GetCurrentDestination());
            }
            env->ReleasePrimitiveArrayCritical(ypointsArray, yPoints, JNI_ABORT);
        }
        env->ReleasePrimitiveArrayCritical(xpointsArray, xPoints, JNI_ABORT);
    }
}

}

#endif // D3D_PPL_DLL
