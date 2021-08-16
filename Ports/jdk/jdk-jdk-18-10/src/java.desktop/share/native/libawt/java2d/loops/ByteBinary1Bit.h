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

#ifndef ByteBinary1Bit_h_Included
#define ByteBinary1Bit_h_Included

#include "AnyByteBinary.h"

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "ByteBinary1Bit".
 */

typedef jubyte  ByteBinary1BitPixelType;
typedef jubyte  ByteBinary1BitDataType;

#define ByteBinary1BitPixelStride      0
#define ByteBinary1BitPixelsPerByte    8
#define ByteBinary1BitBitsPerPixel     1
#define ByteBinary1BitMaxBitOffset     7
#define ByteBinary1BitPixelMask        0x1

#define DeclareByteBinary1BitLoadVars     DeclareByteBinaryLoadVars
#define DeclareByteBinary1BitStoreVars    DeclareByteBinaryStoreVars
#define SetByteBinary1BitStoreVarsYPos    SetByteBinaryStoreVarsYPos
#define SetByteBinary1BitStoreVarsXPos    SetByteBinaryStoreVarsXPos
#define InitByteBinary1BitLoadVars        InitByteBinaryLoadVars
#define InitByteBinary1BitStoreVarsY      InitByteBinaryStoreVarsY
#define InitByteBinary1BitStoreVarsX      InitByteBinaryStoreVarsX
#define NextByteBinary1BitStoreVarsY      NextByteBinaryStoreVarsY
#define NextByteBinary1BitStoreVarsX      NextByteBinaryStoreVarsX

#define DeclareByteBinary1BitInitialLoadVars(pRasInfo, pRas, PREFIX, x) \
    DeclareByteBinaryInitialLoadVars(ByteBinary1Bit, pRasInfo, pRas, PREFIX, x)

#define InitialLoadByteBinary1Bit(pRas, PREFIX) \
    InitialLoadByteBinary(ByteBinary1Bit, pRas, PREFIX)

#define ShiftBitsByteBinary1Bit(PREFIX) \
    ShiftBitsByteBinary(ByteBinary1Bit, PREFIX)

#define FinalStoreByteBinary1Bit(pRas, PREFIX) \
    FinalStoreByteBinary(ByteBinary1Bit, pRas, PREFIX)

#define CurrentPixelByteBinary1Bit(PREFIX) \
    CurrentPixelByteBinary(ByteBinary1Bit, PREFIX)


#define StoreByteBinary1BitPixel(pRas, x, pixel) \
    StoreByteBinaryPixel(ByteBinary1Bit, pRas, x, pixel)

#define StoreByteBinary1BitPixelData(pPix, x, pixel, PREFIX) \
    StoreByteBinaryPixelData(ByteBinary1Bit, pPix, x, pixel, PREFIX)

#define ByteBinary1BitPixelFromArgb(pixel, rgb, pRasInfo) \
    ByteBinaryPixelFromArgb(ByteBinary1Bit, pixel, rgb, pRasInfo)

#define XorByteBinary1BitPixelData(pDst, x, PREFIX, srcpixel, xorpixel, mask)\
    XorByteBinaryPixelData(ByteBinary1Bit, pDst, x, PREFIX, \
                           srcpixel, xorpixel, mask)


#define LoadByteBinary1BitTo1IntRgb(pRas, PREFIX, x, rgb) \
    LoadByteBinaryTo1IntRgb(ByteBinary1Bit, pRas, PREFIX, x, rgb)

#define LoadByteBinary1BitTo1IntArgb(pRas, PREFIX, x, argb) \
    LoadByteBinaryTo1IntArgb(ByteBinary1Bit, pRas, PREFIX, x, argb)

#define LoadByteBinary1BitTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
    LoadByteBinaryTo3ByteRgb(ByteBinary1Bit, pRas, PREFIX, x, r, g, b)

#define LoadByteBinary1BitTo4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    LoadByteBinaryTo4ByteArgb(ByteBinary1Bit, pRas, PREFIX, x, a, r, g, b)

#define StoreByteBinary1BitFrom1IntRgb(pRas, PREFIX, x, rgb) \
    StoreByteBinaryFrom1IntRgb(ByteBinary1Bit, pRas, PREFIX, x, rgb)

#define StoreByteBinary1BitFrom1IntArgb(pRas, PREFIX, x, argb) \
    StoreByteBinaryFrom1IntArgb(ByteBinary1Bit, pRas, PREFIX, x, argb)

#define StoreByteBinary1BitFrom3ByteRgb(pRas, PREFIX, x, r, g, b) \
    StoreByteBinaryFrom3ByteRgb(ByteBinary1Bit, pRas, PREFIX, x, r, g, b)

#define StoreByteBinary1BitFrom4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    StoreByteBinaryFrom4ByteArgb(ByteBinary1Bit, pRas, PREFIX, x, a, r, g, b)


#define DeclareByteBinary1BitAlphaLoadData(PREFIX) \
    DeclareByteBinaryAlphaLoadData(ByteBinary1Bit, PREFIX)

#define InitByteBinary1BitAlphaLoadData(PREFIX, pRasInfo) \
    InitByteBinaryAlphaLoadData(ByteBinary1Bit, PREFIX, pRasInfo)

#define LoadAlphaFromByteBinary1BitFor4ByteArgb(pRas, PREFIX, COMP_PREFIX) \
    LoadAlphaFromByteBinaryFor4ByteArgb(ByteBinary1Bit, pRas, PREFIX, \
                                        COMP_PREFIX)

#define Postload4ByteArgbFromByteBinary1Bit(pRas, PREFIX, COMP_PREFIX) \
    Postload4ByteArgbFromByteBinary(ByteBinary1Bit, pRas, PREFIX, COMP_PREFIX)


#define ByteBinary1BitIsPremultiplied    ByteBinaryIsPremultiplied

#define StoreByteBinary1BitFrom4ByteArgbComps(pRas, PREFIX, x, COMP_PREFIX) \
    StoreByteBinaryFrom4ByteArgbComps(ByteBinary1Bit, pRas, \
                                      PREFIX, x, COMP_PREFIX)

#endif /* ByteBinary1Bit_h_Included */
