/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019 SAP SE. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

#include <jni.h>

JNIEXPORT void JNICALL
  Java_ArrayIndexOutOfBoundsExceptionTest_doNativeArrayStore(JNIEnv *env, jclass klass,
                                                             jobjectArray array, jobject element, jint index) {
  (*env)->SetObjectArrayElement(env, array, index, element);
}

JNIEXPORT jobject JNICALL
  Java_ArrayIndexOutOfBoundsExceptionTest_doNativeArrayLoad(JNIEnv *env, jclass klass,
                                                            jobjectArray array, jint index) {
  return (*env)->GetObjectArrayElement(env, array, index);
}


#define REGIONACCESS(ElementType,NameType) \
JNIEXPORT void JNICALL \
  Java_ArrayIndexOutOfBoundsExceptionTest_doNative##NameType##ArrayRegionLoad(JNIEnv *env, jclass klass, \
                                                                     ElementType##Array array, jint start, jint len) { \
  ElementType clone[100]; \
  (*env)->Get##NameType##ArrayRegion(env, array, start, len, clone); \
} \
JNIEXPORT void JNICALL \
  Java_ArrayIndexOutOfBoundsExceptionTest_doNative##NameType##ArrayRegionStore(JNIEnv *env, jclass klass, \
                                                                      ElementType##Array array, jint start, jint len) { \
  ElementType content[100] = {0}; \
  (*env)->Set##NameType##ArrayRegion(env, array, start, len, content); \
}

REGIONACCESS(jboolean, Boolean)
REGIONACCESS(jbyte,    Byte)
REGIONACCESS(jshort,   Short)
REGIONACCESS(jchar,    Char)
REGIONACCESS(jint,     Int)
REGIONACCESS(jlong,    Long)
REGIONACCESS(jfloat,   Float)
REGIONACCESS(jdouble,  Double)
