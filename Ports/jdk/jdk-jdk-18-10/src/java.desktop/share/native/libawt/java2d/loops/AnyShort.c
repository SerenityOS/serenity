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

#include "AnyShort.h"

/*
 * This file declares, registers, and defines the various graphics
 * primitive loops to manipulate surfaces of type "AnyShort".
 *
 * See also LoopMacros.h
 */

RegisterFunc RegisterAnyShort;

DECLARE_SOLID_FILLRECT(AnyShort);
DECLARE_SOLID_FILLSPANS(AnyShort);
DECLARE_SOLID_PARALLELOGRAM(AnyShort);
DECLARE_SOLID_DRAWLINE(AnyShort);
DECLARE_XOR_FILLRECT(AnyShort);
DECLARE_XOR_FILLSPANS(AnyShort);
DECLARE_XOR_DRAWLINE(AnyShort);
DECLARE_SOLID_DRAWGLYPHLIST(AnyShort);
DECLARE_XOR_DRAWGLYPHLIST(AnyShort);

NativePrimitive AnyShortPrimitives[] = {
    REGISTER_SOLID_FILLRECT(AnyShort),
    REGISTER_SOLID_FILLSPANS(AnyShort),
    REGISTER_SOLID_PARALLELOGRAM(AnyShort),
    REGISTER_SOLID_LINE_PRIMITIVES(AnyShort),
    REGISTER_XOR_FILLRECT(AnyShort),
    REGISTER_XOR_FILLSPANS(AnyShort),
    REGISTER_XOR_LINE_PRIMITIVES(AnyShort),
    REGISTER_SOLID_DRAWGLYPHLIST(AnyShort),
    REGISTER_XOR_DRAWGLYPHLIST(AnyShort),
};

jboolean RegisterAnyShort(JNIEnv *env)
{
    return RegisterPrimitives(env, AnyShortPrimitives,
                              ArraySize(AnyShortPrimitives));
}

DEFINE_ISOCOPY_BLIT(AnyShort)

DEFINE_ISOSCALE_BLIT(AnyShort)

DEFINE_ISOXOR_BLIT(AnyShort)

DEFINE_SOLID_FILLRECT(AnyShort)

DEFINE_SOLID_FILLSPANS(AnyShort)

DEFINE_SOLID_PARALLELOGRAM(AnyShort)

DEFINE_SOLID_DRAWLINE(AnyShort)

DEFINE_XOR_FILLRECT(AnyShort)

DEFINE_XOR_FILLSPANS(AnyShort)

DEFINE_XOR_DRAWLINE(AnyShort)

DEFINE_SOLID_DRAWGLYPHLIST(AnyShort)

DEFINE_XOR_DRAWGLYPHLIST(AnyShort)
