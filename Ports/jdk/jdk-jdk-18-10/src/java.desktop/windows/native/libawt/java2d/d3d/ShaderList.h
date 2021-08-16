/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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

#ifndef ShaderList_h_Included
#define ShaderList_h_Included

#ifdef __cplusplus
extern "C" {
#endif

#include "jni.h"
#include "jlong.h"

typedef void (ShaderDisposeFunc)(jlong programID);

/**
 * The following structures are used to maintain a list of fragment program
 * objects and their associated attributes.  Each logical shader (e.g.
 * RadialGradientPaint shader, ConvolveOp shader) can have a number of
 * different variants depending on a number of factors, such as whether
 * antialiasing is enabled or the current composite mode.  Since the number
 * of possible combinations of these factors is in the hundreds, we need
 * some way to create fragment programs on an as-needed basis, and also
 * keep them in a limited sized cache to avoid creating too many objects.
 *
 * The ShaderInfo structure keeps a reference to the fragment program's
 * handle, as well as some other values that help differentiate one ShaderInfo
 * from another.  ShaderInfos can be chained together to form a linked list.
 *
 * The ShaderList structure acts as a cache for ShaderInfos, placing
 * most-recently used items at the front, and removing items from the
 * cache when its size exceeds the "maxItems" limit.
 */
typedef struct _ShaderInfo ShaderInfo;

typedef struct {
    ShaderInfo        *head;
    ShaderDisposeFunc *dispose;
    jint              maxItems;
} ShaderList;

struct _ShaderInfo {
    ShaderInfo  *next;
    jlong       programID;
    jint        compType;
    jint        compMode;
    jint        flags;
};

void ShaderList_AddProgram(ShaderList *programList,
                           jlong programID,
                           jint compType, jint compMode,
                           jint flags);
jlong ShaderList_FindProgram(ShaderList *programList,
                             jint compType, jint compMode,
                             jint flags);
void ShaderList_Dispose(ShaderList *programList);

#ifdef __cplusplus
};
#endif

#endif /* ShaderList_h_Included */
