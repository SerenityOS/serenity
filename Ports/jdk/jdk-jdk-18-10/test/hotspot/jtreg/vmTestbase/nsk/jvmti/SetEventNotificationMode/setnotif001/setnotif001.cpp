/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
#define SCALE_SIZE (JVMTI_MAX_EVENT_TYPE_VAL + 1)

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jrawMonitorID access_lock;
static jobject notifyFramePopThread = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static int flag = 0;
static unsigned char enbl_scale[SCALE_SIZE];
static unsigned char ev_scale[SCALE_SIZE];

void mark(jvmtiEnv *jvmti_env, jvmtiEvent kind) {
    jvmtiError err;

    if (printdump == JNI_TRUE) {
        printf(">>> catching %s\n", TranslateEvent(kind));
    }
    err = jvmti_env->RawMonitorEnter(access_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorEnter) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    if (enbl_scale[kind] != 1) {
        printf("Wrong notification: event %s (%d) has not been enabled\n",
               TranslateEvent(kind), kind);
        result = STATUS_FAILED;
    }
    ev_scale[kind] = 1;
    err = jvmti_env->RawMonitorExit(access_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorExit) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

void disable(jvmtiEnv *jvmti_env, jvmtiEvent kind) {
    jvmtiError err;

    if (printdump == JNI_TRUE) {
        printf(">>> disabling %s\n", TranslateEvent(kind));
    }
    err = jvmti_env->SetEventNotificationMode(
        JVMTI_DISABLE, kind, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Fail to disable %s: %s (%d)\n",
               TranslateEvent(kind), TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

void enable(jvmtiEnv *jvmti_env, jvmtiEvent kind) {
    jvmtiError err;

    if (printdump == JNI_TRUE) {
        printf(">>> enabling %s\n", TranslateEvent(kind));
    }
    err = jvmti_env->RawMonitorEnter(access_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorEnter) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    err = jvmti_env->SetEventNotificationMode(
        JVMTI_ENABLE, kind, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Fail to enable %s: %s (%d)\n",
               TranslateEvent(kind), TranslateError(err), err);
        result = STATUS_FAILED;
    } else {
        enbl_scale[kind] = 1;
    }
    err = jvmti_env->RawMonitorExit(access_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RawMonitorExit) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

void setWatches(jvmtiEnv *jvmti_env, JNIEnv *env, jclass cls) {
    jvmtiError err;
    jfieldID fid;
    jmethodID mid;

    mid = env->GetStaticMethodID(cls, "meth01", "(I)V");
    if (mid == NULL) {
      printf("(GetStaticMethodID) returns NULL");
      result = STATUS_FAILED;
      return;
    }
    err = jvmti->SetBreakpoint(mid, 0);
    if (err == JVMTI_ERROR_NONE) {
        enable(jvmti_env, JVMTI_EVENT_BREAKPOINT);
    } else {
        result = STATUS_FAILED;
        printf("(SetBreakpoint) unexpected error: %s (%d)\n",
               TranslateError(err), err);
    }

    fid = env->GetStaticFieldID(cls, "fld", "I");
    if (caps.can_generate_field_access_events) {
        err = jvmti->SetFieldAccessWatch(cls, fid);
        if (err == JVMTI_ERROR_NONE) {
            enable(jvmti_env, JVMTI_EVENT_FIELD_ACCESS);
        } else {
            result = STATUS_FAILED;
            printf("(SetFieldAccessWatch) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
        }
    } else {
        printf("Warning: SetFieldAccessWatch is not implemented\n");
    }

    if (caps.can_generate_field_modification_events) {
        err = jvmti_env->SetFieldModificationWatch(cls, fid);
        if (err == JVMTI_ERROR_NONE) {
            enable(jvmti_env, JVMTI_EVENT_FIELD_MODIFICATION);
        } else {
            result = STATUS_FAILED;
            printf("(SetFieldModificationWatch) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
        }
    } else {
        printf("Warning: SetFieldModificationWatch is not implemented\n");
    }
}

void JNICALL VMInit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr) {
    if (printdump == JNI_TRUE) {
        printf(">>> VMInit\n");
    }
    enable(jvmti_env, JVMTI_EVENT_SINGLE_STEP);
    enable(jvmti_env, JVMTI_EVENT_EXCEPTION);
    enable(jvmti_env, JVMTI_EVENT_EXCEPTION_CATCH);
    mark(jvmti_env, JVMTI_EVENT_VM_INIT);
    flag = 1;
}

void JNICALL SingleStep(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jlocation location) {
    if (flag) {
        mark(jvmti_env, JVMTI_EVENT_SINGLE_STEP);
        disable(jvmti_env, JVMTI_EVENT_SINGLE_STEP);
    }
}

void JNICALL MethodEntry(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method) {
    jvmtiError err;
    jboolean isNative;

    if (flag) {
        mark(jvmti_env, JVMTI_EVENT_METHOD_ENTRY);
        if (env->IsSameObject(notifyFramePopThread, thr)) {
            err = jvmti_env->IsMethodNative(method, &isNative);
            if (err != JVMTI_ERROR_NONE) {
                result = STATUS_FAILED;
                printf("(IsMethodNative) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
            }
            if (isNative == JNI_FALSE) {
                err = jvmti_env->NotifyFramePop(thr, 0);
                if (err == JVMTI_ERROR_NONE) {
                    enable(jvmti_env, JVMTI_EVENT_FRAME_POP);
                } else {
                    result = STATUS_FAILED;
                    printf("(NotifyFramePop) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                }
            }
            enable(jvmti_env, JVMTI_EVENT_CLASS_LOAD);
            enable(jvmti_env, JVMTI_EVENT_CLASS_PREPARE);
            disable(jvmti_env, JVMTI_EVENT_METHOD_ENTRY);
        }
    }
}

void JNICALL ExceptionCatch(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr,
        jmethodID method, jlocation location, jobject exception) {
    if (flag) {
        mark(jvmti_env, JVMTI_EVENT_EXCEPTION_CATCH);
    }
}

void JNICALL MethodExit(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method,
        jboolean was_poped_by_exc, jvalue return_value) {
    if (flag) {
        mark(jvmti_env, JVMTI_EVENT_METHOD_EXIT);
        disable(jvmti_env, JVMTI_EVENT_METHOD_EXIT);
    }
}

void JNICALL ThreadStart(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread) {
    if (flag) {
        mark(jvmti_env, JVMTI_EVENT_THREAD_START);
    }
}

void JNICALL ThreadEnd(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread) {
    if (flag) {
        mark(jvmti_env, JVMTI_EVENT_THREAD_END);
    }
}

void JNICALL ClassLoad(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jclass klass) {
    if (flag) {
        mark(jvmti_env, JVMTI_EVENT_CLASS_LOAD);
    }
}

void JNICALL ClassPrepare(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jclass klass) {
    if (flag) {
        mark(jvmti_env, JVMTI_EVENT_CLASS_PREPARE);
    }
}

void JNICALL Exception(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr,
        jmethodID method, jlocation location, jobject exception,
        jmethodID catch_method, jlocation catch_location) {
    if (flag) {
        mark(jvmti_env, JVMTI_EVENT_EXCEPTION);
    }
}

void JNICALL FieldAccess(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr,
        jmethodID method, jlocation location, jclass field_klass, jobject obj, jfieldID field) {
    if (flag) {
        mark(jvmti_env, JVMTI_EVENT_FIELD_ACCESS);
    }
}

void JNICALL FieldModification(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method, jlocation location, jclass field_klass, jobject obj,
        jfieldID field, char sig, jvalue new_value) {
    if (flag) {
        mark(jvmti_env, JVMTI_EVENT_FIELD_MODIFICATION);
    }
}

void JNICALL Breakpoint(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jlocation location) {
    if (flag) {
        mark(jvmti_env, JVMTI_EVENT_BREAKPOINT);
    }
}

void JNICALL FramePop(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jboolean wasPopedByException) {
    if (flag) {
        mark(jvmti_env, JVMTI_EVENT_FRAME_POP);
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setnotif001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setnotif001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setnotif001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    memset(enbl_scale, 0, SCALE_SIZE);
    memset(ev_scale, 0, SCALE_SIZE);

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

    err = jvmti->CreateRawMonitor("_access_lock", &access_lock);
    if (err != JVMTI_ERROR_NONE) {
        printf("(CreateRawMonitor) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    callbacks.VMInit = &VMInit;
    callbacks.ThreadStart = &ThreadStart;
    callbacks.ThreadEnd = &ThreadEnd;
    callbacks.ClassLoad = &ClassLoad;
    callbacks.ClassPrepare = &ClassPrepare;

    if (caps.can_generate_method_entry_events) {
        callbacks.MethodEntry = &MethodEntry;
    }
    if (caps.can_generate_method_exit_events) {
        callbacks.MethodExit = &MethodExit;
    }
    if (caps.can_generate_breakpoint_events) {
        callbacks.Breakpoint = &Breakpoint;
    }
    if (caps.can_generate_single_step_events) {
        callbacks.SingleStep = &SingleStep;
    }
    if (caps.can_generate_frame_pop_events) {
        callbacks.FramePop = &FramePop;
    }
    if (caps.can_generate_exception_events) {
        callbacks.Exception = &Exception;
        callbacks.ExceptionCatch = &ExceptionCatch;
    }
    if (caps.can_generate_field_access_events) {
        callbacks.FieldAccess = &FieldAccess;
    }
    if (caps.can_generate_field_modification_events) {
        callbacks.FieldModification = &FieldModification;
    }
    err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    if (err != JVMTI_ERROR_NONE) {
        printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    enable(jvmti, JVMTI_EVENT_VM_INIT);

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_SetEventNotificationMode_setnotif001_enableEv(JNIEnv *env,
        jclass cls, jobject framePopThread) {
    setWatches(jvmti, env, cls);
    notifyFramePopThread = env->NewGlobalRef(framePopThread);
    enable(jvmti, JVMTI_EVENT_METHOD_ENTRY);
    enable(jvmti, JVMTI_EVENT_METHOD_EXIT);
    enable(jvmti, JVMTI_EVENT_THREAD_START);
    enable(jvmti, JVMTI_EVENT_THREAD_END);
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_SetEventNotificationMode_setnotif001_getRes(JNIEnv *env,
        jclass cls) {
    jint i;

    for (i = 0; i < SCALE_SIZE; i++) {
        jvmtiEvent kind = (jvmtiEvent) i;
        if (enbl_scale[i] == 1 && ev_scale[i] == 0) {
            printf("No notification: event %s (%d)\n", TranslateEvent(kind), i);
            result = STATUS_FAILED;
        }
        if (printdump == JNI_TRUE && ev_scale[i] > 0) {
            printf(">>> %s (%d), notifications: %d\n",
                   TranslateEvent(kind), i, ev_scale[i]);
        }
    }
    return result;
}

}
