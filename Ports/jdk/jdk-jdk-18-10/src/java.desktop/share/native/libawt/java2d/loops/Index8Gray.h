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

#ifndef Index8Gray_h_Included
#define Index8Gray_h_Included

#include "IntDcm.h"
#include "ByteGray.h"

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "Index8Gray".
 */

typedef jubyte  Index8GrayPixelType;
typedef jubyte  Index8GrayDataType;

#define Index8GrayIsOpaque 1

#define Index8GrayPixelStride           1
#define Index8GrayBitsPerPixel          8

#define DeclareIndex8GrayLoadVars(PREFIX) \
    jint *PREFIX ## Lut;

#define DeclareIndex8GrayStoreVars(PREFIX) \
    jint *PREFIX ## InvGrayLut;

#define SetIndex8GrayStoreVarsYPos(PREFIX, pRasInfo, LOC)
#define SetIndex8GrayStoreVarsXPos(PREFIX, pRasInfo, LOC)
#define InitIndex8GrayLoadVars(PREFIX, pRasInfo) \
    PREFIX ## Lut = (pRasInfo)->lutBase

#define InitIndex8GrayStoreVarsY(PREFIX, pRasInfo) \
    PREFIX ## InvGrayLut = (pRasInfo)->invGrayTable;

#define InitIndex8GrayStoreVarsX(PREFIX, pRasInfo)
#define NextIndex8GrayStoreVarsX(PREFIX)
#define NextIndex8GrayStoreVarsY(PREFIX)

#define Index8GrayXparLutEntry                  -1
#define Index8GrayIsXparLutEntry(pix)           (pix < 0)
#define StoreIndex8GrayNonXparFromArgb          StoreIndex8GrayFrom1IntArgb

#define StoreIndex8GrayPixel(pRas, x, pixel) \
    ((pRas)[x] = (jubyte) (pixel))

#define DeclareIndex8GrayPixelData(PREFIX)

#define ExtractIndex8GrayPixelData(PIXEL, PREFIX)

#define StoreIndex8GrayPixelData(pPix, x, pixel, PREFIX) \
    ((pPix)[x] = (jubyte)(pixel))

#define Index8GrayPixelFromArgb(pixel, rgb, pRasInfo) \
    do { \
        jint r, g, b, gray; \
        ExtractIntDcmComponentsX123(rgb, r, g, b); \
        gray = ComposeByteGrayFrom3ByteRgb(r, g, b); \
        (pixel) = (pRasInfo)->invGrayTable[gray]; \
    } while (0)

#define LoadIndex8GrayTo1IntRgb(pRas, PREFIX, x, rgb) \
    (rgb) = PREFIX ## Lut[pRas[x]]

#define LoadIndex8GrayTo1IntArgb(pRas, PREFIX, x, argb) \
    (argb) = PREFIX ## Lut[pRas[x]]

#define LoadIndex8GrayTo1ByteGray(pRas, PREFIX, x, gray) \
    (gray) = (jubyte)PREFIX ## Lut[pRas[x]]

#define LoadIndex8GrayTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
    r = g = b = (jubyte)PREFIX ## Lut[pRas[x]]

#define LoadIndex8GrayTo4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    do { \
        a = 0xff; \
        LoadIndex8GrayTo3ByteRgb(pRas, PREFIX, x, r, g, b); \
    } while (0)

#define StoreIndex8GrayFrom1IntRgb(pRas, PREFIX, x, rgb) \
    do { \
        int r, g, b; \
        ExtractIntDcmComponentsX123(rgb, r, g, b); \
        StoreIndex8GrayFrom3ByteRgb(pRas, PREFIX, x, r, g, b); \
    } while (0)

#define StoreIndex8GrayFrom1IntArgb(pRas, PREFIX, x, argb) \
    StoreIndex8GrayFrom1IntRgb(pRas, PREFIX, x, argb)

#define StoreIndex8GrayFrom3ByteRgb(pRas, PREFIX, x, r, g, b) \
    do { \
        int gray = ComposeByteGrayFrom3ByteRgb(r, g, b); \
        (pRas)[x] = (jubyte) (PREFIX ## InvGrayLut[gray]); \
    } while (0)

#define StoreIndex8GrayFrom4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    StoreIndex8GrayFrom3ByteRgb(pRas, PREFIX, x, r, g, b)

#define StoreIndex8GrayFrom1ByteGray(pRas, PREFIX, x, gray) \
    (pRas)[x] = (jubyte) (PREFIX ## InvGrayLut[gray]);

#define CopyIndex8GrayToIntArgbPre(pRGB, i, PREFIX, pRow, x) \
    (pRGB)[i] = PREFIX ## Lut[pRow[x]]


#define DeclareIndex8GrayAlphaLoadData(PREFIX) \
    jint *PREFIX ## Lut;

#define InitIndex8GrayAlphaLoadData(PREFIX, pRasInfo) \
    PREFIX ## Lut = (pRasInfo)->lutBase

#define LoadAlphaFromIndex8GrayFor1ByteGray(pRas, PREFIX, COMP_PREFIX) \
    COMP_PREFIX ## A = 0xff

#define Postload1ByteGrayFromIndex8Gray(pRas, PREFIX, COMP_PREFIX) \
    COMP_PREFIX ## G = (jubyte)PREFIX ## Lut[(pRas)[0]]

#define StoreIndex8GrayFrom1ByteGrayComps(pRas, PREFIX, x, COMP_PREFIX) \
    StoreIndex8GrayFrom1ByteGray(pRas, PREFIX, x, COMP_PREFIX ## G)

#define Index8GrayIsPremultiplied       0

#endif /* Index8Gray_h_Included */
