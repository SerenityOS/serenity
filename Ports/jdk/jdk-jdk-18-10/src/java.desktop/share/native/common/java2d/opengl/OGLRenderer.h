/*
 * Copyright (c) 2005, 2008, Oracle and/or its affiliates. All rights reserved.
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

#ifndef OGLRenderer_h_Included
#define OGLRenderer_h_Included

#include "sun_java2d_pipe_BufferedRenderPipe.h"
#include "OGLContext.h"

#define BYTES_PER_POLY_POINT \
    sun_java2d_pipe_BufferedRenderPipe_BYTES_PER_POLY_POINT
#define BYTES_PER_SCANLINE \
    sun_java2d_pipe_BufferedRenderPipe_BYTES_PER_SCANLINE
#define BYTES_PER_SPAN \
    sun_java2d_pipe_BufferedRenderPipe_BYTES_PER_SPAN

void OGLRenderer_DrawLine(OGLContext *oglc,
                          jint x1, jint y1, jint x2, jint y2);
void OGLRenderer_DrawRect(OGLContext *oglc,
                          jint x, jint y, jint w, jint h);
void OGLRenderer_DrawPoly(OGLContext *oglc,
                          jint nPoints, jint isClosed,
                          jint transX, jint transY,
                          jint *xPoints, jint *yPoints);
void OGLRenderer_DrawScanlines(OGLContext *oglc,
                               jint count, jint *scanlines);
void OGLRenderer_DrawParallelogram(OGLContext *oglc,
                                   jfloat fx11, jfloat fy11,
                                   jfloat dx21, jfloat dy21,
                                   jfloat dx12, jfloat dy12,
                                   jfloat lw21, jfloat lw12);
void OGLRenderer_DrawAAParallelogram(OGLContext *oglc, OGLSDOps *dstOps,
                                     jfloat fx11, jfloat fy11,
                                     jfloat dx21, jfloat dy21,
                                     jfloat dx12, jfloat dy12,
                                     jfloat lw21, jfloat lw12);

void OGLRenderer_FillRect(OGLContext *oglc,
                          jint x, jint y, jint w, jint h);
void OGLRenderer_FillSpans(OGLContext *oglc,
                           jint count, jint *spans);
void OGLRenderer_FillParallelogram(OGLContext *oglc,
                                   jfloat fx11, jfloat fy11,
                                   jfloat dx21, jfloat dy21,
                                   jfloat dx12, jfloat dy12);
void OGLRenderer_FillAAParallelogram(OGLContext *oglc, OGLSDOps *dstOps,
                                     jfloat fx11, jfloat fy11,
                                     jfloat dx21, jfloat dy21,
                                     jfloat dx12, jfloat dy12);

void OGLRenderer_EnableAAParallelogramProgram();
void OGLRenderer_DisableAAParallelogramProgram();

#endif /* OGLRenderer_h_Included */
