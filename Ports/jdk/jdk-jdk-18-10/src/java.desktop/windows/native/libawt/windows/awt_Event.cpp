/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

#include "awt_Event.h"
#include "awt.h"

/************************************************************************
 * AwtEvent fields
 */

jfieldID AwtEvent::targetID;
jfieldID AwtEvent::xID;
jfieldID AwtEvent::yID;

/************************************************************************
 * AwtEvent native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_java_awt_Event_initIDs(JNIEnv *env, jclass cls) {
    TRY;

    AwtEvent::targetID = env->GetFieldID(cls, "target", "Ljava/lang/Object;");
    DASSERT(AwtEvent::targetID != NULL);
    CHECK_NULL(AwtEvent::targetID);

    AwtEvent::xID = env->GetFieldID(cls, "x", "I");
    DASSERT(AwtEvent::xID != NULL);
    CHECK_NULL(AwtEvent::xID);

    AwtEvent::yID = env->GetFieldID(cls, "y", "I");
    DASSERT(AwtEvent::yID != NULL);
    CHECK_NULL(AwtEvent::yID);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */
