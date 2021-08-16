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

#include "awt_KeyEvent.h"
#include "awt.h"

/************************************************************************
 * AwtKeyEvent fields
 */

jfieldID AwtKeyEvent::keyCodeID;
jfieldID AwtKeyEvent::keyCharID;
jfieldID AwtKeyEvent::rawCodeID;
jfieldID AwtKeyEvent::primaryLevelUnicodeID;
jfieldID AwtKeyEvent::scancodeID;
jfieldID AwtKeyEvent::extendedKeyCodeID;

/************************************************************************
 * AwtKeyEvent native methods
 */

extern "C" {

JNIEXPORT void JNICALL
Java_java_awt_event_KeyEvent_initIDs(JNIEnv *env, jclass cls) {
    TRY;

    AwtKeyEvent::keyCodeID = env->GetFieldID(cls, "keyCode", "I");
    DASSERT(AwtKeyEvent::keyCodeID != NULL);
    CHECK_NULL(AwtKeyEvent::keyCodeID);

    AwtKeyEvent::keyCharID = env->GetFieldID(cls, "keyChar", "C");
    DASSERT(AwtKeyEvent::keyCharID != NULL);
    CHECK_NULL(AwtKeyEvent::keyCharID);

    AwtKeyEvent::rawCodeID = env->GetFieldID(cls, "rawCode", "J");
    DASSERT(AwtKeyEvent::rawCodeID != NULL);
    CHECK_NULL(AwtKeyEvent::rawCodeID);

    AwtKeyEvent::primaryLevelUnicodeID = env->GetFieldID(cls, "primaryLevelUnicode", "J");
    DASSERT(AwtKeyEvent::primaryLevelUnicodeID != NULL);
    CHECK_NULL(AwtKeyEvent::primaryLevelUnicodeID);

    AwtKeyEvent::scancodeID = env->GetFieldID(cls, "scancode", "J");
    DASSERT(AwtKeyEvent::scancodeID != NULL);
    CHECK_NULL(AwtKeyEvent::scancodeID);

    AwtKeyEvent::extendedKeyCodeID = env->GetFieldID(cls, "extendedKeyCode", "J");
    DASSERT(AwtKeyEvent::extendedKeyCodeID != NULL);
    CHECK_NULL(AwtKeyEvent::extendedKeyCodeID);

    CATCH_BAD_ALLOC;
}

} /* extern "C" */
