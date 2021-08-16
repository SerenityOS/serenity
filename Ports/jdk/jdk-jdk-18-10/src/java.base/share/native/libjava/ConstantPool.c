/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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

#include "jvm.h"
#include "jdk_internal_reflect_ConstantPool.h"

JNIEXPORT jint JNICALL Java_jdk_internal_reflect_ConstantPool_getSize0
(JNIEnv *env, jobject unused, jobject jcpool)
{
  return JVM_ConstantPoolGetSize(env, unused, jcpool);
}

JNIEXPORT jclass JNICALL Java_jdk_internal_reflect_ConstantPool_getClassAt0
(JNIEnv *env, jobject unused, jobject jcpool, jint index)
{
  return JVM_ConstantPoolGetClassAt(env, unused, jcpool, index);
}

JNIEXPORT jclass JNICALL Java_jdk_internal_reflect_ConstantPool_getClassAtIfLoaded0
(JNIEnv *env, jobject unused, jobject jcpool, jint index)
{
  return JVM_ConstantPoolGetClassAtIfLoaded(env, unused, jcpool, index);
}

JNIEXPORT jint JNICALL Java_jdk_internal_reflect_ConstantPool_getClassRefIndexAt0
(JNIEnv *env, jobject unused, jobject jcpool, jint index)
{
    return JVM_ConstantPoolGetClassRefIndexAt(env, unused, jcpool, index);
}

JNIEXPORT jobject JNICALL Java_jdk_internal_reflect_ConstantPool_getMethodAt0
(JNIEnv *env, jobject unused, jobject jcpool, jint index)
{
  return JVM_ConstantPoolGetMethodAt(env, unused, jcpool, index);
}

JNIEXPORT jobject JNICALL Java_jdk_internal_reflect_ConstantPool_getMethodAtIfLoaded0
(JNIEnv *env, jobject unused, jobject jcpool, jint index)
{
  return JVM_ConstantPoolGetMethodAtIfLoaded(env, unused, jcpool, index);
}

JNIEXPORT jobject JNICALL Java_jdk_internal_reflect_ConstantPool_getFieldAt0
(JNIEnv *env, jobject unused, jobject jcpool, jint index)
{
  return JVM_ConstantPoolGetFieldAt(env, unused, jcpool, index);
}

JNIEXPORT jobject JNICALL Java_jdk_internal_reflect_ConstantPool_getFieldAtIfLoaded0
(JNIEnv *env, jobject unused, jobject jcpool, jint index)
{
  return JVM_ConstantPoolGetFieldAtIfLoaded(env, unused, jcpool, index);
}

JNIEXPORT jobjectArray JNICALL Java_jdk_internal_reflect_ConstantPool_getMemberRefInfoAt0
(JNIEnv *env, jobject unused, jobject jcpool, jint index)
{
  return JVM_ConstantPoolGetMemberRefInfoAt(env, unused, jcpool, index);
}

JNIEXPORT jint JNICALL Java_jdk_internal_reflect_ConstantPool_getNameAndTypeRefIndexAt0
(JNIEnv *env, jobject unused, jobject jcpool, jint index)
{
    return JVM_ConstantPoolGetNameAndTypeRefIndexAt(env, unused, jcpool, index);
}

JNIEXPORT jobjectArray JNICALL Java_jdk_internal_reflect_ConstantPool_getNameAndTypeRefInfoAt0
(JNIEnv *env, jobject unused, jobject jcpool, jint index)
{
  return JVM_ConstantPoolGetNameAndTypeRefInfoAt(env, unused, jcpool, index);
}

JNIEXPORT jint JNICALL Java_jdk_internal_reflect_ConstantPool_getIntAt0
(JNIEnv *env, jobject unused, jobject jcpool, jint index)
{
  return JVM_ConstantPoolGetIntAt(env, unused, jcpool, index);
}

JNIEXPORT jlong JNICALL Java_jdk_internal_reflect_ConstantPool_getLongAt0
(JNIEnv *env, jobject unused, jobject jcpool, jint index)
{
  return JVM_ConstantPoolGetLongAt(env, unused, jcpool, index);
}

JNIEXPORT jfloat JNICALL Java_jdk_internal_reflect_ConstantPool_getFloatAt0
(JNIEnv *env, jobject unused, jobject jcpool, jint index)
{
  return JVM_ConstantPoolGetFloatAt(env, unused, jcpool, index);
}

JNIEXPORT jdouble JNICALL Java_jdk_internal_reflect_ConstantPool_getDoubleAt0
(JNIEnv *env, jobject unused, jobject jcpool, jint index)
{
  return JVM_ConstantPoolGetDoubleAt(env, unused, jcpool, index);
}

JNIEXPORT jstring JNICALL Java_jdk_internal_reflect_ConstantPool_getStringAt0
(JNIEnv *env, jobject unused, jobject jcpool, jint index)
{
  return JVM_ConstantPoolGetStringAt(env, unused, jcpool, index);
}

JNIEXPORT jstring JNICALL Java_jdk_internal_reflect_ConstantPool_getUTF8At0
(JNIEnv *env, jobject unused, jobject jcpool, jint index)
{
  return JVM_ConstantPoolGetUTF8At(env, unused, jcpool, index);
}

JNIEXPORT jbyte JNICALL Java_jdk_internal_reflect_ConstantPool_getTagAt0
(JNIEnv *env, jobject unused, jobject jcpool, jint index)
{
  return JVM_ConstantPoolGetTagAt(env, unused, jcpool, index);
}

