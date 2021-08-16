/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <stdint.h>

#include "jni.h"

#ifdef __cplusplus
extern "C" {
#endif

jfloat JNICALL SS(jfloat f1, jfloat f2) {
  return f1 + f2;
}

JNIEXPORT jlong JNICALL Java_jdk_vm_ci_code_test_NativeCallTest_getFF(JNIEnv *env, jclass clazz) {
  return (jlong)(intptr_t)SS;
}

JNIEXPORT jfloat JNICALL Java_jdk_vm_ci_code_test_NativeCallTest__1FF(JNIEnv *env, jclass clazz, jfloat a, jfloat b) {
  return SS(a, b);
}

jfloat JNICALL SDILDS(jfloat a, jdouble b, jint c, jlong d, jdouble e, jfloat f) {
  return (jfloat)(a + b + c + d + e + f);
}

JNIEXPORT jlong JNICALL Java_jdk_vm_ci_code_test_NativeCallTest_getSDILDS(JNIEnv *env, jclass clazz) {
  return (jlong)(intptr_t)SDILDS;
}

JNIEXPORT jfloat JNICALL Java_jdk_vm_ci_code_test_NativeCallTest__1SDILDS(JNIEnv *env, jclass clazz,
                                                                          jfloat a, jdouble b, jint c, jlong d, jdouble e, jfloat f) {
  return SDILDS(a, b, c, d, e, f);
}

jfloat JNICALL F32SDILDS(jfloat f00, jfloat f01, jfloat f02, jfloat f03, jfloat f04, jfloat f05, jfloat f06, jfloat f07,
                         jfloat f08, jfloat f09, jfloat f0a, jfloat f0b, jfloat f0c, jfloat f0d, jfloat f0e, jfloat f0f,
                         jfloat f10, jfloat f11, jfloat f12, jfloat f13, jfloat f14, jfloat f15, jfloat f16, jfloat f17,
                         jfloat f18, jfloat f19, jfloat f1a, jfloat f1b, jfloat f1c, jfloat f1d, jfloat f1e, jfloat f1f,
                         jfloat a, jdouble b, jint c, jlong d, jdouble e, jfloat f) {
  return (jfloat)(f00 + f01 + f02 + f03 + f04 + f05 + f06 + f07 +
                  f08 + f09 + f0a + f0b + f0c + f0d + f0e + f0f +
                  f10 + f11 + f12 + f13 + f14 + f15 + f16 + f17 +
                  f18 + f19 + f1a + f1b + f1c + f1d + f1e + f1f +
                  a +   b +   c +   d +   e + f);
}

JNIEXPORT jlong JNICALL Java_jdk_vm_ci_code_test_NativeCallTest_getF32SDILDS(JNIEnv *env, jclass clazz) {
  return (jlong)(intptr_t)F32SDILDS;
}

JNIEXPORT jfloat JNICALL Java_jdk_vm_ci_code_test_NativeCallTest__1F32SDILDS(JNIEnv *env, jclass clazz,
                                                                             jfloat f00, jfloat f01, jfloat f02, jfloat f03,
                                                                             jfloat f04, jfloat f05, jfloat f06, jfloat f07,
                                                                             jfloat f08, jfloat f09, jfloat f0a, jfloat f0b,
                                                                             jfloat f0c, jfloat f0d, jfloat f0e, jfloat f0f,
                                                                             jfloat f10, jfloat f11, jfloat f12, jfloat f13,
                                                                             jfloat f14, jfloat f15, jfloat f16, jfloat f17,
                                                                             jfloat f18, jfloat f19, jfloat f1a, jfloat f1b,
                                                                             jfloat f1c, jfloat f1d, jfloat f1e, jfloat f1f,
                                                                             jfloat a, jdouble b, jint c, jlong d, jdouble e, jfloat f) {
  return F32SDILDS(f00, f01, f02, f03, f04, f05, f06, f07,
                   f08, f09, f0a, f0b, f0c, f0d, f0e, f0f,
                   f10, f11, f12, f13, f14, f15, f16, f17,
                   f18, f19, f1a, f1b, f1c, f1d, f1e, f1f,
                   a,   b,   c,   d,   e,   f);
}


jfloat JNICALL D32SDILDS(jdouble d00, jdouble d01, jdouble d02, jdouble d03, jdouble d04, jdouble d05, jdouble d06, jdouble d07,
                         jdouble d08, jdouble d09, jdouble d0a, jdouble d0b, jdouble d0c, jdouble d0d, jdouble d0e, jdouble d0f,
                         jdouble d10, jdouble d11, jdouble d12, jdouble d13, jdouble d14, jdouble d15, jdouble d16, jdouble d17,
                         jdouble d18, jdouble d19, jdouble d1a, jdouble d1b, jdouble d1c, jdouble d1d, jdouble d1e, jdouble d1f,
                         jfloat a, jdouble b, jint c, jlong d, jdouble e, jfloat f) {
  return (jfloat)(d00 + d01 + d02 + d03 + d04 + d05 + d06 + d07 +
                  d08 + d09 + d0a + d0b + d0c + d0d + d0e + d0f +
                  d10 + d11 + d12 + d13 + d14 + d15 + d16 + d17 +
                  d18 + d19 + d1a + d1b + d1c + d1d + d1e + d1f +
                  a +   b +   c +   d +   e + f);
}

JNIEXPORT jlong JNICALL Java_jdk_vm_ci_code_test_NativeCallTest_getD32SDILDS(JNIEnv *env, jclass clazz) {
  return (jlong)(intptr_t)D32SDILDS;
}

JNIEXPORT jfloat JNICALL Java_jdk_vm_ci_code_test_NativeCallTest__1D32SDILDS(JNIEnv *env, jclass clazz,
                                                                             jdouble d00, jdouble d01, jdouble d02, jdouble d03,
                                                                             jdouble d04, jdouble d05, jdouble d06, jdouble d07,
                                                                             jdouble d08, jdouble d09, jdouble d0a, jdouble d0b,
                                                                             jdouble d0c, jdouble d0d, jdouble d0e, jdouble d0f,
                                                                             jdouble d10, jdouble d11, jdouble d12, jdouble d13,
                                                                             jdouble d14, jdouble d15, jdouble d16, jdouble d17,
                                                                             jdouble d18, jdouble d19, jdouble d1a, jdouble d1b,
                                                                             jdouble d1c, jdouble d1d, jdouble d1e, jdouble d1f,
                                                                             jfloat a, jdouble b, jint c, jlong d, jdouble e, jfloat f) {
  return D32SDILDS(d00, d01, d02, d03, d04, d05, d06, d07,
                   d08, d09, d0a, d0b, d0c, d0d, d0e, d0f,
                   d10, d11, d12, d13, d14, d15, d16, d17,
                   d18, d19, d1a, d1b, d1c, d1d, d1e, d1f,
                   a,   b,   c,   d,   e,   f);
}


jfloat JNICALL I32SDILDS(jint i00, jint i01, jint i02, jint i03, jint i04, jint i05, jint i06, jint i07,
                         jint i08, jint i09, jint i0a, jint i0b, jint i0c, jint i0d, jint i0e, jint i0f,
                         jint i10, jint i11, jint i12, jint i13, jint i14, jint i15, jint i16, jint i17,
                         jint i18, jint i19, jint i1a, jint i1b, jint i1c, jint i1d, jint i1e, jint i1f,
                         jfloat a, jdouble b, jint c, jlong d, jdouble e, jfloat f) {
  return (jfloat)(i00 + i01 + i02 + i03 + i04 + i05 + i06 + i07 +
                  i08 + i09 + i0a + i0b + i0c + i0d + i0e + i0f +
                  i10 + i11 + i12 + i13 + i14 + i15 + i16 + i17 +
                  i18 + i19 + i1a + i1b + i1c + i1d + i1e + i1f +
                  a +   b +   c +   d +   e + f);
}

JNIEXPORT jlong JNICALL Java_jdk_vm_ci_code_test_NativeCallTest_getI32SDILDS(JNIEnv *env, jclass clazz) {
  return (jlong) (intptr_t) I32SDILDS;
}

JNIEXPORT jfloat JNICALL Java_jdk_vm_ci_code_test_NativeCallTest__1I32SDILDS(JNIEnv *env, jclass clazz,
                                                                             jint i00, jint i01, jint i02, jint i03,
                                                                             jint i04, jint i05, jint i06, jint i07,
                                                                             jint i08, jint i09, jint i0a, jint i0b,
                                                                             jint i0c, jint i0d, jint i0e, jint i0f,
                                                                             jint i10, jint i11, jint i12, jint i13,
                                                                             jint i14, jint i15, jint i16, jint i17,
                                                                             jint i18, jint i19, jint i1a, jint i1b,
                                                                             jint i1c, jint i1d, jint i1e, jint i1f,
                                                                             jfloat a, jdouble b, jint c, jlong d, jdouble e, jfloat f) {
  return I32SDILDS(i00, i01, i02, i03, i04, i05, i06, i07,
                   i08, i09, i0a, i0b, i0c, i0d, i0e, i0f,
                   i10, i11, i12, i13, i14, i15, i16, i17,
                   i18, i19, i1a, i1b, i1c, i1d, i1e, i1f,
                   a,   b,   c,   d,   e,   f);
}

jfloat JNICALL L32SDILDS(jlong l00, jlong l01, jlong l02, jlong l03, jlong l04, jlong l05, jlong l06, jlong l07,
                         jlong l08, jlong l09, jlong l0a, jlong l0b, jlong l0c, jlong l0d, jlong l0e, jlong l0f,
                         jlong l10, jlong l11, jlong l12, jlong l13, jlong l14, jlong l15, jlong l16, jlong l17,
                         jlong l18, jlong l19, jlong l1a, jlong l1b, jlong l1c, jlong l1d, jlong l1e, jlong l1f,
                         jfloat a, jdouble b, jint c, jlong d, jdouble e, jfloat f) {
  return (jfloat)(l00 + l01 + l02 + l03 + l04 + l05 + l06 + l07 +
                  l08 + l09 + l0a + l0b + l0c + l0d + l0e + l0f +
                  l10 + l11 + l12 + l13 + l14 + l15 + l16 + l17 +
                  l18 + l19 + l1a + l1b + l1c + l1d + l1e + l1f +
                  a +   b +   c +   d +   e + f);
}

JNIEXPORT jlong JNICALL Java_jdk_vm_ci_code_test_NativeCallTest_getL32SDILDS(JNIEnv *env, jclass clazz) {
  return (jlong)(intptr_t)L32SDILDS;
}

JNIEXPORT jfloat JNICALL Java_jdk_vm_ci_code_test_NativeCallTest__1L32SDILDS(JNIEnv *env, jclass clazz,
                                                                               jlong l00, jlong l01, jlong l02, jlong l03,
                                                                               jlong l04, jlong l05, jlong l06, jlong l07,
                                                                               jlong l08, jlong l09, jlong l0a, jlong l0b,
                                                                               jlong l0c, jlong l0d, jlong l0e, jlong l0f,
                                                                               jlong l10, jlong l11, jlong l12, jlong l13,
                                                                               jlong l14, jlong l15, jlong l16, jlong l17,
                                                                               jlong l18, jlong l19, jlong l1a, jlong l1b,
                                                                               jlong l1c, jlong l1d, jlong l1e, jlong l1f,
                                                                               jfloat a, jdouble b, jint c, jlong d, jdouble e, jfloat f) {
  return L32SDILDS(l00, l01, l02, l03, l04, l05, l06, l07,
                   l08, l09, l0a, l0b, l0c, l0d, l0e, l0f,
                   l10, l11, l12, l13, l14, l15, l16, l17,
                   l18, l19, l1a, l1b, l1c, l1d, l1e, l1f,
                   a,   b,   c,   d,   e,   f);
}

#ifdef __cplusplus
}
#endif

