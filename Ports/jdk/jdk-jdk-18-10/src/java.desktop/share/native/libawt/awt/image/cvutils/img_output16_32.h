/*
 * Copyright (c) 1996, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This file contains macro definitions for the Storing category of
 * the macros used by the generic scaleloop function.
 *
 * This implementation can store 16-bit or 32-bit pixels into an
 * array of shorts, or longs such that the pixel for (srcX, srcY)
 * is stored at index (srcOff + srcY * srcScan + srcX) in the array.
 */

#define DeclareOutputVars                               \
    pixptr dstP;                                        \
    int dst32;

#define InitOutput(cvdata, clrdata, dstX, dstY)                 \
    do {                                                        \
        switch (clrdata->bitsperpixel) {                        \
        case 16: dst32 = 1; break;                              \
        case 32: dst32 = 2; break;                              \
        default:                                                \
            SignalError(0, JAVAPKG "InternalError",             \
                        "unsupported screen depth");            \
            return SCALEFAILURE;                                \
        }                                                       \
        img_check((ScanBytes(cvdata) & ((1 << dst32)-1)) == 0); \
        dstP.vp = cvdata->outbuf;                               \
        dstP.bp += dstY * ScanBytes(cvdata) + (dstX << dst32);  \
    } while (0)

#define PutPixelInc(pixel, red, green, blue)                    \
    do {                                                        \
        switch (dst32) {                                        \
        case 1:                                                 \
            *dstP.sp++ = ((unsigned short) pixel);              \
            break;                                              \
        case 2:                                                 \
            *dstP.ip++ = pixel;                                 \
            break;                                              \
        }                                                       \
    } while (0)

#define EndOutputRow(cvdata, dstY, dstX1, dstX2)                \
    do {                                                        \
        SendRow(cvdata, dstY, dstX1, dstX2);                    \
        dstP.bp += (ScanBytes(cvdata)                           \
                    - ((dstX2 - dstX1) << dst32));              \
    } while (0)

#define EndOutputRect(cvdata, dstX1, dstY1, dstX2, dstY2)       \
    SendBuffer(cvdata, dstX1, dstY1, dstX2, dstY2)
