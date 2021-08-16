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

#ifndef IntRgbx_h_Included
#define IntRgbx_h_Included

#include "IntDcm.h"

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "IntRgbx".
 */

typedef jint    IntRgbxPixelType;
typedef jint    IntRgbxDataType;

#define IntRgbxIsOpaque 1

#define IntRgbxPixelStride      4

#define DeclareIntRgbxLoadVars(PREFIX)
#define DeclareIntRgbxStoreVars(PREFIX)
#define SetIntRgbxStoreVarsYPos(PREFIX, pRasInfo, y)
#define SetIntRgbxStoreVarsXPos(PREFIX, pRasInfo, x)
#define InitIntRgbxLoadVars(PREFIX, pRasInfo)
#define InitIntRgbxStoreVarsY(PREFIX, pRasInfo)
#define InitIntRgbxStoreVarsX(PREFIX, pRasInfo)
#define NextIntRgbxStoreVarsX(PREFIX)
#define NextIntRgbxStoreVarsY(PREFIX)

#define IntRgbxXparLutEntry                     1
#define IntRgbxIsXparLutEntry(pix)              ((pix & 1) != 0)
#define StoreIntRgbxNonXparFromArgb             StoreIntRgbxFromArgb


#define IntRgbxPixelFromArgb(pixel, rgb, pRasInfo) \
    (pixel) = (rgb << 8)

#define StoreIntRgbxPixel(pRas, x, pixel) \
    (pRas)[x] = (pixel)

#define DeclareIntRgbxPixelData(PREFIX)

#define ExtractIntRgbxPixelData(PIXEL, PREFIX)

#define StoreIntRgbxPixelData(pPix, x, pixel, PREFIX) \
    (pPix)[x] = (pixel)


#define LoadIntRgbxTo1IntRgb(pRas, PREFIX, x, rgb) \
    (rgb) = ((pRas)[x] >> 8)

#define LoadIntRgbxTo1IntArgb(pRas, PREFIX, x, argb) \
    (argb) = 0xff000000 | ((pRas)[x] >> 8)

#define LoadIntRgbxTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
    do { \
        jint pixel = (pRas)[x]; \
        ExtractIntDcmComponents123X(pixel, r, g, b); \
    } while (0)

#define LoadIntRgbxTo4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    do { \
        LoadIntRgbxTo3ByteRgb(pRas, PREFIX, x, r, g, b); \
        (a) = 0xff; \
    } while (0)

#define StoreIntRgbxFrom1IntRgb(pRas, PREFIX, x, rgb) \
    (pRas)[x] = ((rgb) << 8)

#define StoreIntRgbxFrom1IntArgb(pRas, PREFIX, x, argb) \
    (pRas)[x] = ((argb) << 8)

#define StoreIntRgbxFrom3ByteRgb(pRas, PREFIX, x, r, g, b) \
    (pRas)[x] = ComposeIntDcmComponents123X(r, g, b)

#define StoreIntRgbxFrom4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    StoreIntRgbxFrom3ByteRgb(pRas, PREFIX, x, r, g, b)

#define CopyIntRgbxToIntArgbPre(pRGB, i, PREFIX, pRow, x) \
    (pRGB)[i] = (((pRow)[x] >> 8) | 0xff000000)


#define DeclareIntRgbxAlphaLoadData(PREFIX)

#define InitIntRgbxAlphaLoadData(PREFIX, pRasInfo)

#define LoadAlphaFromIntRgbxFor4ByteArgb(pRas, PREFIX, COMP_PREFIX) \
    COMP_PREFIX ## A = 0xff

#define Postload4ByteArgbFromIntRgbx(pRas, PREFIX, COMP_PREFIX) \
    LoadIntRgbxTo3ByteRgb(pRas, PREFIX, 0, COMP_PREFIX ## R, \
                          COMP_PREFIX ## G, COMP_PREFIX ## B)

#define StoreIntRgbxFrom4ByteArgbComps(pRas, PREFIX, x, COMP_PREFIX) \
    StoreIntRgbxFrom4ByteArgb(pRas, PREFIX, x, \
                              COMP_PREFIX ## A, COMP_PREFIX ## R, \
                              COMP_PREFIX ## G, COMP_PREFIX ## B)

#define IntRgbxIsPremultiplied  0

#define DeclareIntRgbxBlendFillVars(PREFIX)

#define ClearIntRgbxBlendFillVars(PREFIX, argb) \
    argb = 0

#define InitIntRgbxBlendFillVarsNonPre(PREFIX, argb, COMP_PREFIX)

#define InitIntRgbxBlendFillVarsPre(PREFIX, argb, COMP_PREFIX)

#define StoreIntRgbxBlendFill(pRas, PREFIX, x, argb, COMP_PREFIX) \
    (pRas)[x] = (argb << 8)

#endif /* IntRgbx_h_Included */
