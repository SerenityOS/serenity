/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Native support for ReturnJNIWeak test.
 */

#include "jni.h"

static jweak registered = NULL;

JNIEXPORT void JNICALL
Java_ReturnJNIWeak_registerObject(JNIEnv* env,
                                  jclass jclazz,
                                  jobject value) {
  // assert registered == NULL
  registered = (*env)->NewWeakGlobalRef(env, value);
}

JNIEXPORT void JNICALL
Java_ReturnJNIWeak_unregisterObject(JNIEnv* env, jclass jclazz) {
  if (registered != NULL) {
    (*env)->DeleteWeakGlobalRef(env, registered);
    registered = NULL;
  }
}

JNIEXPORT jobject JNICALL
Java_ReturnJNIWeak_getObject(JNIEnv* env, jclass jclazz) {
  // assert registered != NULL
  return registered;
}
