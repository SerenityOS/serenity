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

#include "sun_java2d_pipe_BufferedRenderPipe.h"
#include "D3DContext.h"

#define BYTES_PER_POLY_POINT \
    sun_java2d_pipe_BufferedRenderPipe_BYTES_PER_POLY_POINT
#define BYTES_PER_SCANLINE \
    sun_java2d_pipe_BufferedRenderPipe_BYTES_PER_SCANLINE
#define BYTES_PER_SPAN \
    sun_java2d_pipe_BufferedRenderPipe_BYTES_PER_SPAN

HRESULT D3DPIPELINE_API
D3DRenderer_DrawLine(D3DContext *d3dc,
                     jint x1, jint y1, jint x2, jint y2);

HRESULT D3DPIPELINE_API
D3DRenderer_DrawRect(D3DContext *d3dc,
                     jint x, jint y, jint w, jint h);

HRESULT D3DPIPELINE_API
D3DRenderer_FillRect(D3DContext *d3dc,
                     jint x, jint y, jint w, jint h);

HRESULT D3DPIPELINE_API
D3DRenderer_DrawPoly(D3DContext *d3dc,
                     jint nPoints, jboolean isClosed,
                     jint transX, jint transY,
                     jint *xPoints, jint *yPoints);

HRESULT D3DPIPELINE_API
D3DRenderer_DrawScanlines(D3DContext *d3dc,
                          jint scanlineCount, jint *scanlines);

HRESULT D3DPIPELINE_API
D3DRenderer_FillSpans(D3DContext *d3dc, jint spanCount, jint *spans);

HRESULT D3DPIPELINE_API
D3DRenderer_FillParallelogram(D3DContext *d3dc,
                              jfloat fx11, jfloat fy11,
                              jfloat dx21, jfloat dy21,
                              jfloat dx12, jfloat dy12);

HRESULT D3DPIPELINE_API
D3DRenderer_DrawParallelogram(D3DContext *d3dc,
                              jfloat fx11, jfloat fy11,
                              jfloat dx21, jfloat dy21,
                              jfloat dx12, jfloat dy12,
                              jfloat lw21, jfloat lw12);

HRESULT D3DPIPELINE_API
D3DRenderer_FillAAParallelogram(D3DContext *d3dc,
                                jfloat fx11, jfloat fy11,
                                jfloat dx21, jfloat dy21,
                                jfloat dx12, jfloat dy12);

HRESULT D3DPIPELINE_API
D3DRenderer_DrawAAParallelogram(D3DContext *d3dc,
                                jfloat fx11, jfloat fy11,
                                jfloat dx21, jfloat dy21,
                                jfloat dx12, jfloat dy12,
                                jfloat lw21, jfloat lw12);
