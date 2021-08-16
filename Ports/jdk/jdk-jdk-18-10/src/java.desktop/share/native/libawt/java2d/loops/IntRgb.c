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
#include "IntRgb.h"
#include "AlphaMacros.h"

#include "IntArgb.h"
#include "IntArgbBm.h"
#include "IntArgbPre.h"
#include "ThreeByteBgr.h"
#include "ByteGray.h"
#include "Index12Gray.h"

/*
 * This file declares, registers, and defines the various graphics
 * primitive loops to manipulate surfaces of type "IntRgb".
 *
 * See also LoopMacros.h
 */

RegisterFunc RegisterIntRgb;

DECLARE_CONVERT_BLIT(IntRgb, IntArgb);
DECLARE_CONVERT_BLIT(ThreeByteBgr, IntRgb);
DECLARE_CONVERT_BLIT(ByteGray, IntRgb);
DECLARE_CONVERT_BLIT(Index12Gray, IntArgb);

DECLARE_XPAR_CONVERT_BLIT(IntArgbBm, IntRgb);
DECLARE_XPAR_BLITBG(IntArgbBm, IntRgb);

DECLARE_SCALE_BLIT(IntRgb, IntArgb);
DECLARE_SCALE_BLIT(ThreeByteBgr, IntRgb);
DECLARE_SCALE_BLIT(ByteGray, IntRgb);
DECLARE_SCALE_BLIT(Index12Gray, IntArgb);

DECLARE_XOR_BLIT(IntArgb, IntRgb);
DECLARE_SRC_MASKFILL(IntRgb);
DECLARE_SRCOVER_MASKFILL(IntRgb);
DECLARE_ALPHA_MASKFILL(IntRgb);
DECLARE_SRCOVER_MASKBLIT(IntArgb, IntRgb);
DECLARE_ALPHA_MASKBLIT(IntArgb, IntRgb);
DECLARE_SRCOVER_MASKBLIT(IntArgbPre, IntRgb);
DECLARE_ALPHA_MASKBLIT(IntArgbPre, IntRgb);
DECLARE_ALPHA_MASKBLIT(IntRgb, IntRgb);
DECLARE_SOLID_DRAWGLYPHLISTAA(IntRgb);
DECLARE_SOLID_DRAWGLYPHLISTLCD(IntRgb);
DECLARE_XPAR_SCALE_BLIT(IntArgbBm, IntArgb);

DECLARE_TRANSFORMHELPER_FUNCS(IntRgb);

NativePrimitive IntRgbPrimitives[] = {
    REGISTER_ANYINT_ISOCOPY_BLIT(IntRgb),
    REGISTER_ANYINT_ISOSCALE_BLIT(IntRgb),
    REGISTER_ANYINT_ISOXOR_BLIT(IntRgb),
    REGISTER_CONVERT_BLIT(IntRgb, IntArgb),
    REGISTER_CONVERT_BLIT_EQUIV(IntArgb, IntRgb,
                                NAME_ISOCOPY_BLIT(AnyInt)),
    REGISTER_CONVERT_BLIT_EQUIV(IntArgbBm, IntRgb,
                                NAME_ISOCOPY_BLIT(AnyInt)),
    REGISTER_CONVERT_BLIT(ThreeByteBgr, IntRgb),
    REGISTER_CONVERT_BLIT(ByteGray, IntRgb),
    REGISTER_CONVERT_BLIT_EQUIV(ByteIndexed, IntRgb,
                                NAME_CONVERT_BLIT(ByteIndexed, IntArgb)),
    REGISTER_CONVERT_BLIT_EQUIV(Index12Gray, IntRgb,
                                NAME_CONVERT_BLIT(Index12Gray, IntArgb)),
    REGISTER_SCALE_BLIT(IntRgb, IntArgb),
    REGISTER_SCALE_BLIT_EQUIV(IntArgb, IntRgb,
                              NAME_ISOSCALE_BLIT(AnyInt)),
    REGISTER_SCALE_BLIT_EQUIV(IntArgbBm, IntRgb,
                              NAME_ISOSCALE_BLIT(AnyInt)),
    REGISTER_SCALE_BLIT(ThreeByteBgr, IntRgb),
    REGISTER_SCALE_BLIT(ByteGray, IntRgb),
    REGISTER_SCALE_BLIT_EQUIV(ByteIndexed, IntRgb,
                              NAME_SCALE_BLIT(ByteIndexed, IntArgb)),
    REGISTER_SCALE_BLIT_EQUIV(Index12Gray, IntRgb,
                              NAME_SCALE_BLIT(Index12Gray, IntArgb)),
    REGISTER_XPAR_CONVERT_BLIT(IntArgbBm, IntRgb),
    REGISTER_XPAR_CONVERT_BLIT_EQUIV(ByteIndexedBm, IntRgb,
                                     NAME_XPAR_CONVERT_BLIT(ByteIndexedBm,
                                                            IntArgb)),
    REGISTER_XPAR_SCALE_BLIT_EQUIV(ByteIndexedBm, IntRgb,
                                   NAME_XPAR_SCALE_BLIT(ByteIndexedBm,
                                                        IntArgb)),
    REGISTER_XPAR_SCALE_BLIT_EQUIV(IntArgbBm, IntRgb,
                                   NAME_XPAR_SCALE_BLIT(IntArgbBm,
                                                        IntArgb)),
    REGISTER_XPAR_BLITBG(IntArgbBm, IntRgb),
    REGISTER_XPAR_BLITBG_EQUIV(ByteIndexedBm, IntRgb,
                               NAME_XPAR_BLITBG(ByteIndexedBm, IntArgb)),

    REGISTER_XOR_BLIT(IntArgb, IntRgb),
    REGISTER_SRC_MASKFILL(IntRgb),
    REGISTER_SRCOVER_MASKFILL(IntRgb),
    REGISTER_ALPHA_MASKFILL(IntRgb),
    REGISTER_SRCOVER_MASKBLIT(IntArgb, IntRgb),
    REGISTER_ALPHA_MASKBLIT(IntArgb, IntRgb),
    REGISTER_SRCOVER_MASKBLIT(IntArgbPre, IntRgb),
    REGISTER_ALPHA_MASKBLIT(IntArgbPre, IntRgb),
    REGISTER_ALPHA_MASKBLIT(IntRgb, IntRgb),
    REGISTER_SOLID_DRAWGLYPHLISTAA(IntRgb),
    REGISTER_SOLID_DRAWGLYPHLISTLCD(IntRgb),

    REGISTER_TRANSFORMHELPER_FUNCS(IntRgb),
};

jboolean RegisterIntRgb(JNIEnv *env)
{
    return RegisterPrimitives(env, IntRgbPrimitives,
                              ArraySize(IntRgbPrimitives));
}

DEFINE_CONVERT_BLIT(IntRgb, IntArgb, 1IntRgb)

DEFINE_CONVERT_BLIT(ThreeByteBgr, IntRgb, 1IntRgb)

DEFINE_CONVERT_BLIT(ByteGray, IntRgb, 1IntRgb)

DEFINE_XPAR_CONVERT_BLIT(IntArgbBm, IntRgb, 1IntRgb)

DEFINE_XPAR_BLITBG(IntArgbBm, IntRgb, 1IntRgb)

DEFINE_SCALE_BLIT(IntRgb, IntArgb, 1IntRgb)

DEFINE_SCALE_BLIT(ThreeByteBgr, IntRgb, 1IntRgb)

DEFINE_SCALE_BLIT(ByteGray, IntRgb, 1IntRgb)

DEFINE_XOR_BLIT(IntArgb, IntRgb, AnyInt)

DEFINE_SRC_MASKFILL(IntRgb, 4ByteArgb)

DEFINE_SRCOVER_MASKFILL(IntRgb, 4ByteArgb)

DEFINE_ALPHA_MASKFILL(IntRgb, 4ByteArgb)

DEFINE_SRCOVER_MASKBLIT(IntArgb, IntRgb, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntArgb, IntRgb, 4ByteArgb)

DEFINE_SRCOVER_MASKBLIT(IntArgbPre, IntRgb, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntArgbPre, IntRgb, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntRgb, IntRgb, 4ByteArgb)

DEFINE_SOLID_DRAWGLYPHLISTAA(IntRgb, 3ByteRgb)

DEFINE_SOLID_DRAWGLYPHLISTLCD(IntRgb, 3ByteRgb)

DEFINE_TRANSFORMHELPERS(IntRgb)
