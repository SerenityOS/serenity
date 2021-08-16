/*
 * Copyright (c) 2018 Red Hat, Inc. All rights reserved.
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

/* Fletcher checksum. This is a nonlinear function which detects both */
/* missing or otherwise incorrect arguments and arguments in the wrong */
/* order. */
static jfloat fcombine(jfloat f[], int len) {
  int i;
  jfloat sum = 0, sum_of_sums = 0;
  for (i = 0; i < len; i++) {
    sum += f[i];
    sum_of_sums += sum;
  }
  return sum + sum_of_sums * sum;
}

static jdouble combine(jdouble f[], int len) {
  int i;
  double sum = 0, sum_of_sums = 0;
  for (i = 0; i < len; i++) {
    sum += f[i];
    sum_of_sums += sum;
  }
  return sum + sum_of_sums * sum;
}

JNIEXPORT jfloat JNICALL Java_compiler_floatingpoint_TestFloatSyncJNIArgs_combine15floats
  (JNIEnv *env, jclass cls,
   jfloat  f1, jfloat  f2, jfloat  f3, jfloat  f4,
   jfloat  f5, jfloat  f6, jfloat  f7, jfloat  f8,
   jfloat  f9, jfloat f10, jfloat f11, jfloat f12,
   jfloat f13, jfloat f14, jfloat f15) {

  jfloat f[15];
  f[0] = f1; f[1] = f2; f[2] = f3; f[3] = f4; f[4] = f5;
  f[5] = f6; f[6] = f7; f[7] = f8; f[8] = f9; f[9] = f10;
  f[10] = f11; f[11] = f12; f[12] = f13; f[13] = f14; f[14] = f15;

  return fcombine(f, sizeof f / sizeof f[0]);
}

JNIEXPORT jdouble JNICALL Java_compiler_floatingpoint_TestFloatSyncJNIArgs_combine15doubles
  (JNIEnv *env, jclass cls,
   jdouble  f1, jdouble  f2, jdouble  f3, jdouble  f4,
   jdouble  f5, jdouble  f6, jdouble  f7, jdouble  f8,
   jdouble  f9, jdouble f10, jdouble f11, jdouble f12,
   jdouble f13, jdouble f14, jdouble f15) {

  jdouble f[15];
  f[0] = f1; f[1] = f2; f[2] = f3; f[3] = f4; f[4] = f5;
  f[5] = f6; f[6] = f7; f[7] = f8; f[8] = f9; f[9] = f10;
  f[10] = f11; f[11] = f12; f[12] = f13; f[13] = f14; f[14] = f15;

  return combine(f, sizeof f / sizeof f[0]);
}


#ifdef __cplusplus
}
#endif
