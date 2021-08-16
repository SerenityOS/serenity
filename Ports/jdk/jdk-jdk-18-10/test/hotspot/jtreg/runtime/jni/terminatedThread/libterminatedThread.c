/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <stdlib.h>

#include <pthread.h>
#include <string.h>

#include "jni.h"

JavaVM* jvm;
jobject nativeThread;

static void * thread_start(void* unused) {
  JNIEnv *env;
  jclass class_id;
  jmethodID method_id;
  int res;

  printf("Native thread is running and attaching as daemon ...\n");

  res = (*jvm)->AttachCurrentThreadAsDaemon(jvm, (void **)&env, NULL);
  if (res != JNI_OK) {
    fprintf(stderr, "Test ERROR. Can't attach current thread: %d\n", res);
    exit(1);
  }

  class_id = (*env)->FindClass (env, "java/lang/Thread");
  if (class_id == NULL) {
    fprintf(stderr, "Test ERROR. Can't load class Thread\n");
    exit(1);
  }

  method_id = (*env)->GetStaticMethodID(env, class_id, "currentThread",
                                        "()Ljava/lang/Thread;");
  if (method_id == NULL) {
    fprintf(stderr, "Test ERROR. Can't find method currentThread\n");
    exit(1);
  }

  nativeThread = (*env)->CallStaticObjectMethod(env, class_id, method_id, NULL);

  if ((*env)->ExceptionOccurred(env) != NULL) {
    (*env)->ExceptionDescribe(env);
    exit(1);
  }
  printf("Native thread terminating\n");

  return NULL;
}

JNIEXPORT jobject JNICALL
Java_TestTerminatedThread_createTerminatedThread
(JNIEnv *env, jclass cls) {
  pthread_t thread;
  int res = (*env)->GetJavaVM(env, &jvm);
  if (res != JNI_OK) {
    fprintf(stderr, "Test ERROR. Can't extract JavaVM: %d\n", res);
    exit(1);
  }

  if ((res = pthread_create(&thread, NULL, thread_start, NULL)) != 0) {
    fprintf(stderr, "TEST ERROR: pthread_create failed: %s (%d)\n", strerror(res), res);
    exit(1);
  }

  if ((res = pthread_join(thread, NULL)) != 0) {
    fprintf(stderr, "TEST ERROR: pthread_join failed: %s (%d)\n", strerror(res), res);
    exit(1);
  }

  return nativeThread;
}
