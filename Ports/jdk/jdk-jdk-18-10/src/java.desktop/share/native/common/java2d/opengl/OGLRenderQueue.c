/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef HEADLESS

#include <stdlib.h>

#include "sun_java2d_pipe_BufferedOpCodes.h"

#include "jlong.h"
#include "OGLBlitLoops.h"
#include "OGLBufImgOps.h"
#include "OGLContext.h"
#include "OGLMaskBlit.h"
#include "OGLMaskFill.h"
#include "OGLPaints.h"
#include "OGLRenderQueue.h"
#include "OGLRenderer.h"
#include "OGLSurfaceData.h"
#include "OGLTextRenderer.h"
#include "OGLVertexCache.h"

/**
 * Used to track whether we are in a series of a simple primitive operations
 * or texturing operations.  This variable should be controlled only via
 * the INIT/CHECK/RESET_PREVIOUS_OP() macros.  See the
 * OGLRenderQueue_CheckPreviousOp() method below for more information.
 */
jint previousOp;

/**
 * References to the "current" context and destination surface.
 */
static OGLContext *oglc = NULL;
static OGLSDOps *dstOps = NULL;

/**
 * The following methods are implemented in the windowing system (i.e. GLX
 * and WGL) source files.
 */
extern OGLContext *OGLSD_SetScratchSurface(JNIEnv *env, jlong pConfigInfo);
extern void OGLGC_DestroyOGLGraphicsConfig(jlong pConfigInfo);
extern void OGLSD_SwapBuffers(JNIEnv *env, jlong window);
extern void OGLSD_Flush(JNIEnv *env);

JNIEXPORT void JNICALL
Java_sun_java2d_opengl_OGLRenderQueue_flushBuffer
    (JNIEnv *env, jobject oglrq,
     jlong buf, jint limit)
{
    jboolean sync = JNI_FALSE;
    unsigned char *b, *end;

    J2dTraceLn1(J2D_TRACE_INFO,
                "OGLRenderQueue_flushBuffer: limit=%d", limit);

    b = (unsigned char *)jlong_to_ptr(buf);
    if (b == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "OGLRenderQueue_flushBuffer: cannot get direct buffer address");
        return;
    }

    INIT_PREVIOUS_OP();
    end = b + limit;

    while (b < end) {
        jint opcode = NEXT_INT(b);

        J2dTraceLn2(J2D_TRACE_VERBOSE,
                    "OGLRenderQueue_flushBuffer: opcode=%d, rem=%d",
                    opcode, (end-b));

        switch (opcode) {

        // draw ops
        case sun_java2d_pipe_BufferedOpCodes_DRAW_LINE:
            {
                jint x1 = NEXT_INT(b);
                jint y1 = NEXT_INT(b);
                jint x2 = NEXT_INT(b);
                jint y2 = NEXT_INT(b);
                OGLRenderer_DrawLine(oglc, x1, y1, x2, y2);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DRAW_RECT:
            {
                jint x = NEXT_INT(b);
                jint y = NEXT_INT(b);
                jint w = NEXT_INT(b);
                jint h = NEXT_INT(b);
                OGLRenderer_DrawRect(oglc, x, y, w, h);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DRAW_POLY:
            {
                jint nPoints      = NEXT_INT(b);
                jboolean isClosed = NEXT_BOOLEAN(b);
                jint transX       = NEXT_INT(b);
                jint transY       = NEXT_INT(b);
                jint *xPoints = (jint *)b;
                jint *yPoints = ((jint *)b) + nPoints;
                OGLRenderer_DrawPoly(oglc, nPoints, isClosed,
                                     transX, transY,
                                     xPoints, yPoints);
                SKIP_BYTES(b, nPoints * BYTES_PER_POLY_POINT);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DRAW_PIXEL:
            {
                jint x = NEXT_INT(b);
                jint y = NEXT_INT(b);
                // Note that we could use GL_POINTS here, but the common
                // use case for DRAW_PIXEL is when rendering a Path2D,
                // which will consist of a mix of DRAW_PIXEL and DRAW_LINE
                // calls.  So to improve batching we use GL_LINES here,
                // even though it requires an extra vertex per pixel.
                CONTINUE_IF_NULL(oglc);
                CHECK_PREVIOUS_OP(GL_LINES);
                j2d_glVertex2i(x, y);
                j2d_glVertex2i(x+1, y+1);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DRAW_SCANLINES:
            {
                jint count = NEXT_INT(b);
                OGLRenderer_DrawScanlines(oglc, count, (jint *)b);
                SKIP_BYTES(b, count * BYTES_PER_SCANLINE);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DRAW_PARALLELOGRAM:
            {
                jfloat x11 = NEXT_FLOAT(b);
                jfloat y11 = NEXT_FLOAT(b);
                jfloat dx21 = NEXT_FLOAT(b);
                jfloat dy21 = NEXT_FLOAT(b);
                jfloat dx12 = NEXT_FLOAT(b);
                jfloat dy12 = NEXT_FLOAT(b);
                jfloat lwr21 = NEXT_FLOAT(b);
                jfloat lwr12 = NEXT_FLOAT(b);
                OGLRenderer_DrawParallelogram(oglc,
                                              x11, y11,
                                              dx21, dy21,
                                              dx12, dy12,
                                              lwr21, lwr12);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DRAW_AAPARALLELOGRAM:
            {
                jfloat x11 = NEXT_FLOAT(b);
                jfloat y11 = NEXT_FLOAT(b);
                jfloat dx21 = NEXT_FLOAT(b);
                jfloat dy21 = NEXT_FLOAT(b);
                jfloat dx12 = NEXT_FLOAT(b);
                jfloat dy12 = NEXT_FLOAT(b);
                jfloat lwr21 = NEXT_FLOAT(b);
                jfloat lwr12 = NEXT_FLOAT(b);
                OGLRenderer_DrawAAParallelogram(oglc, dstOps,
                                                x11, y11,
                                                dx21, dy21,
                                                dx12, dy12,
                                                lwr21, lwr12);
            }
            break;

        // fill ops
        case sun_java2d_pipe_BufferedOpCodes_FILL_RECT:
            {
                jint x = NEXT_INT(b);
                jint y = NEXT_INT(b);
                jint w = NEXT_INT(b);
                jint h = NEXT_INT(b);
                OGLRenderer_FillRect(oglc, x, y, w, h);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_FILL_SPANS:
            {
                jint count = NEXT_INT(b);
                OGLRenderer_FillSpans(oglc, count, (jint *)b);
                SKIP_BYTES(b, count * BYTES_PER_SPAN);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_FILL_PARALLELOGRAM:
            {
                jfloat x11 = NEXT_FLOAT(b);
                jfloat y11 = NEXT_FLOAT(b);
                jfloat dx21 = NEXT_FLOAT(b);
                jfloat dy21 = NEXT_FLOAT(b);
                jfloat dx12 = NEXT_FLOAT(b);
                jfloat dy12 = NEXT_FLOAT(b);
                OGLRenderer_FillParallelogram(oglc,
                                              x11, y11,
                                              dx21, dy21,
                                              dx12, dy12);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_FILL_AAPARALLELOGRAM:
            {
                jfloat x11 = NEXT_FLOAT(b);
                jfloat y11 = NEXT_FLOAT(b);
                jfloat dx21 = NEXT_FLOAT(b);
                jfloat dy21 = NEXT_FLOAT(b);
                jfloat dx12 = NEXT_FLOAT(b);
                jfloat dy12 = NEXT_FLOAT(b);
                OGLRenderer_FillAAParallelogram(oglc, dstOps,
                                                x11, y11,
                                                dx21, dy21,
                                                dx12, dy12);
            }
            break;

        // text-related ops
        case sun_java2d_pipe_BufferedOpCodes_DRAW_GLYPH_LIST:
            {
                jint numGlyphs        = NEXT_INT(b);
                jint packedParams     = NEXT_INT(b);
                jfloat glyphListOrigX = NEXT_FLOAT(b);
                jfloat glyphListOrigY = NEXT_FLOAT(b);
                jboolean usePositions = EXTRACT_BOOLEAN(packedParams,
                                                        OFFSET_POSITIONS);
                jboolean subPixPos    = EXTRACT_BOOLEAN(packedParams,
                                                        OFFSET_SUBPIXPOS);
                jboolean rgbOrder     = EXTRACT_BOOLEAN(packedParams,
                                                        OFFSET_RGBORDER);
                jint lcdContrast      = EXTRACT_BYTE(packedParams,
                                                     OFFSET_CONTRAST);
                unsigned char *images = b;
                unsigned char *positions;
                jint bytesPerGlyph;
                if (usePositions) {
                    positions = (b + numGlyphs * BYTES_PER_GLYPH_IMAGE);
                    bytesPerGlyph = BYTES_PER_POSITIONED_GLYPH;
                } else {
                    positions = NULL;
                    bytesPerGlyph = BYTES_PER_GLYPH_IMAGE;
                }
                OGLTR_DrawGlyphList(env, oglc, dstOps,
                                    numGlyphs, usePositions,
                                    subPixPos, rgbOrder, lcdContrast,
                                    glyphListOrigX, glyphListOrigY,
                                    images, positions);
                SKIP_BYTES(b, numGlyphs * bytesPerGlyph);
            }
            break;

        // copy-related ops
        case sun_java2d_pipe_BufferedOpCodes_COPY_AREA:
            {
                jint x  = NEXT_INT(b);
                jint y  = NEXT_INT(b);
                jint w  = NEXT_INT(b);
                jint h  = NEXT_INT(b);
                jint dx = NEXT_INT(b);
                jint dy = NEXT_INT(b);
                OGLBlitLoops_CopyArea(env, oglc, dstOps,
                                      x, y, w, h, dx, dy);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_BLIT:
            {
                jint packedParams = NEXT_INT(b);
                jint sx1          = NEXT_INT(b);
                jint sy1          = NEXT_INT(b);
                jint sx2          = NEXT_INT(b);
                jint sy2          = NEXT_INT(b);
                jdouble dx1       = NEXT_DOUBLE(b);
                jdouble dy1       = NEXT_DOUBLE(b);
                jdouble dx2       = NEXT_DOUBLE(b);
                jdouble dy2       = NEXT_DOUBLE(b);
                jlong pSrc        = NEXT_LONG(b);
                jlong pDst        = NEXT_LONG(b);
                jint hint         = EXTRACT_BYTE(packedParams, OFFSET_HINT);
                jboolean texture  = EXTRACT_BOOLEAN(packedParams,
                                                    OFFSET_TEXTURE);
                jboolean rtt      = EXTRACT_BOOLEAN(packedParams,
                                                    OFFSET_RTT);
                jboolean xform    = EXTRACT_BOOLEAN(packedParams,
                                                    OFFSET_XFORM);
                jboolean isoblit  = EXTRACT_BOOLEAN(packedParams,
                                                    OFFSET_ISOBLIT);
                if (isoblit) {
                    OGLBlitLoops_IsoBlit(env, oglc, pSrc, pDst,
                                         xform, hint, texture, rtt,
                                         sx1, sy1, sx2, sy2,
                                         dx1, dy1, dx2, dy2);
                } else {
                    jint srctype = EXTRACT_BYTE(packedParams, OFFSET_SRCTYPE);
                    OGLBlitLoops_Blit(env, oglc, pSrc, pDst,
                                      xform, hint, srctype, texture,
                                      sx1, sy1, sx2, sy2,
                                      dx1, dy1, dx2, dy2);
                }
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SURFACE_TO_SW_BLIT:
            {
                jint sx      = NEXT_INT(b);
                jint sy      = NEXT_INT(b);
                jint dx      = NEXT_INT(b);
                jint dy      = NEXT_INT(b);
                jint w       = NEXT_INT(b);
                jint h       = NEXT_INT(b);
                jint dsttype = NEXT_INT(b);
                jlong pSrc   = NEXT_LONG(b);
                jlong pDst   = NEXT_LONG(b);
                OGLBlitLoops_SurfaceToSwBlit(env, oglc,
                                             pSrc, pDst, dsttype,
                                             sx, sy, dx, dy, w, h);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_MASK_FILL:
            {
                jint x        = NEXT_INT(b);
                jint y        = NEXT_INT(b);
                jint w        = NEXT_INT(b);
                jint h        = NEXT_INT(b);
                jint maskoff  = NEXT_INT(b);
                jint maskscan = NEXT_INT(b);
                jint masklen  = NEXT_INT(b);
                unsigned char *pMask = (masklen > 0) ? b : NULL;
                OGLMaskFill_MaskFill(oglc, x, y, w, h,
                                     maskoff, maskscan, masklen, pMask);
                SKIP_BYTES(b, masklen);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_MASK_BLIT:
            {
                jint dstx     = NEXT_INT(b);
                jint dsty     = NEXT_INT(b);
                jint width    = NEXT_INT(b);
                jint height   = NEXT_INT(b);
                jint masklen  = width * height * sizeof(jint);
                OGLMaskBlit_MaskBlit(env, oglc,
                                     dstx, dsty, width, height, b);
                SKIP_BYTES(b, masklen);
            }
            break;

        // state-related ops
        case sun_java2d_pipe_BufferedOpCodes_SET_RECT_CLIP:
            {
                jint x1 = NEXT_INT(b);
                jint y1 = NEXT_INT(b);
                jint x2 = NEXT_INT(b);
                jint y2 = NEXT_INT(b);
                OGLContext_SetRectClip(oglc, dstOps, x1, y1, x2, y2);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_BEGIN_SHAPE_CLIP:
            {
                OGLContext_BeginShapeClip(oglc);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_SHAPE_CLIP_SPANS:
            {
                jint count = NEXT_INT(b);
                OGLRenderer_FillSpans(oglc, count, (jint *)b);
                SKIP_BYTES(b, count * BYTES_PER_SPAN);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_END_SHAPE_CLIP:
            {
                OGLContext_EndShapeClip(oglc, dstOps);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_RESET_CLIP:
            {
                OGLContext_ResetClip(oglc);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_ALPHA_COMPOSITE:
            {
                jint rule         = NEXT_INT(b);
                jfloat extraAlpha = NEXT_FLOAT(b);
                jint flags        = NEXT_INT(b);
                OGLContext_SetAlphaComposite(oglc, rule, extraAlpha, flags);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_XOR_COMPOSITE:
            {
                jint xorPixel = NEXT_INT(b);
                OGLContext_SetXorComposite(oglc, xorPixel);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_RESET_COMPOSITE:
            {
                OGLContext_ResetComposite(oglc);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_TRANSFORM:
            {
                jdouble m00 = NEXT_DOUBLE(b);
                jdouble m10 = NEXT_DOUBLE(b);
                jdouble m01 = NEXT_DOUBLE(b);
                jdouble m11 = NEXT_DOUBLE(b);
                jdouble m02 = NEXT_DOUBLE(b);
                jdouble m12 = NEXT_DOUBLE(b);
                OGLContext_SetTransform(oglc, m00, m10, m01, m11, m02, m12);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_RESET_TRANSFORM:
            {
                OGLContext_ResetTransform(oglc);
            }
            break;

        // context-related ops
        case sun_java2d_pipe_BufferedOpCodes_SET_SURFACES:
            {
                jlong pSrc = NEXT_LONG(b);
                jlong pDst = NEXT_LONG(b);
                if (oglc != NULL) {
                    RESET_PREVIOUS_OP();
                }
                oglc = OGLContext_SetSurfaces(env, pSrc, pDst);
                dstOps = (OGLSDOps *)jlong_to_ptr(pDst);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_SCRATCH_SURFACE:
            {
                jlong pConfigInfo = NEXT_LONG(b);
                if (oglc != NULL) {
                    RESET_PREVIOUS_OP();
                }
                oglc = OGLSD_SetScratchSurface(env, pConfigInfo);
                dstOps = NULL;
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_FLUSH_SURFACE:
            {
                jlong pData = NEXT_LONG(b);
                OGLSDOps *oglsdo = (OGLSDOps *)jlong_to_ptr(pData);
                if (oglsdo != NULL) {
                    CONTINUE_IF_NULL(oglc);
                    RESET_PREVIOUS_OP();
                    OGLSD_Delete(env, oglsdo);
                }
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DISPOSE_SURFACE:
            {
                jlong pData = NEXT_LONG(b);
                OGLSDOps *oglsdo = (OGLSDOps *)jlong_to_ptr(pData);
                if (oglsdo != NULL) {
                    CONTINUE_IF_NULL(oglc);
                    RESET_PREVIOUS_OP();
                    OGLSD_Delete(env, oglsdo);
                    if (oglsdo->privOps != NULL) {
                        free(oglsdo->privOps);
                    }
                }
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DISPOSE_CONFIG:
            {
                jlong pConfigInfo = NEXT_LONG(b);
                CONTINUE_IF_NULL(oglc);
                RESET_PREVIOUS_OP();
                OGLGC_DestroyOGLGraphicsConfig(pConfigInfo);

                // the previous method will call glX/wglMakeCurrent(None),
                // so we should nullify the current oglc and dstOps to avoid
                // calling glFlush() (or similar) while no context is current
                oglc = NULL;
                dstOps = NULL;
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_INVALIDATE_CONTEXT:
            {
                // flush just in case there are any pending operations in
                // the hardware pipe
                if (oglc != NULL) {
                    RESET_PREVIOUS_OP();
                    j2d_glFlush();
                }

                // invalidate the references to the current context and
                // destination surface that are maintained at the native level
                oglc = NULL;
                dstOps = NULL;
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SYNC:
            {
                sync = JNI_TRUE;
            }
            break;

        // multibuffering ops
        case sun_java2d_pipe_BufferedOpCodes_SWAP_BUFFERS:
            {
                jlong window = NEXT_LONG(b);
                if (oglc != NULL) {
                    RESET_PREVIOUS_OP();
                }
                OGLSD_SwapBuffers(env, window);
            }
            break;

        // special no-op (mainly used for achieving 8-byte alignment)
        case sun_java2d_pipe_BufferedOpCodes_NOOP:
            break;

        // paint-related ops
        case sun_java2d_pipe_BufferedOpCodes_RESET_PAINT:
            {
                OGLPaints_ResetPaint(oglc);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_COLOR:
            {
                jint pixel = NEXT_INT(b);
                OGLPaints_SetColor(oglc, pixel);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_GRADIENT_PAINT:
            {
                jboolean useMask= NEXT_BOOLEAN(b);
                jboolean cyclic = NEXT_BOOLEAN(b);
                jdouble p0      = NEXT_DOUBLE(b);
                jdouble p1      = NEXT_DOUBLE(b);
                jdouble p3      = NEXT_DOUBLE(b);
                jint pixel1     = NEXT_INT(b);
                jint pixel2     = NEXT_INT(b);
                OGLPaints_SetGradientPaint(oglc, useMask, cyclic,
                                           p0, p1, p3,
                                           pixel1, pixel2);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_LINEAR_GRADIENT_PAINT:
            {
                jboolean useMask = NEXT_BOOLEAN(b);
                jboolean linear  = NEXT_BOOLEAN(b);
                jint cycleMethod = NEXT_INT(b);
                jint numStops    = NEXT_INT(b);
                jfloat p0        = NEXT_FLOAT(b);
                jfloat p1        = NEXT_FLOAT(b);
                jfloat p3        = NEXT_FLOAT(b);
                void *fractions, *pixels;
                fractions = b; SKIP_BYTES(b, numStops * sizeof(jfloat));
                pixels    = b; SKIP_BYTES(b, numStops * sizeof(jint));
                OGLPaints_SetLinearGradientPaint(oglc, dstOps,
                                                 useMask, linear,
                                                 cycleMethod, numStops,
                                                 p0, p1, p3,
                                                 fractions, pixels);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_RADIAL_GRADIENT_PAINT:
            {
                jboolean useMask = NEXT_BOOLEAN(b);
                jboolean linear  = NEXT_BOOLEAN(b);
                jint numStops    = NEXT_INT(b);
                jint cycleMethod = NEXT_INT(b);
                jfloat m00       = NEXT_FLOAT(b);
                jfloat m01       = NEXT_FLOAT(b);
                jfloat m02       = NEXT_FLOAT(b);
                jfloat m10       = NEXT_FLOAT(b);
                jfloat m11       = NEXT_FLOAT(b);
                jfloat m12       = NEXT_FLOAT(b);
                jfloat focusX    = NEXT_FLOAT(b);
                void *fractions, *pixels;
                fractions = b; SKIP_BYTES(b, numStops * sizeof(jfloat));
                pixels    = b; SKIP_BYTES(b, numStops * sizeof(jint));
                OGLPaints_SetRadialGradientPaint(oglc, dstOps,
                                                 useMask, linear,
                                                 cycleMethod, numStops,
                                                 m00, m01, m02,
                                                 m10, m11, m12,
                                                 focusX,
                                                 fractions, pixels);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_SET_TEXTURE_PAINT:
            {
                jboolean useMask= NEXT_BOOLEAN(b);
                jboolean filter = NEXT_BOOLEAN(b);
                jlong pSrc      = NEXT_LONG(b);
                jdouble xp0     = NEXT_DOUBLE(b);
                jdouble xp1     = NEXT_DOUBLE(b);
                jdouble xp3     = NEXT_DOUBLE(b);
                jdouble yp0     = NEXT_DOUBLE(b);
                jdouble yp1     = NEXT_DOUBLE(b);
                jdouble yp3     = NEXT_DOUBLE(b);
                OGLPaints_SetTexturePaint(oglc, useMask, pSrc, filter,
                                          xp0, xp1, xp3,
                                          yp0, yp1, yp3);
            }
            break;

        // BufferedImageOp-related ops
        case sun_java2d_pipe_BufferedOpCodes_ENABLE_CONVOLVE_OP:
            {
                jlong pSrc        = NEXT_LONG(b);
                jboolean edgeZero = NEXT_BOOLEAN(b);
                jint kernelWidth  = NEXT_INT(b);
                jint kernelHeight = NEXT_INT(b);
                OGLBufImgOps_EnableConvolveOp(oglc, pSrc, edgeZero,
                                              kernelWidth, kernelHeight, b);
                SKIP_BYTES(b, kernelWidth * kernelHeight * sizeof(jfloat));
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DISABLE_CONVOLVE_OP:
            {
                OGLBufImgOps_DisableConvolveOp(oglc);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_ENABLE_RESCALE_OP:
            {
                jlong pSrc          = NEXT_LONG(b);
                jboolean nonPremult = NEXT_BOOLEAN(b);
                jint numFactors     = 4;
                unsigned char *scaleFactors = b;
                unsigned char *offsets = (b + numFactors * sizeof(jfloat));
                OGLBufImgOps_EnableRescaleOp(oglc, pSrc, nonPremult,
                                             scaleFactors, offsets);
                SKIP_BYTES(b, numFactors * sizeof(jfloat) * 2);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DISABLE_RESCALE_OP:
            {
                OGLBufImgOps_DisableRescaleOp(oglc);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_ENABLE_LOOKUP_OP:
            {
                jlong pSrc          = NEXT_LONG(b);
                jboolean nonPremult = NEXT_BOOLEAN(b);
                jboolean shortData  = NEXT_BOOLEAN(b);
                jint numBands       = NEXT_INT(b);
                jint bandLength     = NEXT_INT(b);
                jint offset         = NEXT_INT(b);
                jint bytesPerElem = shortData ? sizeof(jshort):sizeof(jbyte);
                void *tableValues = b;
                OGLBufImgOps_EnableLookupOp(oglc, pSrc, nonPremult, shortData,
                                            numBands, bandLength, offset,
                                            tableValues);
                SKIP_BYTES(b, numBands * bandLength * bytesPerElem);
            }
            break;
        case sun_java2d_pipe_BufferedOpCodes_DISABLE_LOOKUP_OP:
            {
                OGLBufImgOps_DisableLookupOp(oglc);
            }
            break;

        default:
            J2dRlsTraceLn1(J2D_TRACE_ERROR,
                "OGLRenderQueue_flushBuffer: invalid opcode=%d", opcode);
            if (oglc != NULL) {
                RESET_PREVIOUS_OP();
            }
            return;
        }
    }

    if (oglc != NULL) {
        RESET_PREVIOUS_OP();
        if (sync) {
            j2d_glFinish();
        } else {
            j2d_glFlush();
        }
        OGLSD_Flush(env);
    }
}

/**
 * Returns a pointer to the "current" context, as set by the last SET_SURFACES
 * or SET_SCRATCH_SURFACE operation.
 */
OGLContext *
OGLRenderQueue_GetCurrentContext()
{
    return oglc;
}

/**
 * Returns a pointer to the "current" destination surface, as set by the last
 * SET_SURFACES operation.
 */
OGLSDOps *
OGLRenderQueue_GetCurrentDestination()
{
    return dstOps;
}

/**
 * Used to track whether we are within a series of simple primitive operations
 * or texturing operations.  The op parameter determines the nature of the
 * operation that is to follow.  Valid values for this op parameter are:
 *
 *     GL_QUADS
 *     GL_LINES
 *     GL_LINE_LOOP
 *     GL_LINE_STRIP
 *     (basically any of the valid parameters for glBegin())
 *
 *     GL_TEXTURE_2D
 *     GL_TEXTURE_RECTANGLE_ARB
 *
 *     OGL_STATE_RESET
 *     OGL_STATE_CHANGE
 *     OGL_STATE_MASK_OP
 *     OGL_STATE_GLYPH_OP
 *
 * Note that the above constants are guaranteed to be unique values.  The
 * last few are defined to be negative values to differentiate them from
 * the core GL* constants, which are defined to be non-negative.
 *
 * For simple primitives, this method allows us to batch similar primitives
 * within the same glBegin()/glEnd() pair.  For example, if we have 100
 * consecutive FILL_RECT operations, we only have to call glBegin(GL_QUADS)
 * for the first op, and then subsequent operations will consist only of
 * glVertex*() calls, which helps improve performance.  The glEnd() call
 * only needs to be issued before an operation that cannot happen within a
 * glBegin()/glEnd() pair (e.g. updating the clip), or one that requires a
 * different primitive mode (e.g. GL_LINES).
 *
 * For operations that involve texturing, this method helps us to avoid
 * calling glEnable(GL_TEXTURE_2D) and glDisable(GL_TEXTURE_2D) around each
 * operation.  For example, if we have an alternating series of ISO_BLIT
 * and MASK_BLIT operations (both of which involve texturing), we need
 * only to call glEnable(GL_TEXTURE_2D) before the first ISO_BLIT operation.
 * The glDisable(GL_TEXTURE_2D) call only needs to be issued before an
 * operation that cannot (or should not) happen while texturing is enabled
 * (e.g. a context change, or a simple primitive operation like GL_QUADS).
 */
void
OGLRenderQueue_CheckPreviousOp(jint op)
{
    if (previousOp == op) {
        // The op is the same as last time, so we can return immediately.
        return;
    }

    J2dTraceLn1(J2D_TRACE_VERBOSE,
                "OGLRenderQueue_CheckPreviousOp: new op=%d", op);

    switch (previousOp) {
    case GL_TEXTURE_2D:
    case GL_TEXTURE_RECTANGLE_ARB:
        if (op == OGL_STATE_CHANGE) {
            // Optimization: Certain state changes (those marked as
            // OGL_STATE_CHANGE) are allowed while texturing is enabled.
            // In this case, we can allow previousOp to remain as it is and
            // then return early.
            return;
        } else {
            // Otherwise, op must be a primitive operation, or a reset, so
            // we will disable texturing.
            j2d_glDisable(previousOp);
            // This next step of binding to zero should not be strictly
            // necessary, but on some older Nvidia boards (e.g. GeForce 2)
            // problems will arise if GL_TEXTURE_2D and
            // GL_TEXTURE_RECTANGLE_ARB are bound at the same time, so we
            // will do this just to be safe.
            j2d_glBindTexture(previousOp, 0);
        }
        break;
    case OGL_STATE_MASK_OP:
        OGLVertexCache_DisableMaskCache(oglc);
        break;
    case OGL_STATE_GLYPH_OP:
        OGLTR_DisableGlyphVertexCache(oglc);
        break;
    case OGL_STATE_PGRAM_OP:
        OGLRenderer_DisableAAParallelogramProgram();
        break;
    case OGL_STATE_RESET:
    case OGL_STATE_CHANGE:
        // No-op
        break;
    default:
        // In this case, op must be one of:
        //     - the start of a different primitive type (glBegin())
        //     - a texturing operation
        //     - a state change (not allowed within glBegin()/glEnd() pairs)
        //     - a reset
        // so we must first complete the previous primitive operation.
        j2d_glEnd();
        break;
    }

    switch (op) {
    case GL_TEXTURE_2D:
    case GL_TEXTURE_RECTANGLE_ARB:
        // We are starting a texturing operation, so enable texturing.
        j2d_glEnable(op);
        break;
    case OGL_STATE_MASK_OP:
        OGLVertexCache_EnableMaskCache(oglc);
        break;
    case OGL_STATE_GLYPH_OP:
        OGLTR_EnableGlyphVertexCache(oglc);
        break;
    case OGL_STATE_PGRAM_OP:
        OGLRenderer_EnableAAParallelogramProgram();
        break;
    case OGL_STATE_RESET:
    case OGL_STATE_CHANGE:
        // No-op
        break;
    default:
        // We are starting a primitive operation, so call glBegin() with
        // the given primitive type.
        j2d_glBegin(op);
        break;
    }

    previousOp = op;
}

#endif /* !HEADLESS */
