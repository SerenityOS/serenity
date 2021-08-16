/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>
#include <jlong.h>
#include <jni_util.h>
#include "sun_java2d_pipe_BufferedMaskBlit.h"
#include "sun_java2d_pipe_BufferedOpCodes.h"
#include "Trace.h"
#include "GraphicsPrimitiveMgr.h"
#include "IntArgb.h"
#include "IntRgb.h"
#include "IntBgr.h"

#define MAX_MASK_LENGTH (32 * 32)
extern unsigned char mul8table[256][256];

/**
 * This implementation of MaskBlit first combines the source system memory
 * tile with the corresponding alpha mask and stores the resulting
 * IntArgbPre pixels directly into the RenderBuffer.  Those pixels are
 * then eventually pulled off the RenderBuffer and copied to the destination
 * surface in OGL/D3DMaskBlit.
 *
 * Note that currently there are only inner loops defined for IntArgb,
 * IntArgbPre, IntRgb, and IntBgr, as those are the most commonly used
 * formats for this operation.
 */
JNIEXPORT jint JNICALL
Java_sun_java2d_pipe_BufferedMaskBlit_enqueueTile
    (JNIEnv *env, jobject mb,
     jlong buf, jint bpos,
     jobject srcData, jlong pSrcOps, jint srcType,
     jbyteArray maskArray, jint masklen, jint maskoff, jint maskscan,
     jint srcx, jint srcy, jint dstx, jint dsty,
     jint width, jint height)
{
    SurfaceDataOps *srcOps = (SurfaceDataOps *)jlong_to_ptr(pSrcOps);
    SurfaceDataRasInfo srcInfo;
    unsigned char *bbuf;
    jint *pBuf;

    J2dTraceLn1(J2D_TRACE_INFO,
                "BufferedMaskBlit_enqueueTile: bpos=%d",
                bpos);

    if (srcOps == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "BufferedMaskBlit_enqueueTile: srcOps is null");
        return bpos;
    }

    bbuf = (unsigned char *)jlong_to_ptr(buf);
    if (bbuf == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "BufferedMaskBlit_enqueueTile: cannot get direct buffer address");
        return bpos;
    }
    pBuf = (jint *)(bbuf + bpos);

    if (JNU_IsNull(env, maskArray)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "BufferedMaskBlit_enqueueTile: mask array is null");
        return bpos;
    }

    if (masklen > MAX_MASK_LENGTH) {
        // REMIND: this approach is seriously flawed if the mask
        //         length is ever greater than MAX_MASK_LENGTH (won't fit
        //         into the cached mask tile); so far this hasn't
        //         been a problem though...
        J2dRlsTraceLn(J2D_TRACE_ERROR,
            "BufferedMaskBlit_enqueueTile: mask array too large");
        return bpos;
    }

    srcInfo.bounds.x1 = srcx;
    srcInfo.bounds.y1 = srcy;
    srcInfo.bounds.x2 = srcx + width;
    srcInfo.bounds.y2 = srcy + height;

    if (srcOps->Lock(env, srcOps, &srcInfo, SD_LOCK_READ) != SD_SUCCESS) {
        J2dRlsTraceLn(J2D_TRACE_WARNING,
                      "BufferedMaskBlit_enqueueTile: could not acquire lock");
        return bpos;
    }

    if (srcInfo.bounds.x2 > srcInfo.bounds.x1 &&
        srcInfo.bounds.y2 > srcInfo.bounds.y1)
    {
        srcOps->GetRasInfo(env, srcOps, &srcInfo);
        if (srcInfo.rasBase) {
            jint h;
            jint srcScanStride = srcInfo.scanStride;
            jint srcPixelStride = srcInfo.pixelStride;
            jint *pSrc = (jint *)
                PtrCoord(srcInfo.rasBase,
                         srcInfo.bounds.x1, srcInfo.pixelStride,
                         srcInfo.bounds.y1, srcInfo.scanStride);
            unsigned char *pMask, *pMaskAlloc;
            pMask = pMaskAlloc =
                (*env)->GetPrimitiveArrayCritical(env, maskArray, 0);
            if (pMask == NULL) {
                J2dRlsTraceLn(J2D_TRACE_ERROR,
                    "BufferedMaskBlit_enqueueTile: cannot lock mask array");
                SurfaceData_InvokeRelease(env, srcOps, &srcInfo);
                SurfaceData_InvokeUnlock(env, srcOps, &srcInfo);
                return bpos;
            }

            width = srcInfo.bounds.x2 - srcInfo.bounds.x1;
            height = srcInfo.bounds.y2 - srcInfo.bounds.y1;
            maskoff += ((srcInfo.bounds.y1 - srcy) * maskscan +
                        (srcInfo.bounds.x1 - srcx));
            maskscan -= width;
            pMask += maskoff;
            srcScanStride -= width * srcPixelStride;
            h = height;

            J2dTraceLn4(J2D_TRACE_VERBOSE,
                        "  sx=%d sy=%d w=%d h=%d",
                        srcInfo.bounds.x1, srcInfo.bounds.y1, width, height);
            J2dTraceLn2(J2D_TRACE_VERBOSE,
                        "  maskoff=%d maskscan=%d",
                        maskoff, maskscan);
            J2dTraceLn2(J2D_TRACE_VERBOSE,
                        "  pixstride=%d scanstride=%d",
                        srcPixelStride, srcScanStride);

            // enqueue parameters
            pBuf[0] = sun_java2d_pipe_BufferedOpCodes_MASK_BLIT;
            pBuf[1] = dstx;
            pBuf[2] = dsty;
            pBuf[3] = width;
            pBuf[4] = height;
            pBuf += 5;
            bpos += 5 * sizeof(jint);

            // apply alpha values from mask to the source tile, and store
            // resulting IntArgbPre pixels into RenderBuffer (there are
            // separate inner loops for the most common source formats)
            switch (srcType) {
            case sun_java2d_pipe_BufferedMaskBlit_ST_INT_ARGB:
                do {
                    jint w = width;
                    do {
                        jint pathA = *pMask++;
                        if (!pathA) {
                            pBuf[0] = 0;
                        } else {
                            jint pixel = pSrc[0];
                            if (pathA == 0xff && (pixel >> 24) + 1 == 0) {
                                pBuf[0] = pixel;
                            } else {
                                jint r, g, b, a;
                                ExtractIntDcmComponents1234(pixel, a, r, g, b);
                                a = MUL8(pathA, a);
                                r = MUL8(a, r);
                                g = MUL8(a, g);
                                b = MUL8(a, b);
                                pBuf[0] = (a << 24) | (r << 16) | (g << 8) | b;
                            }
                        }
                        pSrc = PtrAddBytes(pSrc, srcPixelStride);
                        pBuf++;
                    } while (--w > 0);
                    pSrc = PtrAddBytes(pSrc, srcScanStride);
                    pMask = PtrAddBytes(pMask, maskscan);
                } while (--h > 0);
                break;

            case sun_java2d_pipe_BufferedMaskBlit_ST_INT_ARGB_PRE:
                do {
                    jint w = width;
                    do {
                        jint pathA = *pMask++;
                        if (!pathA) {
                            pBuf[0] = 0;
                        } else if (pathA == 0xff) {
                            pBuf[0] = pSrc[0];
                        } else {
                            jint r, g, b, a;
                            a = MUL8(pathA, (pSrc[0] >> 24) & 0xff);
                            r = MUL8(pathA, (pSrc[0] >> 16) & 0xff);
                            g = MUL8(pathA, (pSrc[0] >>  8) & 0xff);
                            b = MUL8(pathA, (pSrc[0] >>  0) & 0xff);
                            pBuf[0] = (a << 24) | (r << 16) | (g << 8) | b;
                        }
                        pSrc = PtrAddBytes(pSrc, srcPixelStride);
                        pBuf++;
                    } while (--w > 0);
                    pSrc = PtrAddBytes(pSrc, srcScanStride);
                    pMask = PtrAddBytes(pMask, maskscan);
                } while (--h > 0);
                break;

            case sun_java2d_pipe_BufferedMaskBlit_ST_INT_RGB:
                do {
                    jint w = width;
                    do {
                        jint pathA = *pMask++;
                        if (!pathA) {
                            pBuf[0] = 0;
                        } else if (pathA == 0xff) {
                            pBuf[0] = pSrc[0] | 0xff000000;
                        } else {
                            jint r, g, b, a;
                            LoadIntRgbTo3ByteRgb(pSrc, c, 0, r, g, b);
                            a = pathA;
                            r = MUL8(a, r);
                            g = MUL8(a, g);
                            b = MUL8(a, b);
                            pBuf[0] = (a << 24) | (r << 16) | (g << 8) | b;
                        }
                        pSrc = PtrAddBytes(pSrc, srcPixelStride);
                        pBuf++;
                    } while (--w > 0);
                    pSrc = PtrAddBytes(pSrc, srcScanStride);
                    pMask = PtrAddBytes(pMask, maskscan);
                } while (--h > 0);
                break;

            case sun_java2d_pipe_BufferedMaskBlit_ST_INT_BGR:
                do {
                    jint w = width;
                    do {
                        jint pathA = *pMask++;
                        if (!pathA) {
                            pBuf[0] = 0;
                        } else {
                            jint r, g, b, a;
                            LoadIntBgrTo3ByteRgb(pSrc, c, 0, r, g, b);
                            a = pathA;
                            r = MUL8(a, r);
                            g = MUL8(a, g);
                            b = MUL8(a, b);
                            pBuf[0] = (a << 24) | (r << 16) | (g << 8) | b;
                        }
                        pSrc = PtrAddBytes(pSrc, srcPixelStride);
                        pBuf++;
                    } while (--w > 0);
                    pSrc = PtrAddBytes(pSrc, srcScanStride);
                    pMask = PtrAddBytes(pMask, maskscan);
                } while (--h > 0);
                break;

            default:
                // should not get here, just no-op...
                break;
            }

            // increment current byte position
            bpos += width * height * sizeof(jint);

            (*env)->ReleasePrimitiveArrayCritical(env, maskArray,
                                                  pMaskAlloc, JNI_ABORT);
        }
        SurfaceData_InvokeRelease(env, srcOps, &srcInfo);
    }
    SurfaceData_InvokeUnlock(env, srcOps, &srcInfo);

    // return the current byte position
    return bpos;
}
