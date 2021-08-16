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

typedef struct {
    char *name;
    char *c_cls;
    char *c_name;
    char *c_sig;
    jlocation c_loc;
} writable_exceptionInfo;

typedef struct {
    const char *name;
    const char *c_cls;
    const char *c_name;
    const char *c_sig;
    jlocation c_loc;
} exceptionInfo;

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static exceptionInfo exs[] = {
  { "Lnsk/jvmti/ExceptionCatch/excatch001c;",
    "Lnsk/jvmti/ExceptionCatch/excatch001a;", "run", "()V", 14 },
  { "Ljava/lang/ArithmeticException;",
    "Lnsk/jvmti/ExceptionCatch/excatch001a;", "run", "()V", 24 },
  { "Ljava/lang/ArrayIndexOutOfBoundsException;",
    "Lnsk/jvmti/ExceptionCatch/excatch001a;", "run", "()V", 34 }
};
static int eventsCount = 0;
static int eventsExpected = 0;

void JNICALL
ExceptionCatch(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr,
        jmethodID method, jlocation location, jobject exception) {
    jvmtiError err;
    jclass cls;
    writable_exceptionInfo ex;
    char *generic;
    size_t i;

    if (printdump == JNI_TRUE) {
        printf(">>> retrieving ExceptionCatch info ...\n");
    }
    cls = env->GetObjectClass(exception);
    err = jvmti_env->GetClassSignature(cls, &ex.name, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassSignature#e) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti_env->GetMethodDeclaringClass(method, &cls);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodDeclaringClass) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti_env->GetClassSignature(cls, &ex.c_cls, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassSignature#c) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti_env->GetMethodName(method,
        &ex.c_name, &ex.c_sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodName) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    ex.c_loc = location;
    if (printdump == JNI_TRUE) {
        printf(">>> %s\n", ex.name);
        printf(">>>    catch at %s.%s%s:0x%x%08x\n",
               ex.c_cls, ex.c_name, ex.c_sig,
               (jint)(ex.c_loc >> 32), (jint)ex.c_loc);
        printf(">>> ... done\n");
    }
    for (i = 0; i < sizeof(exs)/sizeof(exceptionInfo); i++) {
        if (ex.name != NULL && strcmp(ex.name, exs[i].name) == 0
         && ex.c_cls != NULL && strcmp(ex.c_cls, exs[i].c_cls) == 0
         && ex.c_name != NULL && strcmp(ex.c_name, exs[i].c_name) == 0
         && ex.c_sig != NULL && strcmp(ex.c_sig, exs[i].c_sig) == 0
         && ex.c_loc == exs[i].c_loc) {
            eventsCount++;
            break;
        }
    }
    if (i == sizeof(exs)/sizeof(exceptionInfo)) {
        printf("Unexpected exception catch event:\n");
        printf("  %s\n", ex.name);
        printf("     catch at %s.%s%s:0x%x%08x\n",
               ex.c_cls, ex.c_name, ex.c_sig,
               (jint)(ex.c_loc >> 32), (jint)ex.c_loc);
        result = STATUS_FAILED;
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_excatch001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_excatch001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_excatch001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiError err;
    jint res;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

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

    if (caps.can_generate_exception_events) {
        callbacks.ExceptionCatch = &ExceptionCatch;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: Exception event is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_ExceptionCatch_excatch001_check(JNIEnv *env, jclass cls) {
    jvmtiError err;
    jclass clz;
    jmethodID mid;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    if (!caps.can_generate_exception_events) {
        return result;
    }

    clz = env->FindClass("nsk/jvmti/ExceptionCatch/excatch001c");
    if (clz == NULL) {
        printf("Cannot find excatch001c class!\n");
        return STATUS_FAILED;
    }
    clz = env->FindClass("nsk/jvmti/ExceptionCatch/excatch001b");
    if (clz == NULL) {
        printf("Cannot find excatch001b class!\n");
        return STATUS_FAILED;
    }
    clz = env->FindClass("nsk/jvmti/ExceptionCatch/excatch001a");
    if (clz == NULL) {
        printf("Cannot find excatch001a class!\n");
        return STATUS_FAILED;
    }
    mid = env->GetStaticMethodID(clz, "run", "()V");
    if (mid == NULL) {
        printf("Cannot find method run!\n");
        return STATUS_FAILED;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_EXCEPTION_CATCH, NULL);
    if (err == JVMTI_ERROR_NONE) {
        eventsExpected = sizeof(exs)/sizeof(exceptionInfo);
    } else {
        printf("Failed to enable JVMTI_EVENT_EXCEPTION_CATCH: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    env->CallStaticVoidMethod(clz, mid);

    err = jvmti->SetEventNotificationMode(JVMTI_DISABLE,
            JVMTI_EVENT_EXCEPTION_CATCH, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to disable JVMTI_EVENT_EXCEPTION_CATCH: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (eventsCount != eventsExpected) {
        printf("Wrong number of exception catch events: %d, expected: %d\n",
            eventsCount, eventsExpected);
        result = STATUS_FAILED;
    }
    return result;
}

}
