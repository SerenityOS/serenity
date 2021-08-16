/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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

#include "ByteBinary2Bit.h"

#include "IntArgb.h"

/*
 * This file declares, registers, and defines the various graphics
 * primitive loops to manipulate surfaces of type "ByteBinary2Bit".
 *
 * See also LoopMacros.h
 */

RegisterFunc RegisterByteBinary2Bit;

DECLARE_SOLID_FILLRECT(ByteBinary2Bit);
DECLARE_SOLID_FILLSPANS(ByteBinary2Bit);
DECLARE_SOLID_DRAWLINE(ByteBinary2Bit);
DECLARE_XOR_FILLRECT(ByteBinary2Bit);
DECLARE_XOR_FILLSPANS(ByteBinary2Bit);
DECLARE_XOR_DRAWLINE(ByteBinary2Bit);
DECLARE_SOLID_DRAWGLYPHLIST(ByteBinary2Bit);
DECLARE_SOLID_DRAWGLYPHLISTAA(ByteBinary2Bit);
DECLARE_XOR_DRAWGLYPHLIST(ByteBinary2Bit);

DECLARE_CONVERT_BLIT(ByteBinary2Bit, ByteBinary2Bit);
DECLARE_CONVERT_BLIT(ByteBinary2Bit, IntArgb);
DECLARE_CONVERT_BLIT(IntArgb, ByteBinary2Bit);
DECLARE_XOR_BLIT(IntArgb, ByteBinary2Bit);

DECLARE_ALPHA_MASKBLIT(ByteBinary2Bit, IntArgb);
DECLARE_ALPHA_MASKBLIT(IntArgb, ByteBinary2Bit);
DECLARE_ALPHA_MASKFILL(ByteBinary2Bit);

NativePrimitive ByteBinary2BitPrimitives[] = {
    REGISTER_SOLID_FILLRECT(ByteBinary2Bit),
    REGISTER_SOLID_FILLSPANS(ByteBinary2Bit),
    REGISTER_SOLID_LINE_PRIMITIVES(ByteBinary2Bit),
    REGISTER_XOR_FILLRECT(ByteBinary2Bit),
    REGISTER_XOR_FILLSPANS(ByteBinary2Bit),
    REGISTER_XOR_LINE_PRIMITIVES(ByteBinary2Bit),
    REGISTER_SOLID_DRAWGLYPHLIST(ByteBinary2Bit),
    REGISTER_SOLID_DRAWGLYPHLISTAA(ByteBinary2Bit),
    REGISTER_XOR_DRAWGLYPHLIST(ByteBinary2Bit),

    REGISTER_CONVERT_BLIT(ByteBinary2Bit, ByteBinary2Bit),
    REGISTER_CONVERT_BLIT(ByteBinary2Bit, IntArgb),
    REGISTER_CONVERT_BLIT(IntArgb, ByteBinary2Bit),
    REGISTER_XOR_BLIT(IntArgb, ByteBinary2Bit),

    REGISTER_ALPHA_MASKBLIT(ByteBinary2Bit, IntArgb),
    REGISTER_ALPHA_MASKBLIT(IntArgb, ByteBinary2Bit),
    REGISTER_ALPHA_MASKFILL(ByteBinary2Bit),
};

jboolean RegisterByteBinary2Bit(JNIEnv *env)
{
    return RegisterPrimitives(env, ByteBinary2BitPrimitives,
                              ArraySize(ByteBinary2BitPrimitives));
}

DEFINE_BYTE_BINARY_SOLID_FILLRECT(ByteBinary2Bit)

DEFINE_BYTE_BINARY_SOLID_FILLSPANS(ByteBinary2Bit)

DEFINE_BYTE_BINARY_SOLID_DRAWLINE(ByteBinary2Bit)

DEFINE_BYTE_BINARY_XOR_FILLRECT(ByteBinary2Bit)

DEFINE_BYTE_BINARY_XOR_FILLSPANS(ByteBinary2Bit)

DEFINE_BYTE_BINARY_XOR_DRAWLINE(ByteBinary2Bit)

DEFINE_BYTE_BINARY_SOLID_DRAWGLYPHLIST(ByteBinary2Bit)

DEFINE_BYTE_BINARY_SOLID_DRAWGLYPHLISTAA(ByteBinary2Bit, 3ByteRgb)

DEFINE_BYTE_BINARY_XOR_DRAWGLYPHLIST(ByteBinary2Bit)

DEFINE_BYTE_BINARY_CONVERT_BLIT(ByteBinary2Bit, ByteBinary2Bit, 1IntRgb)

DEFINE_BYTE_BINARY_CONVERT_BLIT(ByteBinary2Bit, IntArgb, 1IntArgb)

DEFINE_BYTE_BINARY_CONVERT_BLIT(IntArgb, ByteBinary2Bit, 1IntRgb)

DEFINE_BYTE_BINARY_XOR_BLIT(IntArgb, ByteBinary2Bit)

DEFINE_BYTE_BINARY_ALPHA_MASKBLIT(ByteBinary2Bit, IntArgb, 4ByteArgb)

DEFINE_BYTE_BINARY_ALPHA_MASKBLIT(IntArgb, ByteBinary2Bit, 4ByteArgb)

DEFINE_BYTE_BINARY_ALPHA_MASKFILL(ByteBinary2Bit, 4ByteArgb)
