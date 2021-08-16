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

#include "Any3Byte.h"
#include "ThreeByteBgr.h"
#include "AlphaMacros.h"

#include "IntArgb.h"
#include "IntArgbBm.h"
#include "IntArgbPre.h"
#include "IntRgb.h"
#include "ByteGray.h"
#include "ByteIndexed.h"

/*
 * This file declares, registers, and defines the various graphics
 * primitive loops to manipulate surfaces of type "ThreeByteBgr".
 *
 * See also LoopMacros.h
 */

RegisterFunc RegisterThreeByteBgr;

DECLARE_CONVERT_BLIT(ThreeByteBgr, IntArgb);
DECLARE_CONVERT_BLIT(IntArgb, ThreeByteBgr);
DECLARE_CONVERT_BLIT(ByteGray, ThreeByteBgr);
DECLARE_CONVERT_BLIT(ByteIndexed, ThreeByteBgr);
DECLARE_SCALE_BLIT(ThreeByteBgr, IntArgb);
DECLARE_SCALE_BLIT(IntArgb, ThreeByteBgr);
DECLARE_SCALE_BLIT(ByteGray, ThreeByteBgr);
DECLARE_SCALE_BLIT(ByteIndexed, ThreeByteBgr);
DECLARE_XPAR_CONVERT_BLIT(ByteIndexedBm, ThreeByteBgr);
DECLARE_XPAR_SCALE_BLIT(ByteIndexedBm, ThreeByteBgr);
DECLARE_XPAR_SCALE_BLIT(IntArgbBm, ThreeByteBgr);
DECLARE_XPAR_BLITBG(ByteIndexedBm, ThreeByteBgr);
DECLARE_XPAR_CONVERT_BLIT(IntArgbBm, ThreeByteBgr);
DECLARE_XPAR_BLITBG(IntArgbBm, ThreeByteBgr);

DECLARE_XOR_BLIT(IntArgb, ThreeByteBgr);
DECLARE_SRC_MASKFILL(ThreeByteBgr);
DECLARE_SRCOVER_MASKFILL(ThreeByteBgr);
DECLARE_ALPHA_MASKFILL(ThreeByteBgr);
DECLARE_SRCOVER_MASKBLIT(IntArgb, ThreeByteBgr);
DECLARE_ALPHA_MASKBLIT(IntArgb, ThreeByteBgr);
DECLARE_SRCOVER_MASKBLIT(IntArgbPre, ThreeByteBgr);
DECLARE_ALPHA_MASKBLIT(IntArgbPre, ThreeByteBgr);
DECLARE_ALPHA_MASKBLIT(IntRgb, ThreeByteBgr);
DECLARE_SOLID_DRAWGLYPHLISTAA(ThreeByteBgr);
DECLARE_SOLID_DRAWGLYPHLISTLCD(ThreeByteBgr);

DECLARE_TRANSFORMHELPER_FUNCS(ThreeByteBgr);

NativePrimitive ThreeByteBgrPrimitives[] = {
    REGISTER_ANY3BYTE_ISOCOPY_BLIT(ThreeByteBgr),
    REGISTER_ANY3BYTE_ISOSCALE_BLIT(ThreeByteBgr),
    REGISTER_ANY3BYTE_ISOXOR_BLIT(ThreeByteBgr),
    REGISTER_CONVERT_BLIT(ThreeByteBgr, IntArgb),
    REGISTER_CONVERT_BLIT(IntArgb, ThreeByteBgr),
    REGISTER_CONVERT_BLIT_EQUIV(IntRgb, ThreeByteBgr,
                                NAME_CONVERT_BLIT(IntArgb, ThreeByteBgr)),
    REGISTER_CONVERT_BLIT_EQUIV(IntArgbBm, ThreeByteBgr,
                                NAME_CONVERT_BLIT(IntArgb, ThreeByteBgr)),
    REGISTER_CONVERT_BLIT(ByteGray, ThreeByteBgr),
    REGISTER_CONVERT_BLIT(ByteIndexed, ThreeByteBgr),
    REGISTER_SCALE_BLIT(ThreeByteBgr, IntArgb),
    REGISTER_SCALE_BLIT(IntArgb, ThreeByteBgr),
    REGISTER_SCALE_BLIT_EQUIV(IntRgb, ThreeByteBgr,
                              NAME_SCALE_BLIT(IntArgb, ThreeByteBgr)),
    REGISTER_SCALE_BLIT_EQUIV(IntArgbBm, ThreeByteBgr,
                              NAME_SCALE_BLIT(IntArgb, ThreeByteBgr)),
    REGISTER_SCALE_BLIT(ByteGray, ThreeByteBgr),
    REGISTER_SCALE_BLIT(ByteIndexed, ThreeByteBgr),
    REGISTER_XPAR_CONVERT_BLIT(ByteIndexedBm, ThreeByteBgr),
    REGISTER_XPAR_SCALE_BLIT(ByteIndexedBm, ThreeByteBgr),
    REGISTER_XPAR_SCALE_BLIT(IntArgbBm, ThreeByteBgr),
    REGISTER_XPAR_BLITBG(ByteIndexedBm, ThreeByteBgr),
    REGISTER_XPAR_CONVERT_BLIT(IntArgbBm, ThreeByteBgr),
    REGISTER_XPAR_BLITBG(IntArgbBm, ThreeByteBgr),

    REGISTER_XOR_BLIT(IntArgb, ThreeByteBgr),
    REGISTER_SRC_MASKFILL(ThreeByteBgr),
    REGISTER_SRCOVER_MASKFILL(ThreeByteBgr),
    REGISTER_ALPHA_MASKFILL(ThreeByteBgr),
    REGISTER_SRCOVER_MASKBLIT(IntArgb, ThreeByteBgr),
    REGISTER_ALPHA_MASKBLIT(IntArgb, ThreeByteBgr),
    REGISTER_SRCOVER_MASKBLIT(IntArgbPre, ThreeByteBgr),
    REGISTER_ALPHA_MASKBLIT(IntArgbPre, ThreeByteBgr),
    REGISTER_ALPHA_MASKBLIT(IntRgb, ThreeByteBgr),
    REGISTER_SOLID_DRAWGLYPHLISTAA(ThreeByteBgr),
    REGISTER_SOLID_DRAWGLYPHLISTLCD(ThreeByteBgr),

    REGISTER_TRANSFORMHELPER_FUNCS(ThreeByteBgr),
};

jboolean RegisterThreeByteBgr(JNIEnv *env)
{
    return RegisterPrimitives(env, ThreeByteBgrPrimitives,
                              ArraySize(ThreeByteBgrPrimitives));
}

DEFINE_CONVERT_BLIT(ThreeByteBgr, IntArgb, 1IntArgb)

DEFINE_CONVERT_BLIT(IntArgb, ThreeByteBgr, 1IntRgb)

DEFINE_CONVERT_BLIT(ByteGray, ThreeByteBgr, 3ByteRgb)

DEFINE_CONVERT_BLIT_LUT8(ByteIndexed, ThreeByteBgr, ConvertOnTheFly)

DEFINE_SCALE_BLIT(ThreeByteBgr, IntArgb, 1IntArgb)

DEFINE_SCALE_BLIT(IntArgb, ThreeByteBgr, 1IntRgb)

DEFINE_SCALE_BLIT(ByteGray, ThreeByteBgr, 3ByteRgb)

DEFINE_SCALE_BLIT_LUT8(ByteIndexed, ThreeByteBgr, ConvertOnTheFly)

DEFINE_XPAR_CONVERT_BLIT_LUT8(ByteIndexedBm, ThreeByteBgr, ConvertOnTheFly)

DEFINE_XPAR_SCALE_BLIT_LUT8(ByteIndexedBm, ThreeByteBgr, ConvertOnTheFly)

DEFINE_XPAR_SCALE_BLIT(IntArgbBm, ThreeByteBgr, 1IntRgb)

DEFINE_XPAR_BLITBG_LUT8(ByteIndexedBm, ThreeByteBgr, ConvertOnTheFly)

DEFINE_XPAR_CONVERT_BLIT(IntArgbBm, ThreeByteBgr, 1IntRgb)

DEFINE_XPAR_BLITBG(IntArgbBm, ThreeByteBgr, 1IntRgb)

DEFINE_XOR_BLIT(IntArgb, ThreeByteBgr, Any3Byte)

DEFINE_SRC_MASKFILL(ThreeByteBgr, 4ByteArgb)

DEFINE_SRCOVER_MASKFILL(ThreeByteBgr, 4ByteArgb)

DEFINE_ALPHA_MASKFILL(ThreeByteBgr, 4ByteArgb)

DEFINE_SRCOVER_MASKBLIT(IntArgb, ThreeByteBgr, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntArgb, ThreeByteBgr, 4ByteArgb)

DEFINE_SRCOVER_MASKBLIT(IntArgbPre, ThreeByteBgr, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntArgbPre, ThreeByteBgr, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntRgb, ThreeByteBgr, 4ByteArgb)

DEFINE_SOLID_DRAWGLYPHLISTAA(ThreeByteBgr, 3ByteRgb)

DEFINE_SOLID_DRAWGLYPHLISTLCD(ThreeByteBgr, 3ByteRgb)

DEFINE_TRANSFORMHELPERS(ThreeByteBgr)
