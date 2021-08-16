/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <string.h>
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jint result = PASSED;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_framecnt001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_framecnt001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_framecnt001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiError err;
    jint res;

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv !\n");
        return JNI_ERR;
    }

    err = jvmti->GetPotentialCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetPotentialCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->AddCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(AddCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->GetCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    if (!caps.can_suspend) {
        printf("Warning: suspend/resume is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL Java_nsk_jvmti_GetFrameCount_framecnt001_checkFrames(JNIEnv *env, jclass cls,
        jthread thr, jint thr_num, jint fnum) {
    jvmtiError err;
    jint frameCount;

    if (!caps.can_suspend) {
        return;
    }

    if (thr_num != 0) {
        err = jvmti->SuspendThread(thr);
        if (err != JVMTI_ERROR_NONE) {
            printf("(SuspendThread#%d) unexpected error: %s (%d)\n",
                   thr_num, TranslateError(err), err);
            result = STATUS_FAILED;
            return;
        }
    }
    err = jvmti->GetFrameCount(thr, &frameCount);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetFrameCount#%d) unexpected error: %s (%d)\n",
               thr_num, TranslateError(err), err);
        result = STATUS_FAILED;
    } else if (frameCount != fnum) {
        printf("Thread #%d: number of frames expected: %d, got: %d\n",
               thr_num, fnum, frameCount);
        result = STATUS_FAILED;
    }
    if (thr_num != 0) {
        err = jvmti->ResumeThread(thr);
        if (err != JVMTI_ERROR_NONE) {
            printf("(ResumeThread#%d) unexpected error: %s (%d)\n",
                   thr_num, TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }
}

JNIEXPORT jint JNICALL Java_nsk_jvmti_GetFrameCount_framecnt001_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
