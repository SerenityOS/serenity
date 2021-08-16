/*
 * Copyright (c) 1994, 2016, Oracle and/or its affiliates. All rights reserved.
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

#include "jni.h"
#include "fdlibm.h"

#include "java_lang_StrictMath.h"

JNIEXPORT jdouble JNICALL
Java_java_lang_StrictMath_cos(JNIEnv *env, jclass unused, jdouble d)
{
    return (jdouble) jcos((double)d);
}

JNIEXPORT jdouble JNICALL
Java_java_lang_StrictMath_sin(JNIEnv *env, jclass unused, jdouble d)
{
    return (jdouble) jsin((double)d);
}

JNIEXPORT jdouble JNICALL
Java_java_lang_StrictMath_tan(JNIEnv *env, jclass unused, jdouble d)
{
    return (jdouble) jtan((double)d);
}

JNIEXPORT jdouble JNICALL
Java_java_lang_StrictMath_asin(JNIEnv *env, jclass unused, jdouble d)
{
    return (jdouble) jasin((double)d);
}

JNIEXPORT jdouble JNICALL
Java_java_lang_StrictMath_acos(JNIEnv *env, jclass unused, jdouble d)
{
    return (jdouble) jacos((double)d);
}

JNIEXPORT jdouble JNICALL
Java_java_lang_StrictMath_atan(JNIEnv *env, jclass unused, jdouble d)
{
    return (jdouble) jatan((double)d);
}

JNIEXPORT jdouble JNICALL
Java_java_lang_StrictMath_log(JNIEnv *env, jclass unused, jdouble d)
{
    return (jdouble) jlog((double)d);
}

JNIEXPORT jdouble JNICALL
Java_java_lang_StrictMath_log10(JNIEnv *env, jclass unused, jdouble d)
{
    return (jdouble) jlog10((double)d);
}

JNIEXPORT jdouble JNICALL
Java_java_lang_StrictMath_sqrt(JNIEnv *env, jclass unused, jdouble d)
{
    return (jdouble) jsqrt((double)d);
}

JNIEXPORT jdouble JNICALL
Java_java_lang_StrictMath_atan2(JNIEnv *env, jclass unused, jdouble d1, jdouble d2)
{
    return (jdouble) jatan2((double)d1, (double)d2);
}

JNIEXPORT jdouble JNICALL
Java_java_lang_StrictMath_IEEEremainder(JNIEnv *env, jclass unused,
                                  jdouble dividend,
                                  jdouble divisor)
{
    return (jdouble) jremainder(dividend, divisor);
}

JNIEXPORT jdouble JNICALL
Java_java_lang_StrictMath_cosh(JNIEnv *env, jclass unused, jdouble d)
{
    return (jdouble) jcosh((double)d);
}

JNIEXPORT jdouble JNICALL
Java_java_lang_StrictMath_sinh(JNIEnv *env, jclass unused, jdouble d)
{
    return (jdouble) jsinh((double)d);
}

JNIEXPORT jdouble JNICALL
Java_java_lang_StrictMath_tanh(JNIEnv *env, jclass unused, jdouble d)
{
    return (jdouble) jtanh((double)d);
}

JNIEXPORT jdouble JNICALL
Java_java_lang_StrictMath_log1p(JNIEnv *env, jclass unused, jdouble d)
{
    return (jdouble) jlog1p((double)d);
}

JNIEXPORT jdouble JNICALL
Java_java_lang_StrictMath_expm1(JNIEnv *env, jclass unused, jdouble d)
{
    return (jdouble) jexpm1((double)d);
}
