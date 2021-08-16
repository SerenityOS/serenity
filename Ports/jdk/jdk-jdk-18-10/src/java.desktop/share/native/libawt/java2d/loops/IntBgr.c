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
#include "IntBgr.h"
#include "AlphaMacros.h"

#include "IntArgb.h"
#include "IntArgbPre.h"
#include "IntArgbBm.h"
#include "IntRgb.h"
#include "ThreeByteBgr.h"
#include "ByteIndexed.h"
#include "Index8Gray.h"
#include "Index12Gray.h"

/*
 * This file declares, registers, and defines the various graphics
 * primitive loops to manipulate surfaces of type "IntBgr".
 *
 * See also LoopMacros.h
 */

RegisterFunc RegisterIntBgr;

DECLARE_CONVERT_BLIT(IntBgr, IntArgb);
DECLARE_CONVERT_BLIT(IntArgb, IntBgr);
DECLARE_CONVERT_BLIT(ThreeByteBgr, IntBgr);
DECLARE_CONVERT_BLIT(ByteIndexed, IntBgr);
DECLARE_SCALE_BLIT(IntBgr, IntArgb);
DECLARE_SCALE_BLIT(IntArgb, IntBgr);
DECLARE_SCALE_BLIT(ThreeByteBgr, IntBgr);
DECLARE_SCALE_BLIT(ByteIndexed, IntBgr);
DECLARE_SCALE_BLIT(Index12Gray, IntArgb);
DECLARE_XPAR_CONVERT_BLIT(ByteIndexedBm, IntBgr);
DECLARE_XPAR_CONVERT_BLIT(IntArgbBm, IntBgr);
DECLARE_XPAR_SCALE_BLIT(IntArgbBm, IntBgr);
DECLARE_XPAR_SCALE_BLIT(ByteIndexedBm, IntBgr);
DECLARE_XPAR_BLITBG(ByteIndexedBm, IntBgr);
DECLARE_XPAR_BLITBG(IntArgbBm, IntBgr);

/* ByteGray and Index12Gray to IntRgb same as <>Gray to IntBgr... */
DECLARE_CONVERT_BLIT(ByteGray, IntRgb);
DECLARE_SCALE_BLIT(ByteGray, IntRgb);
DECLARE_CONVERT_BLIT(Index12Gray, IntArgb);

DECLARE_XOR_BLIT(IntArgb, IntBgr);
DECLARE_SRC_MASKFILL(IntBgr);
DECLARE_SRCOVER_MASKFILL(IntBgr);
DECLARE_ALPHA_MASKFILL(IntBgr);
DECLARE_SRCOVER_MASKBLIT(IntArgb, IntBgr);
DECLARE_ALPHA_MASKBLIT(IntArgb, IntBgr);
DECLARE_SRCOVER_MASKBLIT(IntArgbPre, IntBgr);
DECLARE_ALPHA_MASKBLIT(IntArgbPre, IntBgr);
DECLARE_ALPHA_MASKBLIT(IntRgb, IntBgr);
DECLARE_ALPHA_MASKBLIT(IntBgr, IntBgr);
DECLARE_SOLID_DRAWGLYPHLISTAA(IntBgr);
DECLARE_SOLID_DRAWGLYPHLISTLCD(IntBgr);

DECLARE_TRANSFORMHELPER_FUNCS(IntBgr);

NativePrimitive IntBgrPrimitives[] = {
    REGISTER_ANYINT_ISOCOPY_BLIT(IntBgr),
    REGISTER_ANYINT_ISOSCALE_BLIT(IntBgr),
    REGISTER_ANYINT_ISOXOR_BLIT(IntBgr),
    REGISTER_CONVERT_BLIT(IntBgr, IntArgb),
    REGISTER_CONVERT_BLIT(IntArgb, IntBgr),
    REGISTER_CONVERT_BLIT(ThreeByteBgr, IntBgr),
    REGISTER_CONVERT_BLIT_EQUIV(IntRgb, IntBgr,
                                NAME_CONVERT_BLIT(IntArgb, IntBgr)),
    REGISTER_CONVERT_BLIT_EQUIV(IntArgbBm, IntBgr,
                                NAME_CONVERT_BLIT(IntArgb, IntBgr)),
    REGISTER_CONVERT_BLIT_EQUIV(IntBgr, IntRgb,
                                NAME_CONVERT_BLIT(IntArgb, IntBgr)),
    REGISTER_CONVERT_BLIT(ByteIndexed, IntBgr),

    REGISTER_SCALE_BLIT(IntBgr, IntArgb),
    REGISTER_SCALE_BLIT(IntArgb, IntBgr),
    REGISTER_SCALE_BLIT(ThreeByteBgr, IntBgr),
    REGISTER_SCALE_BLIT_EQUIV(IntRgb, IntBgr,
                              NAME_SCALE_BLIT(IntArgb, IntBgr)),
    REGISTER_SCALE_BLIT_EQUIV(IntArgbBm, IntBgr,
                              NAME_SCALE_BLIT(IntArgb, IntBgr)),
    REGISTER_SCALE_BLIT_EQUIV(IntBgr, IntRgb,
                              NAME_SCALE_BLIT(IntArgb, IntBgr)),
    REGISTER_SCALE_BLIT_EQUIV(Index8Gray, IntBgr,
                              NAME_SCALE_BLIT(ByteIndexed, IntArgb)),
    REGISTER_SCALE_BLIT_EQUIV(Index12Gray, IntBgr,
                              NAME_SCALE_BLIT(Index12Gray, IntArgb)),
    REGISTER_SCALE_BLIT(ByteIndexed, IntBgr),
    REGISTER_XPAR_CONVERT_BLIT(ByteIndexedBm, IntBgr),
    REGISTER_XPAR_CONVERT_BLIT(IntArgbBm, IntBgr),
    REGISTER_XPAR_SCALE_BLIT(IntArgbBm, IntBgr),
    REGISTER_XPAR_SCALE_BLIT(ByteIndexedBm, IntBgr),
    REGISTER_XPAR_BLITBG(ByteIndexedBm, IntBgr),
    REGISTER_XPAR_BLITBG(IntArgbBm, IntBgr),

    REGISTER_CONVERT_BLIT_EQUIV(ByteGray, IntBgr,
                                NAME_CONVERT_BLIT(ByteGray, IntRgb)),
    REGISTER_SCALE_BLIT_EQUIV(ByteGray, IntBgr,
                              NAME_SCALE_BLIT(ByteGray, IntRgb)),
    REGISTER_CONVERT_BLIT_EQUIV(Index8Gray, IntBgr,
                                NAME_CONVERT_BLIT(ByteIndexed, IntArgb)),
    REGISTER_CONVERT_BLIT_EQUIV(Index12Gray, IntBgr,
                                NAME_CONVERT_BLIT(Index12Gray, IntArgb)),

    REGISTER_XOR_BLIT(IntArgb, IntBgr),
    REGISTER_SRC_MASKFILL(IntBgr),
    REGISTER_SRCOVER_MASKFILL(IntBgr),
    REGISTER_ALPHA_MASKFILL(IntBgr),
    REGISTER_SRCOVER_MASKBLIT(IntArgb, IntBgr),
    REGISTER_ALPHA_MASKBLIT(IntArgb, IntBgr),
    REGISTER_SRCOVER_MASKBLIT(IntArgbPre, IntBgr),
    REGISTER_ALPHA_MASKBLIT(IntArgbPre, IntBgr),
    REGISTER_ALPHA_MASKBLIT(IntRgb, IntBgr),
    REGISTER_ALPHA_MASKBLIT(IntBgr, IntBgr),
    REGISTER_SOLID_DRAWGLYPHLISTAA(IntBgr),
    REGISTER_SOLID_DRAWGLYPHLISTLCD(IntBgr),

    REGISTER_TRANSFORMHELPER_FUNCS(IntBgr),
};

jboolean RegisterIntBgr(JNIEnv *env)
{
    return RegisterPrimitives(env, IntBgrPrimitives,
                              ArraySize(IntBgrPrimitives));
}

jint PixelForIntBgr(SurfaceDataRasInfo *pRasInfo, jint rgb)
{
    return SwapIntDcmComponentsX123ToX321(rgb);
}

DEFINE_CONVERT_BLIT(IntBgr, IntArgb, 1IntRgb)

DEFINE_CONVERT_BLIT(IntArgb, IntBgr, 1IntRgb)

DEFINE_CONVERT_BLIT(ThreeByteBgr, IntBgr, 3ByteRgb)

DEFINE_CONVERT_BLIT_LUT8(ByteIndexed, IntBgr, PreProcessLut)

DEFINE_SCALE_BLIT(IntBgr, IntArgb, 1IntRgb)

DEFINE_SCALE_BLIT(IntArgb, IntBgr, 1IntRgb)

DEFINE_SCALE_BLIT(ThreeByteBgr, IntBgr, 3ByteRgb)

DEFINE_SCALE_BLIT_LUT8(ByteIndexed, IntBgr, PreProcessLut)

DEFINE_XPAR_CONVERT_BLIT_LUT8(ByteIndexedBm, IntBgr, PreProcessLut)

DEFINE_XPAR_CONVERT_BLIT(IntArgbBm, IntBgr, 1IntRgb)

DEFINE_XPAR_SCALE_BLIT(IntArgbBm, IntBgr, 1IntRgb)

DEFINE_XPAR_SCALE_BLIT_LUT8(ByteIndexedBm, IntBgr, PreProcessLut)

DEFINE_XPAR_BLITBG_LUT8(ByteIndexedBm, IntBgr, PreProcessLut)

DEFINE_XPAR_BLITBG(IntArgbBm, IntBgr, 1IntRgb)

DEFINE_XOR_BLIT(IntArgb, IntBgr, AnyInt)

DEFINE_SRC_MASKFILL(IntBgr, 4ByteArgb)

DEFINE_SRCOVER_MASKFILL(IntBgr, 4ByteArgb)

DEFINE_ALPHA_MASKFILL(IntBgr, 4ByteArgb)

DEFINE_SRCOVER_MASKBLIT(IntArgb, IntBgr, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntArgb, IntBgr, 4ByteArgb)

DEFINE_SRCOVER_MASKBLIT(IntArgbPre, IntBgr, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntArgbPre, IntBgr, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntRgb, IntBgr, 4ByteArgb)

DEFINE_ALPHA_MASKBLIT(IntBgr, IntBgr, 4ByteArgb)

DEFINE_SOLID_DRAWGLYPHLISTAA(IntBgr, 3ByteRgb)

DEFINE_SOLID_DRAWGLYPHLISTLCD(IntBgr, 3ByteRgb)

DEFINE_TRANSFORMHELPERS(IntBgr)
