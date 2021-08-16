/*
 * Copyright (c) 2000, 2008, Oracle and/or its affiliates. All rights reserved.
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

#ifndef IntRgb_h_Included
#define IntRgb_h_Included

#include "IntDcm.h"
#include "ByteGray.h"
#include "UshortGray.h"

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "IntRgb".
 */

typedef jint    IntRgbPixelType;
typedef jint    IntRgbDataType;

#define IntRgbIsOpaque 1

#define IntRgbPixelStride       4

#define DeclareIntRgbLoadVars(PREFIX)
#define DeclareIntRgbStoreVars(PREFIX)
#define InitIntRgbLoadVars(PREFIX, pRasInfo)
#define SetIntRgbStoreVarsYPos(PREFIX, pRasInfo, y)
#define SetIntRgbStoreVarsXPos(PREFIX, pRasInfo, x)
#define InitIntRgbStoreVarsY(PREFIX, pRasInfo)
#define InitIntRgbStoreVarsX(PREFIX, pRasInfo)
#define NextIntRgbStoreVarsX(PREFIX)
#define NextIntRgbStoreVarsY(PREFIX)

#define IntRgbPixelFromArgb(pixel, rgb, pRasInfo) \
    (pixel) = (rgb)

#define StoreIntRgbPixel(pRas, x, pixel) \
    (pRas)[x] = (pixel)

#define DeclareIntRgbPixelData(PREFIX)

#define ExtractIntRgbPixelData(PIXEL, PREFIX)

#define StoreIntRgbPixelData(pPix, x, pixel, PREFIX) \
    StoreIntRgbPixel(pPix, x, pixel)


#define LoadIntRgbTo1IntRgb(pRas, PREFIX, x, rgb) \
    (rgb) = (pRas)[x]

#define LoadIntRgbTo1IntArgb(pRas, PREFIX, x, argb) \
    (argb) = 0xff000000 | (pRas)[x]

#define LoadIntRgbTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
    do { \
        jint pixel = (pRas)[x]; \
        ExtractIntDcmComponentsX123(pixel, r, g, b); \
    } while (0)

#define LoadIntRgbTo4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    do { \
        LoadIntRgbTo3ByteRgb(pRas, PREFIX, x, r, g, b); \
        (a) = 0xff; \
    } while (0)

#define StoreIntRgbFrom1IntRgb(pRas, PREFIX, x, rgb) \
    (pRas)[x] = (rgb)

#define StoreIntRgbFrom1IntArgb(pRas, PREFIX, x, argb) \
    (pRas)[x] = (argb)

#define StoreIntRgbFrom3ByteRgb(pRas, PREFIX, x, r, g, b) \
    (pRas)[x] = ComposeIntDcmComponentsX123(r, g, b)

#define StoreIntRgbFrom4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    StoreIntRgbFrom3ByteRgb(pRas, PREFIX, x, r, g, b)

#define CopyIntRgbToIntArgbPre(pRGB, i, PREFIX, pRow, x) \
    LoadIntRgbTo1IntArgb(pRow, PREFIX, x, (pRGB)[i])

#define DeclareIntRgbAlphaLoadData(PREFIX)

#define InitIntRgbAlphaLoadData(PREFIX, pRasInfo)

#define LoadAlphaFromIntRgbFor4ByteArgb(pRas, PREFIX, COMP_PREFIX) \
    COMP_PREFIX ## A = 0xff

#define LoadAlphaFromIntRgbFor1ByteGray(pRas, PREFIX, COMP_PREFIX) \
    COMP_PREFIX ## A = 0xff

#define LoadAlphaFromIntRgbFor1ShortGray(pRas, PREFIX, COMP_PREFIX) \
    COMP_PREFIX ## A = 0xffff

#define Postload4ByteArgbFromIntRgb(pRas, PREFIX, COMP_PREFIX) \
    LoadIntRgbTo3ByteRgb(pRas, PREFIX, 0, COMP_PREFIX ## R, \
                         COMP_PREFIX ## G, COMP_PREFIX ## B)

#define Postload1ByteGrayFromIntRgb(pRas, PREFIX, COMP_PREFIX) \
    do { \
        int r, g, b; \
        ExtractIntDcmComponentsX123(pRas[0], r, g, b); \
        COMP_PREFIX ## G = ComposeByteGrayFrom3ByteRgb(r, g, b); \
    } while (0)

#define Postload1ShortGrayFromIntRgb(pRas, PREFIX, COMP_PREFIX) \
    do { \
        int r, g, b; \
        ExtractIntDcmComponentsX123(pRas[0], r, g, b); \
        COMP_PREFIX ## G = ComposeUshortGrayFrom3ByteRgb(r, g, b); \
    } while (0)


#define IntRgbIsPremultiplied   0

#define DeclareIntRgbBlendFillVars(PREFIX)

#define ClearIntRgbBlendFillVars(PREFIX, argb) \
    argb = 0

#define InitIntRgbBlendFillVarsNonPre(PREFIX, argb, COMP_PREFIX)

#define InitIntRgbBlendFillVarsPre(PREFIX, argb, COMP_PREFIX)

#define StoreIntRgbBlendFill(pRas, PREFIX, x, argb, COMP_PREFIX) \
    (pRas)[x] = (argb)

#define StoreIntRgbFrom4ByteArgbComps(pRas, PREFIX, x, COMP_PREFIX) \
    StoreIntRgbFrom4ByteArgb(pRas, PREFIX, x, \
                             COMP_PREFIX ## A, COMP_PREFIX ## R, \
                             COMP_PREFIX ## G, COMP_PREFIX ## B)

#endif /* IntRgb_h_Included */
