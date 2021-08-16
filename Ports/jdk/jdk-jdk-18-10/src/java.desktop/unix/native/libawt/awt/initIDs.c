/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "java_awt_Color.h"
#include "java_awt_Dimension.h"
#include "java_awt_MenuBar.h"
#include "java_awt_FontMetrics.h"
#include "java_awt_event_MouseEvent.h"
#include "java_awt_Rectangle.h"
#include "java_awt_ScrollPaneAdjustable.h"
#include "java_awt_Toolkit.h"
#include "java_awt_CheckboxMenuItem.h"

#include "jni_util.h"

/*
 * This file contains stubs for JNI field and method id initializers
 * which are used in the win32 awt.
 */

JNIEXPORT jfieldID colorValueID;

JNIEXPORT void JNICALL
Java_java_awt_Color_initIDs
  (JNIEnv *env, jclass clazz)
{
    colorValueID = (*env)->GetFieldID(env, clazz, "value", "I");
}

JNIEXPORT void JNICALL
Java_java_awt_MenuBar_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_Label_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_FontMetrics_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_Toolkit_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_ScrollPaneAdjustable_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_CheckboxMenuItem_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_Choice_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_Dimension_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_Rectangle_initIDs
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT void JNICALL
Java_java_awt_event_MouseEvent_initIDs
  (JNIEnv *env, jclass clazz)
{
}
