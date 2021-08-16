/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include <atomic>
#include "jvmti.h"

extern "C" {

static jvmtiEnv* jvmti = NULL;
static jthread* threads = NULL;
static jsize threads_count = 0;
static std::atomic<bool> is_exited_from_suspend;

#define LOG(...) \
  do { \
    printf(__VA_ARGS__); \
    printf("\n"); \
    fflush(stdout); \
  } while (0)

static void
check_jvmti_status(JNIEnv* jni, jvmtiError err, const char* msg) {
  if (err != JVMTI_ERROR_NONE) {
    LOG("check_jvmti_status: JVMTI function returned error: %d", err);
    jni->FatalError(msg);
  }
}

JNIEXPORT void JNICALL
Java_SuspendWithCurrentThread_registerTestedThreads(JNIEnv *jni, jclass cls, jobjectArray threadsArr) {
  LOG("\nregisterTestedThreads: started");
  threads_count = jni->GetArrayLength(threadsArr);

  jvmtiError err = jvmti->Allocate((threads_count * sizeof(jthread)),
                                   (unsigned char**)&threads);
  check_jvmti_status(jni, err, "registerTestedThreads: error in JVMTI Allocate threads array");

  for (int i = 0; i < threads_count; i++) {
    jobject elem = jni->GetObjectArrayElement(threadsArr, i);
    threads[i] = (jthread)jni->NewGlobalRef(elem);
  }
  LOG("registerTestedThreads: finished\n");
}

/* This function is executed on the suspender thread which is not Main thread */
JNIEXPORT void JNICALL
Java_ThreadToSuspend_suspendTestedThreads(JNIEnv *jni, jclass cls) {
  jvmtiError* results = NULL;
  jvmtiError err;

  LOG("\nsuspendTestedThreads: started");
  err = jvmti->Allocate((threads_count * sizeof(jvmtiError)),
                        (unsigned char**)&results);
  check_jvmti_status(jni, err, "suspendTestedThreads: error in JVMTI Allocate results array");

  LOG("suspendTestedThreads: before JVMTI SuspendThreadList");
  err = jvmti->SuspendThreadList(threads_count, threads, results);
  is_exited_from_suspend.store(true);
  check_jvmti_status(jni, err, "suspendTestedThreads: error in JVMTI SuspendThreadList");

  LOG("suspendTestedThreads: check and print SuspendThreadList results:");
  for (int i = 0; i < threads_count; i++) {
    LOG("  thread #%d: (%d)", i, (int)results[i]);
    check_jvmti_status(jni, results[i], "suspendTestedThreads: error in SuspendThreadList results[i]");
  }
  LOG("suspendTestedThreads: finished\n");

  err = jvmti->Deallocate((unsigned char*)results);
  check_jvmti_status(jni, err, "suspendTestedThreads: error in JVMTI Deallocate results");
}

JNIEXPORT jboolean JNICALL
Java_SuspendWithCurrentThread_checkTestedThreadsSuspended(JNIEnv *jni, jclass cls) {
  LOG("checkTestedThreadsSuspended: started");

  for (int i = 0; i < threads_count; i++) {
    jint state = 0;
    jvmtiError err = jvmti->GetThreadState(threads[i], &state);
    check_jvmti_status(jni, err, "checkTestedThreadsSuspended: error in GetThreadState");

    if ((state & JVMTI_THREAD_STATE_SUSPENDED) == 0) {
      LOG("thread #%d has not been suspended yet: "
             "#   state: (%#x)", i, (int)state);
      return JNI_FALSE;
    }
  }
  if (is_exited_from_suspend.load()) {
    LOG("Thread didn't stop in self suspend.");
    return JNI_FALSE;
  }
  LOG("checkTestedThreadsSuspended: finished\n");
  return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_SuspendWithCurrentThread_resumeTestedThreads(JNIEnv *jni, jclass cls) {
  jvmtiError* results = NULL;
  jvmtiError err;

  LOG("\nresumeTestedThreads: started");
  err = jvmti->Allocate((threads_count * sizeof(jvmtiError)),
                        (unsigned char**)&results);
  check_jvmti_status(jni, err, "resumeTestedThreads: error in JVMTI Allocate results array");

  LOG("resumeTestedThreads: before JVMTI ResumeThreadList");
  err = jvmti->ResumeThreadList(threads_count, threads, results);
  check_jvmti_status(jni, err, "resumeTestedThreads: error in ResumeThreadList");

  LOG("resumeTestedThreads: check and print ResumeThreadList results:");
  for (int i = 0; i < threads_count; i++) {
    LOG("  thread #%d: (%d)", i, (int)results[i]);
    check_jvmti_status(jni, results[i], "resumeTestedThreads: error in ResumeThreadList results[i]");
  }

  err = jvmti->Deallocate((unsigned char*)results);
  check_jvmti_status(jni, err, "resumeTestedThreads: error in JVMTI Deallocate results");

  LOG("resumeTestedThreads: finished\n");
}

JNIEXPORT void JNICALL
Java_SuspendWithCurrentThread_releaseTestedThreadsInfo(JNIEnv *jni, jclass cls) {
  jvmtiError err;

  LOG("\nreleaseTestedThreadsInfo: started");

  for (int i = 0; i < threads_count; i++) {
    if (threads[i] != NULL) {
      jni->DeleteGlobalRef(threads[i]);
    }
  }
  err = jvmti->Deallocate((unsigned char*)threads);
  check_jvmti_status(jni, err, "releaseTestedThreadsInfo: error in JVMTI Deallocate threads");

  LOG("releaseTestedThreadsInfo: finished\n");
}


/** Agent library initialization. */

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
  LOG("\nAgent_OnLoad started");
  is_exited_from_suspend.store(false);
  // create JVMTI environment
  if (jvm->GetEnv((void **) (&jvmti), JVMTI_VERSION) != JNI_OK) {
    return JNI_ERR;
  }

  // add specific capabilities for suspending thread
  jvmtiCapabilities suspendCaps;
  memset(&suspendCaps, 0, sizeof(suspendCaps));
  suspendCaps.can_suspend = 1;

  jvmtiError err = jvmti->AddCapabilities(&suspendCaps);
  if (err != JVMTI_ERROR_NONE) {
    return JNI_ERR;
  }
  LOG("Agent_OnLoad finished\n");
  return JNI_OK;
}

}
