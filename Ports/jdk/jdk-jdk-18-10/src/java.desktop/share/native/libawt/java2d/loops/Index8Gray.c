/*
 * Copyright (c) 2001, 2004, Oracle and/or its affiliates. All rights reserved.
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

#include <string.h>
#include <stdio.h>
#include "AnyByte.h"
#include "Index8Gray.h"
#include "AlphaMacros.h"

#include "IntArgb.h"
#include "IntArgbPre.h"
#include "IntRgb.h"
#include "ThreeByteBgr.h"
#include "ByteGray.h"
#include "ByteIndexed.h"
#include "Index12Gray.h"

/*
 * This file declares, registers, and defines the various graphics
 * primitive loops to manipulate surfaces of type "Index8Gray".
 *
 * See also LoopMacros.h
 */

RegisterFunc RegisterIndex8Gray;

DECLARE_CONVERT_BLIT(IntArgb, Index8Gray);
DECLARE_CONVERT_BLIT(ThreeByteBgr, Index8Gray);
DECLARE_CONVERT_BLIT(ByteGray, Index8Gray);
DECLARE_CONVERT_BLIT(Index12Gray, Index8Gray);
DECLARE_CONVERT_BLIT(ByteIndexed, Index8Gray);
DECLARE_CONVERT_BLIT(Index8Gray, Index8Gray);

DECLARE_SCALE_BLIT(Index8Gray, Index8Gray);
DECLARE_SCALE_BLIT(IntArgb, Index8Gray);
DECLARE_SCALE_BLIT(ThreeByteBgr, Index8Gray);
DECLARE_SCALE_BLIT(UshortGray, Index8Gray);
DECLARE_SCALE_BLIT(ByteIndexed, Index8Gray);
DECLARE_SCALE_BLIT(ByteGray, Index8Gray);
DECLARE_SCALE_BLIT(Index12Gray, Index8Gray);

DECLARE_XPAR_CONVERT_BLIT(ByteIndexedBm, Index8Gray);
DECLARE_XPAR_BLITBG(ByteIndexedBm, Index8Gray);

DECLARE_XOR_BLIT(IntArgb, Index8Gray);
DECLARE_ALPHA_MASKFILL(Index8Gray);
DECLARE_ALPHA_MASKBLIT(IntArgb, Index8Gray);
DECLARE_ALPHA_MASKBLIT(IntArgbPre, Index8Gray);
DECLARE_ALPHA_MASKBLIT(IntRgb, Index8Gray);
DECLARE_SRCOVER_MASKFILL(Index8Gray);
DECLARE_SRCOVER_MASKBLIT(IntArgb, Index8Gray);
DECLARE_SRCOVER_MASKBLIT(IntArgbPre, Index8Gray);
DECLARE_SOLID_DRAWGLYPHLISTAA(Index8Gray);

DECLARE_TRANSFORMHELPER_FUNCS(Index8Gray);

NativePrimitive Index8GrayPrimitives[] = {
    REGISTER_CONVERT_BLIT(IntArgb, Index8Gray),
    REGISTER_CONVERT_BLIT_EQUIV(IntRgb, Index8Gray,
                                NAME_CONVERT_BLIT(IntArgb, Index8Gray)),
    REGISTER_CONVERT_BLIT(ThreeByteBgr, Index8Gray),
    REGISTER_CONVERT_BLIT(ByteGray, Index8Gray),
    REGISTER_CONVERT_BLIT(Index12Gray, Index8Gray),
    REGISTER_CONVERT_BLIT_FLAGS(Index8Gray, Index8Gray,
                                SD_LOCK_LUT,
                                SD_LOCK_LUT | SD_LOCK_INVGRAY),
    REGISTER_CONVERT_BLIT(ByteIndexed, Index8Gray),

    REGISTER_SCALE_BLIT(IntArgb, Index8Gray),
    REGISTER_SCALE_BLIT_EQUIV(IntRgb, Index8Gray,
                              NAME_SCALE_BLIT(IntArgb, Index8Gray)),
    REGISTER_SCALE_BLIT(ThreeByteBgr, Index8Gray),
    REGISTER_SCALE_BLIT(UshortGray, Index8Gray),
    REGISTER_SCALE_BLIT(ByteIndexed, Index8Gray),
    REGISTER_SCALE_BLIT(ByteGray, Index8Gray),
    REGISTER_SCALE_BLIT(Index12Gray, Index8Gray),
    REGISTER_SCALE_BLIT_FLAGS(Index8Gray, Index8Gray, 0,
                              SD_LOCK_LUT | SD_LOCK_INVGRAY),

    REGISTER_XPAR_CONVERT_BLIT(ByteIndexedBm, Index8Gray),
    REGISTER_XPAR_BLITBG(ByteIndexedBm, Index8Gray),

    REGISTER_XOR_BLIT(IntArgb, Index8Gray),
    REGISTER_ALPHA_MASKFILL(Index8Gray),
    REGISTER_ALPHA_MASKBLIT(IntArgb, Index8Gray),
    REGISTER_ALPHA_MASKBLIT(IntArgbPre, Index8Gray),
    REGISTER_ALPHA_MASKBLIT(IntRgb, Index8Gray),
    REGISTER_SRCOVER_MASKFILL(Index8Gray),
    REGISTER_SRCOVER_MASKBLIT(IntArgb, Index8Gray),
    REGISTER_SRCOVER_MASKBLIT(IntArgbPre, Index8Gray),
    REGISTER_SOLID_DRAWGLYPHLISTAA(Index8Gray),

    REGISTER_TRANSFORMHELPER_FUNCS(Index8Gray),
};

extern jboolean checkSameLut(jint *SrcReadLut, jint *DstReadLut,
                             SurfaceDataRasInfo *pSrcInfo,
                             SurfaceDataRasInfo *pDstInfo);

jboolean RegisterIndex8Gray(JNIEnv *env)
{
    return RegisterPrimitives(env, Index8GrayPrimitives,
                              ArraySize(Index8GrayPrimitives));
}

jint PixelForIndex8Gray(SurfaceDataRasInfo *pRasInfo, jint rgb)
{
    jint r, g, b, gray;
    ExtractIntDcmComponentsX123(rgb, r, g, b);
    gray = ComposeByteGrayFrom3ByteRgb(r, g, b);
    return pRasInfo->invGrayTable[gray];
}

DEFINE_CONVERT_BLIT(IntArgb, Index8Gray, 3ByteRgb)

DEFINE_CONVERT_BLIT(ThreeByteBgr, Index8Gray, 3ByteRgb)

DEFINE_CONVERT_BLIT(ByteGray, Index8Gray, 1ByteGray)

DEFINE_CONVERT_BLIT(Index12Gray, Index8Gray, 1ByteGray)

DEFINE_CONVERT_BLIT_LUT8(ByteIndexed, Index8Gray, PreProcessLut)

void NAME_CONVERT_BLIT(Index8Gray, Index8Gray)
    (void *srcBase, void *dstBase,
     juint width, juint height,
     SurfaceDataRasInfo *pSrcInfo,
     SurfaceDataRasInfo *pDstInfo,
     NativePrimitive *pPrim,
     CompositeInfo *pCompInfo)
{
    DeclareIndex8GrayLoadVars(SrcRead)
    DeclareIndex8GrayLoadVars(DstRead)
    jint srcScan = pSrcInfo->scanStride;
    jint dstScan = pDstInfo->scanStride;

    InitIndex8GrayLoadVars(SrcRead, pSrcInfo);
    InitIndex8GrayLoadVars(DstRead, pDstInfo);

    if (checkSameLut(SrcReadLut, DstReadLut, pSrcInfo, pDstInfo)) {
        do {
            memcpy(dstBase, srcBase, width);
            srcBase = PtrAddBytes(srcBase, srcScan);
            dstBase = PtrAddBytes(dstBase, dstScan);
        } while (--height > 0);
    } else {
        DeclareIndex8GrayStoreVars(DstWrite);
        InitIndex8GrayStoreVarsY(DstWrite, pDstInfo);

        BlitLoopWidthHeight(Index8Gray, pSrc, srcBase, pSrcInfo,
                            Index8Gray, pDst, dstBase, pDstInfo, DstWrite,
                            width, height,
                            ConvertVia1ByteGray
                                (pSrc, Index8Gray, SrcRead,
                                 pDst, Index8Gray, DstWrite, 0, 0));
    }
}

void NAME_SCALE_BLIT(Index8Gray, Index8Gray)
    (void *srcBase, void *dstBase,
     juint width, juint height,
     jint sxloc, jint syloc,
     jint sxinc, jint syinc, jint shift,
     SurfaceDataRasInfo *pSrcInfo,
     SurfaceDataRasInfo *pDstInfo,
     NativePrimitive *pPrim,
     CompositeInfo *pCompInfo)
{
    DeclareIndex8GrayLoadVars(SrcRead)
    DeclareIndex8GrayLoadVars(DstRead)
    jint srcScan = pSrcInfo->scanStride;
    jint dstScan = pDstInfo->scanStride;
    DeclareIndex8GrayStoreVars(DstWrite)

    InitIndex8GrayLoadVars(SrcRead, pSrcInfo);
    InitIndex8GrayLoadVars(DstRead, pDstInfo);

    if (checkSameLut(SrcReadLut, DstReadLut, pSrcInfo, pDstInfo)) {
        BlitLoopScaleWidthHeight(Index8Gray, pSrc, srcBase, pSrcInfo,
                                 Index8Gray, pDst, dstBase, pDstInfo, DstWrite,
                                 x, width, height,
                                 sxloc, syloc, sxinc, syinc, shift,
                                 pDst[0] = pSrc[x]);
    } else {
        DeclareIndex8GrayStoreVars(DstWrite);
        InitIndex8GrayStoreVarsY(DstWrite, pDstInfo);
        BlitLoopScaleWidthHeight(Index8Gray, pSrc, srcBase, pSrcInfo,
                                 Index8Gray, pDst, dstBase, pDstInfo, DstWrite,
                                 x, width, height,
                                 sxloc, syloc, sxinc, syinc, shift,
                                 ConvertVia1ByteGray(pSrc, Index8Gray, SrcRead,
                                                     pDst, Index8Gray, DstWrite,
                                                     x, 0));
    }
}

DEFINE_SCALE_BLIT(IntArgb, Index8Gray, 3ByteRgb)

DEFINE_SCALE_BLIT(ThreeByteBgr, Index8Gray, 3ByteRgb)

DEFINE_SCALE_BLIT(UshortGray, Index8Gray, 1ByteGray)

DEFINE_SCALE_BLIT_LUT8(ByteIndexed, Index8Gray, PreProcessLut)

DEFINE_SCALE_BLIT(ByteGray, Index8Gray, 1ByteGray)

DEFINE_SCALE_BLIT(Index12Gray, Index8Gray, 1ByteGray)

DEFINE_XPAR_CONVERT_BLIT_LUT8(ByteIndexedBm, Index8Gray, PreProcessLut)

DEFINE_XPAR_BLITBG_LUT8(ByteIndexedBm, Index8Gray, PreProcessLut)

DEFINE_XOR_BLIT(IntArgb, Index8Gray, AnyByte)

DEFINE_ALPHA_MASKFILL(Index8Gray, 1ByteGray)

DEFINE_ALPHA_MASKBLIT(IntArgb, Index8Gray, 1ByteGray)

DEFINE_ALPHA_MASKBLIT(IntArgbPre, Index8Gray, 1ByteGray)

DEFINE_ALPHA_MASKBLIT(IntRgb, Index8Gray, 1ByteGray)

DEFINE_SRCOVER_MASKFILL(Index8Gray, 1ByteGray)

DEFINE_SRCOVER_MASKBLIT(IntArgb, Index8Gray, 1ByteGray)

DEFINE_SRCOVER_MASKBLIT(IntArgbPre, Index8Gray, 1ByteGray)

DEFINE_SOLID_DRAWGLYPHLISTAA(Index8Gray, 1ByteGray)

DEFINE_TRANSFORMHELPERS(Index8Gray)
