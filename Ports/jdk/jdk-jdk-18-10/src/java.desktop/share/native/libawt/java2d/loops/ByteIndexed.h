/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef ByteIndexed_h_Included
#define ByteIndexed_h_Included

#include "IntDcm.h"

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "ByteIndexed".
 */

typedef jubyte  ByteIndexedPixelType;
typedef jubyte  ByteIndexedDataType;

#define ByteIndexedPixelStride          1
#define ByteIndexedBitsPerPixel         8

#define DeclareByteIndexedLoadVars(PREFIX) \
    jint *PREFIX ## Lut;

#define DeclareByteIndexedStoreVars(PREFIX) \
    int PREFIX ## XDither, PREFIX ## YDither, PREFIX ## RepPrims; \
    char *PREFIX ## rerr, *PREFIX ## gerr, *PREFIX ## berr; \
    unsigned char *PREFIX ## InvLut;

#define SetByteIndexedStoreVarsYPos(PREFIX, pRasInfo, LOC) \
    do { \
         PREFIX ## YDither = ((LOC & 7) << 3); \
    } while (0)

#define SetByteIndexedStoreVarsXPos(PREFIX, pRasInfo, LOC) \
    do { \
        PREFIX ## rerr = (pRasInfo)->redErrTable + PREFIX ## YDither; \
        PREFIX ## gerr = (pRasInfo)->grnErrTable + PREFIX ## YDither; \
        PREFIX ## berr = (pRasInfo)->bluErrTable + PREFIX ## YDither; \
        PREFIX ## XDither = (LOC & 7); \
    } while (0)

#define InitByteIndexedLoadVars(PREFIX, pRasInfo) \
    PREFIX ## Lut = (pRasInfo)->lutBase

/* REMIND Could collapse Init..Store..X and Init..Store..Y into one Init
 * and factor out the Set.. macros.
 */
#define InitByteIndexedStoreVarsY(PREFIX, pRasInfo) \
    do { \
        SetByteIndexedStoreVarsYPos(PREFIX, pRasInfo, (pRasInfo)->bounds.y1); \
        PREFIX ## InvLut = (pRasInfo)->invColorTable; \
        PREFIX ## RepPrims = (pRasInfo)->representsPrimaries; \
    } while (0)

#define InitByteIndexedStoreVarsX(PREFIX, pRasInfo) \
    SetByteIndexedStoreVarsXPos(PREFIX, pRasInfo, (pRasInfo)->bounds.x1);


#define NextByteIndexedStoreVarsX(PREFIX) \
    PREFIX ## XDither = (PREFIX ## XDither + 1) & 7

#define NextByteIndexedStoreVarsY(PREFIX) \
    PREFIX ## YDither = (PREFIX ## YDither + (1 << 3)) & (7 << 3)

typedef jubyte  ByteIndexedBmPixelType;
typedef jubyte  ByteIndexedBmDataType;

#define ByteIndexedBmPixelStride        1
#define ByteIndexedBmBitsPerPixel       8

#define DeclareByteIndexedBmLoadVars    DeclareByteIndexedLoadVars
#define DeclareByteIndexedBmStoreVars   DeclareByteIndexedStoreVars
#define InitByteIndexedBmLoadVars       InitByteIndexedLoadVars
#define InitByteIndexedBmStoreVarsY     InitByteIndexedStoreVarsY
#define InitByteIndexedBmStoreVarsX     InitByteIndexedStoreVarsX
#define NextByteIndexedBmStoreVarsX     NextByteIndexedStoreVarsX
#define NextByteIndexedBmStoreVarsY     NextByteIndexedStoreVarsY

#define LoadByteIndexedBmTo1IntArgb     LoadByteIndexedTo1IntArgb

#define CopyByteIndexedBmToIntArgbPre(pRGB, i, PREFIX, pRow, x) \
    do { \
        jint argb = PREFIX ## Lut[pRow[x]]; \
        (pRGB)[i] = argb & (argb >> 24); \
    } while (0)


#define ByteIndexedXparLutEntry                 -1
#define ByteIndexedIsXparLutEntry(pix)          (pix < 0)
#define StoreByteIndexedNonXparFromArgb         StoreByteIndexedFrom1IntArgb

#define StoreByteIndexedPixel(pRas, x, pixel) \
    ((pRas)[x] = (jubyte) (pixel))

#define DeclareByteIndexedPixelData(PREFIX)
#define ExtractByteIndexedPixelData(PIXEL, PREFIX)

#define StoreByteIndexedPixelData(pPix, x, pixel, PREFIX) \
    (pPix)[x] = (jubyte) (pixel)

#define ByteIndexedPixelFromArgb(pixel, rgb, pRasInfo) \
    do { \
        jint r, g, b; \
        ExtractIntDcmComponentsX123(rgb, r, g, b); \
        (pixel) = SurfaceData_InvColorMap((pRasInfo)->invColorTable, \
                                          r, g, b); \
    } while (0)

#define LoadByteIndexedTo1IntRgb(pRas, PREFIX, x, rgb) \
    (rgb) = PREFIX ## Lut[pRas[x]]

#define LoadByteIndexedTo1IntArgb(pRas, PREFIX, x, argb) \
    (argb) = PREFIX ## Lut[pRas[x]]

#define LoadByteIndexedTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
    do { \
        jint rgb = PREFIX ## Lut[pRas[x]]; \
        ExtractIntDcmComponentsX123(rgb, r, g, b); \
    } while (0)

#define LoadByteIndexedTo4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    do { \
        jint argb = PREFIX ## Lut[pRas[x]]; \
        ExtractIntDcmComponents1234(argb, a, r, g, b); \
    } while (0)

#define ByteClamp1Component(X)  \
    do { if (((X) >> 8) != 0) {X = (~(X >> 31)) & 255; } } while (0)

#define ByteClamp3Components(R, G, B) \
    do { \
        if (((R|G|B) >> 8) != 0) { \
            ByteClamp1Component(R); \
            ByteClamp1Component(G); \
            ByteClamp1Component(B); \
        } \
    } while (0)

#define StoreByteIndexedFrom1IntRgb(pRas, PREFIX, x, rgb) \
    do { \
        int r, g, b; \
        ExtractIntDcmComponentsX123(rgb, r, g, b); \
        StoreByteIndexedFrom3ByteRgb(pRas, PREFIX, x, r, g, b); \
    } while (0)

#define StoreByteIndexedFrom1IntArgb(pRas, PREFIX, x, argb) \
    StoreByteIndexedFrom1IntRgb(pRas, PREFIX, x, argb)

#define StoreByteIndexedFrom3ByteRgb(pRas, PREFIX, x, r, g, b) \
    do { \
        if (!(((r == 0) || (r == 255)) && \
              ((g == 0) || (g == 255)) && \
              ((b == 0) || (b == 255)) && \
              PREFIX ## RepPrims)) { \
            r += PREFIX ## rerr[PREFIX ## XDither]; \
            g += PREFIX ## gerr[PREFIX ## XDither]; \
            b += PREFIX ## berr[PREFIX ## XDither]; \
        } \
        ByteClamp3Components(r, g, b); \
        (pRas)[x] = SurfaceData_InvColorMap(PREFIX ## InvLut, r, g, b); \
    } while (0)

#define StoreByteIndexedFrom4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    StoreByteIndexedFrom3ByteRgb(pRas, PREFIX, x, r, g, b)

#define CopyByteIndexedToIntArgbPre(pRGB, i, PREFIX, pRow, x) \
    do { \
        jint argb = PREFIX ## Lut[pRow[x]]; \
        jint a = URShift(argb, 24); \
        if (a == 0) { \
            argb = 0; \
        } else if (a < 0xff) { \
            jint r = (argb >> 16) & 0xff; \
            jint g = (argb >>  8) & 0xff; \
            jint b = (argb      ) & 0xff; \
            r = MUL8(a, r); \
            g = MUL8(a, g); \
            b = MUL8(a, b); \
            argb = ComposeIntDcmComponents1234(a, r, g, b); \
        } \
        (pRGB)[i] = argb; \
    } while (0)


#define DeclareByteIndexedAlphaLoadData(PREFIX) \
    jint *PREFIX ## Lut; \
    jint PREFIX ## rgb;

#define InitByteIndexedAlphaLoadData(PREFIX, pRasInfo) \
    do { \
        PREFIX ## Lut = (pRasInfo)->lutBase; \
        PREFIX ## rgb = 0; \
    } while (0)

#define LoadAlphaFromByteIndexedFor4ByteArgb(pRas, PREFIX, COMP_PREFIX) \
    do { \
        PREFIX ## rgb = PREFIX ## Lut[(pRas)[0]]; \
        COMP_PREFIX ## A = ((juint) PREFIX ## rgb) >> 24; \
    } while (0)

#define Postload4ByteArgbFromByteIndexed(pRas, PREFIX, COMP_PREFIX) \
    do { \
        COMP_PREFIX ## R = (PREFIX ## rgb >> 16) & 0xff; \
        COMP_PREFIX ## G = (PREFIX ## rgb >>  8) & 0xff; \
        COMP_PREFIX ## B = (PREFIX ## rgb >>  0) & 0xff; \
    } while (0)


#define ByteIndexedIsPremultiplied      0

#define StoreByteIndexedFrom4ByteArgbComps(pRas, PREFIX, x, COMP_PREFIX) \
    StoreByteIndexedFrom4ByteArgb(pRas, PREFIX, x, \
                                  COMP_PREFIX ## A, COMP_PREFIX ## R, \
                                  COMP_PREFIX ## G, COMP_PREFIX ## B)

#endif /* ByteIndexed_h_Included */
