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

#ifndef ThreeByteBgr_h_Included
#define ThreeByteBgr_h_Included

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "ThreeByteBgr".
 */

typedef jint    ThreeByteBgrPixelType;
typedef jubyte  ThreeByteBgrDataType;

#define ThreeByteBgrIsOpaque 1

#define ThreeByteBgrPixelStride         3

#define DeclareThreeByteBgrLoadVars(PREFIX)
#define DeclareThreeByteBgrStoreVars(PREFIX)
#define SetThreeByteBgrStoreVarsYPos(PREFIX, pRasInfo, y)
#define SetThreeByteBgrStoreVarsXPos(PREFIX, pRasInfo, x)
#define InitThreeByteBgrLoadVars(PREFIX, pRasInfo)
#define InitThreeByteBgrStoreVarsY(PREFIX, pRasInfo)
#define InitThreeByteBgrStoreVarsX(PREFIX, pRasInfo)
#define NextThreeByteBgrStoreVarsX(PREFIX)
#define NextThreeByteBgrStoreVarsY(PREFIX)


#define ThreeByteBgrPixelFromArgb(pixel, rgb, pRasInfo) \
    (pixel) = (rgb)

#define StoreThreeByteBgrPixel(pRas, x, pixel) \
    do { \
        (pRas)[3*(x)+0] = (jubyte) ((pixel) >> 0); \
        (pRas)[3*(x)+1] = (jubyte) ((pixel) >> 8); \
        (pRas)[3*(x)+2] = (jubyte) ((pixel) >> 16); \
    } while (0)

#define DeclareThreeByteBgrPixelData(PREFIX) \
    jubyte PREFIX ## 0, PREFIX ## 1, PREFIX ## 2;

#define ExtractThreeByteBgrPixelData(PIXEL, PREFIX) \
    do { \
        PREFIX ## 0 = (jubyte) (PIXEL); \
        PREFIX ## 1 = (jubyte) (PIXEL >> 8); \
        PREFIX ## 2 = (jubyte) (PIXEL >> 16); \
    } while (0)

#define StoreThreeByteBgrPixelData(pPix, x, pixel, PREFIX) \
    do { \
        pPix[3*x+0] = PREFIX ## 0; \
        pPix[3*x+1] = PREFIX ## 1; \
        pPix[3*x+2] = PREFIX ## 2; \
    } while (0)


#define LoadThreeByteBgrTo1IntRgb(pRas, PREFIX, x, rgb) \
    (rgb) = (((pRas)[3*(x)+0] << 0) | \
             ((pRas)[3*(x)+1] << 8) | \
             ((pRas)[3*(x)+2] << 16))

#define LoadThreeByteBgrTo1IntArgb(pRas, PREFIX, x, argb) \
    (argb) = (((pRas)[3*(x)+0] << 0) | \
              ((pRas)[3*(x)+1] << 8) | \
              ((pRas)[3*(x)+2] << 16) | \
              0xff000000)

#define LoadThreeByteBgrTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
    do { \
        (b) = (pRas)[3*(x)+0]; \
        (g) = (pRas)[3*(x)+1]; \
        (r) = (pRas)[3*(x)+2]; \
    } while (0)

#define LoadThreeByteBgrTo4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    do { \
        LoadThreeByteBgrTo3ByteRgb(pRas, PREFIX, x, r, g, b); \
        (a) = 0xff; \
    } while (0)

#define StoreThreeByteBgrFrom1IntRgb(pRas, PREFIX, x, rgb) \
    do { \
        (pRas)[3*(x)+0] = (jubyte) ((rgb) >> 0); \
        (pRas)[3*(x)+1] = (jubyte) ((rgb) >> 8); \
        (pRas)[3*(x)+2] = (jubyte) ((rgb) >> 16); \
    } while (0)

#define StoreThreeByteBgrFrom1IntArgb(pRas, PREFIX, x, argb) \
    StoreThreeByteBgrFrom1IntRgb(pRas, PREFIX, x, argb)

#define StoreThreeByteBgrFrom3ByteRgb(pRas, PREFIX, x, r, g, b) \
    do { \
        (pRas)[3*(x)+0] = (jubyte) (b); \
        (pRas)[3*(x)+1] = (jubyte) (g); \
        (pRas)[3*(x)+2] = (jubyte) (r); \
    } while (0)

#define StoreThreeByteBgrFrom4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    StoreThreeByteBgrFrom3ByteRgb(pRas, PREFIX, x, r, g, b)

#define CopyThreeByteBgrToIntArgbPre(pRGB, i, PREFIX, pRow, x) \
    LoadThreeByteBgrTo1IntArgb(pRow, PREFIX, x, (pRGB)[i])


#define DeclareThreeByteBgrAlphaLoadData(PREFIX)
#define InitThreeByteBgrAlphaLoadData(PREFIX, pRasInfo)

#define LoadAlphaFromThreeByteBgrFor4ByteArgb(pRas, PREFIX, COMP_PREFIX) \
    COMP_PREFIX ## A = 0xff

#define Postload4ByteArgbFromThreeByteBgr(pRas, PREFIX, COMP_PREFIX) \
    LoadThreeByteBgrTo3ByteRgb(pRas, PREFIX, 0, COMP_PREFIX ## R, \
                               COMP_PREFIX ## G, COMP_PREFIX ## B)


#define ThreeByteBgrIsPremultiplied     0

#define DeclareThreeByteBgrBlendFillVars(PREFIX) \
    jubyte PREFIX ## 0, PREFIX ## 1, PREFIX ## 2;

#define ClearThreeByteBgrBlendFillVars(PREFIX, argb) \
    (PREFIX ## 0 = PREFIX ## 1 = PREFIX ## 2 = 0)

#define InitThreeByteBgrBlendFillVarsNonPre(PREFIX, argb, COMP_PREFIX) \
    do { \
        PREFIX ## 0 = (jubyte) COMP_PREFIX ## B; \
        PREFIX ## 1 = (jubyte) COMP_PREFIX ## G; \
        PREFIX ## 2 = (jubyte) COMP_PREFIX ## R; \
    } while (0)

#define InitThreeByteBgrBlendFillVarsPre(PREFIX, argb, COMP_PREFIX)

#define StoreThreeByteBgrBlendFill(pRas, PREFIX, x, argb, COMP_PREFIX) \
    do { \
        pRas[3*x+0] = PREFIX ## 0; \
        pRas[3*x+1] = PREFIX ## 1; \
        pRas[3*x+2] = PREFIX ## 2; \
    } while (0)

#define StoreThreeByteBgrFrom4ByteArgbComps(pRas, PREFIX, x, COMP_PREFIX) \
    StoreThreeByteBgrFrom4ByteArgb(pRas, PREFIX, x, \
                                   COMP_PREFIX ## A, COMP_PREFIX ## R, \
                                   COMP_PREFIX ## G, COMP_PREFIX ## B)

#endif /* ThreeByteBgr_h_Included */
