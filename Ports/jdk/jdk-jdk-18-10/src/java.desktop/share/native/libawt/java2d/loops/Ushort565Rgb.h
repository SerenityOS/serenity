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

#ifndef Ushort565Rgb_h_Included
#define Ushort565Rgb_h_Included

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "Ushort565Rgb".
 */

typedef jushort Ushort565RgbPixelType;
typedef jushort Ushort565RgbDataType;

#define Ushort565RgbIsOpaque 1

#define Ushort565RgbPixelStride         2

#define DeclareUshort565RgbLoadVars(PREFIX)
#define DeclareUshort565RgbStoreVars(PREFIX)
#define SetUshort565RgbStoreVarsYPos(PREFIX, pRasInfo, y)
#define SetUshort565RgbStoreVarsXPos(PREFIX, pRasInfo, x)
#define InitUshort565RgbLoadVars(PREFIX, pRasInfo)
#define InitUshort565RgbStoreVarsY(PREFIX, pRasInfo)
#define InitUshort565RgbStoreVarsX(PREFIX, pRasInfo)
#define NextUshort565RgbStoreVarsX(PREFIX)
#define NextUshort565RgbStoreVarsY(PREFIX)
#define DeclareUshort565RgbPixelData(PREFIX)
#define ExtractUshort565RgbPixelData(PIXEL, PREFIX)

#define Ushort565RgbXparLutEntry                -1
#define Ushort565RgbIsXparLutEntry(pix)         (pix < 0)
#define StoreUshort565RgbNonXparFromArgb        StoreUshort565RgbFrom1IntArgb


#define ComposeUshort565RgbFrom3ByteRgb(r, g, b) \
    (Ushort565RgbPixelType)((((r) >> 3) << 11) | \
                            (((g) >> 2) <<  5) | \
                            (((b) >> 3) <<  0))

#define IntArgbToUshort565Rgb(rgb) \
    (Ushort565RgbPixelType)((((rgb) >> (16 + 3 - 11)) & 0xf800) | \
                            (((rgb) >> ( 8 + 2 -  5)) & 0x07e0) | \
                            (((rgb) >> ( 0 + 3 -  0)) & 0x001f))

#define Ushort565RgbPixelFromArgb(pixel, rgb, pRasInfo) \
    (pixel) = IntArgbToUshort565Rgb(rgb)

#define StoreUshort565RgbPixel(pRas, x, pixel) \
    ((pRas)[x] = (jushort) (pixel))

#define StoreUshort565RgbPixelData(pPix, x, pixel, PREFIX) \
    StoreUshort565RgbPixel(pPix, x, pixel)


#define LoadUshort565RgbTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
    do { \
        jushort pixel = (pRas)[x]; \
        (r) = ((pixel) >> 11) & 0x1f; \
        (r) = ((r) << 3) | ((r) >> 2); \
        (g) = ((pixel) >>  5) & 0x3f; \
        (g) = ((g) << 2) | ((g) >> 4); \
        (b) = ((pixel) >>  0) & 0x1f; \
        (b) = ((b) << 3) | ((b) >> 2); \
    } while (0)

#define LoadUshort565RgbTo4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    do { \
        LoadUshort565RgbTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
        (a) = 0xff; \
    } while (0)

#define StoreUshort565RgbFrom1IntArgb(pRas, PREFIX, x, rgb) \
    (pRas)[x] = IntArgbToUshort565Rgb(rgb)

#define StoreUshort565RgbFrom1IntRgb(pRas, PREFIX, x, rgb) \
    StoreUshort565RgbFrom1IntArgb(pRas, PREFIX, x, rgb)

#define StoreUshort565RgbFrom3ByteRgb(pRas, PREFIX, x, r, g, b) \
    (pRas)[x] = (jushort) ComposeUshort565RgbFrom3ByteRgb(r, g, b)

#define StoreUshort565RgbFrom4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    StoreUshort565RgbFrom3ByteRgb(pRas, PREFIX, x, r, g, b)


#define DeclareUshort565RgbAlphaLoadData(PREFIX)
#define InitUshort565RgbAlphaLoadData(PREFIX, pRasInfo)

#define LoadAlphaFromUshort565RgbFor4ByteArgb(pRas, PREFIX, COMP_PREFIX) \
    COMP_PREFIX ## A = 0xff

#define Postload4ByteArgbFromUshort565Rgb(pRas, PREFIX, COMP_PREFIX) \
    LoadUshort565RgbTo3ByteRgb(pRas, PREFIX, 0, COMP_PREFIX ## R, \
                               COMP_PREFIX ## G, COMP_PREFIX ## B)


#define Ushort565RgbIsPremultiplied     0

#define DeclareUshort565RgbBlendFillVars(PREFIX) \
    jushort PREFIX;

#define ClearUshort565RgbBlendFillVars(PREFIX, argb) \
    PREFIX = 0

#define InitUshort565RgbBlendFillVarsNonPre(PREFIX, argb, COMP_PREFIX) \
    PREFIX = (jushort) ComposeUshort565RgbFrom3ByteRgb(COMP_PREFIX ## R, \
                                                       COMP_PREFIX ## G, \
                                                       COMP_PREFIX ## B)

#define InitUshort565RgbBlendFillVarsPre(PREFIX, argb, COMP_PREFIX)

#define StoreUshort565RgbBlendFill(pRas, PREFIX, x, argb, COMP_PREFIX) \
    (pRas)[x] = PREFIX

#define StoreUshort565RgbFrom4ByteArgbComps(pRas, PREFIX, x, COMP_PREFIX) \
    StoreUshort565RgbFrom4ByteArgb(pRas, PREFIX, x, \
                                   COMP_PREFIX ## A, COMP_PREFIX ## R, \
                                   COMP_PREFIX ## G, COMP_PREFIX ## B)

#endif /* Ushort565Rgb_h_Included */
