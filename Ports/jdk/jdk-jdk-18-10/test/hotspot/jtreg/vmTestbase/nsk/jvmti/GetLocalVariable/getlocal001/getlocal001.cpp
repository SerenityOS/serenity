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


#ifndef JNI_EARG


#endif

#define PASSED 0
#define STATUS_FAILED 2

static jvmtiEnv *jvmti;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jmethodID mid1, mid2, mid3, mid4;
static jfloat fval;
static jdouble dval;

void check1(jvmtiEnv *jvmti_env, jthread thr, jint depth,
        jvmtiLocalVariableEntry *table, jint count) {
    jvmtiError err;
    jlong l = 0;
    jfloat f = 0.0;
    jdouble d = 0.0;
    int i;

    for (i = 0; i < count; i++) {
        if (strcmp(table[i].name, "l") == 0) {
            err = jvmti_env->GetLocalLong(thr, depth, table[i].slot, &l);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalLong#1) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            }
        } else if (strcmp(table[i].name, "f") == 0) {
            err = jvmti_env->GetLocalFloat(thr, depth, table[i].slot, &f);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalFloat#1) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            }
        } else if (strcmp(table[i].name, "d") == 0) {
            err = jvmti_env->GetLocalDouble(thr, depth, table[i].slot, &d);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalDouble#1) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            }
        }
    }
    if ((l != 22) || (f != 6.0) || (d != 7.0)) {
        result = STATUS_FAILED;
        printf("One of values retrieved by GetLocal is wrong (hook):\n");
        printf("    actual: long = 0x%08x%08x, float = %f, double = %f\n",
               (jint)(l >> 32), (jint)l, f, d);
        printf("  expected: long = 0x%08x%08x, float = %f, double = %f\n",
               (jint)0, (jint)22, (double)6.0, (double)7.0);
    }
}

void check2(jvmtiEnv *jvmti_env, jthread thr, jint depth,
        jvmtiLocalVariableEntry *table, jint count) {
    jvmtiError err;
    jint i1 = 0, i2 = 0, i3 = 0, i4 = 0, i5 = JNI_FALSE;
    int i;

    for (i = 0; i < count; i++) {
        if (strcmp(table[i].name, "i1") == 0) {
            err = jvmti_env->GetLocalInt(thr, depth, table[i].slot, &i1);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalInt#2i1) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            }
        } else if (strcmp(table[i].name, "i2") == 0) {
            err = jvmti_env->GetLocalInt(thr, depth, table[i].slot, &i2);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalInt#2i2) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            }
        } else if (strcmp(table[i].name, "i3") == 0) {
            err = jvmti_env->GetLocalInt(thr, depth, table[i].slot, &i3);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalInt#2i3) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            }
        } else if (strcmp(table[i].name, "i4") == 0) {
            err = jvmti_env->GetLocalInt(thr, depth, table[i].slot, &i4);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalInt#2i4) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            }
        } else if (strcmp(table[i].name, "i5") == 0) {
            err = jvmti_env->GetLocalInt(thr, depth, table[i].slot, &i5);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalInt#2i5) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            }
        }
    }
    if ((i1 != 1) || (i2 != 1) || (i3 != 1) || (i4 != 1) || (i5 != JNI_TRUE)) {
        result = STATUS_FAILED;
        printf("One of values retrieved by GetLocal is wrong (locals):\n");
        printf("    actual: int=%d, short=%d, char=%d, byte=%d, boolean=%d\n",
               i1, i2, i3, i4, i5);
        printf("  expected: int=%d, short=%d, char=%d, byte=%d, boolean=%d\n",
               1, 1, 1, 1, JNI_TRUE);
    }
}

void check3(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr, jint depth,
        jvmtiLocalVariableEntry *table, jint count, jmethodID mid) {
    jvmtiError err;
    jobject ob1 = NULL, ob2 = NULL;
    jclass cls;
    jfieldID fid;
    jint fldVal = 0;
    jint arr[10];
    int i;

    for (i = 0; i < count; i++) {
        if (strcmp(table[i].name, "ob1") == 0) {
            err = jvmti_env->GetLocalObject(thr, depth,
                table[i].slot, &ob1);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalObject#1) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
                continue;
            }
            err = jvmti_env->GetMethodDeclaringClass(mid, &cls);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetMethodDeclaringClass) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
                continue;
            }
            fid = env->GetFieldID(cls, "fld", "I");
            if (fid == NULL) {
                printf("Cannot find ID for \"fld\" field of meth03\n");
                env->ExceptionClear();
                result = STATUS_FAILED;
                continue;
            }
            fldVal = env->GetIntField(ob1, fid);
        } else if (strcmp(table[i].name, "ob2") == 0) {
            err = jvmti_env->GetLocalObject(thr, depth,
                table[i].slot, &ob2);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalObject#2) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            }
            env->GetIntArrayRegion((jintArray) ob2, 0, 10, arr);
        }
    }
    if ((fldVal != 17) || (arr[2] != 8)) {
        result = STATUS_FAILED;
        printf("One of objects retrieved by GetLocal contains wrong value:\n");
        printf("    actual: fldVal = %d, int.arr[2] = %d\n", fldVal, arr[2]);
        printf("  expected: fldVal = %d, int.arr[2] = %d\n", 17, 8);
    }
}

void check4(jvmtiEnv *jvmti_env, jthread thr, jint depth,
        jvmtiLocalVariableEntry *table, jint count) {
    jvmtiError err;
    jint i1 = 0, i2 = 0, i3 = 0, i4 = 0, b = JNI_FALSE;
    jlong l, lval = -100;
    jfloat f = 0.0;
    jdouble d = 0.0;
    int i;

    for (i = 0; i < count; i++) {
        if (strcmp(table[i].name, "i1") == 0) {
            err = jvmti_env->GetLocalInt(thr, depth, table[i].slot, &i1);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalInt#4i1) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            }
        } else if (strcmp(table[i].name, "i2") == 0) {
            err = jvmti_env->GetLocalInt(thr, depth, table[i].slot, &i2);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalInt#4i2) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            }
        } else if (strcmp(table[i].name, "i3") == 0) {
            err = jvmti_env->GetLocalInt(thr, depth, table[i].slot, &i3);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalInt#4i3) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            }
        } else if (strcmp(table[i].name, "i4") == 0) {
            err = jvmti_env->GetLocalInt(thr, depth, table[i].slot, &i4);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalInt#4i4) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            }
        } else if (strcmp(table[i].name, "b") == 0) {
            err = jvmti_env->GetLocalInt(thr, depth, table[i].slot, &b);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalInt#4b) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            }
        } else if (strcmp(table[i].name, "l") == 0) {
            err = jvmti_env->GetLocalLong(thr, depth, table[i].slot, &l);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalLong#4) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            }
        } else if (strcmp(table[i].name, "f") == 0) {
            err = jvmti_env->GetLocalFloat(thr, depth, table[i].slot, &f);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalFloat#4) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            }
        } else if (strcmp(table[i].name, "d") == 0) {
            err = jvmti_env->GetLocalDouble(thr, depth, table[i].slot, &d);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetLocalDouble#4) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            }
        }
    }
    if ((i1 != 1) || (i2 != 2) || (i3 != 3) || (i4 != 4) || (b != JNI_TRUE) ||
            (l != lval) || (f != fval) || (d != dval)) {
        result = STATUS_FAILED;
        printf("One of values retrieved by GetLocal is wrong (params):\n");
        printf("    actual: int=%d, short=%d, char=%d, byte=%d, boolean=%d\n",
               i1, i2, i3, i4, b);
        printf("            long = 0x%08x%08x, float = %f, double = %f\n",
               (jint)(l >> 32), (jint)l, f, d);
        printf("  expected: int=%d, short=%d, char=%d, byte=%d, boolean=%d\n",
               1, 2, 3, 4, JNI_TRUE);
        printf("            long = 0x%08x%08x, float = %f, double = %f\n",
               (jint)(lval >> 32), (jint)lval, fval, dval);
    }
}

void JNICALL MethodExit(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID mid,
        jboolean was_poped_by_exception, jvalue return_value) {
    jvmtiError err;
    jvmtiLocalVariableEntry *table = NULL;
    jint entryCount = 0;
    int i;

    if (mid == mid1 || mid == mid2 || mid == mid3 || mid == mid4) {
        err = jvmti_env->GetLocalVariableTable(mid,
            &entryCount, &table);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetLocalVariableTable#1) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
            return;
        }
        if (mid == mid1) {
            if (printdump == JNI_TRUE) {
                printf(">>> exit: meth01\n");
            }
            check1(jvmti_env, thr, 0, table, entryCount);
        } else if (mid == mid2) {
            if (printdump == JNI_TRUE) {
                printf(">>> exit: meth02\n");
            }
            check2(jvmti_env, thr, 0, table, entryCount);
            mid2 = NULL;
        } else if (mid == mid3) {
            if (printdump == JNI_TRUE) {
                printf(">>> exit: meth03\n");
            }
            check3(jvmti_env, (JNIEnv *)env, thr, 0, table, entryCount, mid);
        } else if (mid == mid4) {
            if (printdump == JNI_TRUE) {
                printf(">>> exit: meth04\n");
            }
            check4(jvmti_env, thr, 0, table, entryCount);
        }
    }
    if (table != NULL) {
        for (i = 0; i < entryCount; i++) {
            jvmti_env->Deallocate((unsigned char*)table[i].name);
            jvmti_env->Deallocate((unsigned char*)table[i].signature);
        }
        jvmti_env->Deallocate((unsigned char*)table);
    }
}

void JNICALL Breakpoint(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method, jlocation location) {
    jvmtiError err;
    jvmtiLocalVariableEntry *table = NULL;
    jint entryCount = 0;
    jmethodID mid;
    jlocation loc;
    int i;

    err = jvmti_env->GetFrameLocation(thr, 1, &mid, &loc);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetFrameLocation) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti_env->GetLocalVariableTable(mid, &entryCount, &table);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetLocalVariableTable#2) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    if (mid == mid1) {
        if (printdump == JNI_TRUE) {
            printf(">>> bp: meth01\n");
        }
        check1(jvmti_env, thr, 1, table, entryCount);
    } else if (mid == mid2) {
        if (printdump == JNI_TRUE) {
            printf(">>> bp: meth02\n");
        }
        check2(jvmti_env, thr, 1, table, entryCount);
    } else if (mid == mid3) {
        if (printdump == JNI_TRUE) {
            printf(">>> bp: meth03\n");
        }
        check3(jvmti_env, (JNIEnv *)env, thr, 1, table, entryCount, mid);
    } else if (mid == mid4) {
        if (printdump == JNI_TRUE) {
            printf(">>> bp: meth04\n");
        }
        check4(jvmti_env, thr, 1, table, entryCount);
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
JNIEXPORT jint JNICALL Agent_OnLoad_getlocal001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getlocal001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getlocal001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

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
        printf("Warning: Access to local variables is not implemented\n");
    } else if (caps.can_generate_breakpoint_events &&
               caps.can_generate_method_exit_events) {
        callbacks.MethodExit = &MethodExit;
        callbacks.Breakpoint = &Breakpoint;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: Breakpoint or MethodExit event is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetLocalVariable_getlocal001_getMeth(JNIEnv *env, jclass cls,
        jdouble d, jfloat f) {
    jvmtiError err;
    jmethodID mid;

    if (!caps.can_access_local_variables ||
        !caps.can_generate_breakpoint_events ||
        !caps.can_generate_method_exit_events) return;

    fval = f;
    dval = d;
    mid1 = env->GetMethodID(cls, "meth01", "()D");
    mid2 = env->GetMethodID(cls, "meth02", "(I)V");
    mid3 = env->GetStaticMethodID(cls, "meth03", "(Lnsk/jvmti/GetLocalVariable/getlocal001;)V");
    mid4 = env->GetStaticMethodID(cls, "meth04", "(IJSDCFBZ)V");
    mid = env->GetStaticMethodID(cls, "checkPoint", "()V");
    if (mid == 0 || mid1 == 0 || mid2 == 0 || mid3 == 0 || mid4 == 0) {
        printf("Cannot find Method ID for a method\n");
        env->ExceptionDescribe();
        env->ExceptionClear();
        result = STATUS_FAILED;
        return;
    }
    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_EXIT, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable METHOD_EXIT event: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
    err = jvmti->SetBreakpoint(mid, 0);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to SetBreakpoint: err = %d\n", err);
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
Java_nsk_jvmti_GetLocalVariable_getlocal001_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
