/*
 * Copyright (c) 2001, 2005, Oracle and/or its affiliates. All rights reserved.
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
#include "IntArgbBm.h"
#include "AlphaMacros.h"

#include "IntArgb.h"
#include "IntArgbPre.h"
#include "ByteIndexed.h"

/*
 * This file declares, registers, and defines the various graphics
 * primitive loops to manipulate surfaces of type "IntArgbBm".
 *
 * See also LoopMacros.h
 */

RegisterFunc RegisterIntArgbBm;

DECLARE_CONVERT_BLIT(IntArgbBm, IntArgb);
DECLARE_CONVERT_BLIT(IntArgb, IntArgbBm);
DECLARE_CONVERT_BLIT(ByteIndexed, IntArgbBm);
DECLARE_XPAR_CONVERT_BLIT(ByteIndexedBm, IntArgbBm);

DECLARE_SCALE_BLIT(IntArgb, IntArgbBm);
DECLARE_SCALE_BLIT(ByteIndexed, IntArgbBm);
DECLARE_XPAR_SCALE_BLIT(ByteIndexedBm, IntArgbBm);

DECLARE_XPAR_BLITBG(ByteIndexedBm, IntArgbBm);

DECLARE_XOR_BLIT(IntArgb, IntArgbBm);
DECLARE_ALPHA_MASKFILL(IntArgbBm);
DECLARE_ALPHA_MASKBLIT(IntArgb, IntArgbBm);
DECLARE_ALPHA_MASKBLIT(IntArgbPre, IntArgbBm);
DECLARE_SOLID_DRAWGLYPHLISTAA(IntArgbBm);
DECLARE_SOLID_DRAWGLYPHLISTLCD(IntArgbBm);

DECLARE_TRANSFORMHELPER_FUNCS(IntArgbBm);

NativePrimitive IntArgbBmPrimitives[] = {
    REGISTER_ANYINT_ISOCOPY_BLIT(IntArgbBm),
    REGISTER_ANYINT_ISOSCALE_BLIT(IntArgbBm),
    REGISTER_ANYINT_ISOXOR_BLIT(IntArgbBm),
    REGISTER_CONVERT_BLIT(IntArgbBm, IntArgb),
    REGISTER_CONVERT_BLIT(IntArgb, IntArgbBm),
    REGISTER_CONVERT_BLIT(ByteIndexed, IntArgbBm),
    REGISTER_SCALE_BLIT(IntArgb, IntArgbBm),
    REGISTER_SCALE_BLIT(ByteIndexed, IntArgbBm),
    REGISTER_XPAR_CONVERT_BLIT(ByteIndexedBm, IntArgbBm),
    REGISTER_XPAR_SCALE_BLIT(ByteIndexedBm, IntArgbBm),
    REGISTER_XPAR_BLITBG(ByteIndexedBm, IntArgbBm),

    REGISTER_XOR_BLIT(IntArgb, IntArgbBm),
    REGISTER_ALPHA_MASKFILL(IntArgbBm),
    REGISTER_ALPHA_MASKBLIT(IntArgb, IntArgbBm),
    REGISTER_ALPHA_MASKBLIT(IntArgbPre, IntArgbBm),
    REGISTER_SOLID_DRAWGLYPHLISTAA(IntArgbBm),
    REGISTER_SOLID_DRAWGLYPHLISTLCD(IntArgbBm),

    REGISTER_TRANSFORMHELPER_FUNCS(IntArgbBm),
};

jboolean RegisterIntArgbBm(JNIEnv *env)
{
    return RegisterPrimitives(env, IntArgbBmPrimitives,
                              ArraySize(IntArgbBmPrimitives));
}

jint PixelForIntArgbBm(SurfaceDataRasInfo *pRasInfo, jint rgb)
{
    return (rgb | ((rgb >> 31) << 24));
}

DEFINE_CONVERT_BLIT(IntArgbBm, IntArgb, 1IntArgb)

DEFINE_CONVERT_BLIT(IntArgb, IntArgbBm, 1IntArgb)

DEFINE_CONVERT_BLIT(ByteIndexed, IntArgbBm, 1IntArgb)

DEFINE_SCALE_BLIT(IntArgb, IntArgbBm, 1IntArgb)

DEFINE_SCALE_BLIT(ByteIndexed, IntArgbBm, 1IntArgb)

DEFINE_XPAR_CONVERT_BLIT_LUT8(ByteIndexedBm, IntArgbBm, PreProcessLut)

DEFINE_XPAR_SCALE_BLIT_LUT8(ByteIndexedBm, IntArgbBm, PreProcessLut)

DEFINE_XPAR_BLITBG_LUT8(ByteIndexedBm, IntArgbBm, PreProcessLut)

DEFINE_XOR_BLIT(IntArgb, IntArgbBm, AnyInt)

DEFINE_ALPHA_MASKFILL(IntArgbBm, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntArgb, IntArgbBm, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntArgbPre, IntArgbBm, 4ByteArgb)

DEFINE_SOLID_DRAWGLYPHLISTAA(IntArgbBm, 4ByteArgb)

DEFINE_SOLID_DRAWGLYPHLISTLCD(IntArgbBm, 4ByteArgb)

DEFINE_TRANSFORMHELPERS(IntArgbBm)
