/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifdef HEADLESS
    #error This file should not be included in headless library
#endif

#include <stdlib.h>
#include <stdio.h>
#include "gtk_interface.h"
#include "com_sun_java_swing_plaf_gtk_GTKStyle.h"

const char *getStrFor(JNIEnv *env, jstring val);

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKStyle
 * Method:    nativeGetXThickness
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKStyle_nativeGetXThickness(
    JNIEnv *env, jclass klass, jint widget_type)
{
    jint ret;
    gtk->gdk_threads_enter();
    ret = gtk->get_xthickness(env, widget_type);
    gtk->gdk_threads_leave();
    return ret;
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKStyle
 * Method:    nativeGetYThickness
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKStyle_nativeGetYThickness(
    JNIEnv *env, jclass klass, jint widget_type)
{
    jint ret;
    gtk->gdk_threads_enter();
    ret = gtk->get_ythickness(env, widget_type);
    gtk->gdk_threads_leave();
    return ret;
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKStyle
 * Method:    nativeGetColorForState
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKStyle_nativeGetColorForState(
    JNIEnv *env, jclass klass, jint widget_type,
    jint state_type, jint type_id)
{
    jint ret;
    gtk->gdk_threads_enter();
    ret = gtk->get_color_for_state(env, widget_type, state_type, type_id);
    gtk->gdk_threads_leave();
    return ret;
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKStyle
 * Method:    nativeGetClassValue
 * Signature: (ILjava/lang/String;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKStyle_nativeGetClassValue(
    JNIEnv *env, jclass klass, jint widget_type, jstring key)
{
    jobject ret;
    gtk->gdk_threads_enter();
    ret = gtk->get_class_value(env, widget_type, getStrFor(env, key));
    gtk->gdk_threads_leave();
    return ret;
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKStyle
 * Method:    nativeGetPangoFontName
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKStyle_nativeGetPangoFontName(
    JNIEnv *env, jclass klass, jint widget_type)
{
    jstring ret;
    gtk->gdk_threads_enter();
    ret = gtk->get_pango_font_name(env, widget_type);
    gtk->gdk_threads_leave();
    return ret;
}
