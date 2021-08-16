/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef Ushort4444Argb_h_Included
#define Ushort4444Argb_h_Included

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "Ushort4444Argb".
 */

typedef jushort Ushort4444ArgbPixelType;
typedef jushort Ushort4444ArgbDataType;

#define Ushort4444ArgbIsOpaque 0

#define Ushort4444ArgbPixelStride               2

#define DeclareUshort4444ArgbLoadVars(PREFIX)
#define DeclareUshort4444ArgbStoreVars(PREFIX)
#define SetUshort4444ArgbStoreVarsYPos(PREFIX, pRasInfo, y)
#define SetUshort4444ArgbStoreVarsXPos(PREFIX, pRasInfo, x)
#define InitUshort4444ArgbLoadVars(PREFIX, pRasInfo)
#define InitUshort4444ArgbStoreVarsY(PREFIX, pRasInfo)
#define InitUshort4444ArgbStoreVarsX(PREFIX, pRasInfo)
#define NextUshort4444ArgbStoreVarsX(PREFIX)
#define NextUshort4444ArgbStoreVarsY(PREFIX)
#define DeclareUshort4444ArgbPixelData(PREFIX)
#define ExtractUshort4444ArgbPixelData(PIXEL, PREFIX)

#define Ushort4444ArgbXparLutEntry              -1
#define Ushort4444ArgbIsXparLutEntry(pix)               (pix < 0)
#define StoreUshort4444ArgbNonXparFromArgb      StoreUshort4444ArgbFrom1IntArgb


#define ComposeUshort4444ArgbFrom3ByteRgb(r, g, b)

#define IntArgbToUshort4444Argb(rgb) \
    (Ushort4444ArgbPixelType)((((rgb) << 8) & 0xf000) | \
                              (((rgb) << 4) & 0x0f00) | \
                              (((rgb) << 0) & 0x00f0) | \
                              (((rgb) >> 4) & 0x000f))

#define Ushort4444ArgbPixelFromArgb(pixel, rgb, pRasInfo) \
    (pixel) = IntArgbToUshort4444Argb(rgb)

#define StoreUshort4444ArgbPixel(pRas, x, pixel) \
    ((pRas)[x] = (jushort) (pixel))

#define StoreUshort4444ArgbPixelData(pPix, x, pixel, PREFIX)

#define LoadUshort4444ArgbTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
    do { \
        jushort pixel = (pRas)[x]; \
        (r) = ((pixel) >> 8) & 0xf; \
        (r) = ((r) << 4) | (r); \
        (g) = ((pixel) >>  4) & 0xf; \
        (g) = ((g) << 4) | (g); \
        (b) = ((pixel) >>  0) & 0xf; \
        (b) = ((b) << 4) | (b); \
    } while (0)

#define LoadUshort4444ArgbTo4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    do { \
        jushort pixel = (pRas)[x]; \
        LoadUshort4444ArgbTo3ByteRgb(pRas, PREFIX, x, r, g, b); \
        (a) = ((pixel) >>  12) & 0xf; \
        (a) = ((a) << 4) | (a); \
    } while (0)

#define LoadUshort4444ArgbTo1IntArgb(pRas, PREFIX, x, argb) \
    do { \
        jint a, r, g, b; \
        LoadUshort4444ArgbTo4ByteArgb(pRas, PREFIX, x, a, r, g, b); \
        (argb) = (a << 24) | (r << 16) | (g << 8) | (b << 0); \
    } while (0)

#define LoadUshort4444ArgbTo1IntRgb(pRas, PREFIX, x, rgb) \
    do { \
        jint r, g, b; \
        LoadUshort4444ArgbTo3ByteRgb(pRas, PREFIX, x, r, g, b); \
        (rgb) = 0xff000000 | (r << 16) | (g << 8) | (b << 0); \
    } while (0)

#define StoreUshort4444ArgbFrom1IntArgb(pRas, PREFIX, x, rgb)
#define StoreUshort4444ArgbFrom1IntRgb(pRas, PREFIX, x, rgb)
#define StoreUshort4444ArgbFrom3ByteRgb(pRas, PREFIX, x, r, g, b)

#define StoreUshort4444ArgbFrom4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    do { \
        (pRas)[x] = (jushort)((((a) <<  8) & 0xf000) | \
                              (((r) <<  4) & 0x0f00) | \
                              (((g) <<  0) & 0x00f0) | \
                              (((b) >>  4) & 0x000f)); \
    } while (0)


#define DeclareUshort4444ArgbAlphaLoadData(PREFIX) \
    jint PREFIX;

#define InitUshort4444ArgbAlphaLoadData(PREFIX, pRasInfo) \
    PREFIX = 0

#define LoadAlphaFromUshort4444ArgbFor4ByteArgb(pRas, PREFIX, COMP_PREFIX) \
    do { \
        PREFIX = (pRas)[0]; \
        COMP_PREFIX ## A = (((jushort) PREFIX) >> 12) & 0xf; \
        COMP_PREFIX ## A = ((COMP_PREFIX ## A) << 4) | (COMP_PREFIX ## A); \
    } while (0)

#define Postload4ByteArgbFromUshort4444Argb(pRas, PREFIX, COMP_PREFIX) \
    LoadUshort4444ArgbTo4ByteArgb(pRas, PREFIX, 0, COMP_PREFIX ## A, COMP_PREFIX ## R, \
                                  COMP_PREFIX ## G, COMP_PREFIX ## B)

#define Ushort4444ArgbIsPremultiplied   0

#define StoreUshort4444ArgbFrom4ByteArgbComps(pRas, PREFIX, x, COMP_PREFIX) \
    StoreUshort4444ArgbFrom4ByteArgb(pRas, PREFIX, x, \
                                     COMP_PREFIX ## A, COMP_PREFIX ## R, \
                                     COMP_PREFIX ## G, COMP_PREFIX ## B)

#endif /* Ushort4444Argb_h_Included */
