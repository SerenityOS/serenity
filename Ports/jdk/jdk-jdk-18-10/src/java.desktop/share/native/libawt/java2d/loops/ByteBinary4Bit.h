/*
 * Copyright (c) 2000, 2005, Oracle and/or its affiliates. All rights reserved.
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

#ifndef ByteBinary4Bit_h_Included
#define ByteBinary4Bit_h_Included

#include "AnyByteBinary.h"

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "ByteBinary4Bit".
 */

typedef jubyte  ByteBinary4BitPixelType;
typedef jubyte  ByteBinary4BitDataType;

#define ByteBinary4BitPixelStride      0
#define ByteBinary4BitPixelsPerByte    2
#define ByteBinary4BitBitsPerPixel     4
#define ByteBinary4BitMaxBitOffset     4
#define ByteBinary4BitPixelMask        0xf

#define DeclareByteBinary4BitLoadVars     DeclareByteBinaryLoadVars
#define DeclareByteBinary4BitStoreVars    DeclareByteBinaryStoreVars
#define SetByteBinary4BitStoreVarsYPos    SetByteBinaryStoreVarsYPos
#define SetByteBinary4BitStoreVarsXPos    SetByteBinaryStoreVarsXPos
#define InitByteBinary4BitLoadVars        InitByteBinaryLoadVars
#define InitByteBinary4BitStoreVarsY      InitByteBinaryStoreVarsY
#define InitByteBinary4BitStoreVarsX      InitByteBinaryStoreVarsX
#define NextByteBinary4BitStoreVarsY      NextByteBinaryStoreVarsY
#define NextByteBinary4BitStoreVarsX      NextByteBinaryStoreVarsX

#define DeclareByteBinary4BitInitialLoadVars(pRasInfo, pRas, PREFIX, x) \
    DeclareByteBinaryInitialLoadVars(ByteBinary4Bit, pRasInfo, pRas, PREFIX, x)

#define InitialLoadByteBinary4Bit(pRas, PREFIX) \
    InitialLoadByteBinary(ByteBinary4Bit, pRas, PREFIX)

#define ShiftBitsByteBinary4Bit(PREFIX) \
    ShiftBitsByteBinary(ByteBinary4Bit, PREFIX)

#define FinalStoreByteBinary4Bit(pRas, PREFIX) \
    FinalStoreByteBinary(ByteBinary4Bit, pRas, PREFIX)

#define CurrentPixelByteBinary4Bit(PREFIX) \
    CurrentPixelByteBinary(ByteBinary4Bit, PREFIX)


#define StoreByteBinary4BitPixel(pRas, x, pixel) \
    StoreByteBinaryPixel(ByteBinary4Bit, pRas, x, pixel)

#define StoreByteBinary4BitPixelData(pPix, x, pixel, PREFIX) \
    StoreByteBinaryPixelData(ByteBinary4Bit, pPix, x, pixel, PREFIX)

#define ByteBinary4BitPixelFromArgb(pixel, rgb, pRasInfo) \
    ByteBinaryPixelFromArgb(ByteBinary4Bit, pixel, rgb, pRasInfo)

#define XorByteBinary4BitPixelData(pDst, x, PREFIX, srcpixel, xorpixel, mask)\
    XorByteBinaryPixelData(ByteBinary4Bit, pDst, x, PREFIX, \
                           srcpixel, xorpixel, mask)


#define LoadByteBinary4BitTo1IntRgb(pRas, PREFIX, x, rgb) \
    LoadByteBinaryTo1IntRgb(ByteBinary4Bit, pRas, PREFIX, x, rgb)

#define LoadByteBinary4BitTo1IntArgb(pRas, PREFIX, x, argb) \
    LoadByteBinaryTo1IntArgb(ByteBinary4Bit, pRas, PREFIX, x, argb)

#define LoadByteBinary4BitTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
    LoadByteBinaryTo3ByteRgb(ByteBinary4Bit, pRas, PREFIX, x, r, g, b)

#define LoadByteBinary4BitTo4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    LoadByteBinaryTo4ByteArgb(ByteBinary4Bit, pRas, PREFIX, x, a, r, g, b)

#define StoreByteBinary4BitFrom1IntRgb(pRas, PREFIX, x, rgb) \
    StoreByteBinaryFrom1IntRgb(ByteBinary4Bit, pRas, PREFIX, x, rgb)

#define StoreByteBinary4BitFrom1IntArgb(pRas, PREFIX, x, argb) \
    StoreByteBinaryFrom1IntArgb(ByteBinary4Bit, pRas, PREFIX, x, argb)

#define StoreByteBinary4BitFrom3ByteRgb(pRas, PREFIX, x, r, g, b) \
    StoreByteBinaryFrom3ByteRgb(ByteBinary4Bit, pRas, PREFIX, x, r, g, b)

#define StoreByteBinary4BitFrom4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    StoreByteBinaryFrom4ByteArgb(ByteBinary4Bit, pRas, PREFIX, x, a, r, g, b)


#define DeclareByteBinary4BitAlphaLoadData(PREFIX) \
    DeclareByteBinaryAlphaLoadData(ByteBinary4Bit, PREFIX)

#define InitByteBinary4BitAlphaLoadData(PREFIX, pRasInfo) \
    InitByteBinaryAlphaLoadData(ByteBinary4Bit, PREFIX, pRasInfo)

#define LoadAlphaFromByteBinary4BitFor4ByteArgb(pRas, PREFIX, COMP_PREFIX) \
    LoadAlphaFromByteBinaryFor4ByteArgb(ByteBinary4Bit, pRas, PREFIX, \
                                        COMP_PREFIX)

#define Postload4ByteArgbFromByteBinary4Bit(pRas, PREFIX, COMP_PREFIX) \
    Postload4ByteArgbFromByteBinary(ByteBinary4Bit, pRas, PREFIX, COMP_PREFIX)


#define ByteBinary4BitIsPremultiplied    ByteBinaryIsPremultiplied

#define StoreByteBinary4BitFrom4ByteArgbComps(pRas, PREFIX, x, COMP_PREFIX) \
    StoreByteBinaryFrom4ByteArgbComps(ByteBinary4Bit, pRas, \
                                      PREFIX, x, COMP_PREFIX)

#endif /* ByteBinary4Bit_h_Included */
