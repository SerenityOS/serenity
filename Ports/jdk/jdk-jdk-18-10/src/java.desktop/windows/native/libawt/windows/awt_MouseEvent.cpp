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

#include "awt_MouseEvent.h"
#include "awt.h"

/************************************************************************
 * AwtMouseEvent fields
 */

jfieldID AwtMouseEvent::xID;
jfieldID AwtMouseEvent::yID;
jfieldID AwtMouseEvent::causedByTouchEventID;
jfieldID AwtMouseEvent::buttonID;

/************************************************************************
 * AwtMouseEvent native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_java_awt_event_MouseEvent_initIDs(JNIEnv *env, jclass cls) {
    TRY;

    AwtMouseEvent::xID = env->GetFieldID(cls, "x", "I");
    DASSERT(AwtMouseEvent::xID != NULL);
    CHECK_NULL(AwtMouseEvent::xID);

    AwtMouseEvent::yID = env->GetFieldID(cls, "y", "I");
    DASSERT(AwtMouseEvent::yID != NULL);
    CHECK_NULL(AwtMouseEvent::yID);

    AwtMouseEvent::causedByTouchEventID = env->GetFieldID(
        cls, "causedByTouchEvent", "Z");
    DASSERT(AwtMouseEvent::causedByTouchEventID != NULL);
    CHECK_NULL(AwtMouseEvent::causedByTouchEventID);

    AwtMouseEvent::buttonID = env->GetFieldID(cls, "button", "I");
    DASSERT(AwtMouseEvent::buttonID != NULL);
    CHECK_NULL(AwtMouseEvent::buttonID);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */
