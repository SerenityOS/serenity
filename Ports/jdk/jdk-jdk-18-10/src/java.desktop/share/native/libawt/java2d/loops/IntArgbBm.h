/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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

#ifndef IntArgbBm_h_Included
#define IntArgbBm_h_Included

#include "IntDcm.h"
#include "ByteGray.h"
#include "UshortGray.h"

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "IntArgbBm".
 */

typedef jint    IntArgbBmPixelType;
typedef jint    IntArgbBmDataType;

#define IntArgbBmIsOpaque 0

#define IntArgbBmPixelStride    4

#define DeclareIntArgbBmLoadVars(PREFIX)
#define DeclareIntArgbBmStoreVars(PREFIX)
#define InitIntArgbBmLoadVars(PREFIX, pRasInfo)
#define SetIntArgbBmStoreVarsYPos(PREFIX, pRasInfo, y)
#define SetIntArgbBmStoreVarsXPos(PREFIX, pRasInfo, x)
#define InitIntArgbBmStoreVarsY(PREFIX, pRasInfo)
#define InitIntArgbBmStoreVarsX(PREFIX, pRasInfo)
#define NextIntArgbBmStoreVarsX(PREFIX)
#define NextIntArgbBmStoreVarsY(PREFIX)
#define DeclareIntArgbBmInitialLoadVars(pRasInfo, pRas, PREFIX, x)
#define InitialLoadIntArgbBm(pRas, PREFIX)
#define ShiftBitsIntArgbBm(PREFIX)
#define FinalStoreIntArgbBm(pRas, PREFIX)

#define IntArgbBmXparLutEntry           0
#define IntArgbBmIsXparLutEntry(pix)    (pix == 0)
#define StoreIntArgbBmNonXparFromArgb(pRas, PREFIX, x, argb) \
    StoreIntArgbBmFrom1IntArgb(pRas, PREFIX, x, argb)

#define DeclareIntArgbBmData(PREFIX) \
    jint PREFIX;

#define LoadIntArgbBmData(pRas, LOADPREFIX, x, DATAPREFIX) \
    (DATAPREFIX) = (pRas)[x]

#define IsIntArgbBmDataTransparent(DATAPREFIX) \
    (((DATAPREFIX) >> 24) == 0)

#define ConvertIntArgbBmDataTo1IntRgb(DATAPREFIX, rgb) \
    (rgb) = (DATAPREFIX)

#define IntArgbBmPixelFromArgb(pixel, rgb, pRasInfo) \
    (pixel) = ((rgb) | (((rgb) >> 31) << 24))

#define StoreIntArgbBmPixel(pRas, x, pixel) \
    (pRas)[x] = (pixel)

#define DeclareIntArgbBmPixelData(PREFIX)

#define ExtractIntArgbBmPixelData(PIXEL, PREFIX)

#define StoreIntArgbBmPixelData(pPix, x, pixel, PREFIX) \
    (pPix)[x] = (pixel)


#define LoadIntArgbBmTo1IntRgb(pRas, PREFIX, x, rgb) \
    (rgb) = (pRas)[x]

#define LoadIntArgbBmTo1IntArgb(pRas, PREFIX, x, argb) \
    do { \
        (argb) = (pRas)[x]; \
        (argb) = (((argb) << 7) >> 7); \
    } while (0)

#define LoadIntArgbBmTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
    do { \
        jint pixel = (pRas)[x]; \
        ExtractIntDcmComponentsX123(pixel, r, g, b); \
    } while (0)

#define LoadIntArgbBmTo4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    do { \
        jint pixel = (pRas)[x]; \
        pixel = ((pixel << 7) >> 7); \
        ExtractIntDcmComponents1234(pixel, a, r, g, b); \
    } while (0)

#define StoreIntArgbBmFrom1IntRgb(pRas, PREFIX, x, rgb) \
    (pRas)[x] = 0x01000000 | (rgb)

#define StoreIntArgbBmFrom1IntArgb(pRas, PREFIX, x, argb) \
    (pRas)[x] = ((argb) | (((argb) >> 31) << 24))

#define StoreIntArgbBmFrom3ByteRgb(pRas, PREFIX, x, r, g, b) \
    StoreIntArgbBmFrom4ByteArgb(pRas, PREFIX, x, 0x01, r, g, b)

#define StoreIntArgbBmFrom4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    (pRas)[x] = ComposeIntDcmComponents1234((a >> 7), r, g, b)

#define CopyIntArgbBmToIntArgbPre(pRGB, i, PREFIX, pRow, x) \
    do { \
        jint argb = (pRow)[x]; \
        argb = ((argb << 7) >> 7); /* Propagate alpha bit */ \
        argb &= (argb >> 24); /* Mask off colors if alpha=0 */ \
        (pRGB)[i] = argb; \
    } while (0)


#define DeclareIntArgbBmAlphaLoadData(PREFIX) \
    jint PREFIX;

#define InitIntArgbBmAlphaLoadData(PREFIX, pRasInfo) \
    PREFIX = 0

#define LoadAlphaFromIntArgbBmFor4ByteArgb(pRas, PREFIX, COMP_PREFIX) \
    do { \
        PREFIX = (pRas)[0]; \
        PREFIX = ((PREFIX << 7) >> 7); \
        COMP_PREFIX ## A = ((juint) PREFIX) >> 24; \
    } while (0)

#define LoadAlphaFromIntArgbBmFor1ByteGray(pRas, PREFIX, COMP_PREFIX) \
    LoadAlphaFromIntArgbBmFor4ByteArgb(pRas, PREFIX, COMP_PREFIX)

#define LoadAlphaFromIntArgbBmFor1ShortGray(pRas, PREFIX, COMP_PREFIX) \
    do { \
        LoadAlphaFromIntArgbBmFor4ByteArgb(pRas, PREFIX, COMP_PREFIX); \
        COMP_PREFIX ## A = (COMP_PREFIX ## A << 8) + COMP_PREFIX ## A; \
    } while (0)

#define Postload4ByteArgbFromIntArgbBm(pRas, PREFIX, COMP_PREFIX) \
    do { \
        COMP_PREFIX ## R = (PREFIX >> 16) & 0xff; \
        COMP_PREFIX ## G = (PREFIX >>  8) & 0xff; \
        COMP_PREFIX ## B = (PREFIX >>  0) & 0xff; \
    } while (0)

#define Postload1ByteGrayFromIntArgb(pRas, PREFIX, COMP_PREFIX) \
    do { \
        int r, g, b; \
        ExtractIntDcmComponentsX123(PREFIX, r, g, b); \
        COMP_PREFIX ## G = ComposeByteGrayFrom3ByteRgb(r, g, b); \
    } while (0)

#define Postload1ShortGrayFromIntArgb(pRas, PREFIX, COMP_PREFIX) \
    do { \
        int r, g, b; \
        ExtractIntDcmComponentsX123(PREFIX, r, g, b); \
        COMP_PREFIX ## G = ComposeUshortGrayFrom3ByteRgb(r, g, b); \
    } while (0)


#define IntArgbBmIsPremultiplied        0

#define StoreIntArgbBmFrom4ByteArgbComps(pRas, PREFIX, x, COMP_PREFIX) \
    StoreIntArgbBmFrom4ByteArgb(pRas, PREFIX, x, \
                                COMP_PREFIX ## A, COMP_PREFIX ## R, \
                                COMP_PREFIX ## G, COMP_PREFIX ## B)

/*
 * Extract ## STRATEGY ## CompsAndAlphaFromArgb(pixel, COMP_PREFIX)
 */
#define Extract3ByteRgbCompsAndAlphaFromArgb(pixel, COMP_PREFIX) \
    ExtractIntDcmComponents1234(pixel, COMP_PREFIX ## A, COMP_PREFIX ## R, \
                                COMP_PREFIX ## G, COMP_PREFIX ## B)

#define Extract4ByteArgbCompsAndAlphaFromArgb(pixel, COMP_PREFIX) \
    Extract3ByteRgbCompsAndAlphaFromArgb(pixel, COMP_PREFIX)

#define Extract1ByteGrayCompsAndAlphaFromArgb(pixel, COMP_PREFIX) \
    do { \
        int r, g, b; \
        ExtractIntDcmComponents1234(pixel, COMP_PREFIX ## A, r, g, b); \
        COMP_PREFIX ## G = ComposeByteGrayFrom3ByteRgb(r, g, b); \
    } while (0)

#define Extract1ShortGrayCompsAndAlphaFromArgb(pixel, COMP_PREFIX) \
    do { \
        int r, g, b; \
        ExtractIntDcmComponents1234(pixel, COMP_PREFIX ## A, r, g, b); \
        COMP_PREFIX ## G = ComposeUshortGrayFrom3ByteRgb(r, g, b); \
        COMP_PREFIX ## A = (COMP_PREFIX ## A << 8) + COMP_PREFIX ## A; \
    } while (0)

/*
 * SrcOver ## TYPE ## BlendFactor
 * Returns appropriate blend value for use in blending calculations.
 */
#define SrcOverIntArgbBmBlendFactor(dF, dA) \
    (dA)

#endif /* IntArgbBm_h_Included */
