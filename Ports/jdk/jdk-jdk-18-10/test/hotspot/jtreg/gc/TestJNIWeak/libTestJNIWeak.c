/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Native support for TestJNIWeak test.
 */

#include "jni.h"

static jweak registered = NULL;

JNIEXPORT void JNICALL
Java_gc_TestJNIWeak_TestJNIWeak_registerObject(JNIEnv* env, jclass jclazz, jobject value) {
  // assert registered == NULL
  registered = (*env)->NewWeakGlobalRef(env, value);
}

JNIEXPORT void JNICALL
Java_gc_TestJNIWeak_TestJNIWeak_unregisterObject(JNIEnv* env, jclass jclazz) {
  if (registered != NULL) {
    (*env)->DeleteWeakGlobalRef(env, registered);
    registered = NULL;
  }
}

// Directly return jweak, to be resolved by native call's return value handling.
JNIEXPORT jobject JNICALL
Java_gc_TestJNIWeak_TestJNIWeak_getReturnedWeak(JNIEnv* env, jclass jclazz) {
  // assert registered != NULL
  return registered;
}

// Directly resolve jweak and return the result.
JNIEXPORT jobject JNICALL
Java_gc_TestJNIWeak_TestJNIWeak_getResolvedWeak(JNIEnv* env, jclass jclazz) {
  // assert registered != NULL
  return (*env)->NewLocalRef(env, registered);
}
