/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

JNIEXPORT jboolean JNICALL
Java_TestCheckedReleaseCriticalArray_modifyArray(JNIEnv *env,
                                                 jclass clazz,
                                                 jintArray iarr) {
  jboolean isCopy;
  jint* arr = (jint *)(*env)->GetPrimitiveArrayCritical(env, iarr, &isCopy);
  if (arr == NULL) {
    (*env)->FatalError(env, "Unexpected NULL return from GetPrimitiveArrayCritical");
  }
  if (isCopy == JNI_FALSE) {
    jint len = (*env)->GetArrayLength(env, iarr);
    // make arbitrary changes to the array
    for (int i = 0; i < len; i++) {
      arr[i] *= 2;
    }
    // write-back using JNI_COMMIT to test for memory leak
    (*env)->ReleasePrimitiveArrayCritical(env, iarr, arr, JNI_COMMIT);
  }
  // we skip the test if the VM makes a copy - as it will definitely leak
  return !isCopy;
}
