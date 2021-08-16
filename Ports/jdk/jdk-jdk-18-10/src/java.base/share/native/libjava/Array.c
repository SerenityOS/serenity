/*
 * Copyright (c) 1996, 1998, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "java_lang_reflect_Array.h"

/*
 * Native code for java.lang.reflect.Array.
 *
 * TODO: Performance
 */

/*
 *
 */
JNIEXPORT jint JNICALL
Java_java_lang_reflect_Array_getLength(JNIEnv *env, jclass ignore, jobject arr)
{
    return JVM_GetArrayLength(env, arr);
}

/*
 *
 */
JNIEXPORT jobject JNICALL
Java_java_lang_reflect_Array_get(JNIEnv *env, jclass ignore, jobject arr,
                                 jint index)
{
    return JVM_GetArrayElement(env, arr, index);
}

JNIEXPORT jboolean JNICALL
Java_java_lang_reflect_Array_getBoolean(JNIEnv *env, jclass ignore, jobject arr,
                                        jint index)
{
    return JVM_GetPrimitiveArrayElement(env, arr, index, JVM_T_BOOLEAN).z;
}

JNIEXPORT jbyte JNICALL
Java_java_lang_reflect_Array_getByte(JNIEnv *env, jclass ignore, jobject arr,
                                     jint index)
{
    return JVM_GetPrimitiveArrayElement(env, arr, index, JVM_T_BYTE).b;
}

JNIEXPORT jchar JNICALL
Java_java_lang_reflect_Array_getChar(JNIEnv *env, jclass ignore, jobject arr,
                                     jint index)
{
    return JVM_GetPrimitiveArrayElement(env, arr, index, JVM_T_CHAR).c;
}

JNIEXPORT jshort JNICALL
Java_java_lang_reflect_Array_getShort(JNIEnv *env, jclass ignore, jobject arr,
                                     jint index)
{
    return JVM_GetPrimitiveArrayElement(env, arr, index, JVM_T_SHORT).s;
}

JNIEXPORT jint JNICALL
Java_java_lang_reflect_Array_getInt(JNIEnv *env, jclass ignore, jobject arr,
                                     jint index)
{
    return JVM_GetPrimitiveArrayElement(env, arr, index, JVM_T_INT).i;
}

JNIEXPORT jlong JNICALL
Java_java_lang_reflect_Array_getLong(JNIEnv *env, jclass ignore, jobject arr,
                                     jint index)
{
    return JVM_GetPrimitiveArrayElement(env, arr, index, JVM_T_LONG).j;
}

JNIEXPORT jfloat JNICALL
Java_java_lang_reflect_Array_getFloat(JNIEnv *env, jclass ignore, jobject arr,
                                     jint index)
{
    return JVM_GetPrimitiveArrayElement(env, arr, index, JVM_T_FLOAT).f;
}

JNIEXPORT jdouble JNICALL
Java_java_lang_reflect_Array_getDouble(JNIEnv *env, jclass ignore, jobject arr,
                                     jint index)
{
    return JVM_GetPrimitiveArrayElement(env, arr, index, JVM_T_DOUBLE).d;
}

/*
 *
 */
JNIEXPORT void JNICALL
Java_java_lang_reflect_Array_set(JNIEnv *env, jclass ignore, jobject arr,
                                 jint index, jobject val)
{
    JVM_SetArrayElement(env, arr, index, val);
}

JNIEXPORT void JNICALL
Java_java_lang_reflect_Array_setBoolean(JNIEnv *env, jclass ignore,
                                        jobject arr, jint index, jboolean z)
{
    jvalue v;
    v.z = z;
    JVM_SetPrimitiveArrayElement(env, arr, index, v, JVM_T_BOOLEAN);
}

JNIEXPORT void JNICALL
Java_java_lang_reflect_Array_setByte(JNIEnv *env, jclass ignore,
                                        jobject arr, jint index, jbyte b)
{
    jvalue v;
    v.b = b;
    JVM_SetPrimitiveArrayElement(env, arr, index, v, JVM_T_BYTE);
}

JNIEXPORT void JNICALL
Java_java_lang_reflect_Array_setChar(JNIEnv *env, jclass ignore,
                                        jobject arr, jint index, jchar c)
{
    jvalue v;
    v.c = c;
    JVM_SetPrimitiveArrayElement(env, arr, index, v, JVM_T_CHAR);
}

JNIEXPORT void JNICALL
Java_java_lang_reflect_Array_setShort(JNIEnv *env, jclass ignore,
                                        jobject arr, jint index, jshort s)
{
    jvalue v;
    v.s = s;
    JVM_SetPrimitiveArrayElement(env, arr, index, v, JVM_T_SHORT);
}

JNIEXPORT void JNICALL
Java_java_lang_reflect_Array_setInt(JNIEnv *env, jclass ignore,
                                        jobject arr, jint index, jint i)
{
    jvalue v;
    v.i = i;
    JVM_SetPrimitiveArrayElement(env, arr, index, v, JVM_T_INT);
}

JNIEXPORT void JNICALL
Java_java_lang_reflect_Array_setLong(JNIEnv *env, jclass ignore,
                                        jobject arr, jint index, jlong j)
{
    jvalue v;
    v.j = j;
    JVM_SetPrimitiveArrayElement(env, arr, index, v, JVM_T_LONG);
}

JNIEXPORT void JNICALL
Java_java_lang_reflect_Array_setFloat(JNIEnv *env, jclass ignore,
                                        jobject arr, jint index, jfloat f)
{
    jvalue v;
    v.f = f;
    JVM_SetPrimitiveArrayElement(env, arr, index, v, JVM_T_FLOAT);
}

JNIEXPORT void JNICALL
Java_java_lang_reflect_Array_setDouble(JNIEnv *env, jclass ignore,
                                        jobject arr, jint index, jdouble d)
{
    jvalue v;
    v.d = d;
    JVM_SetPrimitiveArrayElement(env, arr, index, v, JVM_T_DOUBLE);
}

/*
 *
 */
JNIEXPORT jobject JNICALL
Java_java_lang_reflect_Array_newArray(JNIEnv *env, jclass ignore,
                                      jclass eltClass, jint length)
{
    return JVM_NewArray(env, eltClass, length);
}

JNIEXPORT jobject JNICALL
Java_java_lang_reflect_Array_multiNewArray(JNIEnv *env, jclass ignore,
                                           jclass eltClass, jintArray dim)
{
    return JVM_NewMultiArray(env, eltClass, dim);
}
