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

#include "AnyInt.h"
#include "IntArgbPre.h"
#include "AlphaMacros.h"

#include "IntArgb.h"
#include "IntRgb.h"
#include "ThreeByteBgr.h"
#include "ByteGray.h"
#include "ByteIndexed.h"

/*
 * This file declares, registers, and defines the various graphics
 * primitive loops to manipulate surfaces of type "IntArgbPre".
 *
 * See also LoopMacros.h
 */

RegisterFunc RegisterIntArgbPre;

DECLARE_CONVERT_BLIT(IntArgbPre, IntArgb);
DECLARE_CONVERT_BLIT(IntArgb, IntArgbPre);
DECLARE_CONVERT_BLIT(IntRgb, IntArgbPre);
DECLARE_CONVERT_BLIT(ThreeByteBgr, IntArgbPre);
DECLARE_CONVERT_BLIT(ByteGray, IntArgbPre);
DECLARE_CONVERT_BLIT(ByteIndexed, IntArgbPre);
DECLARE_SCALE_BLIT(IntArgbPre, IntArgb);
DECLARE_SCALE_BLIT(IntArgb, IntArgbPre);
DECLARE_SCALE_BLIT(IntRgb, IntArgbPre);
DECLARE_SCALE_BLIT(ThreeByteBgr, IntArgbPre);
DECLARE_SCALE_BLIT(ByteGray, IntArgbPre);
DECLARE_SCALE_BLIT(ByteIndexed, IntArgbPre);
DECLARE_XPAR_CONVERT_BLIT(ByteIndexedBm, IntArgbPre);
DECLARE_XPAR_SCALE_BLIT(ByteIndexedBm, IntArgbPre);
DECLARE_XPAR_BLITBG(ByteIndexedBm, IntArgbPre);

DECLARE_XOR_BLIT(IntArgb, IntArgbPre);
DECLARE_SRC_MASKFILL(IntArgbPre);
DECLARE_SRCOVER_MASKFILL(IntArgbPre);
DECLARE_ALPHA_MASKFILL(IntArgbPre);
DECLARE_SRCOVER_MASKBLIT(IntArgb, IntArgbPre);
DECLARE_ALPHA_MASKBLIT(IntArgb, IntArgbPre);
DECLARE_SRCOVER_MASKBLIT(IntArgbPre, IntArgbPre);
DECLARE_ALPHA_MASKBLIT(IntArgbPre, IntArgbPre);
DECLARE_ALPHA_MASKBLIT(IntRgb, IntArgbPre);
DECLARE_SOLID_DRAWGLYPHLISTAA(IntArgbPre);
DECLARE_SOLID_DRAWGLYPHLISTLCD(IntArgbPre);

DECLARE_TRANSFORMHELPER_FUNCS(IntArgbPre);

NativePrimitive IntArgbPrePrimitives[] = {
    REGISTER_ANYINT_ISOCOPY_BLIT(IntArgbPre),
    REGISTER_ANYINT_ISOSCALE_BLIT(IntArgbPre),
    REGISTER_CONVERT_BLIT(IntArgbPre, IntArgb),
    REGISTER_CONVERT_BLIT(IntArgb, IntArgbPre),
    REGISTER_CONVERT_BLIT(IntRgb, IntArgbPre),
    REGISTER_CONVERT_BLIT(ThreeByteBgr, IntArgbPre),
    REGISTER_CONVERT_BLIT(ByteGray, IntArgbPre),
    REGISTER_CONVERT_BLIT(ByteIndexed, IntArgbPre),
    REGISTER_SCALE_BLIT(IntArgbPre, IntArgb),
    REGISTER_SCALE_BLIT(IntArgb, IntArgbPre),
    REGISTER_SCALE_BLIT(IntRgb, IntArgbPre),
    REGISTER_SCALE_BLIT(ThreeByteBgr, IntArgbPre),
    REGISTER_SCALE_BLIT(ByteGray, IntArgbPre),
    REGISTER_SCALE_BLIT(ByteIndexed, IntArgbPre),
    REGISTER_XPAR_CONVERT_BLIT(ByteIndexedBm, IntArgbPre),
    REGISTER_XPAR_SCALE_BLIT(ByteIndexedBm, IntArgbPre),
    REGISTER_XPAR_BLITBG(ByteIndexedBm, IntArgbPre),

    REGISTER_XOR_BLIT(IntArgb, IntArgbPre),
    REGISTER_SRC_MASKFILL(IntArgbPre),
    REGISTER_SRCOVER_MASKFILL(IntArgbPre),
    REGISTER_ALPHA_MASKFILL(IntArgbPre),
    REGISTER_SRCOVER_MASKBLIT(IntArgb, IntArgbPre),
    REGISTER_ALPHA_MASKBLIT(IntArgb, IntArgbPre),
    REGISTER_SRCOVER_MASKBLIT(IntArgbPre, IntArgbPre),
    REGISTER_ALPHA_MASKBLIT(IntArgbPre, IntArgbPre),
    REGISTER_ALPHA_MASKBLIT(IntRgb, IntArgbPre),
    REGISTER_SOLID_DRAWGLYPHLISTAA(IntArgbPre),
    REGISTER_SOLID_DRAWGLYPHLISTLCD(IntArgbPre),

    REGISTER_TRANSFORMHELPER_FUNCS(IntArgbPre),
};

jboolean RegisterIntArgbPre(JNIEnv *env)
{
    return RegisterPrimitives(env, IntArgbPrePrimitives,
                              ArraySize(IntArgbPrePrimitives));
}

jint PixelForIntArgbPre(SurfaceDataRasInfo *pRasInfo, jint rgb)
{
    jint a, r, g, b;
    if (((rgb >> 24) + 1) == 0) {
        return rgb;
    }
    ExtractIntDcmComponents1234(rgb, a, r, g, b);
    r = MUL8(a, r);
    g = MUL8(a, g);
    b = MUL8(a, b);
    return ComposeIntDcmComponents1234(a, r, g, b);
}

DEFINE_CONVERT_BLIT(IntArgbPre, IntArgb, 1IntArgb)

DEFINE_CONVERT_BLIT(IntArgb, IntArgbPre, 1IntArgb)

DEFINE_CONVERT_BLIT(IntRgb, IntArgbPre, 1IntArgb)

DEFINE_CONVERT_BLIT(ThreeByteBgr, IntArgbPre, 1IntArgb)

DEFINE_CONVERT_BLIT(ByteGray, IntArgbPre, 1IntArgb)

DEFINE_CONVERT_BLIT_LUT8(ByteIndexed, IntArgbPre, ConvertOnTheFly)

DEFINE_SCALE_BLIT(IntArgbPre, IntArgb, 1IntArgb)

DEFINE_SCALE_BLIT(IntArgb, IntArgbPre, 1IntArgb)

DEFINE_SCALE_BLIT(IntRgb, IntArgbPre, 1IntArgb)

DEFINE_SCALE_BLIT(ThreeByteBgr, IntArgbPre, 1IntArgb)

DEFINE_SCALE_BLIT(ByteGray, IntArgbPre, 1IntArgb)

DEFINE_SCALE_BLIT_LUT8(ByteIndexed, IntArgbPre, ConvertOnTheFly)

DEFINE_XPAR_CONVERT_BLIT_LUT8(ByteIndexedBm, IntArgbPre, ConvertOnTheFly)

DEFINE_XPAR_SCALE_BLIT_LUT8(ByteIndexedBm, IntArgbPre, ConvertOnTheFly)

DEFINE_XPAR_BLITBG_LUT8(ByteIndexedBm, IntArgbPre, ConvertOnTheFly)

DEFINE_XOR_BLIT(IntArgb, IntArgbPre, AnyInt)

DEFINE_SRC_MASKFILL(IntArgbPre, 4ByteArgb)

DEFINE_SRCOVER_MASKFILL(IntArgbPre, 4ByteArgb)

DEFINE_ALPHA_MASKFILL(IntArgbPre, 4ByteArgb)

DEFINE_SRCOVER_MASKBLIT(IntArgb, IntArgbPre, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntArgb, IntArgbPre, 4ByteArgb)

DEFINE_SRCOVER_MASKBLIT(IntArgbPre, IntArgbPre, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntArgbPre, IntArgbPre, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntRgb, IntArgbPre, 4ByteArgb)

DEFINE_SOLID_DRAWGLYPHLISTAA(IntArgbPre, 4ByteArgb)

DEFINE_SOLID_DRAWGLYPHLISTLCD(IntArgbPre, 4ByteArgb)

DEFINE_TRANSFORMHELPERS(IntArgbPre)
