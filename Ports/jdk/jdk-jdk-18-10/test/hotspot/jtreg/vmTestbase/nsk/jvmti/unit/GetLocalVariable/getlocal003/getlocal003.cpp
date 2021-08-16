/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "jni_tools.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define STATUS_PASSED 0
#define STATUS_FAILED 2

static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jint result = STATUS_PASSED;
static jboolean printdump = JNI_FALSE;
static jmethodID mid = NULL;
static jvmtiLocalVariableEntry *table = NULL;
static jint entryCount = 0;
static jint methodExitCnt = -1;

void print_LocalVariableEntry(jvmtiLocalVariableEntry *lvt_elem) {
  printf("\n Var name: %s, slot: %d", lvt_elem->name, lvt_elem->slot);
  printf(", start_bci: %" LL "d", lvt_elem->start_location);
  printf(", end_bci: %" LL "d",   lvt_elem->start_location + lvt_elem->length);
  printf(", signature: %s\n", lvt_elem->signature);
}

static void
test_locals(jvmtiEnv *jvmti, jthread thr, jlocation location) {
    jvmtiError err;
    jint intVal;
    jlong longVal;
    jfloat floatVal;
    jdouble doubleVal;
    jobject obj;
    jint i;

    for (i = 0; i < entryCount; i++) {
        if (table[i].start_location > location ||
            table[i].start_location + table[i].length < location) {
            continue;  /* The local variable is not visible */
        }
        print_LocalVariableEntry(&table[i]);
        char sig = table[i].signature[0];
        int slot = table[i].slot;

        if (sig == 'Z' || sig == 'B' || sig == 'C' || sig == 'S') {
            sig = 'I'; // covered by GetLocalInt
        }
        err = jvmti->GetLocalInt(thr, 0, slot, &intVal);
        printf(" GetLocalInt:     %s (%d)\n", TranslateError(err), err);
        if (err != JVMTI_ERROR_NONE && sig == 'I') {
            printf("FAIL: GetLocalInt failed to get value of int\n");
            result = STATUS_FAILED;
        } else if (err != JVMTI_ERROR_TYPE_MISMATCH && sig != 'I') {
            printf("FAIL: GetLocalInt did not return JVMTI_ERROR_TYPE_MISMATCH for non-int\n");
            result = STATUS_FAILED;
        }

        err = jvmti->GetLocalLong(thr, 0, slot, &longVal);
        printf(" GetLocalLong:    %s (%d)\n", TranslateError(err), err);
        if (err != JVMTI_ERROR_NONE && sig == 'J') {
            printf("FAIL: GetLocalLong failed to get value of long\n");
            result = STATUS_FAILED;
        } else if (err != JVMTI_ERROR_INVALID_SLOT &&
                   err != JVMTI_ERROR_TYPE_MISMATCH &&
                   sig != 'J') {
            printf("FAIL: GetLocalLong did not return JVMTI_ERROR_INVALID_SLOT"
                   " nor JVMTI_ERROR_TYPE_MISMATCH for non-long\n");
            result = STATUS_FAILED;
        }

        err = jvmti->GetLocalFloat(thr, 0, slot, &floatVal);
        printf(" GetLocalFloat:   %s (%d)\n", TranslateError(err), err);
        if (err != JVMTI_ERROR_NONE && sig == 'F') {
            printf("FAIL: GetLocalFloat failed to get value of float\n");
            result = STATUS_FAILED;
        } else if (err != JVMTI_ERROR_TYPE_MISMATCH && sig != 'F') {
            printf("FAIL: GetLocalFloat did not return JVMTI_ERROR_TYPE_MISMATCH for non-float\n");
            result = STATUS_FAILED;
        }

        err = jvmti->GetLocalDouble(thr, 0, slot, &doubleVal);
        printf(" GetLocalDouble:  %s (%d)\n", TranslateError(err), err);
        if (err != JVMTI_ERROR_NONE && sig == 'D') {
            printf("FAIL: GetLocalDouble failed to get value of double\n");
            result = STATUS_FAILED;
        } else if (err != JVMTI_ERROR_INVALID_SLOT &&
                   err != JVMTI_ERROR_TYPE_MISMATCH &&
                   sig != 'D') {
            printf("FAIL: GetLocalDouble did not return JVMTI_ERROR_INVALID_SLOT"
                   " nor JVMTI_ERROR_TYPE_MISMATCH for non-double\n");
            result = STATUS_FAILED;
        }

        err = jvmti->GetLocalObject(thr, 0, slot, &obj);
        printf(" GetLocalObject:  %s (%d)\n", TranslateError(err), err);
        if (err != JVMTI_ERROR_NONE && sig == 'L') {
            printf("FAIL: GetLocalObject failed to get value of object\n");
            result = STATUS_FAILED;
        } else if (err != JVMTI_ERROR_TYPE_MISMATCH && sig != 'L') {
            printf("FAIL: GetLocalObject did not return JVMTI_ERROR_TYPE_MISMATCH for non-object\n");
            result = STATUS_FAILED;
        }
    }
}

static void JNICALL
MethodExit(jvmtiEnv *jvmti_env,
           JNIEnv *env,
           jthread thr,
           jmethodID method,
           jboolean was_poped_by_exception,
           jvalue return_value) {

    jvmtiError err;
    jlocation location;
    jmethodID frame_method = NULL;

    if (mid != method) {
        return;
    }
    err = jvmti->GetFrameLocation(thr, 0, &frame_method, &location);
    if (err != JVMTI_ERROR_NONE) {
        printf("\t failure: %s (%d)\n", TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    if (frame_method != method) {
        printf("\t failure: GetFrameLocation returned wrong jmethodID\n");
        result = STATUS_FAILED;
        return;
    }

    printf("\n MethodExit: BEGIN %d\n",  ++methodExitCnt);

    test_locals(jvmti, thr, location);

    printf("\n MethodExit: END %d\n\n", methodExitCnt);
    fflush(stdout);
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getlocal003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getlocal003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getlocal003(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;
    static jvmtiCapabilities caps;

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
        return JNI_ERR;
    }
    if (!caps.can_generate_method_exit_events) {
        printf("Warning: MethodExit event is not implemented\n");
        return JNI_ERR;
    }
    callbacks.MethodExit = &MethodExit;
    err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    if (err != JVMTI_ERROR_NONE) {
        printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }
    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_GetLocalVariable_getlocal003_instMeth(JNIEnv *env, jobject inst) {
    jvmtiError err;
    jobject obj = NULL;

    printf("\n Native instMeth: started\n");

    // Test GetLocalInstance with native instance method instMeth() frame
    err = jvmti->GetLocalInstance(NULL, 0, &obj);
    printf(" Native instMeth: GetLocalInstance: %s (%d)\n", TranslateError(err), err);
    if (err != JVMTI_ERROR_NONE) {
        printf("FAIL: GetLocalInstance failed to get instance for native instance method frame\n");
        result = STATUS_FAILED;
    }
    if (env->IsSameObject(inst, obj) == JNI_FALSE) {
        printf("FAIL: GetLocalInstance returned unexpected instance for native instance method frame\n");
        result = STATUS_FAILED;
    }

    // Test GetLocalInstance with java instance method meth01() frame
    err = jvmti->GetLocalInstance(NULL, 1, &obj);
    printf(" Native instMeth: GetLocalInstance: %s (%d)\n", TranslateError(err), err);
    if (err != JVMTI_ERROR_NONE) {
        printf("FAIL: GetLocalInstance failed to get instance for java instance method frame\n");
        result = STATUS_FAILED;
    }
    if (env->IsSameObject(inst, obj) == JNI_FALSE) {
        printf("FAIL: GetLocalInstance returned unexpected instance for java instance method frame\n");
        result = STATUS_FAILED;
    }
    printf(" Native instMeth: finished\n\n");
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_GetLocalVariable_getlocal003_getMeth(JNIEnv *env, jclass cls) {
    jvmtiError err;
    jobject obj = NULL;

    printf("\n Native getMeth: started\n");

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    mid = env->GetStaticMethodID(cls, "staticMeth", "(I)I");
    if (mid == NULL) {
        printf("Cannot find Method ID for staticMeth\n");
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

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_EXIT, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable metod exit event: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    // Test GetLocalInstance with native static method getMeth() frame
    err = jvmti->GetLocalInstance(NULL, 0, &obj);
    printf(" Native getMeth: GetLocalInstance: %s (%d)\n", TranslateError(err), err);
    if (err != JVMTI_ERROR_INVALID_SLOT) {
        printf("FAIL: GetLocalInstance failed to return JVMTI_ERROR_INVALID_SLOT for native static method frame\n");
        result = STATUS_FAILED;
    }

    // Test GetLocalInstance with java static method run() frame
    err = jvmti->GetLocalInstance(NULL, 1, &obj);
    printf(" Native getMeth: GetLocalInstance: %s (%d)\n", TranslateError(err), err);
    if (err != JVMTI_ERROR_INVALID_SLOT) {
        printf("FAIL: GetLocalInstance failed to return JVMTI_ERROR_INVALID_SLOT for java static method frame\n");
        result = STATUS_FAILED;
    }

    printf(" Native getMeth: finished\n\n");
    fflush(stdout);
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_GetLocalVariable_getlocal003_checkLoc(JNIEnv *env,
        jclass cls, jthread thr) {
    jvmtiError err;
    jvmtiLocalVariableEntry *table;
    jint entryCount;
    jmethodID mid;
    jint locVar;
    jint i, j;
    int  overlap = 0;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }
    printf("\n checkLoc: START\n");

    mid = env->GetStaticMethodID(cls, "staticMeth", "(I)I");
    if (mid == NULL) {
        printf("Cannot find Method ID for staticMeth\n");
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
        print_LocalVariableEntry(&table[i]);

        err = jvmti->GetLocalInt(thr, 1, table[i].slot, &locVar);

        if (strcmp(table[i].name, "intArg") == 0) {
            if (err != JVMTI_ERROR_NONE) {
                printf(" GetLocalInt: %s (%d)\n", TranslateError(err), err);
                printf(" failure: JVMTI_ERROR_NONE is expected\n");
                result = STATUS_FAILED;
            }
        }
        else if (strcmp(table[i].name, "pi") == 0) {
            if (err != JVMTI_ERROR_TYPE_MISMATCH) {
                printf(" GetLocalInt: %s (%d)\n", TranslateError(err), err);
                printf(" failure: JVMTI_ERROR_TYPE_MISMATCH is expected\n");
                result = STATUS_FAILED;
            }
        } else {
            if (err != JVMTI_ERROR_INVALID_SLOT) {
                printf(" GetLocalInt: %s (%d)\n", TranslateError(err), err);
                printf(" failure: JVMTI_ERROR_INVALID_SLOT is expected\n");
                result = STATUS_FAILED;
            }
        }
        if (table[i].slot != 2) {
           continue;
        }

        for (j = 0; j < entryCount; j++) {
           /* We do cross checks between all variables having slot #2.
            * No overlapping between location ranges are allowed.
            */
           if (table[j].slot != 2 || i == j) {
              continue;
           }
           if (table[i].start_location > table[j].start_location + table[j].length ||
               table[j].start_location > table[i].start_location + table[i].length
           ) {
               continue; /* Everything is Ok */
           }

           printf(" failure: locations of vars with slot #2 are overlapped:\n");
           print_LocalVariableEntry(&table[i]);
           print_LocalVariableEntry(&table[j]);
           overlap++;
           result = STATUS_FAILED;
        }
    }
    if (!overlap) {
        printf("\n Success: locations of vars with slot #2 are NOT overlapped\n");
    }
    printf("\n checkLoc: END\n\n");
    fflush(stdout);
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_GetLocalVariable_getlocal003_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
