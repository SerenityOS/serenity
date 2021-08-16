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

static jint* arr;

JNIEXPORT jboolean JNICALL
Java_TestCheckedReleaseArrayElements_init(JNIEnv *env,
                                          jclass* clazz,
                                          jintArray target) {
  jboolean isCopy;
  arr = (*env)->GetIntArrayElements(env, target, &isCopy);
  if (arr == NULL) {
    (*env)->FatalError(env, "Unexpected NULL return from GetIntArrayElements");
  }
  if (isCopy == JNI_FALSE) {
    (*env)->ReleaseIntArrayElements(env, target, arr, JNI_ABORT);
  }
  return isCopy;
}

JNIEXPORT void JNICALL
Java_TestCheckedReleaseArrayElements_cleanup(JNIEnv *env,
                                             jclass* clazz,
                                             jintArray target) {
  (*env)->ReleaseIntArrayElements(env, target, arr, JNI_ABORT);
}


JNIEXPORT void JNICALL
Java_TestCheckedReleaseArrayElements_fill(JNIEnv *env,
                                          jclass* clazz,
                                          jintArray target,
                                          jint start,
                                          jint count) {
  // Update a slice of the raw array
  int i;
  for (i = start; count > 0; i++, count--) {
    arr[i] = i;
  }
  // Write the results back to target, leaving it usable for future updates
  (*env)->ReleaseIntArrayElements(env, target, arr, JNI_COMMIT);
}
