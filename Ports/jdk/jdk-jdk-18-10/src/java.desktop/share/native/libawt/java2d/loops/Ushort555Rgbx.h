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

#ifndef Ushort555Rgbx_h_Included
#define Ushort555Rgbx_h_Included

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "Ushort555Rgbx".
 */

typedef jushort Ushort555RgbxPixelType;
typedef jushort Ushort555RgbxDataType;

#define Ushort555RgbxIsOpaque 1

#define Ushort555RgbxPixelStride        2

#define DeclareUshort555RgbxLoadVars(PREFIX)
#define DeclareUshort555RgbxStoreVars(PREFIX)
#define SetUshort555RgbxStoreVarsYPos(PREFIX, pRasInfo, y)
#define SetUshort555RgbxStoreVarsXPos(PREFIX, pRasInfo, x)
#define InitUshort555RgbxLoadVars(PREFIX, pRasInfo)
#define InitUshort555RgbxStoreVarsY(PREFIX, pRasInfo)
#define InitUshort555RgbxStoreVarsX(PREFIX, pRasInfo)
#define NextUshort555RgbxStoreVarsX(PREFIX)
#define NextUshort555RgbxStoreVarsY(PREFIX)
#define DeclareUshort555RgbxPixelData(PREFIX)
#define ExtractUshort555RgbxPixelData(PIXEL, PREFIX)

#define Ushort555RgbxXparLutEntry               -1
#define Ushort555RgbxIsXparLutEntry(pix)        (pix < 0)
#define StoreUshort555RgbxNonXparFromArgb       StoreUshort555RgbxFrom1IntArgb


#define IntArgbToUshort555Rgbx(rgb) \
    (Ushort555RgbxPixelType)((((rgb) >> (16 + 3 - 11)) & 0xf800) | \
                             (((rgb) >> ( 8 + 3 -  6)) & 0x07c0) | \
                             (((rgb) >> ( 0 + 3 -  1)) & 0x003e))

#define Ushort555RgbxPixelFromArgb(pixel, rgb, pRasInfo) \
    (pixel) = IntArgbToUshort555Rgbx(rgb)

#define StoreUshort555RgbxPixel(pRas, x, pixel) \
    ((pRas)[x] = (jushort) (pixel))

#define StoreUshort555RgbxPixelData(pPix, x, pixel, PREFIX) \
    StoreUshort555RgbxPixel(pPix, x, pixel)


#define LoadUshort555RgbxTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
    do { \
        jushort pixel = (pRas)[x]; \
        (r) = ((pixel) >> 11) & 0x1f; \
        (r) = ((r) << 3) | ((r) >> 2); \
        (g) = ((pixel) >>  6) & 0x1f; \
        (g) = ((g) << 3) | ((g) >> 2); \
        (b) = ((pixel) >>  1) & 0x1f; \
        (b) = ((b) << 3) | ((b) >> 2); \
    } while (0)

#define LoadUshort555RgbxTo4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    do { \
        LoadUshort555RgbxTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
        (a) = 0xff; \
    } while (0)

#define StoreUshort555RgbxFrom1IntArgb(pRas, PREFIX, x, rgb) \
    (pRas)[x] = IntArgbToUshort555Rgbx(rgb)

#define StoreUshort555RgbxFrom1IntRgb(pRas, PREFIX, x, rgb) \
    StoreUshort555RgbxFrom1IntArgb(pRas, PREFIX, x, rgb)

#define StoreUshort555RgbxFrom3ByteRgb(pRas, PREFIX, x, r, g, b) \
    (pRas)[x] = (jushort) ((((r) >> 3) << 11) | \
                           (((g) >> 3) <<  6) | \
                           (((b) >> 3) <<  1))

#define StoreUshort555RgbxFrom4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    StoreUshort555RgbxFrom3ByteRgb(pRas, PREFIX, x, r, g, b)

#endif /* Ushort555Rgbx_h_Included */
