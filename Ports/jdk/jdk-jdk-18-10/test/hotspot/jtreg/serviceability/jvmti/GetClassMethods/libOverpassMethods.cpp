/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
#include <string.h>
#include "jvmti.h"

#ifdef __cplusplus
extern "C" {
#endif

#define ACC_STATIC 0x0008

static jvmtiEnv *jvmti = NULL;

JNIEXPORT
jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
  return JNI_VERSION_9;
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
  vm->GetEnv((void **)&jvmti, JVMTI_VERSION_11);

  if (options != NULL && strcmp(options, "maintain_original_method_order") == 0) {
    printf("Enabled capability: maintain_original_method_order\n");
    jvmtiCapabilities caps = {};
    caps.can_maintain_original_method_order = 1;

    jvmtiError err = jvmti->AddCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
      printf("Agent_OnLoad: AddCapabilities failed with error: %d\n", err);
      return JNI_ERR;
    }
  }
  return JNI_OK;
}

JNIEXPORT jobjectArray JNICALL Java_OverpassMethods_getJVMTIDeclaredMethods(JNIEnv *env, jclass static_klass, jclass klass) {
  jint method_count = 0;
  jmethodID* methods = NULL;
  jvmtiError err = jvmti->GetClassMethods(klass, &method_count, &methods);
  if (err != JVMTI_ERROR_NONE) {
    printf("GetClassMethods failed with error: %d\n", err);
    return NULL;
  }

  jclass method_cls = env->FindClass("java/lang/reflect/Method");
  if (method_cls == NULL) {
    printf("FindClass (Method) failed\n");
    return NULL;
  }
  jobjectArray array = env->NewObjectArray(method_count, method_cls, NULL);
  if (array == NULL) {
    printf("NewObjectArray failed\n");
    return NULL;
  }

  for (int i = 0; i < method_count; i++) {
    jint modifiers = 0;
    err = jvmti->GetMethodModifiers(methods[i], &modifiers);
    if (err != JVMTI_ERROR_NONE) {
      printf("GetMethodModifiers failed with error: %d\n", err);
      return NULL;
    }

    jobject m = env->ToReflectedMethod(klass, methods[i], (modifiers & ACC_STATIC) == ACC_STATIC);
    if (array == NULL) {
      printf("ToReflectedMethod failed\n");
      return NULL;
    }
    env->SetObjectArrayElement(array, i, m);

    env->DeleteLocalRef(m);
  }
  jvmti->Deallocate((unsigned char *)methods);

  return array;
}
#ifdef __cplusplus
}
#endif
