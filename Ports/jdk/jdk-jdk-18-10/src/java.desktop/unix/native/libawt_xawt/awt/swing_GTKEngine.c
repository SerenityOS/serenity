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

#include "gtk_interface.h"
#include "com_sun_java_swing_plaf_gtk_GTKEngine.h"
#include <jni_util.h>
#include <stdlib.h>
#include <string.h>

/* Static buffer for conversion from java.lang.String to UTF-8 */
static char conversionBuffer[(CONV_BUFFER_SIZE - 1) * 3 + 1];

const char *getStrFor(JNIEnv *env, jstring val)
{
    int length = (*env)->GetStringLength(env, val);
    if (length > CONV_BUFFER_SIZE-1)
    {
        length = CONV_BUFFER_SIZE-1;
    }

    memset(conversionBuffer, 0, sizeof(conversionBuffer));
    (*env)->GetStringUTFRegion(env, val, 0, length, conversionBuffer);
    return conversionBuffer;
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    native_paint_arrow
 * Signature: (IIILjava/lang/String;IIIII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_native_1paint_1arrow(
        JNIEnv *env, jobject this,
        jint widget_type, jint state, jint shadow_type, jstring detail,
        jint x, jint y, jint w, jint h, jint arrow_type)
{
    gtk->gdk_threads_enter();
    gtk->paint_arrow(widget_type, state, shadow_type, getStrFor(env, detail),
            x, y, w, h, arrow_type, TRUE);
    gtk->gdk_threads_leave();
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    native_paint_box
 * Signature: (IIILjava/lang/String;IIIIII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_native_1paint_1box(
        JNIEnv *env, jobject this,
        jint widget_type, jint state, jint shadow_type, jstring detail,
        jint x, jint y, jint w, jint h,
        jint synth_state, jint dir)
{
    gtk->gdk_threads_enter();
    gtk->paint_box(widget_type, state, shadow_type, getStrFor(env, detail),
                   x, y, w, h, synth_state, dir);
    gtk->gdk_threads_leave();
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    native_paint_box_gap
 * Signature: (IIILjava/lang/String;IIIIIII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_native_1paint_1box_1gap(
        JNIEnv *env, jobject this,
        jint widget_type, jint state, jint shadow_type, jstring detail,
        jint x, jint y, jint w, jint h,
        jint gap_side, jint gap_x, jint gap_w)
{
    gtk->gdk_threads_enter();
    gtk->paint_box_gap(widget_type, state, shadow_type, getStrFor(env, detail),
            x, y, w, h, gap_side, gap_x, gap_w);
    gtk->gdk_threads_leave();
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    native_paint_check
 * Signature: (IILjava/lang/String;IIII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_native_1paint_1check(
        JNIEnv *env, jobject this,
        jint widget_type, jint synth_state, jstring detail,
        jint x, jint y, jint w, jint h)
{
    gtk->gdk_threads_enter();
    gtk->paint_check(widget_type, synth_state, getStrFor(env, detail),
                     x, y, w, h);
    gtk->gdk_threads_leave();
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    native_paint_expander
 * Signature: (IILjava/lang/String;IIIII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_native_1paint_1expander(
        JNIEnv *env, jobject this,
        jint widget_type, jint state, jstring detail,
        jint x, jint y, jint w, jint h, jint expander_style)
{
    gtk->gdk_threads_enter();
    gtk->paint_expander(widget_type, state, getStrFor(env, detail),
            x, y, w, h, expander_style);
    gtk->gdk_threads_leave();
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    native_paint_extension
 * Signature: (IIILjava/lang/String;IIIII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_native_1paint_1extension(
        JNIEnv *env, jobject this,
        jint widget_type, jint state, jint shadow_type, jstring detail,
        jint x, jint y, jint w, jint h, jint placement)
{
    gtk->gdk_threads_enter();
    gtk->paint_extension(widget_type, state, shadow_type,
            getStrFor(env, detail), x, y, w, h, placement);
    gtk->gdk_threads_leave();
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    native_paint_flat_box
 * Signature: (IIILjava/lang/String;IIII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_native_1paint_1flat_1box(
        JNIEnv *env, jobject this,
        jint widget_type, jint state, jint shadow_type, jstring detail,
        jint x, jint y, jint w, jint h, jboolean has_focus)
{
    gtk->gdk_threads_enter();
    gtk->paint_flat_box(widget_type, state, shadow_type,
            getStrFor(env, detail), x, y, w, h, has_focus);
    gtk->gdk_threads_leave();
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    native_paint_focus
 * Signature: (IILjava/lang/String;IIII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_native_1paint_1focus(
        JNIEnv *env, jobject this,
        jint widget_type, jint state, jstring detail,
        jint x, jint y, jint w, jint h)
{
    gtk->gdk_threads_enter();
    gtk->paint_focus(widget_type, state, getStrFor(env, detail),
            x, y, w, h);
    gtk->gdk_threads_leave();
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    native_paint_handle
 * Signature: (IIILjava/lang/String;IIIII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_native_1paint_1handle(
        JNIEnv *env, jobject this,
        jint widget_type, jint state, jint shadow_type, jstring detail,
        jint x, jint y, jint w, jint h, jint orientation)
{
    gtk->gdk_threads_enter();
    gtk->paint_handle(widget_type, state, shadow_type, getStrFor(env, detail),
            x, y, w, h, orientation);
    gtk->gdk_threads_leave();
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    native_paint_hline
 * Signature: (IILjava/lang/String;IIII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_native_1paint_1hline(
        JNIEnv *env, jobject this,
        jint widget_type, jint state, jstring detail,
        jint x, jint y, jint w, jint h)
{
    gtk->gdk_threads_enter();
    gtk->paint_hline(widget_type, state, getStrFor(env, detail),
            x, y, w, h);
    gtk->gdk_threads_leave();
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    native_paint_option
 * Signature: (IILjava/lang/String;IIII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_native_1paint_1option(
        JNIEnv *env, jobject this,
        jint widget_type, jint synth_state, jstring detail,
        jint x, jint y, jint w, jint h)
{
    gtk->gdk_threads_enter();
    gtk->paint_option(widget_type, synth_state, getStrFor(env, detail),
                      x, y, w, h);
    gtk->gdk_threads_leave();
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    native_paint_shadow
 * Signature: (IIILjava/lang/String;IIIIII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_native_1paint_1shadow(
        JNIEnv *env, jobject this,
        jint widget_type, jint state, jint shadow_type, jstring detail,
        jint x, jint y, jint w, jint h,
        jint synth_state, jint dir)
{
    gtk->gdk_threads_enter();
    gtk->paint_shadow(widget_type, state, shadow_type, getStrFor(env, detail),
                      x, y, w, h, synth_state, dir);
    gtk->gdk_threads_leave();
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    native_paint_slider
 * Signature: (IIILjava/lang/String;IIIII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_native_1paint_1slider(
        JNIEnv *env, jobject this,
        jint widget_type, jint state, jint shadow_type, jstring detail,
        jint x, jint y, jint w, jint h, jint orientation, jboolean has_focus)
{
    gtk->gdk_threads_enter();
    gtk->paint_slider(widget_type, state, shadow_type, getStrFor(env, detail),
            x, y, w, h, orientation, has_focus);
    gtk->gdk_threads_leave();
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    native_paint_vline
 * Signature: (IILjava/lang/String;IIII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_native_1paint_1vline(
        JNIEnv *env, jobject this,
        jint widget_type, jint state, jstring detail,
        jint x, jint y, jint w, jint h)
{
    gtk->gdk_threads_enter();
    gtk->paint_vline(widget_type, state, getStrFor(env, detail),
            x, y, w, h);
    gtk->gdk_threads_leave();
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    native_paint_background
 * Signature: (IIIIII)V
 */
JNIEXPORT void JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_native_1paint_1background(
        JNIEnv *env, jobject this, jint widget_type, jint state,
        jint x, jint y, jint w, jint h)
{
    gtk->gdk_threads_enter();
    gtk->paint_background(widget_type, state, x, y, w, h);
    gtk->gdk_threads_leave();
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    nativeStartPainting
 * Signature: (II)V
 */
JNIEXPORT void JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_nativeStartPainting(
        JNIEnv *env, jobject this, jint w, jint h)
{
    if (w > 0x7FFF || h > 0x7FFF || (uintptr_t)4 * w * h > 0x7FFFFFFFL) {
        // Same limitation as in X11SurfaceData.c
        JNU_ThrowOutOfMemoryError(env, "Can't create offscreen surface");
        return;
    }
    gtk->gdk_threads_enter();
    gtk->init_painting(env, w, h);
    gtk->gdk_threads_leave();
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    nativeFinishPainting
 * Signature: ([III)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_nativeFinishPainting(
        JNIEnv *env, jobject this, jintArray dest, jint width, jint height)
{
    jint transparency;
    gint *buffer = (gint*) (*env)->GetPrimitiveArrayCritical(env, dest, 0);
    gtk->gdk_threads_enter();
    transparency = gtk->copy_image(buffer, width, height);
    gtk->gdk_threads_leave();
    (*env)->ReleasePrimitiveArrayCritical(env, dest, buffer, 0);
    return transparency;
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    native_switch_theme
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_java_swing_plaf_gtk_GTKEngine_native_1switch_1theme(
        JNIEnv *env, jobject this)
{
    // Note that gtk->flush_event_loop takes care of locks (7053002), gdk_threads_enter/gdk_threads_leave should not be used.
    gtk->flush_event_loop();
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    native_get_gtk_setting
 * Signature: (I)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_com_sun_java_swing_plaf_gtk_GTKEngine_native_1get_1gtk_1setting(
        JNIEnv *env, jobject this, jint property)
{
    jobject obj;
    gtk->gdk_threads_enter();
    obj = gtk->get_setting(env, property);
    gtk->gdk_threads_leave();
    return obj;
}

/*
 * Class:     com_sun_java_swing_plaf_gtk_GTKEngine
 * Method:    nativeSetRangeValue
 * Signature: (IDDDD)V
 */
JNIEXPORT void JNICALL
Java_com_sun_java_swing_plaf_gtk_GTKEngine_nativeSetRangeValue(
        JNIEnv *env, jobject this, jint widget_type,
        jdouble value, jdouble min, jdouble max, jdouble visible)
{
    gtk->gdk_threads_enter();
    gtk->set_range_value(widget_type, value, min, max, visible);
    gtk->gdk_threads_leave();
}
