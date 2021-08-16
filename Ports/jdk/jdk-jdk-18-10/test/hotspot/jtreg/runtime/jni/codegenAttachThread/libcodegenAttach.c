/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Azul Systems, Inc. All rights reserved.
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
#include <stdbool.h>

#include <string.h>

#include "jni.h"

#if defined(__APPLE__) && defined(__aarch64__)

#include <pthread.h>
#include <sys/mman.h>

JavaVM* jvm;

static void* codegen;

static int thread_start2(int val) {
  JNIEnv *env;
  jclass class_id;
  jmethodID method_id;
  int res;

  printf("Native thread is running and attaching ...\n");

  res = (*jvm)->AttachCurrentThread(jvm, (void **)&env, NULL);
  if (res != JNI_OK) {
    fprintf(stderr, "Test ERROR. Can't attach current thread: %d\n", res);
    exit(1);
  }

  res = (*jvm)->DetachCurrentThread(jvm);
  if (res != JNI_OK) {
    fprintf(stderr, "Test ERROR. Can't detach current thread: %d\n", res);
    exit(1);
  }

  printf("Native thread is about to finish\n");
  return 1 + val;
}

static int trampoline(int(*fn)(int), int arg) {
  int val = fn(arg);
  // ensure code in MAP_JIT area after target function returns
  return 1 + val;
}

static void * thread_start(void* unused) {
  int val = ((int(*)(int(*)(int),int))codegen)(thread_start2, 10);
  printf("return val = %d\n", val);
  return NULL;
}

JNIEXPORT void JNICALL
Java_TestCodegenAttach_testCodegenAttach
(JNIEnv *env, jclass cls) {

  codegen = mmap(NULL, 0x1000,
      PROT_READ | PROT_WRITE | PROT_EXEC,
      MAP_PRIVATE | MAP_ANONYMOUS | MAP_JIT, -1, 0);
  if (codegen == MAP_FAILED) {
    perror("mmap");
    exit(1);
  }

  pthread_jit_write_protect_np(false);

  memcpy(codegen, trampoline, 128);

  pthread_jit_write_protect_np(true);

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
}

#else

JNIEXPORT void JNICALL
Java_TestCodegenAttach_testCodegenAttach
(JNIEnv *env, jclass cls) {
  printf("should not reach here\n");
  exit(1);
}

#endif // __APPLE__ && __aarch64__
