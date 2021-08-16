/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

extern "C" {

static jvmtiEnv* jvmti = NULL;

#define LOG(...) \
  do { \
    printf(__VA_ARGS__); \
    printf("\n"); \
    fflush(stdout); \
  } while (0)

JNIEXPORT jint JNICALL
Java_GetLockOwnerName_wait4ContendedEnter(JNIEnv *jni, jclass cls, jthread thr) {
  jvmtiError err;
  jint thread_state;
  do {
    err = jvmti->GetThreadState(thr, &thread_state);
    if (err != JVMTI_ERROR_NONE) {
      return err;
    }
    if ((thread_state & JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER) != 0) {
      // For this test, we need the contended monitor reference and
      // that is saved right after the thread state is updated so we
      // only return from this function when we have both.
      jobject monitor_ptr;
      err = jvmti->GetCurrentContendedMonitor(thr, &monitor_ptr);
      if (err != JVMTI_ERROR_NONE) {
        return err;
      }
      if (monitor_ptr != NULL) {
        break;
      }
    }
  } while (true);
  return err;
}

/** Agent library initialization. */

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
  LOG("\nAgent_OnLoad started");

  // create JVMTI environment
  if (jvm->GetEnv((void **) (&jvmti), JVMTI_VERSION) != JNI_OK) {
    return JNI_ERR;
  }

  // add capabilities needed for this test
  jvmtiCapabilities needCaps;
  memset(&needCaps, 0, sizeof(needCaps));
  needCaps.can_get_current_contended_monitor = 1;

  jvmtiError err = jvmti->AddCapabilities(&needCaps);
  if (err != JVMTI_ERROR_NONE) {
    return JNI_ERR;
  }
  LOG("Agent_OnLoad finished\n");
  return JNI_OK;
}

}
