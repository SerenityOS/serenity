/*
 * Copyright (c) 2000, 2004, Oracle and/or its affiliates. All rights reserved.
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

#include "AnyShort.h"
#include "UshortGray.h"
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
 * primitive loops to manipulate surfaces of type "UshortGray".
 *
 * See also LoopMacros.h
 */

RegisterFunc RegisterUshortGray;

DECLARE_CONVERT_BLIT(UshortGray, IntArgb);
DECLARE_CONVERT_BLIT(IntArgb, UshortGray);
DECLARE_CONVERT_BLIT(ThreeByteBgr, UshortGray);
DECLARE_CONVERT_BLIT(ByteGray, UshortGray);
DECLARE_CONVERT_BLIT(ByteIndexed, UshortGray);
DECLARE_SCALE_BLIT(UshortGray, IntArgb);
DECLARE_SCALE_BLIT(IntArgb, UshortGray);
DECLARE_SCALE_BLIT(ThreeByteBgr, UshortGray);
DECLARE_SCALE_BLIT(ByteGray, UshortGray);
DECLARE_SCALE_BLIT(ByteIndexed, UshortGray);
DECLARE_XPAR_CONVERT_BLIT(ByteIndexedBm, UshortGray);
DECLARE_XPAR_SCALE_BLIT(ByteIndexedBm, UshortGray);
DECLARE_XPAR_SCALE_BLIT(IntArgbBm, UshortGray);
DECLARE_XPAR_BLITBG(ByteIndexedBm, UshortGray);

DECLARE_XOR_BLIT(IntArgb, UshortGray);
DECLARE_SRC_MASKFILL(UshortGray);
DECLARE_SRCOVER_MASKFILL(UshortGray);
DECLARE_ALPHA_MASKFILL(UshortGray);
DECLARE_SRCOVER_MASKBLIT(IntArgb, UshortGray);
DECLARE_ALPHA_MASKBLIT(IntArgb, UshortGray);
DECLARE_SRCOVER_MASKBLIT(IntArgbPre, UshortGray);
DECLARE_ALPHA_MASKBLIT(IntArgbPre, UshortGray);
DECLARE_ALPHA_MASKBLIT(IntRgb, UshortGray);
DECLARE_SOLID_DRAWGLYPHLISTAA(UshortGray);

NativePrimitive UshortGrayPrimitives[] = {
    REGISTER_ANYSHORT_ISOCOPY_BLIT(UshortGray),
    REGISTER_ANYSHORT_ISOSCALE_BLIT(UshortGray),
    REGISTER_ANYSHORT_ISOXOR_BLIT(UshortGray),
    REGISTER_CONVERT_BLIT(UshortGray, IntArgb),
    REGISTER_CONVERT_BLIT(IntArgb, UshortGray),
    REGISTER_CONVERT_BLIT_EQUIV(IntRgb, UshortGray,
                                NAME_CONVERT_BLIT(IntArgb, UshortGray)),
    REGISTER_CONVERT_BLIT(ThreeByteBgr, UshortGray),
    REGISTER_CONVERT_BLIT(ByteGray, UshortGray),
    REGISTER_CONVERT_BLIT(ByteIndexed, UshortGray),
    REGISTER_SCALE_BLIT(UshortGray, IntArgb),
    REGISTER_SCALE_BLIT(IntArgb, UshortGray),
    REGISTER_SCALE_BLIT_EQUIV(IntRgb, UshortGray,
                              NAME_SCALE_BLIT(IntArgb, UshortGray)),
    REGISTER_SCALE_BLIT(ThreeByteBgr, UshortGray),
    REGISTER_SCALE_BLIT(ByteGray, UshortGray),
    REGISTER_SCALE_BLIT(ByteIndexed, UshortGray),
    REGISTER_XPAR_CONVERT_BLIT(ByteIndexedBm, UshortGray),
    REGISTER_XPAR_SCALE_BLIT(ByteIndexedBm, UshortGray),
    REGISTER_XPAR_SCALE_BLIT(IntArgbBm, UshortGray),
    REGISTER_XPAR_BLITBG(ByteIndexedBm, UshortGray),

    REGISTER_XOR_BLIT(IntArgb, UshortGray),
    REGISTER_SRC_MASKFILL(UshortGray),
    REGISTER_SRCOVER_MASKFILL(UshortGray),
    REGISTER_ALPHA_MASKFILL(UshortGray),
    REGISTER_SRCOVER_MASKBLIT(IntArgb, UshortGray),
    REGISTER_ALPHA_MASKBLIT(IntArgb, UshortGray),
    REGISTER_SRCOVER_MASKBLIT(IntArgbPre, UshortGray),
    REGISTER_ALPHA_MASKBLIT(IntArgbPre, UshortGray),
    REGISTER_ALPHA_MASKBLIT(IntRgb, UshortGray),
    REGISTER_SOLID_DRAWGLYPHLISTAA(UshortGray),
};

jboolean RegisterUshortGray(JNIEnv *env)
{
    return RegisterPrimitives(env, UshortGrayPrimitives,
                              ArraySize(UshortGrayPrimitives));
}

jint PixelForUshortGray(SurfaceDataRasInfo *pRasInfo, jint rgb)
{
    jint r, g, b;
    ExtractIntDcmComponentsX123(rgb, r, g, b);
    return ComposeUshortGrayFrom3ByteRgb(r, g, b);
}

DEFINE_CONVERT_BLIT(UshortGray, IntArgb, 1IntArgb)

DEFINE_CONVERT_BLIT(IntArgb, UshortGray, 3ByteRgb)

DEFINE_CONVERT_BLIT(ThreeByteBgr, UshortGray, 3ByteRgb)

DEFINE_CONVERT_BLIT(ByteGray, UshortGray, 1ByteGray)

DEFINE_CONVERT_BLIT_LUT8(ByteIndexed, UshortGray, PreProcessLut)

DEFINE_SCALE_BLIT(UshortGray, IntArgb, 1IntArgb)

DEFINE_SCALE_BLIT(IntArgb, UshortGray, 3ByteRgb)

DEFINE_SCALE_BLIT(ThreeByteBgr, UshortGray, 3ByteRgb)

DEFINE_SCALE_BLIT(ByteGray, UshortGray, 1ByteGray)

DEFINE_SCALE_BLIT_LUT8(ByteIndexed, UshortGray, PreProcessLut)

DEFINE_XPAR_CONVERT_BLIT_LUT8(ByteIndexedBm, UshortGray, PreProcessLut)

DEFINE_XPAR_SCALE_BLIT_LUT8(ByteIndexedBm, UshortGray, PreProcessLut)

DEFINE_XPAR_SCALE_BLIT(IntArgbBm, UshortGray, 1IntRgb)

DEFINE_XPAR_BLITBG_LUT8(ByteIndexedBm, UshortGray, PreProcessLut)

DEFINE_XOR_BLIT(IntArgb, UshortGray, AnyShort)

DEFINE_SRC_MASKFILL(UshortGray, 1ShortGray)

DEFINE_SRCOVER_MASKFILL(UshortGray, 1ShortGray)

DEFINE_ALPHA_MASKFILL(UshortGray, 1ShortGray)

DEFINE_SRCOVER_MASKBLIT(IntArgb, UshortGray, 1ShortGray)

DEFINE_ALPHA_MASKBLIT(IntArgb, UshortGray, 1ShortGray)

DEFINE_SRCOVER_MASKBLIT(IntArgbPre, UshortGray, 1ShortGray)

DEFINE_ALPHA_MASKBLIT(IntArgbPre, UshortGray, 1ShortGray)

DEFINE_ALPHA_MASKBLIT(IntRgb, UshortGray, 1ShortGray)

DEFINE_SOLID_DRAWGLYPHLISTAA(UshortGray, 1ShortGray)
