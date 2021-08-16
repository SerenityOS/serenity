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

#include "AnyShort.h"
#include "Ushort555Rgbx.h"

#include "IntArgb.h"
#include "IntArgbBm.h"
#include "ThreeByteBgr.h"
#include "ByteGray.h"
#include "ByteIndexed.h"

/*
 * This file declares, registers, and defines the various graphics
 * primitive loops to manipulate surfaces of type "Ushort555Rgbx".
 *
 * See also LoopMacros.h
 */

RegisterFunc RegisterUshort555Rgbx;

DECLARE_CONVERT_BLIT(Ushort555Rgbx, IntArgb);
DECLARE_CONVERT_BLIT(IntArgb, Ushort555Rgbx);
DECLARE_CONVERT_BLIT(ThreeByteBgr, Ushort555Rgbx);
DECLARE_CONVERT_BLIT(ByteGray, Ushort555Rgbx);
DECLARE_CONVERT_BLIT(ByteIndexed, Ushort555Rgbx);
DECLARE_SCALE_BLIT(Ushort555Rgbx, IntArgb);
DECLARE_SCALE_BLIT(IntArgb, Ushort555Rgbx);
DECLARE_SCALE_BLIT(ThreeByteBgr, Ushort555Rgbx);
DECLARE_SCALE_BLIT(ByteGray, Ushort555Rgbx);
DECLARE_SCALE_BLIT(ByteIndexed, Ushort555Rgbx);
DECLARE_XPAR_CONVERT_BLIT(ByteIndexedBm, Ushort555Rgbx);
DECLARE_XPAR_SCALE_BLIT(ByteIndexedBm, Ushort555Rgbx);
DECLARE_XPAR_SCALE_BLIT(IntArgbBm, Ushort555Rgbx);
DECLARE_XPAR_BLITBG(ByteIndexedBm, Ushort555Rgbx);
DECLARE_XOR_BLIT(IntArgb, Ushort555Rgbx);
DECLARE_SOLID_DRAWGLYPHLISTAA(Ushort555Rgbx);
DECLARE_SOLID_DRAWGLYPHLISTLCD(Ushort555Rgbx);

NativePrimitive Ushort555RgbxPrimitives[] = {
    REGISTER_ANYSHORT_ISOCOPY_BLIT(Ushort555Rgbx),
    REGISTER_ANYSHORT_ISOSCALE_BLIT(Ushort555Rgbx),
    REGISTER_ANYSHORT_ISOXOR_BLIT(Ushort555Rgbx),
    REGISTER_CONVERT_BLIT(Ushort555Rgbx, IntArgb),
    REGISTER_CONVERT_BLIT(IntArgb, Ushort555Rgbx),
    REGISTER_CONVERT_BLIT_EQUIV(IntRgb, Ushort555Rgbx,
                                NAME_CONVERT_BLIT(IntArgb, Ushort555Rgbx)),
    REGISTER_CONVERT_BLIT(ThreeByteBgr, Ushort555Rgbx),
    REGISTER_CONVERT_BLIT(ByteGray, Ushort555Rgbx),
    REGISTER_CONVERT_BLIT(ByteIndexed, Ushort555Rgbx),
    REGISTER_SCALE_BLIT(Ushort555Rgbx, IntArgb),
    REGISTER_SCALE_BLIT(IntArgb, Ushort555Rgbx),
    REGISTER_SCALE_BLIT_EQUIV(IntRgb, Ushort555Rgbx,
                              NAME_SCALE_BLIT(IntArgb, Ushort555Rgbx)),
    REGISTER_SCALE_BLIT(ThreeByteBgr, Ushort555Rgbx),
    REGISTER_SCALE_BLIT(ByteGray, Ushort555Rgbx),
    REGISTER_SCALE_BLIT(ByteIndexed, Ushort555Rgbx),
    REGISTER_XPAR_CONVERT_BLIT(ByteIndexedBm, Ushort555Rgbx),
    REGISTER_XPAR_SCALE_BLIT(ByteIndexedBm, Ushort555Rgbx),
    REGISTER_XPAR_SCALE_BLIT(IntArgbBm, Ushort555Rgbx),
    REGISTER_XPAR_BLITBG(ByteIndexedBm, Ushort555Rgbx),
    REGISTER_XOR_BLIT(IntArgb, Ushort555Rgbx),
    REGISTER_SOLID_DRAWGLYPHLISTAA(Ushort555Rgbx),
    REGISTER_SOLID_DRAWGLYPHLISTLCD(Ushort555Rgbx),
};

jboolean RegisterUshort555Rgbx(JNIEnv *env)
{
    return RegisterPrimitives(env, Ushort555RgbxPrimitives,
                              ArraySize(Ushort555RgbxPrimitives));
}

jint PixelForUshort555Rgbx(SurfaceDataRasInfo *pRasInfo, jint rgb)
{
    return IntArgbToUshort555Rgbx(rgb);
}

DEFINE_CONVERT_BLIT(Ushort555Rgbx, IntArgb, 3ByteRgb)

DEFINE_CONVERT_BLIT(IntArgb, Ushort555Rgbx, 1IntRgb)

DEFINE_CONVERT_BLIT(ThreeByteBgr, Ushort555Rgbx, 3ByteRgb)

DEFINE_CONVERT_BLIT(ByteGray, Ushort555Rgbx, 3ByteRgb)

DEFINE_CONVERT_BLIT_LUT8(ByteIndexed, Ushort555Rgbx, PreProcessLut)

DEFINE_SCALE_BLIT(Ushort555Rgbx, IntArgb, 3ByteRgb)

DEFINE_SCALE_BLIT(IntArgb, Ushort555Rgbx, 1IntRgb)

DEFINE_SCALE_BLIT(ThreeByteBgr, Ushort555Rgbx, 3ByteRgb)

DEFINE_SCALE_BLIT(ByteGray, Ushort555Rgbx, 3ByteRgb)

DEFINE_SCALE_BLIT_LUT8(ByteIndexed, Ushort555Rgbx, PreProcessLut)

DEFINE_XPAR_CONVERT_BLIT_LUT8(ByteIndexedBm, Ushort555Rgbx, PreProcessLut)

DEFINE_XPAR_SCALE_BLIT_LUT8(ByteIndexedBm, Ushort555Rgbx, PreProcessLut)

DEFINE_XPAR_SCALE_BLIT(IntArgbBm, Ushort555Rgbx, 1IntRgb)

DEFINE_XPAR_BLITBG_LUT8(ByteIndexedBm, Ushort555Rgbx, PreProcessLut)

DEFINE_XOR_BLIT(IntArgb, Ushort555Rgbx, AnyShort)

DEFINE_SOLID_DRAWGLYPHLISTAA(Ushort555Rgbx, 3ByteRgb)

DEFINE_SOLID_DRAWGLYPHLISTLCD(Ushort555Rgbx, 3ByteRgb)
