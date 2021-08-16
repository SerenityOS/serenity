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

#include "Any4Byte.h"
#include "FourByteAbgr.h"
#include "AlphaMacros.h"

#include "IntArgb.h"
#include "IntArgbBm.h"
#include "IntArgbPre.h"
#include "IntRgb.h"
#include "ThreeByteBgr.h"
#include "ByteGray.h"
#include "ByteIndexed.h"

/*
 * This file declares, registers, and defines the various graphics
 * primitive loops to manipulate surfaces of type "FourByteAbgr".
 *
 * See also LoopMacros.h
 */

RegisterFunc RegisterFourByteAbgr;

DECLARE_CONVERT_BLIT(FourByteAbgr, IntArgb);
DECLARE_CONVERT_BLIT(IntArgb, FourByteAbgr);
DECLARE_CONVERT_BLIT(IntRgb, FourByteAbgr);
DECLARE_CONVERT_BLIT(ThreeByteBgr, FourByteAbgr);
DECLARE_CONVERT_BLIT(ByteGray, FourByteAbgr);
DECLARE_CONVERT_BLIT(ByteIndexed, FourByteAbgr);
DECLARE_SCALE_BLIT(FourByteAbgr, IntArgb);
DECLARE_SCALE_BLIT(IntArgb, FourByteAbgr);
DECLARE_SCALE_BLIT(IntRgb, FourByteAbgr);
DECLARE_SCALE_BLIT(ThreeByteBgr, FourByteAbgr);
DECLARE_SCALE_BLIT(ByteGray, FourByteAbgr);
DECLARE_SCALE_BLIT(ByteIndexed, FourByteAbgr);
DECLARE_XPAR_CONVERT_BLIT(ByteIndexedBm, FourByteAbgr);
DECLARE_XPAR_SCALE_BLIT(ByteIndexedBm, FourByteAbgr);
DECLARE_XPAR_SCALE_BLIT(IntArgbBm, FourByteAbgr);
DECLARE_XPAR_BLITBG(ByteIndexedBm, FourByteAbgr);

DECLARE_XOR_BLIT(IntArgb, FourByteAbgr);
DECLARE_SRC_MASKFILL(FourByteAbgr);
DECLARE_SRCOVER_MASKFILL(FourByteAbgr);
DECLARE_ALPHA_MASKFILL(FourByteAbgr);
DECLARE_SRCOVER_MASKBLIT(IntArgb, FourByteAbgr);
DECLARE_ALPHA_MASKBLIT(IntArgb, FourByteAbgr);
DECLARE_SRCOVER_MASKBLIT(IntArgbPre, FourByteAbgr);
DECLARE_ALPHA_MASKBLIT(IntArgbPre, FourByteAbgr);
DECLARE_ALPHA_MASKBLIT(IntRgb, FourByteAbgr);
DECLARE_SOLID_DRAWGLYPHLISTAA(FourByteAbgr);
DECLARE_SOLID_DRAWGLYPHLISTLCD(FourByteAbgr);

DECLARE_TRANSFORMHELPER_FUNCS(FourByteAbgr);

NativePrimitive FourByteAbgrPrimitives[] = {
    REGISTER_ANY4BYTE_ISOCOPY_BLIT(FourByteAbgr),
    REGISTER_ANY4BYTE_ISOSCALE_BLIT(FourByteAbgr),
    REGISTER_CONVERT_BLIT(FourByteAbgr, IntArgb),
    REGISTER_CONVERT_BLIT(IntArgb, FourByteAbgr),
    REGISTER_CONVERT_BLIT(IntRgb, FourByteAbgr),
    REGISTER_CONVERT_BLIT(ThreeByteBgr, FourByteAbgr),
    REGISTER_CONVERT_BLIT(ByteGray, FourByteAbgr),
    REGISTER_CONVERT_BLIT(ByteIndexed, FourByteAbgr),
    REGISTER_SCALE_BLIT(FourByteAbgr, IntArgb),
    REGISTER_SCALE_BLIT(IntArgb, FourByteAbgr),
    REGISTER_SCALE_BLIT(IntRgb, FourByteAbgr),
    REGISTER_SCALE_BLIT(ThreeByteBgr, FourByteAbgr),
    REGISTER_SCALE_BLIT(ByteGray, FourByteAbgr),
    REGISTER_SCALE_BLIT(ByteIndexed, FourByteAbgr),
    REGISTER_XPAR_CONVERT_BLIT(ByteIndexedBm, FourByteAbgr),
    REGISTER_XPAR_SCALE_BLIT(ByteIndexedBm, FourByteAbgr),
    REGISTER_XPAR_SCALE_BLIT(IntArgbBm, FourByteAbgr),
    REGISTER_XPAR_BLITBG(ByteIndexedBm, FourByteAbgr),

    REGISTER_XOR_BLIT(IntArgb, FourByteAbgr),
    REGISTER_SRC_MASKFILL(FourByteAbgr),
    REGISTER_SRCOVER_MASKFILL(FourByteAbgr),
    REGISTER_ALPHA_MASKFILL(FourByteAbgr),
    REGISTER_SRCOVER_MASKBLIT(IntArgb, FourByteAbgr),
    REGISTER_ALPHA_MASKBLIT(IntArgb, FourByteAbgr),
    REGISTER_SRCOVER_MASKBLIT(IntArgbPre, FourByteAbgr),
    REGISTER_ALPHA_MASKBLIT(IntArgbPre, FourByteAbgr),
    REGISTER_ALPHA_MASKBLIT(IntRgb, FourByteAbgr),
    REGISTER_SOLID_DRAWGLYPHLISTAA(FourByteAbgr),
    REGISTER_SOLID_DRAWGLYPHLISTLCD(FourByteAbgr),

    REGISTER_TRANSFORMHELPER_FUNCS(FourByteAbgr),
};

jboolean RegisterFourByteAbgr(JNIEnv *env)
{
    return RegisterPrimitives(env, FourByteAbgrPrimitives,
                              ArraySize(FourByteAbgrPrimitives));
}

jint PixelForFourByteAbgr(SurfaceDataRasInfo *pRasInfo, jint rgb)
{
    return ((rgb << 8) | (((juint) rgb) >> 24));
}

DEFINE_CONVERT_BLIT(FourByteAbgr, IntArgb, 1IntArgb)

DEFINE_CONVERT_BLIT(IntArgb, FourByteAbgr, 4ByteArgb)

DEFINE_CONVERT_BLIT(IntRgb, FourByteAbgr, 3ByteRgb)

DEFINE_CONVERT_BLIT(ThreeByteBgr, FourByteAbgr, 3ByteRgb)

DEFINE_CONVERT_BLIT(ByteGray, FourByteAbgr, 3ByteRgb)

DEFINE_CONVERT_BLIT_LUT8(ByteIndexed, FourByteAbgr, ConvertOnTheFly)

DEFINE_SCALE_BLIT(FourByteAbgr, IntArgb, 1IntArgb)

DEFINE_SCALE_BLIT(IntArgb, FourByteAbgr, 4ByteArgb)

DEFINE_SCALE_BLIT(IntRgb, FourByteAbgr, 3ByteRgb)

DEFINE_SCALE_BLIT(ThreeByteBgr, FourByteAbgr, 3ByteRgb)

DEFINE_SCALE_BLIT(ByteGray, FourByteAbgr, 3ByteRgb)

DEFINE_SCALE_BLIT_LUT8(ByteIndexed, FourByteAbgr, ConvertOnTheFly)

DEFINE_XPAR_CONVERT_BLIT_LUT8(ByteIndexedBm, FourByteAbgr, ConvertOnTheFly)

DEFINE_XPAR_SCALE_BLIT_LUT8(ByteIndexedBm, FourByteAbgr, ConvertOnTheFly)

DEFINE_XPAR_SCALE_BLIT(IntArgbBm, FourByteAbgr, 1IntRgb)

DEFINE_XPAR_BLITBG_LUT8(ByteIndexedBm, FourByteAbgr, ConvertOnTheFly)

DEFINE_XOR_BLIT(IntArgb, FourByteAbgr, Any4Byte)

DEFINE_SRC_MASKFILL(FourByteAbgr, 4ByteArgb)

DEFINE_SRCOVER_MASKFILL(FourByteAbgr, 4ByteArgb)

DEFINE_ALPHA_MASKFILL(FourByteAbgr, 4ByteArgb)

DEFINE_SRCOVER_MASKBLIT(IntArgb, FourByteAbgr, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntArgb, FourByteAbgr, 4ByteArgb)

DEFINE_SRCOVER_MASKBLIT(IntArgbPre, FourByteAbgr, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntArgbPre, FourByteAbgr, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntRgb, FourByteAbgr, 4ByteArgb)

DEFINE_SOLID_DRAWGLYPHLISTAA(FourByteAbgr, 4ByteArgb)

DEFINE_SOLID_DRAWGLYPHLISTLCD(FourByteAbgr, 4ByteArgb)

DEFINE_TRANSFORMHELPERS(FourByteAbgr)
