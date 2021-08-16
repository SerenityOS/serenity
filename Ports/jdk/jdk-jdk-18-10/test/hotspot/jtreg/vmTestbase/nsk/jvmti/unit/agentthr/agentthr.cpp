/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

static JavaVM *jvm_ins;
static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jboolean agent_was_started = JNI_FALSE;
static jboolean done = JNI_FALSE;


jthread jthr(JNIEnv *env) {
    jclass thrClass;
    jmethodID cid;
    jthread res;

    thrClass = env->FindClass("java/lang/Thread");
    cid = env->GetMethodID(thrClass, "<init>", "()V");
    res = env->NewObject(thrClass, cid);
    return res;
}

void JNICALL agent_start(jvmtiEnv* jvmti_env, JNIEnv* jni_env, void *p) {
    JNIEnv *env;

    if (jvmti_env != jvmti) {
        printf("(agent_start) JVMTI envs don't match\n");
        result = STATUS_FAILED;
    }

    jvm_ins->GetEnv((void **) &env, JNI_VERSION_1_2);
    if (jni_env != env) {
        printf("(agent_start) JNI envs don't match\n");
        result = STATUS_FAILED;
    }

    if ((size_t)p != 12345) {
        printf("(agent_start) args don't match\n");
        result = STATUS_FAILED;
    }
    done = JNI_TRUE;
}



void JNICALL vm_init(jvmtiEnv* jvmti_env, JNIEnv *env, jthread thread) {
    jvmtiError err;

    if (!agent_was_started) {
        agent_was_started = JNI_TRUE;

        err = jvmti->RunAgentThread(jthr(env), agent_start,
            (void *)999, JVMTI_THREAD_MAX_PRIORITY+1);
        if (err != JVMTI_ERROR_INVALID_PRIORITY) {
            printf("(RunAgentThread#1) expected JVMTI_ERROR_INVALID_PRIORITY got error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
        err = jvmti->RunAgentThread(jthr(env), agent_start,
            (void *)999, JVMTI_THREAD_MIN_PRIORITY-1);
        if (err != JVMTI_ERROR_INVALID_PRIORITY) {
            printf("(RunAgentThread#1) expected JVMTI_ERROR_INVALID_PRIORITY got error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
        err = jvmti->RunAgentThread(jthr(env), agent_start,
            (void *)12345, JVMTI_THREAD_NORM_PRIORITY);
        if (err != JVMTI_ERROR_NONE) {
            printf("(RunAgentThread#2) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_agentthr(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_agentthr(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_agentthr(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiError err;
    jint res;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    jvm_ins = jvm;
    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    callbacks.VMInit = &vm_init;
    err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    if (err != JVMTI_ERROR_NONE) {
        printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable JVMTI_EVENT_THREAD_START: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_unit_agentthr_isOver(JNIEnv *env, jclass cls) {
    return done;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_agentthr_getRes(JNIEnv *env, jclass cls) {
    if (!done) {
        printf("agent thread has not completed\n");
        result = STATUS_FAILED;
    }
    return result;
}

}
