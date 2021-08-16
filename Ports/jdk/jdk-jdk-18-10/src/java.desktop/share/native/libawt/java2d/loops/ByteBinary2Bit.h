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

#ifndef ByteBinary2Bit_h_Included
#define ByteBinary2Bit_h_Included

#include "AnyByteBinary.h"

/*
 * This file contains macro and type definitions used by the macros in
 * LoopMacros.h to manipulate a surface of type "ByteBinary2Bit".
 */

typedef jubyte  ByteBinary2BitPixelType;
typedef jubyte  ByteBinary2BitDataType;

#define ByteBinary2BitPixelStride      0
#define ByteBinary2BitPixelsPerByte    4
#define ByteBinary2BitBitsPerPixel     2
#define ByteBinary2BitMaxBitOffset     6
#define ByteBinary2BitPixelMask        0x3

#define DeclareByteBinary2BitLoadVars     DeclareByteBinaryLoadVars
#define DeclareByteBinary2BitStoreVars    DeclareByteBinaryStoreVars
#define SetByteBinary2BitStoreVarsYPos    SetByteBinaryStoreVarsYPos
#define SetByteBinary2BitStoreVarsXPos    SetByteBinaryStoreVarsXPos
#define InitByteBinary2BitLoadVars        InitByteBinaryLoadVars
#define InitByteBinary2BitStoreVarsY      InitByteBinaryStoreVarsY
#define InitByteBinary2BitStoreVarsX      InitByteBinaryStoreVarsX
#define NextByteBinary2BitStoreVarsY      NextByteBinaryStoreVarsY
#define NextByteBinary2BitStoreVarsX      NextByteBinaryStoreVarsX

#define DeclareByteBinary2BitInitialLoadVars(pRasInfo, pRas, PREFIX, x) \
    DeclareByteBinaryInitialLoadVars(ByteBinary2Bit, pRasInfo, pRas, PREFIX, x)

#define InitialLoadByteBinary2Bit(pRas, PREFIX) \
    InitialLoadByteBinary(ByteBinary2Bit, pRas, PREFIX)

#define ShiftBitsByteBinary2Bit(PREFIX) \
    ShiftBitsByteBinary(ByteBinary2Bit, PREFIX)

#define FinalStoreByteBinary2Bit(pRas, PREFIX) \
    FinalStoreByteBinary(ByteBinary2Bit, pRas, PREFIX)

#define CurrentPixelByteBinary2Bit(PREFIX) \
    CurrentPixelByteBinary(ByteBinary2Bit, PREFIX)


#define StoreByteBinary2BitPixel(pRas, x, pixel) \
    StoreByteBinaryPixel(ByteBinary2Bit, pRas, x, pixel)

#define StoreByteBinary2BitPixelData(pPix, x, pixel, PREFIX) \
    StoreByteBinaryPixelData(ByteBinary2Bit, pPix, x, pixel, PREFIX)

#define ByteBinary2BitPixelFromArgb(pixel, rgb, pRasInfo) \
    ByteBinaryPixelFromArgb(ByteBinary2Bit, pixel, rgb, pRasInfo)

#define XorByteBinary2BitPixelData(pDst, x, PREFIX, srcpixel, xorpixel, mask)\
    XorByteBinaryPixelData(ByteBinary2Bit, pDst, x, PREFIX, \
                           srcpixel, xorpixel, mask)


#define LoadByteBinary2BitTo1IntRgb(pRas, PREFIX, x, rgb) \
    LoadByteBinaryTo1IntRgb(ByteBinary2Bit, pRas, PREFIX, x, rgb)

#define LoadByteBinary2BitTo1IntArgb(pRas, PREFIX, x, argb) \
    LoadByteBinaryTo1IntArgb(ByteBinary2Bit, pRas, PREFIX, x, argb)

#define LoadByteBinary2BitTo3ByteRgb(pRas, PREFIX, x, r, g, b) \
    LoadByteBinaryTo3ByteRgb(ByteBinary2Bit, pRas, PREFIX, x, r, g, b)

#define LoadByteBinary2BitTo4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    LoadByteBinaryTo4ByteArgb(ByteBinary2Bit, pRas, PREFIX, x, a, r, g, b)

#define StoreByteBinary2BitFrom1IntRgb(pRas, PREFIX, x, rgb) \
    StoreByteBinaryFrom1IntRgb(ByteBinary2Bit, pRas, PREFIX, x, rgb)

#define StoreByteBinary2BitFrom1IntArgb(pRas, PREFIX, x, argb) \
    StoreByteBinaryFrom1IntArgb(ByteBinary2Bit, pRas, PREFIX, x, argb)

#define StoreByteBinary2BitFrom3ByteRgb(pRas, PREFIX, x, r, g, b) \
    StoreByteBinaryFrom3ByteRgb(ByteBinary2Bit, pRas, PREFIX, x, r, g, b)

#define StoreByteBinary2BitFrom4ByteArgb(pRas, PREFIX, x, a, r, g, b) \
    StoreByteBinaryFrom4ByteArgb(ByteBinary2Bit, pRas, PREFIX, x, a, r, g, b)


#define DeclareByteBinary2BitAlphaLoadData(PREFIX) \
    DeclareByteBinaryAlphaLoadData(ByteBinary2Bit, PREFIX)

#define InitByteBinary2BitAlphaLoadData(PREFIX, pRasInfo) \
    InitByteBinaryAlphaLoadData(ByteBinary2Bit, PREFIX, pRasInfo)

#define LoadAlphaFromByteBinary2BitFor4ByteArgb(pRas, PREFIX, COMP_PREFIX) \
    LoadAlphaFromByteBinaryFor4ByteArgb(ByteBinary2Bit, pRas, PREFIX, \
                                        COMP_PREFIX)

#define Postload4ByteArgbFromByteBinary2Bit(pRas, PREFIX, COMP_PREFIX) \
    Postload4ByteArgbFromByteBinary(ByteBinary2Bit, pRas, PREFIX, COMP_PREFIX)


#define ByteBinary2BitIsPremultiplied    ByteBinaryIsPremultiplied

#define StoreByteBinary2BitFrom4ByteArgbComps(pRas, PREFIX, x, COMP_PREFIX) \
    StoreByteBinaryFrom4ByteArgbComps(ByteBinary2Bit, pRas, \
                                      PREFIX, x, COMP_PREFIX)

#endif /* ByteBinary2Bit_h_Included */
