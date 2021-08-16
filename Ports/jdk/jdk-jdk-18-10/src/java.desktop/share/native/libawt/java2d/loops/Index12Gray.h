/*
 * Copyright (c) 2001, 2008, Oracle and/or its affiliates. All rights reserved.
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

#ifndef Index12Gray_h_Included
#define Index12Gray_h_Included

#include "IntDcm.h"
#include "ByteGray.h"

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "Index12Gray".
 */

typedef jushort Index12GrayPixelType;
typedef jushort Index12GrayDataType;

#define Index12GrayIsOpaque 1

#define Index12GrayPixelStride          2
#define Index12GrayBitsPerPixel        12

#define DeclareIndex12GrayLoadVars(PREFIX) \
    jint *PREFIX ## Lut;

#define DeclareIndex12GrayStoreVars(PREFIX) \
    jint *PREFIX ## InvGrayLut;

#define SetIndex12GrayStoreVarsYPos(PREFIX, pRasInfo, LOC)
#define SetIndex12GrayStoreVarsXPos(PREFIX, pRasInfo, LOC)
#define InitIndex12GrayLoadVars(PREFIX, pRasInfo) \
    PREFIX ## Lut = (pRasInfo)->lutBase

#define InitIndex12GrayStoreVarsY(PREFIX, pRasInfo) \
    PREFIX ## InvGrayLut = (pRasInfo)->invGrayTable;

#define InitIndex12GrayStoreVarsX(PREFIX, pRasInfo)
#define NextIndex12GrayStoreVarsX(PREFIX)
#define NextIndex12GrayStoreVarsY(PREFIX)

#define Index12GrayXparLutEntry                 -1
#define Index12GrayIsXparLutEntry(pix)          (pix < 0)
#define StoreIndex12GrayNonXparFromArgb         StoreIndex12GrayFrom1IntArgb

#define StoreIndex12GrayPixel(pRas, x, pixel) \
    ((pRas)[x] = (jushort) (pixel))

#define DeclareIndex12GrayPixelData(PREFIX)

#define ExtractIndex12GrayPixelData(PIXEL, PREFIX)

#define StoreIndex12GrayPixelData(pPix, x, pixel, PREFIX) \
    ((pPix)[x] = (jushort) (pixel))

#define Index12GrayPixelFromArgb(pixel, rgb, pRasInfo) \
    do { \
        jint r, g, b, gray; \
        ExtractIntDcmComponentsX123(rgb, r, g, b); \
        gray = ComposeByteGrayFrom3ByteRgb(r, g, b); \
        (pixel) = (pRasInfo)->invGrayTable[gray]; \
    } while (0)

#define LoadIndex12GrayTo1IntRgb(pRas, PREFIX, x, rgb) \
    (rgb) = PREFIX ## Lut[pRas[x] & 0xfff]

#define LoadIndex12GrayTo1IntArgb(pRas, PREFIX, x, argb) \
    (argb) = PREFIX ## Lut[pRas[x] & 0xfff]

#define LoadIndex12GrayTo1ByteGray(pRas, PREFIX, x, gray) \
    (gray) = (jubyte)PREFIX ## Lut[pRas[x] & 0xfff]

#define LoadIndex12GrayTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
    r = g = b = (jubyte)PREFIX ## Lut[pRas[x] & 0xfff]

#define LoadIndex12GrayTo4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    do { \
        a = 0xff; \
        LoadIndex12GrayTo3ByteRgb(pRas, PREFIX, x, r, g, b); \
    } while (0)

#define StoreIndex12GrayFrom1IntRgb(pRas, PREFIX, x, rgb) \
    do { \
        int r, g, b; \
        ExtractIntDcmComponentsX123(rgb, r, g, b); \
        StoreIndex12GrayFrom3ByteRgb(pRas, PREFIX, x, r, g, b); \
    } while (0)

#define StoreIndex12GrayFrom1IntArgb(pRas, PREFIX, x, argb) \
    StoreIndex12GrayFrom1IntRgb(pRas, PREFIX, x, argb)

#define StoreIndex12GrayFrom3ByteRgb(pRas, PREFIX, x, r, g, b) \
    do { \
        int gray = ComposeByteGrayFrom3ByteRgb(r, g, b); \
        (pRas)[x] = (jushort) (PREFIX ## InvGrayLut[gray]); \
    } while (0)

#define StoreIndex12GrayFrom4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    StoreIndex12GrayFrom3ByteRgb(pRas, PREFIX, x, r, g, b)

#define StoreIndex12GrayFrom1ByteGray(pRas, PREFIX, x, gray) \
    (pRas)[x] = (jushort) (PREFIX ## InvGrayLut[gray]);

#define CopyIndex12GrayToIntArgbPre(pRGB, i, PREFIX, pRow, x) \
    (pRGB)[i] = PREFIX ## Lut[(pRow)[x] & 0xfff]


#define DeclareIndex12GrayAlphaLoadData(PREFIX) \
    jint *PREFIX ## Lut;

#define InitIndex12GrayAlphaLoadData(PREFIX, pRasInfo) \
    PREFIX ## Lut = (pRasInfo)->lutBase

#define LoadAlphaFromIndex12GrayFor1ByteGray(pRas, PREFIX, COMP_PREFIX) \
    COMP_PREFIX ## A = 0xff

#define Postload1ByteGrayFromIndex12Gray(pRas, PREFIX, COMP_PREFIX) \
    COMP_PREFIX ## G = (jubyte)PREFIX ## Lut[(pRas)[0] & 0xfff]

#define StoreIndex12GrayFrom1ByteGrayComps(pRas, PREFIX, x, COMP_PREFIX) \
    StoreIndex12GrayFrom1ByteGray(pRas, PREFIX, x, COMP_PREFIX ## G)

#define Index12GrayIsPremultiplied      0

#endif /* Index12Gray_h_Included */
