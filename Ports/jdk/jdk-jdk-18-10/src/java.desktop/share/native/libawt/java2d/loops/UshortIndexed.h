/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef UshortIndexed_h_Included
#define UshortIndexed_h_Included

#include "IntDcm.h"
#include "ByteIndexed.h"

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "UshortIndexed".
 */

typedef jushort UshortIndexedPixelType;
typedef jushort UshortIndexedDataType;

#define UshortIndexedPixelStride                2
/*
 * Note that even though the type is called UshortIndex it is
 * really only used as 12-bit indexed (per the BitsPerPixel
 * define), thus we need to mask 12 bits of the index into Lut.
 */
#define UshortIndexedBitsPerPixel               12
#define UshortIndexedLutMask                    0xfff

#define DeclareUshortIndexedLoadVars(PREFIX) \
    jint *PREFIX ## Lut;

#define DeclareUshortIndexedStoreVars(PREFIX) \
    int PREFIX ## XDither, PREFIX ## YDither; \
    char *PREFIX ## rerr, *PREFIX ## gerr, *PREFIX ## berr; \
    unsigned char *PREFIX ## InvLut;

#define SetUshortIndexedStoreVarsYPos(PREFIX, pRasInfo, LOC) \
    do { \
         PREFIX ## YDither = ((LOC & 7) << 3); \
    } while (0)

#define SetUshortIndexedStoreVarsXPos(PREFIX, pRasInfo, LOC) \
    do { \
        PREFIX ## rerr = (pRasInfo)->redErrTable + PREFIX ## YDither; \
        PREFIX ## gerr = (pRasInfo)->grnErrTable + PREFIX ## YDither; \
        PREFIX ## berr = (pRasInfo)->bluErrTable + PREFIX ## YDither; \
        PREFIX ## XDither = (LOC & 7); \
    } while (0)

#define InitUshortIndexedLoadVars(PREFIX, pRasInfo) \
    PREFIX ## Lut = (pRasInfo)->lutBase

/* REMIND Could collapse Init..Store..X and Init..Store..Y into one Init
 * and factor out the Set.. macros.
 */
#define InitUshortIndexedStoreVarsY(PREFIX, pRasInfo) \
    do { \
        SetUshortIndexedStoreVarsYPos(PREFIX, pRasInfo, (pRasInfo)->bounds.y1); \
        PREFIX ## InvLut = (pRasInfo)->invColorTable; \
    } while (0)

#define InitUshortIndexedStoreVarsX(PREFIX, pRasInfo) \
    SetUshortIndexedStoreVarsXPos(PREFIX, pRasInfo, (pRasInfo)->bounds.x1);


#define NextUshortIndexedStoreVarsX(PREFIX) \
    PREFIX ## XDither = (PREFIX ## XDither + 1) & 7

#define NextUshortIndexedStoreVarsY(PREFIX) \
    PREFIX ## YDither = (PREFIX ## YDither + (1 << 3)) & (7 << 3)

typedef jushort UshortIndexedBmPixelType;
typedef jushort UshortIndexedBmDataType;

#define UshortIndexedBmPixelStride      2
#define UshortIndexedBmBitsPerPixel     12

#define DeclareUshortIndexedBmLoadVars  DeclareUshortIndexedLoadVars
#define DeclareUshortIndexedBmStoreVars DeclareUshortIndexedStoreVars
#define InitUshortIndexedBmLoadVars     InitUshortIndexedLoadVars
#define InitUshortIndexedBmStoreVarsY   InitUshortIndexedStoreVarsY
#define InitUshortIndexedBmStoreVarsX   InitUshortIndexedStoreVarsX
#define NextUshortIndexedBmStoreVarsX   NextUshortIndexedStoreVarsX
#define NextUshortIndexedBmStoreVarsY   NextUshortIndexedStoreVarsY

#define UshortIndexedXparLutEntry                       -1
#define UshortIndexedIsXparLutEntry(pix)                (pix < 0)
#define StoreUshortIndexedNonXparFromArgb               StoreUshortIndexedFrom1IntArgb

#define StoreUshortIndexedPixel(pRas, x, pixel) \
    ((pRas)[x] = (jushort) (pixel))

#define DeclareUshortIndexedPixelData(PREFIX)
#define ExtractUshortIndexedPixelData(PIXEL, PREFIX)

#define StoreUshortIndexedPixelData(pPix, x, pixel, PREFIX) \
    (pPix)[x] = (jushort) (pixel)

#define UshortIndexedPixelFromArgb(pixel, rgb, pRasInfo) \
    do { \
        jint r, g, b; \
        ExtractIntDcmComponentsX123(rgb, r, g, b); \
        (pixel) = SurfaceData_InvColorMap((pRasInfo)->invColorTable, \
                                          r, g, b); \
    } while (0)

#define LoadUshortIndexedTo1IntRgb(pRas, PREFIX, x, rgb) \
   (rgb) = PREFIX ## Lut[(pRas[x])&UshortIndexedLutMask];

#define LoadUshortIndexedTo1IntArgb(pRas, PREFIX, x, argb) \
   (argb) = PREFIX ## Lut[(pRas[x])&UshortIndexedLutMask];

#define LoadUshortIndexedTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
    do { \
        jint rgb = PREFIX ## Lut[(pRas[x])&UshortIndexedLutMask]; \
        ExtractIntDcmComponentsX123(rgb, r, g, b); \
    } while (0)

#define LoadUshortIndexedTo4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    do { \
        jint argb = PREFIX ## Lut[(pRas[x])&UshortIndexedLutMask]; \
        ExtractIntDcmComponents1234(argb, a, r, g, b); \
    } while (0)

#define StoreUshortIndexedFrom1IntRgb(pRas, PREFIX, x, rgb) \
    do { \
        int r, g, b; \
        ExtractIntDcmComponentsX123(rgb, r, g, b); \
        StoreUshortIndexedFrom3ByteRgb(pRas, PREFIX, x, r, g, b); \
    } while (0)

#define StoreUshortIndexedFrom1IntArgb(pRas, PREFIX, x, argb) \
    StoreUshortIndexedFrom1IntRgb(pRas, PREFIX, x, argb)

#define StoreUshortIndexedFrom3ByteRgb(pRas, PREFIX, x, r, g, b) \
    do { \
        r += PREFIX ## rerr[PREFIX ## XDither]; \
        g += PREFIX ## gerr[PREFIX ## XDither]; \
        b += PREFIX ## berr[PREFIX ## XDither]; \
        ByteClamp3Components(r, g, b); \
        (pRas)[x] = SurfaceData_InvColorMap(PREFIX ## InvLut, r, g, b); \
    } while (0)

#define StoreUshortIndexedFrom4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    StoreUshortIndexedFrom3ByteRgb(pRas, PREFIX, x, r, g, b)


#define DeclareUshortIndexedAlphaLoadData(PREFIX) \
    jint *PREFIX ## Lut; \
    jint PREFIX ## rgb;

#define InitUshortIndexedAlphaLoadData(PREFIX, pRasInfo) \
    do { \
        PREFIX ## Lut = (pRasInfo)->lutBase; \
        PREFIX ## rgb = 0; \
    } while (0)

#define LoadAlphaFromUshortIndexedFor4ByteArgb(pRas, PREFIX, COMP_PREFIX) \
    do { \
        PREFIX ## rgb = PREFIX ## Lut[((pRas)[0])&UshortIndexedLutMask]; \
        COMP_PREFIX ## A = ((juint) PREFIX ## rgb) >> 24; \
    } while (0)

#define Postload4ByteArgbFromUshortIndexed(pRas, PREFIX, COMP_PREFIX) \
    do { \
        COMP_PREFIX ## R = (PREFIX ## rgb >> 16) & 0xff; \
        COMP_PREFIX ## G = (PREFIX ## rgb >>  8) & 0xff; \
        COMP_PREFIX ## B = (PREFIX ## rgb >>  0) & 0xff; \
    } while (0)


#define UshortIndexedIsPremultiplied    0

#define StoreUshortIndexedFrom4ByteArgbComps(pRas, PREFIX, x, COMP_PREFIX) \
    StoreUshortIndexedFrom4ByteArgb(pRas, PREFIX, x, \
                                  COMP_PREFIX ## A, COMP_PREFIX ## R, \
                                  COMP_PREFIX ## G, COMP_PREFIX ## B)

#endif /* UshortIndexed_h_Included */
