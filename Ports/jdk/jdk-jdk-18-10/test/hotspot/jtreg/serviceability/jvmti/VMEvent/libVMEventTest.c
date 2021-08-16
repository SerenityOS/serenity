/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, Google and/or its affiliates. All rights reserved.
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

#include <string.h>
#include "jvmti.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifndef JNI_ENV_ARG

#ifdef __cplusplus
#define JNI_ENV_ARG(x)
#define JNI_ENV_ARGS2(x, y) y
#define JNI_ENV_ARGS3(x, y, z) y, z
#define JNI_ENV_ARGS4(x, y, z, w) y, z, w
#define JNI_ENV_PTR(x) x
#else
#define JNI_ENV_ARG(x) x
#define JNI_ENV_ARGS2(x,y) x, y
#define JNI_ENV_ARGS3(x, y, z) x, y, z
#define JNI_ENV_ARGS4(x, y, z, w) x, y, z, w
#define JNI_ENV_PTR(x) (*x)
#endif

#endif

extern JNIEXPORT void JNICALL VMObjectAlloc(jvmtiEnv *jvmti,
                                            JNIEnv* jni,
                                            jthread thread,
                                            jobject object,
                                            jclass klass,
                                            jlong size) {
  char *signature = NULL;
  jvmtiError error = (*jvmti)->GetClassSignature(jvmti, klass, &signature, NULL);

  if (error != JVMTI_ERROR_NONE || signature == NULL) {
    JNI_ENV_PTR(jni)->FatalError(
        JNI_ENV_ARGS2(jni, "Failed during the GetClassSignature call"));
  }

  // If it is our test class, call clone now.
  if (!strcmp(signature, "LMyPackage/VMEventRecursionTest;")) {
    jmethodID clone_method =
        JNI_ENV_PTR(jni)->GetMethodID(JNI_ENV_ARGS4(jni, klass, "clone", "()Ljava/lang/Object;"));

    if (JNI_ENV_PTR(jni)->ExceptionOccurred(JNI_ENV_ARG(jni))) {
      JNI_ENV_PTR(jni)->FatalError(
          JNI_ENV_ARGS2(jni, "Failed during the GetMethodID call"));
    }

    JNI_ENV_PTR(jni)->CallObjectMethod(JNI_ENV_ARGS3(jni, object, clone_method));

    if (JNI_ENV_PTR(jni)->ExceptionOccurred(JNI_ENV_ARG(jni))) {
      JNI_ENV_PTR(jni)->FatalError(
          JNI_ENV_ARGS2(jni, "Failed during the CallObjectMethod call"));
    }
  }
}

extern JNIEXPORT void JNICALL OnVMInit(jvmtiEnv *jvmti, JNIEnv *jni, jthread thread) {
  (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_VM_OBJECT_ALLOC, NULL);
}

extern JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options,
                                           void *reserved) {
  jvmtiEnv *jvmti;
  jvmtiEventCallbacks callbacks;
  jvmtiCapabilities caps;

  if ((*jvm)->GetEnv(jvm, (void **) (&jvmti), JVMTI_VERSION) != JNI_OK) {
    return JNI_ERR;
  }

  memset(&callbacks, 0, sizeof(callbacks));
  callbacks.VMObjectAlloc = &VMObjectAlloc;
  callbacks.VMInit = &OnVMInit;

  memset(&caps, 0, sizeof(caps));
  caps.can_generate_vm_object_alloc_events = 1;
  (*jvmti)->AddCapabilities(jvmti, &caps);

  (*jvmti)->SetEventCallbacks(jvmti, &callbacks, sizeof(jvmtiEventCallbacks));
  (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);
  return 0;
}

#ifdef __cplusplus
}
#endif
