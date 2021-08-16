/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

#import "GeomUtilities.h"

static jobject NewJavaRect(JNIEnv *env, jdouble x, jdouble y, jdouble w, jdouble h) {
    DECLARE_CLASS_RETURN(sjc_Rectangle2DDouble, "java/awt/geom/Rectangle2D$Double", NULL);
    DECLARE_METHOD_RETURN(ctor_Rectangle2DDouble, sjc_Rectangle2DDouble, "<init>", "(DDDD)V", NULL);
    jobject o = (*env)->NewObject(env, sjc_Rectangle2DDouble, ctor_Rectangle2DDouble, x, y, w, h);
    CHECK_EXCEPTION();
    return o;
}

jobject CGToJavaRect(JNIEnv *env, CGRect rect) {
   return NewJavaRect(env,
                      rect.origin.x,
                      rect.origin.y,
                      rect.size.width,
                      rect.size.height);
}

jobject NSToJavaRect(JNIEnv *env, NSRect rect) {
    return NewJavaRect(env,
                       rect.origin.x,
                       rect.origin.y,
                       rect.size.width,
                       rect.size.height);
}

NSRect JavaToNSRect(JNIEnv *env, jobject rect) {
    DECLARE_CLASS_RETURN(sjc_Rectangle2D, "java/awt/geom/Rectangle2D", NSZeroRect);
    DECLARE_METHOD_RETURN(jm_rect_getX, sjc_Rectangle2D, "getX", "()D", NSZeroRect);
    DECLARE_METHOD_RETURN(jm_rect_getY, sjc_Rectangle2D, "getY", "()D", NSZeroRect);
    DECLARE_METHOD_RETURN(jm_rect_getWidth, sjc_Rectangle2D, "getWidth", "()D", NSZeroRect);
    DECLARE_METHOD_RETURN(jm_rect_getHeight, sjc_Rectangle2D, "getHeight", "()D", NSZeroRect);
    jdouble x = (*env)->CallDoubleMethod(env, rect, jm_rect_getX); CHECK_EXCEPTION();
    jdouble y = (*env)->CallDoubleMethod(env, rect, jm_rect_getY); CHECK_EXCEPTION();
    jdouble w = (*env)->CallDoubleMethod(env, rect, jm_rect_getWidth); CHECK_EXCEPTION();
    jdouble h = (*env)->CallDoubleMethod(env, rect, jm_rect_getHeight); CHECK_EXCEPTION();
    return NSMakeRect(x, y, w, h);
}

jobject NSToJavaPoint(JNIEnv *env, NSPoint point) {
    DECLARE_CLASS_RETURN(sjc_Point2DDouble, "java/awt/geom/Point2D$Double", NULL);
    DECLARE_METHOD_RETURN(ctor_Point2DDouble, sjc_Point2DDouble, "<init>", "(DD)V", NULL);
    jobject o =  (*env)->NewObject(env, sjc_Point2DDouble, ctor_Point2DDouble, (jdouble)point.x, (jdouble)point.y);
    CHECK_EXCEPTION();
    return o;
}

NSPoint JavaToNSPoint(JNIEnv *env, jobject point) {
    DECLARE_CLASS_RETURN(sjc_Point2D, "java/awt/geom/Point2D", NSZeroPoint);
    DECLARE_METHOD_RETURN(jm_pt_getX, sjc_Point2D, "getX", "()D", NSZeroPoint);
    DECLARE_METHOD_RETURN(jm_pt_getY, sjc_Point2D, "getY", "()D", NSZeroPoint);
    jdouble x = (*env)->CallDoubleMethod(env, point, jm_pt_getX); CHECK_EXCEPTION();
    jdouble y = (*env)->CallDoubleMethod(env, point, jm_pt_getY); CHECK_EXCEPTION();
    return NSMakePoint(x, y);
}

jobject NSToJavaSize(JNIEnv *env, NSSize size) {
    DECLARE_CLASS_RETURN(sjc_Dimension2DDouble, "java/awt/Dimension", NULL); // No Dimension2D$Double :-(
    DECLARE_METHOD_RETURN(ctor_Dimension2DDouble, sjc_Dimension2DDouble, "<init>", "(II)V", NULL);
    jobject o = (*env)->NewObject(env, sjc_Dimension2DDouble, ctor_Dimension2DDouble, (jint)size.width, (jint)size.height);
    CHECK_EXCEPTION();
    return o;
}

NSSize JavaToNSSize(JNIEnv *env, jobject dimension) {
    DECLARE_CLASS_RETURN(sjc_Dimension2D, "java/awt/geom/Dimension2D", NSZeroSize);
    DECLARE_METHOD_RETURN(jm_sz_getWidth, sjc_Dimension2D, "getWidth", "()D", NSZeroSize);
    DECLARE_METHOD_RETURN(jm_sz_getHeight, sjc_Dimension2D, "getHeight", "()D", NSZeroSize);
    jdouble w = (*env)->CallDoubleMethod(env, dimension, jm_sz_getWidth); CHECK_EXCEPTION();
    jdouble h = (*env)->CallDoubleMethod(env, dimension, jm_sz_getHeight); CHECK_EXCEPTION();
    return NSMakeSize(w, h);
}

static NSScreen *primaryScreen(JNIEnv *env) {
    NSScreen *primaryScreen = [[NSScreen screens] objectAtIndex:0];
    if (primaryScreen != nil) return primaryScreen;
    if ((env != NULL) && ([NSThread isMainThread] == NO)) {
        JNU_ThrowByName(env, "java/lang/RuntimeException", "Failed to convert, no screen.");
    }
    [NSException raise:NSGenericException format:@"Failed to convert, no screen."];
    return nil;
}

NSPoint ConvertNSScreenPoint(JNIEnv *env, NSPoint point) {
    point.y = [primaryScreen(env) frame].size.height - point.y;
    return point;
}

NSRect ConvertNSScreenRect(JNIEnv *env, NSRect rect) {
    rect.origin.y = [primaryScreen(env) frame].size.height - rect.origin.y - rect.size.height;
    return rect;
}
