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

#ifndef IntBgr_h_Included
#define IntBgr_h_Included

#include "IntDcm.h"
#include "ByteGray.h"
#include "UshortGray.h"

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "IntBgr".
 */

typedef jint    IntBgrPixelType;
typedef jint    IntBgrDataType;

#define IntBgrIsOpaque 1

#define IntBgrPixelStride       4

#define DeclareIntBgrLoadVars(PREFIX)
#define DeclareIntBgrStoreVars(PREFIX)
#define InitIntBgrLoadVars(PREFIX, pRasInfo)
#define SetIntBgrStoreVarsYPos(PREFIX, pRasInfo, y)
#define SetIntBgrStoreVarsXPos(PREFIX, pRasInfo, x)
#define InitIntBgrStoreVarsY(PREFIX, pRasInfo)
#define InitIntBgrStoreVarsX(PREFIX, pRasInfo)
#define NextIntBgrStoreVarsX(PREFIX)
#define NextIntBgrStoreVarsY(PREFIX)

#define IntBgrXparLutEntry              -1
#define IntBgrIsXparLutEntry(pix)       (pix < 0)
#define StoreIntBgrNonXparFromArgb(pRas, PREFIX, x, argb) \
    (pRas)[x] = SwapIntDcmComponentsX123ToC321(argb)


#define IntBgrPixelFromArgb(pixel, rgb, pRasInfo) \
    (pixel) = SwapIntDcmComponentsX123ToX321(rgb)

#define StoreIntBgrPixel(pRas, x, pixel) \
    (pRas)[x] = (pixel)

#define DeclareIntBgrPixelData(PREFIX)

#define ExtractIntBgrPixelData(PIXEL, PREFIX)

#define StoreIntBgrPixelData(pPix, x, pixel, PREFIX) \
    StoreIntBgrPixel(pPix, x, pixel)


#define LoadIntBgrTo1IntRgb(pRas, PREFIX, x, rgb) \
    do { \
        jint pixel = (pRas)[x]; \
        (rgb) = SwapIntDcmComponentsX123ToX321(pixel); \
    } while (0)

#define LoadIntBgrTo1IntArgb(pRas, PREFIX, x, argb) \
    do { \
        jint pixel = (pRas)[x]; \
        (argb) = SwapIntDcmComponentsX123ToS321(pixel); \
    } while (0)

#define LoadIntBgrTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
    do { \
        jint pixel = (pRas)[x]; \
        ExtractIntDcmComponentsX123(pixel, b, g, r); \
    } while (0)

#define LoadIntBgrTo4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    do { \
        LoadIntBgrTo3ByteRgb(pRas, PREFIX, x, r, g, b); \
        (a) = 0xff; \
    } while (0)

#define StoreIntBgrFrom1IntRgb(pRas, PREFIX, x, rgb) \
    (pRas)[x] = SwapIntDcmComponentsX123ToX321(rgb)

#define StoreIntBgrFrom1IntArgb(pRas, PREFIX, x, argb) \
    StoreIntBgrFrom1IntRgb(pRas, PREFIX, x, argb)

#define StoreIntBgrFrom3ByteRgb(pRas, PREFIX, x, r, g, b) \
    (pRas)[x] = ComposeIntDcmComponentsX123(b, g, r)

#define StoreIntBgrFrom4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    StoreIntBgrFrom3ByteRgb(pRas, PREFIX, x, r, g, b)

#define CopyIntBgrToIntArgbPre(pRGB, i, PREFIX, pRow, x) \
    LoadIntBgrTo1IntArgb(pRow, PREFIX, x, (pRGB)[i])


#define DeclareIntBgrAlphaLoadData(PREFIX)
#define InitIntBgrAlphaLoadData(PREFIX, pRasInfo)

#define LoadAlphaFromIntBgrFor4ByteArgb(pRas, PREFIX, COMP_PREFIX) \
    COMP_PREFIX ## A = 0xff

#define Postload4ByteArgbFromIntBgr(pRas, PREFIX, COMP_PREFIX) \
    LoadIntBgrTo3ByteRgb(pRas, PREFIX, 0, COMP_PREFIX ## R, \
                         COMP_PREFIX ## G, COMP_PREFIX ## B)


#define IntBgrIsPremultiplied   0

#define DeclareIntBgrBlendFillVars(PREFIX) \
    jint PREFIX;

#define ClearIntBgrBlendFillVars(PREFIX, argb) \
    PREFIX = 0

#define InitIntBgrBlendFillVarsNonPre(PREFIX, argb, COMP_PREFIX) \
    PREFIX = ComposeIntDcmComponentsX123(COMP_PREFIX ## B, COMP_PREFIX ## G, \
                                         COMP_PREFIX ## R)

#define InitIntBgrBlendFillVarsPre(PREFIX, argb, COMP_PREFIX)

#define StoreIntBgrBlendFill(pRas, PREFIX, x, argb, COMP_PREFIX) \
    (pRas)[x] = PREFIX

#define StoreIntBgrFrom4ByteArgbComps(pRas, PREFIX, x, COMP_PREFIX) \
    StoreIntBgrFrom4ByteArgb(pRas, PREFIX, x, \
                             COMP_PREFIX ## A, COMP_PREFIX ## R, \
                             COMP_PREFIX ## G, COMP_PREFIX ## B)

#endif /* IntBgr_h_Included */
