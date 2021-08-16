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
#include "FourByteAbgrPre.h"
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
 * primitive loops to manipulate surfaces of type "FourByteAbgrPre".
 *
 * See also LoopMacros.h
 */

RegisterFunc RegisterFourByteAbgrPre;

DECLARE_CONVERT_BLIT(FourByteAbgrPre, IntArgb);
DECLARE_CONVERT_BLIT(IntArgb, FourByteAbgrPre);
DECLARE_CONVERT_BLIT(IntRgb, FourByteAbgrPre);
DECLARE_CONVERT_BLIT(ThreeByteBgr, FourByteAbgrPre);
DECLARE_CONVERT_BLIT(ByteGray, FourByteAbgrPre);
DECLARE_CONVERT_BLIT(ByteIndexed, FourByteAbgrPre);
DECLARE_SCALE_BLIT(FourByteAbgrPre, IntArgb);
DECLARE_SCALE_BLIT(IntArgb, FourByteAbgrPre);
DECLARE_SCALE_BLIT(IntRgb, FourByteAbgrPre);
DECLARE_SCALE_BLIT(ThreeByteBgr, FourByteAbgrPre);
DECLARE_SCALE_BLIT(ByteGray, FourByteAbgrPre);
DECLARE_SCALE_BLIT(ByteIndexed, FourByteAbgrPre);
DECLARE_XPAR_CONVERT_BLIT(ByteIndexedBm, FourByteAbgrPre);
DECLARE_XPAR_SCALE_BLIT(ByteIndexedBm, FourByteAbgrPre);
DECLARE_XPAR_SCALE_BLIT(IntArgbBm, FourByteAbgrPre);
DECLARE_XPAR_BLITBG(ByteIndexedBm, FourByteAbgrPre);

DECLARE_XOR_BLIT(IntArgb, FourByteAbgrPre);
DECLARE_SRC_MASKFILL(FourByteAbgrPre);
DECLARE_SRCOVER_MASKFILL(FourByteAbgrPre);
DECLARE_ALPHA_MASKFILL(FourByteAbgrPre);
DECLARE_SRCOVER_MASKBLIT(IntArgb, FourByteAbgrPre);
DECLARE_ALPHA_MASKBLIT(IntArgb, FourByteAbgrPre);
DECLARE_SRCOVER_MASKBLIT(IntArgbPre, FourByteAbgrPre);
DECLARE_ALPHA_MASKBLIT(IntArgbPre, FourByteAbgrPre);
DECLARE_ALPHA_MASKBLIT(IntRgb, FourByteAbgrPre);
DECLARE_SOLID_DRAWGLYPHLISTAA(FourByteAbgrPre);
DECLARE_SOLID_DRAWGLYPHLISTLCD(FourByteAbgrPre);

DECLARE_TRANSFORMHELPER_FUNCS(FourByteAbgrPre);

NativePrimitive FourByteAbgrPrePrimitives[] = {
    REGISTER_ANY4BYTE_ISOCOPY_BLIT(FourByteAbgrPre),
    REGISTER_ANY4BYTE_ISOSCALE_BLIT(FourByteAbgrPre),
    REGISTER_CONVERT_BLIT(FourByteAbgrPre, IntArgb),
    REGISTER_CONVERT_BLIT(IntArgb, FourByteAbgrPre),
    REGISTER_CONVERT_BLIT(IntRgb, FourByteAbgrPre),
    REGISTER_CONVERT_BLIT(ThreeByteBgr, FourByteAbgrPre),
    REGISTER_CONVERT_BLIT(ByteGray, FourByteAbgrPre),
    REGISTER_CONVERT_BLIT(ByteIndexed, FourByteAbgrPre),
    REGISTER_SCALE_BLIT(FourByteAbgrPre, IntArgb),
    REGISTER_SCALE_BLIT(IntArgb, FourByteAbgrPre),
    REGISTER_SCALE_BLIT(IntRgb, FourByteAbgrPre),
    REGISTER_SCALE_BLIT(ThreeByteBgr, FourByteAbgrPre),
    REGISTER_SCALE_BLIT(ByteGray, FourByteAbgrPre),
    REGISTER_SCALE_BLIT(ByteIndexed, FourByteAbgrPre),
    REGISTER_XPAR_CONVERT_BLIT(ByteIndexedBm, FourByteAbgrPre),
    REGISTER_XPAR_SCALE_BLIT(ByteIndexedBm, FourByteAbgrPre),
    REGISTER_XPAR_SCALE_BLIT(IntArgbBm, FourByteAbgrPre),
    REGISTER_XPAR_BLITBG(ByteIndexedBm, FourByteAbgrPre),

    REGISTER_XOR_BLIT(IntArgb, FourByteAbgrPre),
    REGISTER_SRC_MASKFILL(FourByteAbgrPre),
    REGISTER_SRCOVER_MASKFILL(FourByteAbgrPre),
    REGISTER_ALPHA_MASKFILL(FourByteAbgrPre),
    REGISTER_SRCOVER_MASKBLIT(IntArgb, FourByteAbgrPre),
    REGISTER_ALPHA_MASKBLIT(IntArgb, FourByteAbgrPre),
    REGISTER_SRCOVER_MASKBLIT(IntArgbPre, FourByteAbgrPre),
    REGISTER_ALPHA_MASKBLIT(IntArgbPre, FourByteAbgrPre),
    REGISTER_ALPHA_MASKBLIT(IntRgb, FourByteAbgrPre),
    REGISTER_SOLID_DRAWGLYPHLISTAA(FourByteAbgrPre),
    REGISTER_SOLID_DRAWGLYPHLISTLCD(FourByteAbgrPre),

    REGISTER_TRANSFORMHELPER_FUNCS(FourByteAbgrPre),
};

jboolean RegisterFourByteAbgrPre(JNIEnv *env)
{
    return RegisterPrimitives(env, FourByteAbgrPrePrimitives,
                              ArraySize(FourByteAbgrPrePrimitives));
}

jint PixelForFourByteAbgrPre(SurfaceDataRasInfo *pRasInfo, jint rgb)
{
    jint a, r, g, b;
    if ((rgb >> 24) == -1) {
        return ((rgb << 8) | (((juint) rgb) >> 24));
    }
    ExtractIntDcmComponents1234(rgb, a, r, g, b);
    r = MUL8(a, r);
    g = MUL8(a, g);
    b = MUL8(a, b);
    return ComposeIntDcmComponents1234(r, g, b, a);
}

DEFINE_CONVERT_BLIT(FourByteAbgrPre, IntArgb, 1IntArgb)

DEFINE_CONVERT_BLIT(IntArgb, FourByteAbgrPre, 4ByteArgb)

DEFINE_CONVERT_BLIT(IntRgb, FourByteAbgrPre, 3ByteRgb)

DEFINE_CONVERT_BLIT(ThreeByteBgr, FourByteAbgrPre, 3ByteRgb)

DEFINE_CONVERT_BLIT(ByteGray, FourByteAbgrPre, 3ByteRgb)

DEFINE_CONVERT_BLIT_LUT8(ByteIndexed, FourByteAbgrPre, ConvertOnTheFly)

DEFINE_SCALE_BLIT(FourByteAbgrPre, IntArgb, 1IntArgb)

DEFINE_SCALE_BLIT(IntArgb, FourByteAbgrPre, 4ByteArgb)

DEFINE_SCALE_BLIT(IntRgb, FourByteAbgrPre, 3ByteRgb)

DEFINE_SCALE_BLIT(ThreeByteBgr, FourByteAbgrPre, 3ByteRgb)

DEFINE_SCALE_BLIT(ByteGray, FourByteAbgrPre, 3ByteRgb)

DEFINE_SCALE_BLIT_LUT8(ByteIndexed, FourByteAbgrPre, ConvertOnTheFly)

DEFINE_XPAR_CONVERT_BLIT_LUT8(ByteIndexedBm, FourByteAbgrPre,ConvertOnTheFly)

DEFINE_XPAR_SCALE_BLIT_LUT8(ByteIndexedBm, FourByteAbgrPre,ConvertOnTheFly)

DEFINE_XPAR_SCALE_BLIT(IntArgbBm, FourByteAbgrPre, 1IntRgb)

DEFINE_XPAR_BLITBG_LUT8(ByteIndexedBm, FourByteAbgrPre, ConvertOnTheFly)

DEFINE_XOR_BLIT(IntArgb, FourByteAbgrPre, Any4Byte)

DEFINE_SRC_MASKFILL(FourByteAbgrPre, 4ByteArgb)

DEFINE_SRCOVER_MASKFILL(FourByteAbgrPre, 4ByteArgb)

DEFINE_ALPHA_MASKFILL(FourByteAbgrPre, 4ByteArgb)

DEFINE_SRCOVER_MASKBLIT(IntArgb, FourByteAbgrPre, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntArgb, FourByteAbgrPre, 4ByteArgb)

DEFINE_SRCOVER_MASKBLIT(IntArgbPre, FourByteAbgrPre, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntArgbPre, FourByteAbgrPre, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntRgb, FourByteAbgrPre, 4ByteArgb)

DEFINE_SOLID_DRAWGLYPHLISTAA(FourByteAbgrPre, 4ByteArgb)

DEFINE_SOLID_DRAWGLYPHLISTLCD(FourByteAbgrPre, 4ByteArgb)

DEFINE_TRANSFORMHELPERS(FourByteAbgrPre)
