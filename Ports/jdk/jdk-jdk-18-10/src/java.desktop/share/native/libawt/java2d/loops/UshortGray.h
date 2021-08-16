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

#ifndef UshortGray_h_Included
#define UshortGray_h_Included

#include "IntDcm.h"

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "UshortGray".
 */

typedef jushort UshortGrayPixelType;
typedef jushort UshortGrayDataType;

#define UshortGrayIsOpaque 1

#define UshortGrayPixelStride           2
#define UshortGrayBitsPerPixel         16

#define DeclareUshortGrayLoadVars(PREFIX)
#define DeclareUshortGrayStoreVars(PREFIX)
#define SetUshortGrayStoreVarsYPos(PREFIX, pRasInfo, y)
#define SetUshortGrayStoreVarsXPos(PREFIX, pRasInfo, x)
#define InitUshortGrayLoadVars(PREFIX, pRasInfo)
#define InitUshortGrayStoreVarsY(PREFIX, pRasInfo)
#define InitUshortGrayStoreVarsX(PREFIX, pRasInfo)
#define NextUshortGrayStoreVarsX(PREFIX)
#define NextUshortGrayStoreVarsY(PREFIX)
#define DeclareUshortGrayPixelData(PREFIX)
#define ExtractUshortGrayPixelData(PIXEL, PREFIX)

#define UshortGrayXparLutEntry                  -1
#define UshortGrayIsXparLutEntry(pix)           (pix < 0)
#define StoreUshortGrayNonXparFromArgb          StoreUshortGrayFrom1IntArgb


/*
 * Note: The following (original) equation was incorrect:
 *   gray = (((19595*r) + (38470*g) + (7471*b) + 32768) / 65536);
 *
 * The new component coefficients were derived from the following equation:
 *   k*rf*255 + k*gf*255 + k*bf*255 = 2^24 - 1
 *
 * The new calculated coefficients are:
 *   rf = 19672
 *   gf = 38620
 *   bf = 7500
 *
 * Thus the new equation would be:
 *   gray = (((19672*r) + (38620*g) + (7500*b) + 128) / 255)
 * but it has been tweaked so the faster "divide by 256" can be performed and
 * the "add 128" can be removed.  Therefore, the resultant formula is optimal:
 *   gray = (((19672*r) + (38621*g) + (7500*b)) / 256)
 */
#define ComposeUshortGrayFrom3ByteRgb(r, g, b) \
    (UshortGrayPixelType)(((19672*(r)) + (38621*(g)) + (7500*(b))) / 256)

#define UshortGrayPixelFromArgb(pixel, rgb, pRasInfo) \
    do { \
        int r, g, b; \
        ExtractIntDcmComponentsX123(rgb, r, g, b); \
        (pixel) = ComposeUshortGrayFrom3ByteRgb(r, g, b); \
    } while (0)

#define StoreUshortGrayPixel(pRas, x, pixel) \
    ((pRas)[x] = (jushort) (pixel))

#define StoreUshortGrayPixelData(pPix, x, pixel, PREFIX) \
    StoreUshortGrayPixel(pPix, x, pixel)


#define LoadUshortGrayTo1IntRgb(pRas, PREFIX, x, rgb) \
    do { \
        int gray = (pRas)[x] >> 8; \
        (rgb) = (((gray << 8) | gray) << 8) | gray; \
    } while (0)

#define LoadUshortGrayTo1IntArgb(pRas, PREFIX, x, argb) \
    do { \
        int gray = (pRas)[x] >> 8; \
        (argb) = (((((0xff << 8) | gray) << 8) | gray) << 8) | gray; \
    } while (0)

#define LoadUshortGrayTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
    ((r) = (g) = (b) = ((pRas)[x] >> 8))

#define LoadUshortGrayTo4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    do { \
        LoadUshortGrayTo3ByteRgb(pRas, PREFIX, x, r, g, b); \
        (a) = 0xff; \
    } while (0)

#define LoadUshortGrayTo1ByteGray(pRas, PREFIX, x, gray) \
    (gray) = ((pRas)[x] >> 8)

#define LoadUshortGrayTo1ShortGray(pRas, PREFIX, x, gray) \
    (gray) = (pRas)[x]

#define StoreUshortGrayFrom1IntRgb(pRas, PREFIX, x, rgb) \
    do { \
        int r, g, b; \
        ExtractIntDcmComponentsX123(rgb, r, g, b); \
        StoreUshortGrayFrom3ByteRgb(pRas, PREFIX, x, r, g, b); \
    } while (0)

#define StoreUshortGrayFrom1IntArgb(pRas, PREFIX, x, argb) \
    StoreUshortGrayFrom1IntRgb(pRas, PREFIX, x, argb)

#define StoreUshortGrayFrom3ByteRgb(pRas, PREFIX, x, r, g, b) \
    (pRas)[x] = ComposeUshortGrayFrom3ByteRgb(r, g, b)

#define StoreUshortGrayFrom4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    StoreUshortGrayFrom3ByteRgb(pRas, PREFIX, x, r, g, b)

#define StoreUshortGrayFrom1ByteGray(pRas, PREFIX, x, gray) \
    (pRas)[x] = (jushort) (((gray) << 8) + (gray))

#define StoreUshortGrayFrom1ShortGray(pRas, PREFIX, x, gray) \
    StoreUshortGrayPixel(pRas, x, gray)


#define DeclareUshortGrayAlphaLoadData(PREFIX)
#define InitUshortGrayAlphaLoadData(PREFIX, pRasInfo)

#define LoadAlphaFromUshortGrayFor1ShortGray(pRas, PREFIX, COMP_PREFIX) \
    COMP_PREFIX ## A = 0xffff

#define Postload1ShortGrayFromUshortGray(pRas, PREFIX, COMP_PREFIX) \
    COMP_PREFIX ## G = (pRas)[0]


#define UshortGrayIsPremultiplied       0

#define DeclareUshortGrayBlendFillVars(PREFIX) \
    jushort PREFIX;

#define ClearUshortGrayBlendFillVars(PREFIX, argb) \
    PREFIX = 0

#define InitUshortGrayBlendFillVarsNonPre(PREFIX, argb, COMP_PREFIX) \
    PREFIX = (jushort) COMP_PREFIX ## G

#define InitUshortGrayBlendFillVarsPre(PREFIX, argb, COMP_PREFIX)

#define StoreUshortGrayBlendFill(pRas, PREFIX, x, argb, COMP_PREFIX) \
    (pRas)[x] = PREFIX

#define StoreUshortGrayFrom1ShortGrayComps(pRas, PREFIX, x, COMP_PREFIX) \
    StoreUshortGrayPixel(pRas, x, COMP_PREFIX ## G)

#endif /* UshortGray_h_Included */
