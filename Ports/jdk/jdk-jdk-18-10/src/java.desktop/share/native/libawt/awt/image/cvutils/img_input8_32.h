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
 * This file contains macro definitions for the Fetching category of
 * the macros used by the generic scaleloop function.
 *
 * This implementation can load either 8-bit or 32-bit pixels from an
 * array of bytes or longs where the data for pixel (srcX, srcY) is
 * loaded from index (srcOff + srcY * srcScan + srcX) in the array.
 *
 * This file can be used to provide the default implementation of the
 * Fetching macros to handle all input sizes.
 */

#define DeclareInputVars                                        \
    pixptr srcP;                                                \
    int src32;

#define InitInput(srcBPP)                                               \
    do {                                                                \
        switch (srcBPP) {                                               \
        case 8: src32 = 0; break;                                       \
        case 32: src32 = 1; break;                                      \
        default:                                                        \
            SignalError(0, JAVAPKG "InternalError",                     \
                        "unsupported source depth");                    \
            return SCALEFAILURE;                                        \
        }                                                               \
    } while (0)

#define SetInputRow(pixels, srcOff, srcScan, srcY, srcOY)               \
    do {                                                                \
        srcP.vp = pixels;                                               \
        if (src32) {                                                    \
            srcP.ip += srcOff + ((srcY-srcOY) * srcScan);               \
        } else {                                                        \
            srcP.bp += srcOff + ((srcY-srcOY) * srcScan);               \
        }                                                               \
    } while (0)

#define GetPixelInc()                                                   \
    (src32 ? *srcP.ip++ : ((int) *srcP.bp++))

#define GetPixel(srcX)                                                  \
    (src32 ? srcP.ip[srcX] : ((int) srcP.bp[srcX]))

#define InputPixelInc(X)                                                \
    do {                                                                \
        if (src32) {                                                    \
            srcP.ip += X;                                               \
        } else {                                                        \
            srcP.bp += X;                                               \
        }                                                               \
    } while (0)

#define VerifyPixelRange(pixel, mapsize)                                \
    do {                                                                \
        if (((unsigned int) pixel) >= mapsize) {                        \
            SignalError(0, JAVAPKG "ArrayIndexOutOfBoundsException", 0);\
            return SCALEFAILURE;                                        \
        }                                                               \
    } while (0)
