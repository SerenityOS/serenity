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
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jmethodID mid1, mid2;

static jthread currThread = NULL, popThread = NULL;
static jclass currClass = NULL, popClass = NULL;
static jmethodID currMethod = NULL, popMethod = NULL;
static jboolean currFlag = JNI_FALSE, popFlag = JNI_FALSE;
static jint currLoc = 0, popLoc = 0;

void JNICALL
FramePop(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread,
        jmethodID method, jboolean wasPopedByException) {
    jvmtiError err;

    popThread = env->NewGlobalRef(thread);

    err = jvmti_env->GetMethodDeclaringClass(method, &popClass);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodDeclaringClass) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    popClass = (jclass) env->NewGlobalRef(popClass);

    popMethod = method;
    popFlag = wasPopedByException;

    err = jvmti_env->GetLocalInt(thread, 0, 1, &popLoc);
    if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY &&
            !caps.can_access_local_variables) {
        /* It is OK */
    } else if (err != JVMTI_ERROR_NONE) {
        printf("(GetLocalInt#pop) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

void JNICALL
ExceptionCatch(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread,
        jmethodID method, jlocation location, jobject exception) {
    jvmtiError err;

    if (method == mid1 || method == mid2) {
        currThread = env->NewGlobalRef(thread);

        err = jvmti_env->GetMethodDeclaringClass(
            method, &currClass);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetMethodDeclaringClass) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
            return;
        }
        currClass = (jclass) env->NewGlobalRef(currClass);

        currMethod = method;

        err = jvmti_env->GetLocalInt(thread, 0, 1, &currLoc);
        if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY &&
                !caps.can_access_local_variables) {
            /* It is OK */
        } else if (err != JVMTI_ERROR_NONE) {
            printf("(GetLocalInt#catch) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
        if (method == mid2) {
            currFlag = JNI_TRUE;
        }

        err = jvmti_env->NotifyFramePop(thread, 0);
        if (err != JVMTI_ERROR_NONE) {
            printf("(NotifyFramePop#catch) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_nframepop001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_nframepop001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_nframepop001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv!\n");
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

    if (caps.can_generate_frame_pop_events &&
            caps.can_generate_exception_events) {
        callbacks.ExceptionCatch = &ExceptionCatch;
        callbacks.FramePop = &FramePop;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: FramePop or ExceptionCatch event is not implemented\n");
    }

    if (!caps.can_access_local_variables) {
        printf("Warning: GetLocalInt is not implemented\n");
    }

    if (!caps.can_suspend) {
        printf("Warning: suspend/resume is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_NotifyFramePop_nframepop001_getMethIds(JNIEnv *env, jclass cl) {
    jvmtiError err;

    if (caps.can_generate_frame_pop_events) {
        err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_FRAME_POP, NULL);
        if (err != JVMTI_ERROR_NONE) {
            printf("Failed to enable FRAME_POP event: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
            return;
        }
    }

    mid1 = env->GetMethodID(cl, "meth01", "(I)V");
    if (mid1 == NULL) {
        printf("Cannot find method \"meth01\"\n");
        result = STATUS_FAILED;
        return;
    }

    mid2 = env->GetMethodID(cl, "meth02", "(I)V");
    if (mid2 == NULL) {
        printf("Cannot find method \"meth02\"\n");
        result = STATUS_FAILED;
        return;
    }

    if (caps.can_generate_exception_events) {
        err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_EXCEPTION_CATCH, NULL);
        if (err != JVMTI_ERROR_NONE) {
            printf("Failed to enable EXCEPTION_CATCH event: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
            return;
        }
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_NotifyFramePop_nframepop001_setFramePopNotif(JNIEnv *env,
        jclass cl, jthread thr) {
    jvmtiError err;

    if (!caps.can_generate_frame_pop_events || !caps.can_suspend) {
        return;
    }

    err = jvmti->SuspendThread(thr);
    if (err != JVMTI_ERROR_NONE) {
        printf("(SuspendThread) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    currThread = env->NewGlobalRef(thr);

    currClass = env->FindClass("nsk/jvmti/NotifyFramePop/nframepop001a");
    if (currClass == NULL) {
        printf("Cannot find nsk.jvmti.NotifyFramePop.nframepop001a class!\n");
        result = STATUS_FAILED;
        return;
    }
    currClass = (jclass) env->NewGlobalRef(currClass);

    currMethod = env->GetMethodID(currClass, "run", "()V");
    if (currMethod == NULL) {
        printf("Cannot find method \"run\"\n");
        result = STATUS_FAILED;
    }

    err = jvmti->GetLocalInt(thr, 0, 1, &currLoc);
    if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY &&
            !caps.can_access_local_variables) {
        /* It is OK */
    } else if (err != JVMTI_ERROR_NONE) {
        printf("(GetLocalInt) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti->NotifyFramePop(thr, 0);
    if (err != JVMTI_ERROR_NONE) {
        printf("(NotifyFramePop) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti->ResumeThread(thr);
    if (err != JVMTI_ERROR_NONE) {
        printf("(ResumeThread) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_NotifyFramePop_nframepop001_checkFrame(JNIEnv *env,
        jclass cls, jint point) {

    if (!caps.can_generate_frame_pop_events) {
        return;
    }

    if (!env->IsSameObject(currThread, popThread)) {
        printf("Point %d: thread is not the same as expected\n", point);
        result = STATUS_FAILED;
    }

    if (!env->IsSameObject(currClass, popClass)) {
        printf("Point %d: class is not the same as expected\n", point);
        result = STATUS_FAILED;
    }

    if (currMethod != popMethod) {
        printf("Point %d: method ID expected: 0x%p, actual: 0x%p\n",
            point, currMethod, popMethod);
        result = STATUS_FAILED;
    }

    if (currFlag != popFlag) {
        printf("Point %d: was_poped_by_exception expected: %d, actual: %d\n",
            point, currFlag, popFlag);
        result = STATUS_FAILED;
    }

    if (currLoc != popLoc) {
        printf("Point %d: local expected: %d, actual: %d\n",
            point, currLoc, popLoc);
        result = STATUS_FAILED;
    }

    currThread = NULL; popThread = NULL;
    currClass = NULL; popClass = NULL;
    currMethod = NULL; popMethod = NULL;
    currFlag = JNI_FALSE; popFlag = JNI_FALSE;
    currLoc = 0; popLoc = 0;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_NotifyFramePop_nframepop001_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
