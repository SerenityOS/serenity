/*
 * Copyright (c) 1998, 1999, Oracle and/or its affiliates. All rights reserved.
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

#include "awt_Insets.h"
#include "awt.h"

/************************************************************************
 * AwtInsets fields
 */

jfieldID AwtInsets::leftID;
jfieldID AwtInsets::rightID;
jfieldID AwtInsets::topID;
jfieldID AwtInsets::bottomID;

/************************************************************************
 * AwtInsets native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_java_awt_Insets_initIDs(JNIEnv *env, jclass cls) {
    TRY;

    AwtInsets::leftID = env->GetFieldID(cls, "left", "I");
    DASSERT(AwtInsets::leftID != NULL);
    CHECK_NULL(AwtInsets::leftID);

    AwtInsets::rightID = env->GetFieldID(cls, "right", "I");
    DASSERT(AwtInsets::rightID != NULL);
    CHECK_NULL(AwtInsets::rightID);

    AwtInsets::topID = env->GetFieldID(cls, "top", "I");
    DASSERT(AwtInsets::topID != NULL);
    CHECK_NULL(AwtInsets::topID);

    AwtInsets::bottomID = env->GetFieldID(cls, "bottom", "I");
    DASSERT(AwtInsets::bottomID != NULL);
    CHECK_NULL(AwtInsets::bottomID);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */
