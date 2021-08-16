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

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jmethodID mid = NULL;

void JNICALL MethodExit(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method,
        jboolean was_poped_by_exception, jvalue return_value) {
    jvmtiError err;
    jint entryCount, i;
    jvmtiLocalVariableEntry *table;
    jint intVal;
    jfloat floatVal;
    jdouble doubleVal;

    if (mid == method) {
        err = jvmti->SetEventNotificationMode(JVMTI_DISABLE,
            JVMTI_EVENT_METHOD_EXIT, NULL);
        if (err != JVMTI_ERROR_NONE) {
            printf("Failed to disable metod exit event: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }
        err = jvmti->GetLocalVariableTable(mid,
            &entryCount, &table);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetLocalVariableTable) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
            return;
        }
        for (i = 0; i < entryCount; i++) {
            if (strcmp(table[i].name, "f") == 0) {
                if (printdump == JNI_TRUE) {
                    printf(">>> (float/int) type mismatch check ...\n");
                }
                err = jvmti->GetLocalInt(thr, 0, table[i].slot, &intVal);
                if (err != JVMTI_ERROR_TYPE_MISMATCH) {
                    printf("Error expected: JVMTI_ERROR_TYPE_MISMATCH,\n");
                    printf("\tactual: %s (%d)\n", TranslateError(err), err);
                    result = STATUS_FAILED;
                }
                if (printdump == JNI_TRUE) {
                    printf(">>> (float/double) type mismatch check ...\n");
                }
                err = jvmti->GetLocalDouble(thr, 0, table[i].slot, &doubleVal);
                if (err != JVMTI_ERROR_TYPE_MISMATCH) {
                    printf("Error expected: JVMTI_ERROR_TYPE_MISMATCH,\n");
                    printf("\tactual: %s (%d)\n", TranslateError(err), err);
                    result = STATUS_FAILED;
                }
                if (printdump == JNI_TRUE) {
                    printf(">>> invalid slot check ...\n");
                }
                err = jvmti->GetLocalFloat(thr, 0, 10, &floatVal);
                if (err != JVMTI_ERROR_INVALID_SLOT) {
                    printf("Error expected: JVMTI_ERROR_INVALID_SLOT,\n");
                    printf("\tactual: %s (%d)\n", TranslateError(err), err);
                    result = STATUS_FAILED;
                }
                if (printdump == JNI_TRUE) {
                    printf(">>> null pointer check ...\n");
                }
                err = jvmti->GetLocalFloat(thr, 0, table[i].slot, NULL);
                if (err != JVMTI_ERROR_NULL_POINTER) {
                    printf("Error expected: JVMTI_ERROR_NULL_POINTER,\n");
                    printf("\tactual: %s (%d)\n", TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            }
        }
        for (i = 0; i < entryCount; i++) {
            jvmti->Deallocate((unsigned char*)table[i].name);
            jvmti->Deallocate((unsigned char*)table[i].signature);
        }
        jvmti->Deallocate((unsigned char*)table);
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getlocal002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getlocal002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getlocal002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

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
        printf("Warning: Access to local variables is not implemented\n");
    } else if (caps.can_generate_method_exit_events) {
        callbacks.MethodExit = &MethodExit;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: MethodExit event is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetLocalVariable_getlocal002_getMeth(JNIEnv *env, jclass cls) {
    jvmtiError err;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (!caps.can_access_local_variables ||
        !caps.can_generate_method_exit_events) return;

    mid = env->GetMethodID(cls, "meth01", "()D");
    if (mid == NULL) {
        printf("Cannot find Method ID for meth01\n");
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_METHOD_EXIT, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable metod exit event: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetLocalVariable_getlocal002_checkLoc(JNIEnv *env,
        jclass cls, jthread thr) {
    jvmtiError err;
    jvmtiLocalVariableEntry *table;
    jint entryCount, i;
    jmethodID mid;
    jint i1;

    if (jvmti == NULL) {
        return;
    }

    mid = env->GetStaticMethodID(cls, "meth02", "()V");
    if (mid == NULL) {
        printf("Cannot find Method ID for meth02\n");
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->GetLocalVariableTable(mid, &entryCount, &table);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetLocalVariableTable) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    for (i = 0; i < entryCount; i++) {
        if (strcmp(table[i].name, "i1") == 0) {
            if (printdump == JNI_TRUE) {
                printf(">>> invalid thread check ...\n");
            }
            err = jvmti->GetLocalInt(cls, 0, table[i].slot, &i1);
            if (err != JVMTI_ERROR_INVALID_THREAD) {
                printf("Error expected: JVMTI_ERROR_INVALID_THREAD,\n");
                printf("\tactual: %s (%d)\n", TranslateError(err), err);
                result = STATUS_FAILED;
            }
            if (printdump == JNI_TRUE) {
                printf(">>> invalid depth check ...\n");
            }
            err = jvmti->GetLocalInt(thr, -1, table[i].slot, &i1);
            if (err != JVMTI_ERROR_ILLEGAL_ARGUMENT) {
                printf("Error expected: JVMTI_ERROR_ILLEGAL_ARGUMENT,\n");
                printf("\tactual: %s (%d)\n", TranslateError(err), err);
                result = STATUS_FAILED;
            }
            if (printdump == JNI_TRUE) {
                printf(">>> opaque frame check ...\n");
            }
            err = jvmti->GetLocalInt(thr, 0, table[i].slot, &i1);
            if (err != JVMTI_ERROR_OPAQUE_FRAME) {
                printf("Error expected: JVMTI_ERROR_OPAQUE_FRAME,\n");
                printf("\tactual: %s (%d)\n", TranslateError(err), err);
                result = STATUS_FAILED;
            }
        }
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetLocalVariable_getlocal002_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
