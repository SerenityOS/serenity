/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include <stdio.h>
#include "jnihelper.h"

extern "C" {

JNIEXPORT void JNICALL
Java_nsk_stress_jni_JNIter007_incCount (JNIEnv *env, jobject jobj, jstring name) {
  jclass clazz;
  jfieldID fld;
  jint value;
  const char *str = env->GetStringUTFChars(name, 0); CE

  CHECK(env->MonitorEnter(jobj));
  clazz = env->GetObjectClass(jobj); CE
  fld = env->GetStaticFieldID(clazz, "nativeCount", "I"); CE
  value = env->GetStaticIntField(clazz, fld); CE
  env->SetStaticIntField(clazz, fld, (jint)(++value)); CE
  CHECK(env->MonitorExit(jobj));
  if (value % 1000 == 0) {
    printf("in %s Count after %u\n", str, value);
  }
}

}
