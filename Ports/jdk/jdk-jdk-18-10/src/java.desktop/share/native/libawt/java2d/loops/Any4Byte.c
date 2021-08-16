/*
 * Copyright (c) 2000, 2010, Oracle and/or its affiliates. All rights reserved.
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

#include <string.h>

#include "Any4Byte.h"

#include "AlphaMath.h"
#include "IntDcm.h"

/*
 * This file declares, registers, and defines the various graphics
 * primitive loops to manipulate surfaces of type "Any4Byte".
 *
 * See also LoopMacros.h
 */

RegisterFunc RegisterAny4Byte;

DECLARE_SOLID_FILLRECT(Any4Byte);
DECLARE_SOLID_FILLSPANS(Any4Byte);
DECLARE_SOLID_PARALLELOGRAM(Any4Byte);
DECLARE_SOLID_DRAWLINE(Any4Byte);
DECLARE_XOR_FILLRECT(Any4Byte);
DECLARE_XOR_FILLSPANS(Any4Byte);
DECLARE_XOR_DRAWLINE(Any4Byte);
DECLARE_SOLID_DRAWGLYPHLIST(Any4Byte);
DECLARE_XOR_DRAWGLYPHLIST(Any4Byte);

NativePrimitive Any4BytePrimitives[] = {
    REGISTER_SOLID_FILLRECT(Any4Byte),
    REGISTER_SOLID_FILLSPANS(Any4Byte),
    REGISTER_SOLID_PARALLELOGRAM(Any4Byte),
    REGISTER_SOLID_LINE_PRIMITIVES(Any4Byte),
    REGISTER_XOR_FILLRECT(Any4Byte),
    REGISTER_XOR_FILLSPANS(Any4Byte),
    REGISTER_XOR_LINE_PRIMITIVES(Any4Byte),
    REGISTER_SOLID_DRAWGLYPHLIST(Any4Byte),
    REGISTER_XOR_DRAWGLYPHLIST(Any4Byte),
};

jboolean RegisterAny4Byte(JNIEnv *env)
{
    return RegisterPrimitives(env, Any4BytePrimitives,
                              ArraySize(Any4BytePrimitives));
}

DEFINE_ISOCOPY_BLIT(Any4Byte)

DEFINE_ISOSCALE_BLIT(Any4Byte)

DEFINE_ISOXOR_BLIT(Any4Byte)

DEFINE_SOLID_FILLRECT(Any4Byte)

DEFINE_SOLID_FILLSPANS(Any4Byte)

DEFINE_SOLID_PARALLELOGRAM(Any4Byte)

DEFINE_SOLID_DRAWLINE(Any4Byte)

DEFINE_XOR_FILLRECT(Any4Byte)

DEFINE_XOR_FILLSPANS(Any4Byte)

DEFINE_XOR_DRAWLINE(Any4Byte)

DEFINE_SOLID_DRAWGLYPHLIST(Any4Byte)

DEFINE_XOR_DRAWGLYPHLIST(Any4Byte)
