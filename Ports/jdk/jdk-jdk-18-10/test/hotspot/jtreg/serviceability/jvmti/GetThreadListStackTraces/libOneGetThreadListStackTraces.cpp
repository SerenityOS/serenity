/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, NTT DATA.
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
#include <jvmti.h>
#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>

#define MAX_FRAMES 100
#define ERR_MSG_LEN 1024

#ifdef __cplusplus
extern "C" {
#endif

static jvmtiEnv *jvmti = NULL;

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
  return jvm->GetEnv(reinterpret_cast<void**>(&jvmti), JVMTI_VERSION_11);
}

static void check_frame_info(JNIEnv *env, jvmtiFrameInfo *fi1, jvmtiFrameInfo *fi2) {
  char err_msg[ERR_MSG_LEN] = {0};
  if (fi1->method != fi2->method) { /* jvmtiFrameInfo::method */
    snprintf(err_msg, sizeof(err_msg),
             "method is different: fi1 = %" PRIxPTR ", fi2 = %" PRIxPTR,
             (uintptr_t)fi1->method, (uintptr_t)fi2->method);
    env->FatalError(err_msg);
  } else if (fi1->location != fi2->location) { /* jvmtiFrameInfo::location */
    snprintf(err_msg, sizeof(err_msg),
             "location is different: fi1 = %" PRId64 ", fi2 = %" PRId64,
             (int64_t)fi1->location, (int64_t)fi2->location);
    env->FatalError(err_msg);
  }
}

static void check_stack_info(JNIEnv *env, jvmtiStackInfo *si1, jvmtiStackInfo *si2) {
  char err_msg[ERR_MSG_LEN] = {0};

  jboolean is_same = env->IsSameObject(si1->thread, si2->thread);
  if (env->ExceptionOccurred()) {
    env->ExceptionDescribe();
    env->FatalError(__FILE__);
  }

  if (!is_same) { /* jvmtiStackInfo::thread */
    snprintf(err_msg, sizeof(err_msg),
             "thread is different: si1 = %" PRIxPTR ", si2 = %" PRIxPTR,
             (uintptr_t)si1->thread, (uintptr_t)si2->thread);
    env->FatalError(err_msg);
  } else if (si1->state != si2->state) { /* jvmtiStackInfo::state */
    snprintf(err_msg, sizeof(err_msg),
             "state is different: si1 = %d, si2 = %d", si1->state, si2->state);
    env->FatalError(err_msg);
  } else if (si1->frame_count != si2->frame_count) { /* jvmtiStackInfo::frame_count */
    snprintf(err_msg, sizeof(err_msg),
             "frame_count is different: si1 = %d, si2 = %d",
             si1->frame_count, si2->frame_count);
    env->FatalError(err_msg);
  } else {
    /* Iterate all jvmtiFrameInfo to check */
    for (int i = 0; i < si1->frame_count; i++) {
      check_frame_info(env, &si1->frame_buffer[i], &si2->frame_buffer[i]);
    }
  }
}

JNIEXPORT void JNICALL Java_OneGetThreadListStackTraces_checkCallStacks(JNIEnv *env, jclass cls, jthread thread) {
  jvmtiStackInfo *stack_info, *target_info, *target_one_info;
  jvmtiError result;
  char err_msg[ERR_MSG_LEN] = {0};

  /* Get all stack traces */
  jint num_threads;
  result = jvmti->GetAllStackTraces(MAX_FRAMES, &stack_info, &num_threads);
  if (result != JVMTI_ERROR_NONE) {
    snprintf(err_msg, sizeof(err_msg),
             "GetAllStackTraces(): result = %d", result);
    env->FatalError(err_msg);
  }

  /* Find jvmtiStackInfo for `thread` (in arguments) */
  jboolean is_same;
  target_info = NULL;
  for (jint i = 0; i < num_threads; i++) {
    is_same = env->IsSameObject(stack_info[i].thread, thread);
    if (env->ExceptionOccurred()) {
      env->ExceptionDescribe();
      env->FatalError(__FILE__);
    }
    if (is_same) {
      target_info = &stack_info[i];
      break;
    }
  }
  if (target_info == NULL) {
    env->FatalError("Target thread not found");
  }

  /*
   * Get jvmtiStackInfo via GetThreadListStackTraces().
   * It expects to perform in Thread Local Handshake because thread count is 1.
   */
  result = jvmti->GetThreadListStackTraces(1, &thread,
                                           MAX_FRAMES, &target_one_info);
  if (result != JVMTI_ERROR_NONE) {
    snprintf(err_msg, sizeof(err_msg),
             "GetThreadListStackTraces(): result = %d", result);
    env->FatalError(err_msg);
  }

  check_stack_info(env, target_info, target_one_info);

  jvmti->Deallocate(reinterpret_cast<unsigned char *>(stack_info));
  jvmti->Deallocate(reinterpret_cast<unsigned char *>(target_one_info));
}

#ifdef __cplusplus
}
#endif
