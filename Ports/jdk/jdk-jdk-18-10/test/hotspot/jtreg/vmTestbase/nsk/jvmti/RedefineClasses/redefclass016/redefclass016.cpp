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
static jboolean printdump = JNI_FALSE;
static jmethodID mid = NULL;
static jint bpeakpointsExpected = 0;
static jint bpeakpointsCount = 0;
static jlocation loc = 0;
static jbyteArray classBytes;
static jint magicNumber;

void check(jvmtiEnv *jvmti_env, jthread thr, jclass cls, jmethodID mid, jint i) {
    jvmtiError err;
    char *sigClass, *name, *sig, *generic;
    jvmtiLocalVariableEntry *table = NULL;
    jint entryCount = 0;
    jint varValue = -1;
    jint j;

    err = jvmti_env->GetClassSignature(cls, &sigClass, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassSignature#%d) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti_env->GetMethodName(mid, &name, &sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodName#%d) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti_env->GetLocalVariableTable(mid,
        &entryCount, &table);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetLocalVariableTable#%d) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    if (table != NULL) {
        for (j = 0; j < entryCount; j++) {
            if (strcmp(table[j].name, "localVar") == 0) {
                err = jvmti_env->GetLocalInt(thr, 0,
                    table[j].slot, &varValue);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(GetLocalInt#%d) unexpected error: %s (%d)\n",
                           i, TranslateError(err), err);
                    result = STATUS_FAILED;
                }
            }
        }
    }

    if (printdump == JNI_TRUE) {
        printf(">>> bp %d: \"%s.%s%s\"", i, sigClass, name, sig);
        printf(", localVar value: %d\n", varValue);
    }

    if (varValue != i) {
        printf("(bp %d) wrong localVar value: %d,", i, varValue);
        printf(" expected: %d\n", i);
        result = STATUS_FAILED;
    }

    if (sigClass != NULL) {
        jvmti_env->Deallocate((unsigned char*)sigClass);
    }
    if (name != NULL) {
        jvmti_env->Deallocate((unsigned char*)name);
    }
    if (sig != NULL) {
        jvmti_env->Deallocate((unsigned char*)sig);
    }
    if (table != NULL) {
        for (j = 0; j < entryCount; j++) {
            jvmti_env->Deallocate((unsigned char*)(table[j].name));
            jvmti_env->Deallocate((unsigned char*)(table[j].signature));
        }
        jvmti_env->Deallocate((unsigned char*)table);
    }
}

void JNICALL Breakpoint(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jlocation location) {
    jvmtiError err;
    jclass klass;
    unsigned char *bytes;
    jvmtiClassDefinition classDef;
    jboolean found = JNI_FALSE;
    jint i;

    if (mid != method) {
        printf("bp: don't know where we get called from");
        result = STATUS_FAILED;
        return;
    }

    err = jvmti_env->GetMethodDeclaringClass(method, &klass);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodDeclaringClass#%d) unexpected error: %s (%d)\n",
               bpeakpointsCount, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    check(jvmti_env, thread, klass, method, bpeakpointsCount);
    bpeakpointsCount++;

    classDef.klass = klass;
    classDef.class_byte_count = env->GetArrayLength(classBytes);
    bytes = (unsigned char *) env->GetByteArrayElements(classBytes, NULL);

    for (i = 0; i < classDef.class_byte_count - 3; i++) {
        if (((jint)bytes[i+3] |
            ((jint)bytes[i+2] << 8) |
            ((jint)bytes[i+1] << 16) |
            ((jint)bytes[i] << 24)) == magicNumber) {
            found = JNI_TRUE;
            break;
        }
    }

    if (found == JNI_TRUE) {
        bytes[i] = 0;
        bytes[i+1] = 0;
        bytes[i+2] = 0;
        bytes[i+3] = (jbyte)bpeakpointsCount;
    } else {
        printf("Cannot find magic number\n");
        result = STATUS_FAILED;
        return;
    }

    classDef.class_bytes = bytes;
    err = jvmti_env->RedefineClasses(1, &classDef);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RedefineClasses#%d) unexpected error: %s (%d)\n",
               bpeakpointsCount, TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti_env->SetBreakpoint(mid, loc);
    if (err != JVMTI_ERROR_NONE) {
        printf("(SetBreakpoint#%d) unexpected error: %s (%d)\n",
               bpeakpointsCount, TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_redefclass016(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_redefclass016(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_redefclass016(JavaVM *jvm, char *options, void *reserved) {
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

    if (!caps.can_redefine_classes) {
        printf("Warning: RedefineClasses is not implemented\n");
    }

    if (!caps.can_get_line_numbers) {
        printf("Warning: GetLineNumberTable is not implemented\n");
    }

    if (!caps.can_access_local_variables) {
        printf("Warning: access to local variables is not implemented\n");
    }

    if (caps.can_generate_breakpoint_events) {
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
Java_nsk_jvmti_RedefineClasses_redefclass016_getReady(JNIEnv *env, jclass cls,
        jclass clazz, jbyteArray bytes, jint magic, jint line) {
    jvmtiError err;
    jvmtiLineNumberEntry *lines = NULL;
    jint i = 0, entryCount = 0;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (!caps.can_redefine_classes ||
        !caps.can_generate_breakpoint_events ||
        !caps.can_get_line_numbers ||
        !caps.can_access_local_variables) return;

    mid = env->GetMethodID(clazz, "run", "()V");
    if (mid == NULL) {
        printf("Cannot find Method ID for method run\n");
        result = STATUS_FAILED;
        return;
    }

    classBytes = (jbyteArray) env->NewGlobalRef(bytes);

    err = jvmti->GetLineNumberTable(mid, &entryCount, &lines);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetLineNumberTable) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (lines != NULL && entryCount > 0) {
        for (i = 0; i < entryCount; i++) {
            if (line == lines[i].line_number) {
                loc = lines[i].start_location;
                break;
            }
        }
    }
    if (i == entryCount) {
        printf("Cannot find line number entry for %d\n", line);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->SetBreakpoint(mid, loc);
    if (err != JVMTI_ERROR_NONE) {
        printf("(SetBreakpoint) unexpected error: %s (%d)\n",
               TranslateError(err), err);
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

    magicNumber = magic;
    bpeakpointsExpected = 8;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RedefineClasses_redefclass016_check(JNIEnv *env, jclass cls) {
    if (bpeakpointsCount != bpeakpointsExpected) {
        printf("Wrong number of breakpoints: %d, expected: %d\n",
            bpeakpointsCount, bpeakpointsExpected);
        result = STATUS_FAILED;
    }
    return result;
}

}
