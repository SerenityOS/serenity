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

#ifndef Ushort555Rgb_h_Included
#define Ushort555Rgb_h_Included

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "Ushort555Rgb".
 */

typedef jushort Ushort555RgbPixelType;
typedef jushort Ushort555RgbDataType;

#define Ushort555RgbIsOpaque 1

#define Ushort555RgbPixelStride         2

#define DeclareUshort555RgbLoadVars(PREFIX)
#define DeclareUshort555RgbStoreVars(PREFIX)
#define SetUshort555RgbStoreVarsYPos(PREFIX, pRasInfo, y)
#define SetUshort555RgbStoreVarsXPos(PREFIX, pRasInfo, x)
#define InitUshort555RgbLoadVars(PREFIX, pRasInfo)
#define InitUshort555RgbStoreVarsY(PREFIX, pRasInfo)
#define InitUshort555RgbStoreVarsX(PREFIX, pRasInfo)
#define NextUshort555RgbStoreVarsX(PREFIX)
#define NextUshort555RgbStoreVarsY(PREFIX)
#define DeclareUshort555RgbPixelData(PREFIX)
#define ExtractUshort555RgbPixelData(PIXEL, PREFIX)

#define Ushort555RgbXparLutEntry                -1
#define Ushort555RgbIsXparLutEntry(pix)         (pix < 0)
#define StoreUshort555RgbNonXparFromArgb        StoreUshort555RgbFrom1IntArgb


#define ComposeUshort555RgbFrom3ByteRgb(r, g, b) \
    (Ushort555RgbPixelType)((((r) >> 3) << 10) | \
                            (((g) >> 3) <<  5) | \
                            (((b) >> 3) <<  0))

#define IntArgbToUshort555Rgb(rgb) \
    (Ushort555RgbPixelType)((((rgb) >> (16 + 3 - 10)) & 0x7c00) | \
                            (((rgb) >> ( 8 + 3 -  5)) & 0x03e0) | \
                            (((rgb) >> ( 0 + 3 -  0)) & 0x001f))

#define Ushort555RgbPixelFromArgb(pixel, rgb, pRasInfo) \
    (pixel) = IntArgbToUshort555Rgb(rgb)

#define StoreUshort555RgbPixel(pRas, x, pixel) \
    ((pRas)[x] = (jushort) (pixel))

#define StoreUshort555RgbPixelData(pPix, x, pixel, PREFIX) \
    StoreUshort555RgbPixel(pPix, x, pixel)


#define LoadUshort555RgbTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
    do { \
        jushort pixel = (pRas)[x]; \
        (r) = ((pixel) >> 10) & 0x1f; \
        (r) = ((r) << 3) | ((r) >> 2); \
        (g) = ((pixel) >>  5) & 0x1f; \
        (g) = ((g) << 3) | ((g) >> 2); \
        (b) = ((pixel) >>  0) & 0x1f; \
        (b) = ((b) << 3) | ((b) >> 2); \
    } while (0)

#define LoadUshort555RgbTo4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    do { \
        LoadUshort555RgbTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
        (a) = 0xff; \
    } while (0)

#define StoreUshort555RgbFrom1IntRgb(pRas, PREFIX, x, rgb) \
    StoreUshort555RgbFrom1IntArgb(pRas, PREFIX, x, rgb)

#define StoreUshort555RgbFrom1IntArgb(pRas, PREFIX, x, rgb) \
    (pRas)[x] = IntArgbToUshort555Rgb(rgb)

#define StoreUshort555RgbFrom3ByteRgb(pRas, PREFIX, x, r, g, b) \
    (pRas)[x] = (jushort) ComposeUshort555RgbFrom3ByteRgb(r, g, b)

#define StoreUshort555RgbFrom4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    StoreUshort555RgbFrom3ByteRgb(pRas, PREFIX, x, r, g, b)


#define DeclareUshort555RgbAlphaLoadData(PREFIX)
#define InitUshort555RgbAlphaLoadData(PREFIX, pRasInfo)

#define LoadAlphaFromUshort555RgbFor4ByteArgb(pRas, PREFIX, COMP_PREFIX) \
    COMP_PREFIX ## A = 0xff

#define Postload4ByteArgbFromUshort555Rgb(pRas, PREFIX, COMP_PREFIX) \
    LoadUshort555RgbTo3ByteRgb(pRas, PREFIX, 0, COMP_PREFIX ## R, \
                               COMP_PREFIX ## G, COMP_PREFIX ## B)


#define Ushort555RgbIsPremultiplied     0

#define DeclareUshort555RgbBlendFillVars(PREFIX) \
    jushort PREFIX;

#define ClearUshort555RgbBlendFillVars(PREFIX, argb) \
    PREFIX = 0

#define InitUshort555RgbBlendFillVarsNonPre(PREFIX, argb, COMP_PREFIX) \
    PREFIX = (jushort) ComposeUshort555RgbFrom3ByteRgb(COMP_PREFIX ## R, \
                                                       COMP_PREFIX ## G, \
                                                       COMP_PREFIX ## B)

#define InitUshort555RgbBlendFillVarsPre(PREFIX, argb, COMP_PREFIX)

#define StoreUshort555RgbBlendFill(pRas, PREFIX, x, argb, COMP_PREFIX) \
    (pRas)[x] = PREFIX

#define StoreUshort555RgbFrom4ByteArgbComps(pRas, PREFIX, x, COMP_PREFIX) \
    StoreUshort555RgbFrom4ByteArgb(pRas, PREFIX, x, \
                                   COMP_PREFIX ## A, COMP_PREFIX ## R, \
                                   COMP_PREFIX ## G, COMP_PREFIX ## B)

#endif /* Ushort555Rgb_h_Included */
