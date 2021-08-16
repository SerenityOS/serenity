/*
 * Copyright (c) 2015, 2016. All rights reserved.
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

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jfloat JNICALL Java_compiler_floatingpoint_TestFloatJNIArgs_add15floats
  (JNIEnv *env, jclass cls,
   jfloat  f1, jfloat  f2, jfloat  f3, jfloat  f4,
   jfloat  f5, jfloat  f6, jfloat  f7, jfloat  f8,
   jfloat  f9, jfloat f10, jfloat f11, jfloat f12,
   jfloat f13, jfloat f14, jfloat f15) {
  return f1 + f2 + f3 + f4 + f5 + f6 + f7 + f8 + f9 + f10 + f11 + f12 + f13 + f14 + f15;
}

JNIEXPORT jfloat JNICALL Java_compiler_floatingpoint_TestFloatJNIArgs_add10floats
  (JNIEnv *env, jclass cls,
   jfloat  f1, jfloat  f2, jfloat  f3, jfloat  f4,
   jfloat  f5, jfloat  f6, jfloat  f7, jfloat  f8,
   jfloat  f9, jfloat f10) {
  return f1 + f2 + f3 + f4 + f5 + f6 + f7 + f8 + f9 + f10;
}

JNIEXPORT jfloat JNICALL Java_compiler_floatingpoint_TestFloatJNIArgs_addFloatsInts
  (JNIEnv *env, jclass cls,
   jfloat  f1, jfloat  f2, jfloat  f3, jfloat  f4,
   jfloat  f5, jfloat  f6, jfloat  f7, jfloat  f8,
   jfloat  f9, jfloat f10, jfloat f11, jfloat f12,
   jfloat f13, jfloat f14, jfloat f15, jint a16, jint a17) {
  return f1 + f2 + f3 + f4 + f5 + f6 + f7 + f8 + f9 + f10 + f11 + f12 + f13 + f14 + f15 + a16 + a17;
}

JNIEXPORT jdouble JNICALL Java_compiler_floatingpoint_TestFloatJNIArgs_add15doubles
  (JNIEnv *env, jclass cls,
   jdouble  f1, jdouble  f2, jdouble  f3, jdouble  f4,
   jdouble  f5, jdouble  f6, jdouble  f7, jdouble  f8,
   jdouble  f9, jdouble f10, jdouble f11, jdouble f12,
   jdouble f13, jdouble f14, jdouble f15) {
  return f1 + f2 + f3 + f4 + f5 + f6 + f7 + f8 + f9 + f10 + f11 + f12 + f13 + f14 + f15;
}


#ifdef __cplusplus
}
#endif
