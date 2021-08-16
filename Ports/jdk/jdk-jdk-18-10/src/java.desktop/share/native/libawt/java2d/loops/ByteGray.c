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

#include "AnyByte.h"
#include "ByteGray.h"
#include "AlphaMacros.h"

#include "IntArgb.h"
#include "IntArgbBm.h"
#include "IntArgbPre.h"
#include "IntRgb.h"
#include "ThreeByteBgr.h"
#include "UshortGray.h"
#include "ByteIndexed.h"
#include "Index8Gray.h"
#include "Index12Gray.h"

/*
 * This file declares, registers, and defines the various graphics
 * primitive loops to manipulate surfaces of type "ByteGray".
 *
 * See also LoopMacros.h
 */

RegisterFunc RegisterByteGray;

DECLARE_CONVERT_BLIT(ByteGray, IntArgb);
DECLARE_CONVERT_BLIT(IntArgb, ByteGray);
DECLARE_CONVERT_BLIT(ThreeByteBgr, ByteGray);
DECLARE_CONVERT_BLIT(UshortGray, ByteGray);
DECLARE_CONVERT_BLIT(ByteIndexed, ByteGray);
DECLARE_CONVERT_BLIT(Index8Gray, ByteGray);
DECLARE_CONVERT_BLIT(Index12Gray, ByteGray);
DECLARE_SCALE_BLIT(ByteGray, IntArgb);
DECLARE_SCALE_BLIT(IntArgb, ByteGray);
DECLARE_SCALE_BLIT(ThreeByteBgr, ByteGray);
DECLARE_SCALE_BLIT(UshortGray, ByteGray);
DECLARE_SCALE_BLIT(ByteIndexed, ByteGray);
DECLARE_SCALE_BLIT(Index8Gray, ByteGray);
DECLARE_SCALE_BLIT(Index12Gray, ByteGray);
DECLARE_XPAR_CONVERT_BLIT(ByteIndexedBm, ByteGray);
DECLARE_XPAR_SCALE_BLIT(ByteIndexedBm, ByteGray);
DECLARE_XPAR_SCALE_BLIT(IntArgbBm, ByteGray);
DECLARE_XPAR_BLITBG(ByteIndexedBm, ByteGray);
DECLARE_XPAR_CONVERT_BLIT(IntArgbBm, ByteGray);
DECLARE_XPAR_BLITBG(IntArgbBm, ByteGray);

DECLARE_XOR_BLIT(IntArgb, ByteGray);
DECLARE_SRC_MASKFILL(ByteGray);
DECLARE_SRCOVER_MASKFILL(ByteGray);
DECLARE_ALPHA_MASKFILL(ByteGray);
DECLARE_SRCOVER_MASKBLIT(IntArgb, ByteGray);
DECLARE_ALPHA_MASKBLIT(IntArgb, ByteGray);
DECLARE_SRCOVER_MASKBLIT(IntArgbPre, ByteGray);
DECLARE_ALPHA_MASKBLIT(IntArgbPre, ByteGray);
DECLARE_ALPHA_MASKBLIT(IntRgb, ByteGray);
DECLARE_SOLID_DRAWGLYPHLISTAA(ByteGray);

DECLARE_TRANSFORMHELPER_FUNCS(ByteGray);

NativePrimitive ByteGrayPrimitives[] = {
    REGISTER_ANYBYTE_ISOCOPY_BLIT(ByteGray),
    REGISTER_ANYBYTE_ISOSCALE_BLIT(ByteGray),
    REGISTER_ANYBYTE_ISOXOR_BLIT(ByteGray),
    REGISTER_CONVERT_BLIT(ByteGray, IntArgb),
    REGISTER_CONVERT_BLIT(IntArgb, ByteGray),
    REGISTER_CONVERT_BLIT_EQUIV(IntRgb, ByteGray,
                                NAME_CONVERT_BLIT(IntArgb, ByteGray)),
    REGISTER_CONVERT_BLIT_EQUIV(IntArgbBm, ByteGray,
                                NAME_CONVERT_BLIT(IntArgb, ByteGray)),
    REGISTER_CONVERT_BLIT(ThreeByteBgr, ByteGray),
    REGISTER_CONVERT_BLIT(UshortGray, ByteGray),
    REGISTER_CONVERT_BLIT(ByteIndexed, ByteGray),
    REGISTER_CONVERT_BLIT(Index8Gray, ByteGray),
    REGISTER_CONVERT_BLIT(Index12Gray, ByteGray),
    REGISTER_SCALE_BLIT(ByteGray, IntArgb),
    REGISTER_SCALE_BLIT(IntArgb, ByteGray),
    REGISTER_SCALE_BLIT_EQUIV(IntRgb, ByteGray,
                              NAME_SCALE_BLIT(IntArgb, ByteGray)),
    REGISTER_SCALE_BLIT_EQUIV(IntArgbBm, ByteGray,
                              NAME_SCALE_BLIT(IntArgb, ByteGray)),
    REGISTER_SCALE_BLIT(ThreeByteBgr, ByteGray),
    REGISTER_SCALE_BLIT(UshortGray, ByteGray),
    REGISTER_SCALE_BLIT(ByteIndexed, ByteGray),
    REGISTER_SCALE_BLIT(Index8Gray, ByteGray),
    REGISTER_SCALE_BLIT(Index12Gray, ByteGray),
    REGISTER_XPAR_CONVERT_BLIT(ByteIndexedBm, ByteGray),
    REGISTER_XPAR_SCALE_BLIT(ByteIndexedBm, ByteGray),
    REGISTER_XPAR_SCALE_BLIT(IntArgbBm, ByteGray),
    REGISTER_XPAR_BLITBG(ByteIndexedBm, ByteGray),
    REGISTER_XPAR_CONVERT_BLIT(IntArgbBm, ByteGray),
    REGISTER_XPAR_BLITBG(IntArgbBm, ByteGray),

    REGISTER_XOR_BLIT(IntArgb, ByteGray),
    REGISTER_SRC_MASKFILL(ByteGray),
    REGISTER_SRCOVER_MASKFILL(ByteGray),
    REGISTER_ALPHA_MASKFILL(ByteGray),
    REGISTER_SRCOVER_MASKBLIT(IntArgb, ByteGray),
    REGISTER_ALPHA_MASKBLIT(IntArgb, ByteGray),
    REGISTER_SRCOVER_MASKBLIT(IntArgbPre, ByteGray),
    REGISTER_ALPHA_MASKBLIT(IntArgbPre, ByteGray),
    REGISTER_ALPHA_MASKBLIT(IntRgb, ByteGray),
    REGISTER_SOLID_DRAWGLYPHLISTAA(ByteGray),

    REGISTER_TRANSFORMHELPER_FUNCS(ByteGray),
};

jboolean RegisterByteGray(JNIEnv *env)
{
    return RegisterPrimitives(env, ByteGrayPrimitives,
                              ArraySize(ByteGrayPrimitives));
}

jint PixelForByteGray(SurfaceDataRasInfo *pRasInfo, jint rgb)
{
    jint r, g, b;
    ExtractIntDcmComponentsX123(rgb, r, g, b);
    return ComposeByteGrayFrom3ByteRgb(r, g, b);
}

DEFINE_CONVERT_BLIT(ByteGray, IntArgb, 1IntArgb)

DEFINE_CONVERT_BLIT(IntArgb, ByteGray, 3ByteRgb)

DEFINE_CONVERT_BLIT(ThreeByteBgr, ByteGray, 3ByteRgb)

DEFINE_CONVERT_BLIT(UshortGray, ByteGray, 1ByteGray)

DEFINE_CONVERT_BLIT_LUT8(ByteIndexed, ByteGray, PreProcessLut)

DEFINE_CONVERT_BLIT(Index8Gray, ByteGray, 1ByteGray)

DEFINE_CONVERT_BLIT(Index12Gray, ByteGray, 1ByteGray)

DEFINE_SCALE_BLIT(ByteGray, IntArgb, 1IntArgb)

DEFINE_SCALE_BLIT(IntArgb, ByteGray, 3ByteRgb)

DEFINE_SCALE_BLIT(ThreeByteBgr, ByteGray, 3ByteRgb)

DEFINE_SCALE_BLIT(UshortGray, ByteGray, 1ByteGray)

DEFINE_SCALE_BLIT(Index8Gray, ByteGray, 1ByteGray)

DEFINE_SCALE_BLIT(Index12Gray, ByteGray, 1ByteGray)

DEFINE_SCALE_BLIT_LUT8(ByteIndexed, ByteGray, PreProcessLut)

DEFINE_XPAR_CONVERT_BLIT_LUT8(ByteIndexedBm, ByteGray, PreProcessLut)

DEFINE_XPAR_SCALE_BLIT_LUT8(ByteIndexedBm, ByteGray, PreProcessLut)

DEFINE_XPAR_SCALE_BLIT(IntArgbBm, ByteGray, 1IntRgb)

DEFINE_XPAR_BLITBG_LUT8(ByteIndexedBm, ByteGray, PreProcessLut)

DEFINE_XPAR_CONVERT_BLIT(IntArgbBm, ByteGray, 1IntRgb)

DEFINE_XPAR_BLITBG(IntArgbBm, ByteGray, 1IntRgb)

DEFINE_XOR_BLIT(IntArgb, ByteGray, AnyByte)

DEFINE_SRC_MASKFILL(ByteGray, 1ByteGray)

DEFINE_SRCOVER_MASKFILL(ByteGray, 1ByteGray)

DEFINE_ALPHA_MASKFILL(ByteGray, 1ByteGray)

DEFINE_SRCOVER_MASKBLIT(IntArgb, ByteGray, 1ByteGray)

DEFINE_ALPHA_MASKBLIT(IntArgb, ByteGray, 1ByteGray)

DEFINE_SRCOVER_MASKBLIT(IntArgbPre, ByteGray, 1ByteGray)

DEFINE_ALPHA_MASKBLIT(IntArgbPre, ByteGray, 1ByteGray)

DEFINE_ALPHA_MASKBLIT(IntRgb, ByteGray, 1ByteGray)

DEFINE_SOLID_DRAWGLYPHLISTAA(ByteGray, 1ByteGray)

DEFINE_TRANSFORMHELPERS(ByteGray)
