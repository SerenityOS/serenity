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
#define INV_SLOT (-1)

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;

void JNICALL Breakpoint(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method, jlocation location) {
    jvmtiError err;
    jmethodID mid;
    jlocation loc;
    jint entryCount;
    jvmtiLocalVariableEntry *table;
    int i;

    err = jvmti_env->GetFrameLocation(thr, 1, &mid, &loc);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetFrameLocation) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> obtaining local variables mapping ...\n");
    }

    err = jvmti_env->GetLocalVariableTable(mid, &entryCount, &table);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetLocalVariableTable) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> checking on invalid slot ...\n");
    }
    for (i = 0; i < entryCount; i++) {
        if (strcmp(table[i].name, "o") == 0) {
            err = jvmti_env->SetLocalObject(thr, 1,
                INV_SLOT, (jobject)thr);
            if (err != JVMTI_ERROR_INVALID_SLOT) {
                printf("(%s) ", table[i].name);
                printf("Error expected: JVMTI_ERROR_INVALID_SLOT,\n");
                printf("\t    actual: %s (%d)\n", TranslateError(err), err);
                result = STATUS_FAILED;
            }
        } else if (strcmp(table[i].name, "i") == 0) {
            err = jvmti_env->SetLocalInt(thr, 1,
                INV_SLOT, (jint)0);
            if (err != JVMTI_ERROR_INVALID_SLOT) {
                printf("(%s) ", table[i].name);
                printf("Error expected: JVMTI_ERROR_INVALID_SLOT,\n");
                printf("\t    actual: %s (%d)\n", TranslateError(err), err);
                result = STATUS_FAILED;
            }
        } else if (strcmp(table[i].name, "l") == 0) {
            err = jvmti_env->SetLocalLong(thr, 1,
                INV_SLOT, (jlong)0);
            if (err != JVMTI_ERROR_INVALID_SLOT) {
                printf("(%s) ", table[i].name);
                printf("Error expected: JVMTI_ERROR_INVALID_SLOT,\n");
                printf("\t    actual: %s (%d)\n", TranslateError(err), err);
                result = STATUS_FAILED;
            }
        } else if (strcmp(table[i].name, "f") == 0) {
            err = jvmti_env->SetLocalFloat(thr, 1,
                INV_SLOT, (jfloat)0);
            if (err != JVMTI_ERROR_INVALID_SLOT) {
                printf("(%s) ", table[i].name);
                printf("Error expected: JVMTI_ERROR_INVALID_SLOT,\n");
                printf("\t    actual: %s (%d)\n", TranslateError(err), err);
                result = STATUS_FAILED;
            }
        } else if (strcmp(table[i].name, "d") == 0) {
            err = jvmti_env->SetLocalDouble(thr, 1,
                INV_SLOT, (jdouble)0);
            if (err != JVMTI_ERROR_INVALID_SLOT) {
                printf("(%s) ", table[i].name);
                printf("Error expected: JVMTI_ERROR_INVALID_SLOT,\n");
                printf("\t    actual: %s (%d)\n", TranslateError(err), err);
                result = STATUS_FAILED;
            }
        }
    }

    if (printdump == JNI_TRUE) {
        printf(">>> ... done\n");
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setlocal003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setlocal003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setlocal003(JavaVM *jvm, char *options, void *reserved) {
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

    if (!caps.can_access_local_variables) {
        printf("Warning: access to local variables is not implemented\n");
    } else if (caps.can_generate_breakpoint_events) {
        callbacks.Breakpoint = &Breakpoint;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: Breakpoint event is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_SetLocalVariable_setlocal003_getReady(JNIEnv *env, jclass cls) {
    jvmtiError err;
    jmethodID mid;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (!caps.can_access_local_variables ||
        !caps.can_generate_breakpoint_events) return;

    mid = env->GetStaticMethodID(cls, "checkPoint", "()V");
    if (mid == NULL) {
        printf("Cannot find Method ID for method checkPoint\n");
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->SetBreakpoint(mid, 0);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to SetBreakpoint: %s (%d)\n", TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_BREAKPOINT, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable BREAKPOINT event: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_SetLocalVariable_setlocal003_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
