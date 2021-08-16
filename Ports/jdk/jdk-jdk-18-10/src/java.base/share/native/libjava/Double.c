/*
 * Copyright (c) 1997, 2005, Oracle and/or its affiliates. All rights reserved.
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
#include "jni_util.h"
#include "jlong.h"
#include "jvm.h"

#include "java_lang_Double.h"

/*
 * Find the double float corresponding to a given bit pattern
 */
JNIEXPORT jdouble JNICALL
Java_java_lang_Double_longBitsToDouble(JNIEnv *env, jclass unused, jlong v)
{
    union {
        jlong l;
        double d;
    } u;
    jlong_to_jdouble_bits(&v);
    u.l = v;
    return (jdouble)u.d;
}

/*
 * Find the bit pattern corresponding to a given double float, NOT collapsing NaNs
 */
JNIEXPORT jlong JNICALL
Java_java_lang_Double_doubleToRawLongBits(JNIEnv *env, jclass unused, jdouble v)
{
    union {
        jlong l;
        double d;
    } u;
    jdouble_to_jlong_bits(&v);
    u.d = (double)v;
    return u.l;
}
