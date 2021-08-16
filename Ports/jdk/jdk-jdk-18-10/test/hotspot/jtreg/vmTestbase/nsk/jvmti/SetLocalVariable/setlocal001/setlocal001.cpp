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


#define PASSED  0
#define STATUS_FAILED  2

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jmethodID mid1, mid2, mid3, mid4;
static jlong longVal = 22L;
static jfloat floatVal;
static jdouble doubleVal;
static jobject objVal;
static jobject arrVal;

void JNICALL Breakpoint(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method, jlocation location) {
    jvmtiError err;
    jmethodID mid;
    jlocation loc;
    jint entryCount;
    jvmtiLocalVariableEntry *table = NULL;
    int i;

    err = jvmti_env->GetFrameLocation(thr, 1, &mid, &loc);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetFrameLocation) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti_env->GetLocalVariableTable(mid,
        &entryCount, &table);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetLocalVariableTable) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return;
    }

    if (mid == mid1) {
        for (i = 0; i < entryCount; i++) {
            if (strcmp(table[i].name, "l") == 0) {
                err = jvmti_env->SetLocalLong(thr, 1,
                    table[i].slot, longVal);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalLong) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            } else if (strcmp(table[i].name, "f") == 0) {
                err = jvmti_env->SetLocalFloat(thr, 1,
                    table[i].slot, floatVal);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalFloat) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            } else if (strcmp(table[i].name, "d") == 0) {
                err = jvmti_env->SetLocalDouble(thr, 1,
                    table[i].slot, doubleVal);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalDouble) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            }
        }
    } else if (mid == mid2) {
        for (i = 0; i < entryCount; i++) {
            if (strcmp(table[i].name, "i1") == 0) {
                err = jvmti_env->SetLocalInt(thr, 1,
                    table[i].slot, 1);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalInt#i1) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            } else if (strcmp(table[i].name, "i2") == 0) {
                err = jvmti_env->SetLocalInt(thr, 1,
                    table[i].slot, 1);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalInt#i2) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            } else if (strcmp(table[i].name, "i3") == 0) {
                err = jvmti_env->SetLocalInt(thr, 1,
                    table[i].slot, 1);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalInt#i3) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            } else if (strcmp(table[i].name, "i4") == 0) {
                err = jvmti_env->SetLocalInt(thr, 1,
                    table[i].slot, 1);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalInt#i4) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            } else if (strcmp(table[i].name, "i5") == 0) {
                err = jvmti_env->SetLocalInt(thr, 1,
                    table[i].slot, 1);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalInt#i5) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            }
        }
    } else if (mid == mid3) {
        for (i = 0; i < entryCount; i++) {
            if (strcmp(table[i].name, "ob1") == 0) {
                err = jvmti_env->SetLocalObject(thr, 1,
                    table[i].slot, objVal);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalObject#ob1) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            } else if (strcmp(table[i].name, "ob2") == 0) {
                err = jvmti_env->SetLocalObject(thr, 1,
                    table[i].slot, arrVal);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalObject#ob2) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            }
        }
    } else if (mid == mid4) {
        for (i = 0; i < entryCount; i++) {
            if (strcmp(table[i].name, "i1") == 0) {
                err = jvmti_env->SetLocalInt(thr, 1,
                    table[i].slot, 1);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalInt#i1,param) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            } else if (strcmp(table[i].name, "i2") == 0) {
                err = jvmti_env->SetLocalInt(thr, 1,
                    table[i].slot, 2);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalInt#i2,param) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            } else if (strcmp(table[i].name, "i3") == 0) {
                err = jvmti_env->SetLocalInt(thr, 1,
                    table[i].slot, 3);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalInt#i3,param) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            } else if (strcmp(table[i].name, "i4") == 0) {
                err = jvmti_env->SetLocalInt(thr, 1,
                    table[i].slot, 4);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalInt#i4,param) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            } else if (strcmp(table[i].name, "b") == 0) {
                err = jvmti_env->SetLocalInt(thr, 1,
                    table[i].slot, JNI_TRUE);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalInt#b,param) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            } else if (strcmp(table[i].name, "l") == 0) {
                err = jvmti_env->SetLocalLong(thr, 1,
                    table[i].slot, longVal);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalLong,param) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            } else if (strcmp(table[i].name, "f") == 0) {
                err = jvmti_env->SetLocalFloat(thr, 1,
                    table[i].slot, floatVal);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalFloat,param) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            } else if (strcmp(table[i].name, "d") == 0) {
                err = jvmti_env->SetLocalDouble(thr, 1,
                    table[i].slot, doubleVal);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(SetLocalDouble,param) unexpected error: %s (%d)\n",
                           TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            }
        }
    } else {
        printf("ERROR: didn't know where we got called from");
        result = STATUS_FAILED;
    }

    if (table != NULL) {
        for (i = 0; i < entryCount; i++) {
            jvmti_env->Deallocate((unsigned char*)table[i].name);
            jvmti_env->Deallocate((unsigned char*)table[i].signature);
        }
        jvmti_env->Deallocate((unsigned char*)table);
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setlocal001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setlocal001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setlocal001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

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
Java_nsk_jvmti_SetLocalVariable_setlocal001_getMethReady(JNIEnv *env,
        jclass cls, jfloat f, jdouble d, jobject o, jobject a) {
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
    mid1 = env->GetMethodID(cls, "meth01", "()D");
    mid2 = env->GetMethodID(cls, "meth02", "(I)V");
    mid3 = env->GetStaticMethodID(cls, "meth03", "()V");
    mid4 = env->GetStaticMethodID(cls, "meth04", "(IJSDCFBZ)V");
    if (mid == 0 || mid1 == 0 || mid2 == 0 || mid3 == 0 || mid4 == 0) {
        printf("Cannot find Method ID for a method\n");
    }
    floatVal = f;
    doubleVal = d;
    objVal = env->NewGlobalRef(o);
    arrVal = env->NewGlobalRef(a);

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
Java_nsk_jvmti_SetLocalVariable_setlocal001_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
