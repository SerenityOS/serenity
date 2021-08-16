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
#include "D3DVertexCacher.h"
#include "D3DPaints.h"

#include "math.h"

// non-texturized macros

#define ADD_VERTEX_XYC(X, Y, VCOLOR) \
do { \
    vertices[firstUnusedVertex].x = (X); \
    vertices[firstUnusedVertex].y = (Y); \
    vertices[firstUnusedVertex].color = (DWORD)(VCOLOR); \
    firstUnusedVertex++; \
} while (0)

#define ADD_LINE_XYC(X1, Y1, X2, Y2, VCOLOR) \
do { \
    ADD_VERTEX_XYC(X1, Y1, VCOLOR); \
    ADD_VERTEX_XYC(X2, Y2, VCOLOR); \
    batches[currentBatch].pNum++;   \
} while (0)

#define ADD_LINE_SEG_XYC(X, Y, VCOLOR) \
do { \
    ADD_VERTEX_XYC(X, Y, VCOLOR); \
    batches[currentBatch].pNum++;   \
} while (0)

#define ADD_TRIANGLE_XYC(X1, Y1, X2, Y2, X3, Y3, VCOLOR) \
do { \
    ADD_VERTEX_XYC(X1, Y1, VCOLOR); \
    ADD_VERTEX_XYC(X2, Y2, VCOLOR); \
    ADD_VERTEX_XYC(X3, Y3, VCOLOR); \
    batches[currentBatch].pNum++;   \
} while (0)

// texturized macros

#define ADD_VERTEX_XYUVC(X, Y, U1, V1, VCOLOR) \
do { \
    vertices[firstUnusedVertex].x = (X); \
    vertices[firstUnusedVertex].y = (Y); \
    vertices[firstUnusedVertex].tu1 = (U1); \
    vertices[firstUnusedVertex].tv1 = (V1); \
    vertices[firstUnusedVertex].color = (DWORD)(VCOLOR); \
    firstUnusedVertex++; \
} while (0)

#define ADD_VERTEX_XYUVUVC(X, Y, U1, V1, U2, V2, VCOLOR) \
do { \
    vertices[firstUnusedVertex].tu2 = (U2); \
    vertices[firstUnusedVertex].tv2 = (V2); \
    ADD_VERTEX_XYUVC(X, Y, U1, V1, VCOLOR); \
} while (0)

#define ADD_TRIANGLE_XYUVC(X1, Y1, X2, Y2, X3, Y3,         \
                           U1, V1, U2, V2, U3, V3, VCOLOR) \
do { \
    ADD_VERTEX_XYUVC(X1, Y1, U1, V1, VCOLOR); \
    ADD_VERTEX_XYUVC(X2, Y2, U2, V2, VCOLOR); \
    ADD_VERTEX_XYUVC(X3, Y3, U3, V3, VCOLOR); \
    batches[currentBatch].pNum++;   \
} while (0)

#define ADD_TRIANGLE_XYUVUVC(X1, Y1, X2, Y2, X3, Y3,       \
                             U11, V11, U12, V12, U13, V13, \
                             U21, V21, U22, V22, U23, V23, \
                             VCOLOR)                       \
do { \
    ADD_VERTEX_XYUVUVC(X1, Y1, U11, V11, U21, V21, VCOLOR); \
    ADD_VERTEX_XYUVUVC(X2, Y2, U12, V12, U22, V22, VCOLOR); \
    ADD_VERTEX_XYUVUVC(X3, Y3, U13, V13, U23, V23, VCOLOR); \
    batches[currentBatch].pNum++;   \
} while (0)

// These are fudge factors for rendering lines found by experimenting.
// They are used to tweak the geometry such that the rendering (mostly) matches
// our software rendering on most hardware. The main goal was to pick the
// numbers such that the beginning and ending pixels of lines match.
#define LINE_FUDGE
// fudge factors
#ifdef LINE_FUDGE

// Horiz/vertical
#define HV_FF1 ( 0.0f)
#define HV_FF2 ( 0.51f)
// For the record: value below (or larger) is required for Intel 855, but
// breaks Nvidia, ATI and Intel 965, and since the pipeline is disabled on
// 855 anyway we'll use 0.51f.
//#define HV_FF2 ( 0.5315f)
#define HV_FF3 (-0.2f)
// single pixel
#define SP_FF4 ( 0.3f)

// diagonal, down
#define DD_FX1 (-0.1f)
#define DD_FY1 (-0.25f)
#define DD_FX2 ( 0.2f)
#define DD_FY2 ( 0.304f)
// For the record: with this value diagonal-down lines with Texture paint
// are a bit off on all chipsets but Intel 965. So instead we'll use
// .304f which makes it better for the rest, but at a price of a bit
// of pixel/texel shifting on 965G
//#define DD_FY2 ( 0.4f)
// diagonal, up
#define DU_FX1 (-0.1f)
#define DU_FY1 ( 0.4f)
#define DU_FX2 ( 0.3f)
#define DU_FY2 (-0.3f)

#else

#define HV_FF1 (0.0f)
#define HV_FF2 (0.0f)
#define HV_FF3 (0.0f)
#define SP_FF4 (0.0f)

#define DD_FX1 (0.0f)
#define DD_FY1 (0.0f)
#define DD_FX2 (0.0f)
#define DD_FY2 (0.0f)
#define DU_FX1 (0.0f)
#define DU_FY1 (0.0f)
#define DU_FX2 (0.0f)
#define DU_FY2 (0.0f)

#endif

HRESULT
D3DVertexCacher::CreateInstance(D3DContext *pCtx, D3DVertexCacher **ppVC)
{
    HRESULT res;

    J2dTraceLn(J2D_TRACE_INFO, "D3DVertexCacher::CreateInstance");

    *ppVC = new D3DVertexCacher();
    if (FAILED(res = (*ppVC)->Init(pCtx))) {
        delete *ppVC;
        *ppVC = NULL;
    }
    return res;
}

D3DVertexCacher::D3DVertexCacher()
{
    lpD3DDevice = NULL;
    lpD3DVertexBuffer = NULL;
}

HRESULT
D3DVertexCacher::Init(D3DContext *pCtx)
{
    D3DCAPS9 caps;

    RETURN_STATUS_IF_NULL(pCtx, E_FAIL);

    ReleaseDefPoolResources();

    this->pCtx = pCtx;

    firstPendingBatch = 0;
    firstPendingVertex = 0;
    firstUnusedVertex = 0;
    currentBatch = 0;
    ZeroMemory(vertices, sizeof(vertices));
    ZeroMemory(batches, sizeof(batches));

    lpD3DDevice = pCtx->Get3DDevice();
    RETURN_STATUS_IF_NULL(lpD3DDevice, E_FAIL);

    ZeroMemory(&caps, sizeof(caps));
    lpD3DDevice->GetDeviceCaps(&caps);

    D3DPOOL pool = (caps.DeviceType == D3DDEVTYPE_HAL) ?
            D3DPOOL_DEFAULT : D3DPOOL_SYSTEMMEM;
    // usage depends on whether we use hw or sw vertex processing
    HRESULT res =
        lpD3DDevice->CreateVertexBuffer(MAX_BATCH_SIZE*sizeof(J2DLVERTEX),
            D3DUSAGE_DYNAMIC|D3DUSAGE_WRITEONLY, D3DFVF_J2DLVERTEX,
            pool, &lpD3DVertexBuffer, NULL);
    RETURN_STATUS_IF_FAILED(res);

    res = lpD3DDevice->SetStreamSource(0, lpD3DVertexBuffer, 0,
                                       sizeof(J2DLVERTEX));
    RETURN_STATUS_IF_FAILED(res);

    lpD3DDevice->SetFVF(D3DFVF_J2DLVERTEX);
    return res;
}

void
D3DVertexCacher::ReleaseDefPoolResources()
{
    SAFE_RELEASE(lpD3DVertexBuffer);
    pCtx = NULL;
}

HRESULT D3DVertexCacher::DrawLine(int x1, int y1, int x2, int y2)
{
    HRESULT res;
    if (SUCCEEDED(res = EnsureCapacity(D3DPT_LINELIST, 1*2))) {
        float fx1, fy1, fx2, fy2;
        if (y1 == y2) {
            // horizontal
            fy1  = (float)y1+HV_FF1;
            fy2  = fy1;

            if (x1 > x2) {
                fx1 = (float)x2+HV_FF3;
                fx2 = (float)x1+HV_FF2;
            } else if (x1 < x2) {
                fx1 = (float)x1+HV_FF3;
                fx2 = (float)x2+HV_FF2;
            } else {
                // single point, offset a little so that a single
                // pixel is rendered
                fx1 = (float)x1-SP_FF4;
                fy1 = (float)y1-SP_FF4;
                fx2 = (float)x2+SP_FF4;
                fy2 = (float)y2+SP_FF4;
            }
        } else if (x1 == x2) {
            // vertical
            fx1  = (float)x1+HV_FF1;
            fx2  = fx1;
            if (y1 > y2) {
                fy1 = (float)y2+HV_FF3;
                fy2 = (float)y1+HV_FF2;
            } else {
                fy1 = (float)y1+HV_FF3;
                fy2 = (float)y2+HV_FF2;
            }
        } else {
            // diagonal
            if (x1 > x2 && y1 > y2) {
                // ^
                //  \ case -> inverse
                fx1 = (float)x2;
                fy1 = (float)y2;
                fx2 = (float)x1;
                fy2 = (float)y1;
            } else if (x1 > x2 && y2 > y1) {
                //  /
                // v  case - inverse
                fx1 = (float)x2;
                fy1 = (float)y2;
                fx2 = (float)x1;
                fy2 = (float)y1;
            } else {
                // \      ^
                //  v or /  - leave as is
                fx1 = (float)x1;
                fy1 = (float)y1;
                fx2 = (float)x2;
                fy2 = (float)y2;
            }

            if (fx2 > fx1 && fy2 > fy1) {
                // \
                //  v
                fx1 += DD_FX1;
                fy1 += DD_FY1;
                fx2 += DD_FX2;
                fy2 += DD_FY2;
            } else {
                //   ^
                //  /
                fx1 += DU_FX1;
                fy1 += DU_FY1;
                fx2 += DU_FX2;
                fy2 += DU_FY2;
            }
        }
        ADD_LINE_XYC(fx1, fy1, fx2, fy2, color);
    }
    return res;
}

HRESULT
D3DVertexCacher::DrawPoly(jint nPoints, jboolean isClosed,
                          jint transX, jint transY,
                          jint *xPoints, jint *yPoints)
{
    HRESULT res;
    jfloat mx = (jfloat)xPoints[0];
    jfloat my = (jfloat)yPoints[0];
    jboolean isEmpty = TRUE;

    if (nPoints == 0) {
        return S_OK;
    }

    if (isClosed &&
        xPoints[nPoints - 1] == xPoints[0] &&
        yPoints[nPoints - 1] == yPoints[0])
    {
        isClosed = FALSE;
    }

    // npoints is exactly the number of vertices we need,
    // possibly plus one (if the path is closed)
    UINT reqVerts = nPoints * 1;
    int i = 0;
    do {
        // leave room for one possible additional closing point
        UINT vertsInBatch = min(MAX_BATCH_SIZE-1, max(2, reqVerts));
        if (SUCCEEDED(res = EnsureCapacity(D3DPT_LINESTRIP, vertsInBatch+1))) {
            reqVerts -= vertsInBatch;
            do {
                jfloat x = (jfloat)xPoints[i];
                jfloat y = (jfloat)yPoints[i];

                isEmpty = isEmpty && (x == mx && y == my);

                ADD_LINE_SEG_XYC(x + transX, y + transY, color);
                i++;
                vertsInBatch--;
            } while (vertsInBatch > 0);
            // include the last point from the current batch into the next
            if (reqVerts > 0) {
                i--;
                reqVerts++;
                // loop continues
            } else if (isClosed && !isEmpty) {
                // if this was the last batch, see if the closing point is needed;
                // note that we have left the room for it
                ADD_LINE_SEG_XYC(mx + transX, my + transY, color);
                // for clarity, the loop is ended anyway
                break;
            } else if (isEmpty || !isClosed) {
                // - either we went nowhere, then change the last point
                // so that a single pixel is rendered
                // - or it's not empty and not closed - add another
                // point because on some boards the last point is not rendered
                mx = xPoints[nPoints-1] + transX +SP_FF4;
                my = yPoints[nPoints-1] + transY +SP_FF4;
                ADD_LINE_SEG_XYC(mx, my, color);
                // for clarity
                break;
            }
        }
    } while (reqVerts > 0 && SUCCEEDED(res));

    return res;
}

HRESULT
D3DVertexCacher::DrawScanlines(jint scanlineCount, jint *scanlines)
{
    HRESULT res;
    float x1, x2, y;
    UINT reqVerts = scanlineCount*2/*vertices per line*/;

    if (scanlineCount == 0) {
        return S_OK;
    }

    do {
        UINT vertsInBatch = min(2*(MAX_BATCH_SIZE/2), reqVerts);
        if (SUCCEEDED(res = EnsureCapacity(D3DPT_LINELIST, vertsInBatch))) {
            reqVerts -= vertsInBatch;
            do {
                x1 = ((float)*(scanlines++)) +HV_FF3;
                x2 = ((float)*(scanlines++)) +HV_FF2;
                y  = ((float)*(scanlines++)) +HV_FF1;
                ADD_LINE_XYC(x1, y, x2, y, color);
                vertsInBatch -= 2;
            } while (vertsInBatch > 0);
        }
    } while (reqVerts > 0 && SUCCEEDED(res));
    return res;
}

HRESULT
D3DVertexCacher::FillSpans(jint spanCount, jint *spans)
{
    HRESULT res;
    float x1, y1, x2, y2;
    UINT reqVerts = spanCount*2*3/*vertices per span: two triangles*/;

    if (spanCount == 0) {
        return S_OK;
    }

    do {
        UINT vertsInBatch = min(6*(MAX_BATCH_SIZE/6), reqVerts);
        if (SUCCEEDED(res = EnsureCapacity(D3DPT_TRIANGLELIST, vertsInBatch))) {
            reqVerts -= vertsInBatch;
            do {
                x1 = ((float)*(spans++));
                y1 = ((float)*(spans++));
                x2 = ((float)*(spans++));
                y2 = ((float)*(spans++));

                ADD_TRIANGLE_XYC(x1, y1, x2, y1, x1, y2, color);
                ADD_TRIANGLE_XYC(x1, y2, x2, y1, x2, y2, color);
                vertsInBatch -= 6;
            } while (vertsInBatch > 0);
        }
    } while (reqVerts > 0 && SUCCEEDED(res));

    return res;
}

HRESULT D3DVertexCacher::DrawRect(int x1, int y1, int x2, int y2)
{
    HRESULT res;

    if ((x2 - x1) < 2 || (y2 - y1) < 2) {
        return FillRect(x1, y1, x2+1, y2+1);
    }
    if (SUCCEEDED(res = EnsureCapacity(D3DPT_LINELIST, 4*2))) {

        float fx1 = (float)x1;
        float fy1 = (float)y1;
        float fx2 = (float)x2;
        float fy2 = (float)y2;

        // horiz: top left - top right
        ADD_LINE_XYC(fx1+HV_FF3, fy1+HV_FF1, fx2-1.0f+HV_FF2, fy1+HV_FF1,color);
        // horiz: bottom left - bottom right
        ADD_LINE_XYC(fx1+1.0f+HV_FF3, fy2+HV_FF1, fx2+HV_FF2, fy2+HV_FF1,color);
        // vert : top right - bottom right
        ADD_LINE_XYC(fx2+HV_FF1, fy1+HV_FF3, fx2+HV_FF1, fy2-1.0f+HV_FF2,color);
        // vert : top left - bottom left
        ADD_LINE_XYC(fx1+HV_FF1, fy1+1.0f+HV_FF3, fx1+HV_FF1, fy2+HV_FF2,color);
    }
    return res;
}

HRESULT D3DVertexCacher::FillRect(int x1, int y1, int x2, int y2)
{
    HRESULT res;
    if (SUCCEEDED(res = EnsureCapacity(D3DPT_TRIANGLELIST, 2*3))) {
        float fx1 = (float)x1;
        float fy1 = (float)y1;
        float fx2 = (float)x2;
        float fy2 = (float)y2;
        ADD_TRIANGLE_XYUVC(fx1, fy1, fx2, fy1, fx1, fy2,
                           0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
                           color);
        ADD_TRIANGLE_XYUVC(fx1, fy2, fx2, fy1, fx2, fy2,
                           0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
                           color);
    }
    return res;
}

HRESULT D3DVertexCacher::FillParallelogram(float fx11, float fy11,
                                           float dx21, float dy21,
                                           float dx12, float dy12)
{
    HRESULT res;
    if (SUCCEEDED(res = EnsureCapacity(D3DPT_TRIANGLELIST, 2*3))) {
        // correct texel to pixel mapping; see D3DContext::SetTransform()
        // for non-id tx case
        if (pCtx->IsIdentityTx()) {
            fx11 -= 0.5f;
            fy11 -= 0.5f;
        }
        dx21 += fx11;
        dy21 += fy11;
        float fx22 = dx21 + dx12;
        float fy22 = dy21 + dy12;
        dx12 += fx11;
        dy12 += fy11;

        ADD_TRIANGLE_XYUVC(fx11, fy11, dx21, dy21, dx12, dy12,
                           0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
                           color);
        ADD_TRIANGLE_XYUVC(dx12, dy12, dx21, dy21, fx22, fy22,
                           0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
                           color);
    }
    return res;
}

#define ADJUST_PGRAM(V, DV, DIM) \
    do { \
        if ((DV) >= 0) { \
            (DIM) += (DV); \
        } else { \
            (DIM) -= (DV); \
            (V) += (DV); \
        } \
    } while (0)

// Invert the following transform:
// DeltaT(0, 0) == (0,       0)
// DeltaT(1, 0) == (DX1,     DY1)
// DeltaT(0, 1) == (DX2,     DY2)
// DeltaT(1, 1) == (DX1+DX2, DY1+DY2)
// TM00 = DX1,   TM01 = DX2,   (TM02 = X11)
// TM10 = DY1,   TM11 = DY2,   (TM12 = Y11)
// Determinant = TM00*TM11 - TM01*TM10
//             =  DX1*DY2  -  DX2*DY1
// Inverse is:
// IM00 =  TM11/det,   IM01 = -TM01/det
// IM10 = -TM10/det,   IM11 =  TM00/det
// IM02 = (TM01 * TM12 - TM11 * TM02) / det,
// IM12 = (TM10 * TM02 - TM00 * TM12) / det,

#define DECLARE_MATRIX(MAT) \
    float MAT ## 00, MAT ## 01, MAT ## 02, MAT ## 10, MAT ## 11, MAT ## 12

#define GET_INVERTED_MATRIX(MAT, X11, Y11, DX1, DY1, DX2, DY2, RET_CODE) \
    do { \
        float det = DX1*DY2 - DX2*DY1; \
        if (det == 0) { \
            RET_CODE; \
        } \
        MAT ## 00 = DY2/det; \
        MAT ## 01 = -DX2/det; \
        MAT ## 10 = -DY1/det; \
        MAT ## 11 = DX1/det; \
        MAT ## 02 = (DX2 * Y11 - DY2 * X11) / det; \
        MAT ## 12 = (DY1 * X11 - DX1 * Y11) / det; \
    } while (0)

#define TRANSFORM(MAT, TX, TY, X, Y) \
    do { \
        TX = (X) * MAT ## 00 + (Y) * MAT ## 01 + MAT ## 02; \
        TY = (X) * MAT ## 10 + (Y) * MAT ## 11 + MAT ## 12; \
    } while (0)

HRESULT D3DVertexCacher::FillParallelogramAA(float fx11, float fy11,
                                             float dx21, float dy21,
                                             float dx12, float dy12)
{
    HRESULT res;
    DECLARE_MATRIX(om);

    GET_INVERTED_MATRIX(om, fx11, fy11, dx21, dy21, dx12, dy12,
                        return D3D_OK);

    if (SUCCEEDED(res = EnsureCapacity(D3DPT_TRIANGLELIST, 2*3))) {
        float px = fx11, py = fy11;
        float pw = 0.0f, ph = 0.0f;
        ADJUST_PGRAM(px, dx21, pw);
        ADJUST_PGRAM(py, dy21, ph);
        ADJUST_PGRAM(px, dx12, pw);
        ADJUST_PGRAM(py, dy12, ph);
        float px1 = floor(px);
        float py1 = floor(py);
        float px2 = ceil(px + pw);
        float py2 = ceil(py + ph);
        float u11, v11, u12, v12, u21, v21, u22, v22;
        TRANSFORM(om, u11, v11, px1, py1);
        TRANSFORM(om, u21, v21, px2, py1);
        TRANSFORM(om, u12, v12, px1, py2);
        TRANSFORM(om, u22, v22, px2, py2);
        ADD_TRIANGLE_XYUVUVC(px1, py1, px2, py1, px1, py2,
                             u11, v11, u21, v21, u12, v12,
                             5.0, 5.0, 6.0, 5.0, 5.0, 6.0,
                             color);
        ADD_TRIANGLE_XYUVUVC(px1, py2, px2, py1, px2, py2,
                             u12, v12, u21, v21, u22, v22,
                             5.0, 6.0, 6.0, 5.0, 6.0, 6.0,
                             color);
    }
    return res;
}

HRESULT D3DVertexCacher::DrawParallelogramAA(float ox11, float oy11,
                                             float ox21, float oy21,
                                             float ox12, float oy12,
                                             float ix11, float iy11,
                                             float ix21, float iy21,
                                             float ix12, float iy12)
{
    HRESULT res;
    DECLARE_MATRIX(om);
    DECLARE_MATRIX(im);

    GET_INVERTED_MATRIX(im, ix11, iy11, ix21, iy21, ix12, iy12,
                        // inner parallelogram is degenerate
                        // therefore it encloses no area
                        // fill outer
                        return FillParallelogramAA(ox11, oy11,
                                                   ox21, oy21,
                                                   ox12, oy12));
    GET_INVERTED_MATRIX(om, ox11, oy11, ox21, oy21, ox12, oy12,
                        return D3D_OK);

    if (SUCCEEDED(res = EnsureCapacity(D3DPT_TRIANGLELIST, 2*3))) {
        float ox = ox11, oy = oy11;
        float ow = 0.0f, oh = 0.0f;
        ADJUST_PGRAM(ox, ox21, ow);
        ADJUST_PGRAM(oy, oy21, oh);
        ADJUST_PGRAM(ox, ox12, ow);
        ADJUST_PGRAM(oy, oy12, oh);
        float ox11 = floor(ox);
        float oy11 = floor(oy);
        float ox22 = ceil(ox + ow);
        float oy22 = ceil(oy + oh);
        float ou11, ov11, ou12, ov12, ou21, ov21, ou22, ov22;
        TRANSFORM(om, ou11, ov11, ox11, oy11);
        TRANSFORM(om, ou21, ov21, ox22, oy11);
        TRANSFORM(om, ou12, ov12, ox11, oy22);
        TRANSFORM(om, ou22, ov22, ox22, oy22);
        float iu11, iv11, iu12, iv12, iu21, iv21, iu22, iv22;
        TRANSFORM(im, iu11, iv11, ox11, oy11);
        TRANSFORM(im, iu21, iv21, ox22, oy11);
        TRANSFORM(im, iu12, iv12, ox11, oy22);
        TRANSFORM(im, iu22, iv22, ox22, oy22);
        ADD_TRIANGLE_XYUVUVC(ox11, oy11, ox22, oy11, ox11, oy22,
                             ou11, ov11, ou21, ov21, ou12, ov12,
                             iu11, iv11, iu21, iv21, iu12, iv12,
                             color);
        ADD_TRIANGLE_XYUVUVC(ox11, oy22, ox22, oy11, ox22, oy22,
                             ou12, ov12, ou21, ov21, ou22, ov22,
                             iu12, iv12, iu21, iv21, iu22, iv22,
                             color);
    }
    return res;
}

HRESULT
D3DVertexCacher::DrawTexture(float x1, float y1, float x2, float y2,
                             float u1, float v1, float u2, float v2)
{
    HRESULT res;
    if (SUCCEEDED(res = EnsureCapacity(D3DPT_TRIANGLELIST, 2*3))) {
        // correct texel to pixel mapping; see D3DContext::SetTransform()
        // for non-id tx case
        if (pCtx->IsIdentityTx()) {
            x1 -= 0.5f;
            y1 -= 0.5f;
            x2 -= 0.5f;
            y2 -= 0.5f;
        }

        ADD_TRIANGLE_XYUVC(x1, y1, x2, y1, x1, y2,
                           u1, v1, u2, v1, u1, v2,
                           color);
        ADD_TRIANGLE_XYUVC(x1, y2, x2, y1, x2, y2,
                           u1, v2, u2, v1, u2, v2,
                           color);
    }
    return res;
}

HRESULT
D3DVertexCacher::DrawTexture(float  x1, float  y1, float  x2, float  y2,
                             float u11, float v11, float u12, float v12,
                             float u21, float v21, float u22, float v22)
{
    HRESULT res;
    if (SUCCEEDED(res = EnsureCapacity(D3DPT_TRIANGLELIST, 2*3))) {
        // correct texel to pixel mapping; see D3DContext::SetTransform()
        // for non-id tx case
        if (pCtx->IsIdentityTx()) {
            x1 -= 0.5f;
            y1 -= 0.5f;
            x2 -= 0.5f;
            y2 -= 0.5f;
        }

        ADD_TRIANGLE_XYUVUVC(x1, y1, x2, y1, x1, y2,
                             u11, v11, u12, v11, u11, v12,
                             u21, v21, u22, v21, u21, v22,
                             color);
        ADD_TRIANGLE_XYUVUVC(x1, y2, x2, y1, x2, y2,
                             u11, v12, u12, v11, u12, v12,
                             u21, v22, u22, v21, u22, v22,
                             color);
    }
    return res;
}

HRESULT D3DVertexCacher::Render(int actionType)
{
    J2DLVERTEX *lpVert;
    HRESULT res;
    DWORD dwLockFlags;
    UINT pendingVertices = firstUnusedVertex - firstPendingVertex;

    // nothing to render
    if (pendingVertices == 0) {
        if (actionType == RESET_ACTION) {
            firstPendingBatch = 0;
            firstPendingVertex = 0;
            firstUnusedVertex = 0;
            currentBatch = 0;
        }
        return D3D_OK;
    }

    if (firstPendingVertex == 0) {
        // no data in the buffer yet, we don't care about
        // vertex buffer's contents
        dwLockFlags = D3DLOCK_DISCARD;
    } else {
        // append to the existing data in the vertex buffer
        dwLockFlags = D3DLOCK_NOOVERWRITE;
    }

    if (SUCCEEDED(res =
        lpD3DVertexBuffer->Lock((UINT)firstPendingVertex*sizeof(J2DLVERTEX),
                                (UINT)pendingVertices*sizeof(J2DLVERTEX),
                                (void**)&lpVert, dwLockFlags)))
    {
        // copy only new vertices
        memcpy((void *)lpVert,
               (void *)(vertices + firstPendingVertex),
               pendingVertices * sizeof(J2DLVERTEX));
        res = lpD3DVertexBuffer->Unlock();
        UINT currentVertex = firstPendingVertex;
        UINT batchSize;
        J2dTraceLn2(J2D_TRACE_VERBOSE,
                    "D3DVC::Render Starting flushing of %d vertices "\
                    "in %d batches",
                    pendingVertices,
                    (currentBatch - firstPendingBatch + 1));


        for (UINT b = firstPendingBatch; b <= currentBatch; b++) {
            D3DPRIMITIVETYPE pType = batches[b].pType;
            UINT primCount = batches[b].pNum;
            switch (pType) {
                // the macro for adding a line segment adds one too many prims
                case D3DPT_LINESTRIP: batchSize = primCount; primCount--; break;
                case D3DPT_LINELIST: batchSize = primCount*2; break;
                default: batchSize = primCount*3; break;
            }
            res = lpD3DDevice->DrawPrimitive(pType, currentVertex, primCount);
            currentVertex += batchSize;
            // init to something it can never be
            batches[b].pType = (D3DPRIMITIVETYPE)0;
            batches[b].pNum = 0;
        }
    } else {
        DebugPrintD3DError(res, "Can't lock vertex buffer");
    }

    // REMIND: may need to rethink what to do in case of an error,
    // should we try to render them later?
    if (actionType == RESET_ACTION) {
        firstPendingBatch = 0;
        firstPendingVertex = 0;
        firstUnusedVertex = 0;
        currentBatch = 0;
    } else {
        firstPendingBatch = currentBatch;
        firstPendingVertex = firstUnusedVertex;
    }

    return res;
}

HRESULT D3DVertexCacher::EnsureCapacity(D3DPRIMITIVETYPE newPType, UINT vNum)
{
    HRESULT res = D3D_OK;
    if (vNum > MAX_BATCH_SIZE) {
        // REMIND: need to define our own errors
        return D3DERR_NOTAVAILABLE;
    }
    if ((firstUnusedVertex + vNum) > MAX_BATCH_SIZE) {
        // if we can't fit new vertices in the vertex buffer,
        // render whatever we have in the buffer and start
        // from the beginning of the vertex buffer
        J2dTraceLn2(J2D_TRACE_VERBOSE,
                    "D3DVC::EnsureCapacity exceeded capacity. "\
                    "current v: %d, requested vertices: %d\n",
                    firstUnusedVertex, vNum);
        if (FAILED(res = Render(RESET_ACTION))) {
            return res;
        }
    }

    J2dTraceLn5(J2D_TRACE_VERBOSE,
                "D3DVC::EnsureCapacity current batch: %d "\
                " batch.type=%d newType=%d vNum=%d firstUnusedV=%d",
                currentBatch, batches[currentBatch].pType, newPType, vNum,
                firstUnusedVertex);
    // there should not be multiple linestrips in a batch,
    // or they will be counted as a single line strip
    if (batches[currentBatch].pType != newPType ||
        batches[currentBatch].pType == D3DPT_LINESTRIP)
    {
        // if this is a first unused batch, use it
        if (firstUnusedVertex == firstPendingVertex) {
            // record the first batch and vertex scheduled for rendering
            firstPendingBatch = currentBatch;
            firstPendingVertex = firstUnusedVertex;
        } else {
            // otherwise go to the next batch
            currentBatch++;
        }
        batches[currentBatch].pType = newPType;
        batches[currentBatch].pNum = 0;
    }
    // firstUnusedVertex is updated when new vertices are added
    // to the vertices array

    return res;
}
