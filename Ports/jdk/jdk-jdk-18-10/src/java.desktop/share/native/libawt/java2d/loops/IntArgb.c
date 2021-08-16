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
#include "IntArgb.h"
#include "IntArgbBm.h"
#include "AlphaMacros.h"

#include "IntArgbPre.h"
#include "IntRgb.h"
#include "ByteIndexed.h"
#include "Index12Gray.h"

/*
 * This file declares, registers, and defines the various graphics
 * primitive loops to manipulate surfaces of type "IntArgb".
 *
 * See also LoopMacros.h
 */

RegisterFunc RegisterIntArgb;

DECLARE_CONVERT_BLIT(Index12Gray, IntArgb);
DECLARE_XOR_BLIT(IntArgb, IntArgb);
DECLARE_SRC_MASKFILL(IntArgb);
DECLARE_SRCOVER_MASKFILL(IntArgb);
DECLARE_ALPHA_MASKFILL(IntArgb);
DECLARE_SRCOVER_MASKBLIT(IntArgb, IntArgb);
DECLARE_ALPHA_MASKBLIT(IntArgb, IntArgb);
DECLARE_SRCOVER_MASKBLIT(IntArgbPre, IntArgb);
DECLARE_ALPHA_MASKBLIT(IntArgbPre, IntArgb);
DECLARE_ALPHA_MASKBLIT(IntRgb, IntArgb);
DECLARE_SOLID_DRAWGLYPHLISTAA(IntArgb);
DECLARE_SOLID_DRAWGLYPHLISTLCD(IntArgb);
DECLARE_XPAR_SCALE_BLIT(IntArgbBm, IntArgb);

DECLARE_TRANSFORMHELPER_FUNCS(IntArgb);

NativePrimitive IntArgbPrimitives[] = {
    REGISTER_ANYINT_ISOCOPY_BLIT(IntArgb),
    REGISTER_ANYINT_ISOSCALE_BLIT(IntArgb),
    REGISTER_CONVERT_BLIT(ByteIndexed, IntArgb),
    REGISTER_CONVERT_BLIT(Index12Gray, IntArgb),
    REGISTER_SCALE_BLIT(ByteIndexed, IntArgb),
    REGISTER_XPAR_CONVERT_BLIT(ByteIndexedBm, IntArgb),
    REGISTER_XPAR_SCALE_BLIT(ByteIndexedBm, IntArgb),
    REGISTER_XPAR_SCALE_BLIT(IntArgbBm, IntArgb),
    REGISTER_XPAR_BLITBG(ByteIndexedBm, IntArgb),

    REGISTER_XOR_BLIT(IntArgb, IntArgb),
    REGISTER_SRC_MASKFILL(IntArgb),
    REGISTER_SRCOVER_MASKFILL(IntArgb),
    REGISTER_ALPHA_MASKFILL(IntArgb),
    REGISTER_SRCOVER_MASKBLIT(IntArgb, IntArgb),
    REGISTER_ALPHA_MASKBLIT(IntArgb, IntArgb),
    REGISTER_SRCOVER_MASKBLIT(IntArgbPre, IntArgb),
    REGISTER_ALPHA_MASKBLIT(IntArgbPre, IntArgb),
    REGISTER_ALPHA_MASKBLIT(IntRgb, IntArgb),
    REGISTER_SOLID_DRAWGLYPHLISTAA(IntArgb),
    REGISTER_SOLID_DRAWGLYPHLISTLCD(IntArgb),

    REGISTER_TRANSFORMHELPER_FUNCS(IntArgb),
};

jboolean RegisterIntArgb(JNIEnv *env)
{
    return RegisterPrimitives(env, IntArgbPrimitives,
                              ArraySize(IntArgbPrimitives));
}

DEFINE_CONVERT_BLIT_LUT8(ByteIndexed, IntArgb, ConvertOnTheFly)

DEFINE_CONVERT_BLIT_LUT8(Index12Gray, IntArgb, ConvertOnTheFly)

DEFINE_SCALE_BLIT_LUT8(ByteIndexed, IntArgb, ConvertOnTheFly)

DEFINE_XPAR_CONVERT_BLIT_LUT8(ByteIndexedBm, IntArgb, ConvertOnTheFly)

DEFINE_XPAR_SCALE_BLIT_LUT8(ByteIndexedBm, IntArgb, ConvertOnTheFly)

DEFINE_XPAR_SCALE_BLIT(IntArgbBm, IntArgb, 1IntRgb)

DEFINE_XPAR_BLITBG_LUT8(ByteIndexedBm, IntArgb, ConvertOnTheFly)

DEFINE_XOR_BLIT(IntArgb, IntArgb, AnyInt)

DEFINE_SRC_MASKFILL(IntArgb, 4ByteArgb)

DEFINE_SRCOVER_MASKFILL(IntArgb, 4ByteArgb)

DEFINE_ALPHA_MASKFILL(IntArgb, 4ByteArgb)

DEFINE_SRCOVER_MASKBLIT(IntArgb, IntArgb, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntArgb, IntArgb, 4ByteArgb)

DEFINE_SRCOVER_MASKBLIT(IntArgbPre, IntArgb, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntArgbPre, IntArgb, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntRgb, IntArgb, 4ByteArgb)

DEFINE_SOLID_DRAWGLYPHLISTAA(IntArgb, 4ByteArgb)

DEFINE_SOLID_DRAWGLYPHLISTLCD(IntArgb, 4ByteArgb)

DEFINE_TRANSFORMHELPERS(IntArgb)
