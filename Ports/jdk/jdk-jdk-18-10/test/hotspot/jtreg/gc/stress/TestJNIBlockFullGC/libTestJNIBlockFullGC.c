/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2017 SAP SE and/or its affiliates. All rights reserved.
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

#include "jni.h"

JNIEXPORT jint JNICALL
Java_gc_stress_TestJNIBlockFullGC_TestJNIBlockFullGC_TestCriticalArray0(JNIEnv *env, jclass jCls, jintArray jIn) {
  jint *bufIn = NULL;
  jint jInLen = (*env)->GetArrayLength(env, jIn);
  jint result = 0;
  jint i;

  if (jInLen != 0) {
    bufIn = (jint*)(*env)->GetPrimitiveArrayCritical(env, jIn, 0);
  }

  for (i = 0; i < jInLen; ++i) {
    result += bufIn[i]; // result = sum of all array elements
  }

  if (bufIn != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, jIn, bufIn, 0);
  }

  return result;
}

