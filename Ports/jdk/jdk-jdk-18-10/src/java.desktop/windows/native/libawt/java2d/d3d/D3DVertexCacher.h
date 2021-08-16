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

#ifndef D3DVERTEXCACHER_H
#define D3DVERTEXCACHER_H

#include "jni.h"
#include "D3DContext.h"

#define MAX_BATCH_SIZE 1024
#define APPEND_ACTION 0x0
#define RESET_ACTION  0x1
#define D3DFVF_J2DLVERTEX \
    (D3DFVF_XYZ | D3DFVF_DIFFUSE | D3DFVF_TEX2 | \
    D3DFVF_TEXCOORDSIZE2(0) | D3DFVF_TEXCOORDSIZE2(1) )
typedef struct _J2DLVERTEX {
    float x, y, z;
    DWORD color;
    float tu1, tv1;
    float tu2, tv2;
} J2DLVERTEX;

typedef struct {
    D3DPRIMITIVETYPE pType; // type of primitives in this batch
    UINT pNum; // number of primitives of pType in this batch
} VertexBatch;

class D3DContext;

class D3DPIPELINE_API D3DVertexCacher {
public:
    HRESULT Init(D3DContext *pCtx);
            ~D3DVertexCacher() { ReleaseDefPoolResources(); }
    void    ReleaseDefPoolResources();

    jint    GetColor() { return color; }
    void    SetColor(jint newColor) { color = newColor; }
    HRESULT DrawLine(int x1, int y1, int x2, int y2);
    HRESULT DrawPoly(jint nPoints, jboolean isClosed,
                     jint transX, jint transY,
                     jint *xPoints, jint *yPoints);
    HRESULT DrawScanlines(jint scanlineCount, jint *scanlines);
    HRESULT DrawRect(int x1, int y1, int x2, int y2);
    HRESULT FillRect(int x1, int y1, int x2, int y2);
    HRESULT FillParallelogramAA(float fx11, float fy11,
                                float dx21, float dy21,
                                float dx12, float dy12);
    HRESULT DrawParallelogramAA(float ox11, float oy11,
                                float ox21, float oy21,
                                float ox12, float oy12,
                                float ix11, float iy11,
                                float ix21, float iy21,
                                float ix12, float iy12);
    HRESULT FillParallelogram(float fx11, float fy11,
                              float dx21, float dy21,
                              float dx12, float dy12);
    HRESULT FillSpans(jint spansCount, jint *spans);
    HRESULT DrawTexture(float dx1, float dy1, float dx2, float dy2,
                        float tx1, float ty1, float tx2, float ty2);
    HRESULT DrawTexture(float  dx1, float  dy1, float  dx2, float  dy2,
                        float t1x1, float t1y1, float t1x2, float t1y2,
                        float t2x1, float t2y1, float t2x2, float t2y2);
    HRESULT Render(int actionType = APPEND_ACTION);
    UINT    GetFreeVertices() { return (MAX_BATCH_SIZE - firstUnusedVertex); }

static
    HRESULT CreateInstance(D3DContext *pCtx, D3DVertexCacher **ppVC);

private:
            D3DVertexCacher();
    HRESULT EnsureCapacity(D3DPRIMITIVETYPE newPType, UINT vNum);

private:
    UINT firstPendingBatch;
    UINT firstPendingVertex;
    UINT firstUnusedVertex;
    UINT currentBatch;
    J2DLVERTEX              vertices[MAX_BATCH_SIZE];
    VertexBatch             batches[MAX_BATCH_SIZE];
    IDirect3DVertexBuffer9  *lpD3DVertexBuffer;
    IDirect3DDevice9        *lpD3DDevice;
    D3DContext              *pCtx;
    jint                    color;
};

#endif // D3DVERTEXCACHER_H
