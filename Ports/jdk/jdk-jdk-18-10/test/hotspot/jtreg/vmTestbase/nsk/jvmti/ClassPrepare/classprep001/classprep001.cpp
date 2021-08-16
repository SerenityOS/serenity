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
#include <inttypes.h>
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

#define EXP_STATUS (JVMTI_CLASS_STATUS_VERIFIED | JVMTI_CLASS_STATUS_PREPARED)

typedef struct {
    char *sig;
    jint status;
    jint mcount;
    jint fcount;
    jint icount;
} writable_class_info;

typedef struct {
    const char *sig;
    jint status;
    jint mcount;
    jint fcount;
    jint icount;
} class_info;

static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static size_t eventsCount = 0;
static size_t eventsExpected = 0;
static class_info classes[] = {
    { "Lnsk/jvmti/ClassPrepare/classprep001$TestInterface;", EXP_STATUS, 2, 1, 0 },
    { "Lnsk/jvmti/ClassPrepare/classprep001$TestClass;", EXP_STATUS, 3, 2, 1 }
};
// These classes are loaded on a different thread.
// We should not get ClassPrepare events for them.
static const class_info unexpectedClasses[] = {
    { "Lnsk/jvmti/ClassPrepare/classprep001$TestInterface2;", 0, 0, 0, 0 },
    { "Lnsk/jvmti/ClassPrepare/classprep001$TestClass2;", 0, 0, 0, 0}
};

void printStatus(jint status) {
    int flags = 0;
    if ((status & JVMTI_CLASS_STATUS_VERIFIED) != 0) {
        printf("JVMTI_CLASS_STATUS_VERIFIED");
        flags++;
    }
    if ((status & JVMTI_CLASS_STATUS_PREPARED) != 0) {
        if (flags > 0) printf(" | ");
        printf("JVMTI_CLASS_STATUS_PREPARED");
        flags++;
    }
    if ((status & JVMTI_CLASS_STATUS_INITIALIZED) != 0) {
        if (flags > 0) printf(" | ");
        printf("JVMTI_CLASS_STATUS_INITIALIZED");
        flags++;
    }
    if ((status & JVMTI_CLASS_STATUS_ERROR) != 0) {
        if (flags > 0) printf(" | ");
        printf("JVMTI_CLASS_STATUS_ERROR");
        flags++;
    }
    printf(" (0x%x)\n", status);
}

const size_t NOT_FOUND = (size_t)(-1);

size_t findClass(const char *classSig, const class_info *arr, int size) {
    for (int i = 0; i < size; i++) {
        if (strcmp(classSig, arr[i].sig) == 0) {
            return i;
        }
    }
    return NOT_FOUND;
}

void JNICALL ClassPrepare(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jclass cls) {
    jvmtiError err;
    writable_class_info inf;
    jmethodID *methods;
    jfieldID *fields;
    jclass *interfaces;
    char *name, *sig, *generic;
    int i;

    err = jvmti_env->GetClassSignature(cls, &inf.sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassSignature#%" PRIuPTR ") unexpected error: %s (%d)\n",
               eventsCount, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti_env->GetClassStatus(cls, &inf.status);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassStatus#%" PRIuPTR ") unexpected error: %s (%d)\n",
               eventsCount, TranslateError(err), err);
        result = STATUS_FAILED;
    }
    err = jvmti_env->GetClassMethods(cls, &inf.mcount, &methods);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassMethods#%" PRIuPTR ") unexpected error: %s (%d)\n",
               eventsCount, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti_env->GetClassFields(cls, &inf.fcount, &fields);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassMethods#%" PRIuPTR ") unexpected error: %s (%d)\n",
               eventsCount, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    err = jvmti_env->GetImplementedInterfaces(cls,
        &inf.icount, &interfaces);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetImplementedInterfaces#%" PRIuPTR ") unexpected error: %s (%d)\n",
               eventsCount, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> [class prepare event #%" PRIuPTR "]", eventsCount);
        printf(" \"%s\"\n", inf.sig);
        printf(">>>   status: ");
        printStatus(inf.status);
        printf(">>>   %d methods:", inf.mcount);
        for (i = 0; i < inf.mcount; i++) {
            if (i > 0) printf(",");
            if (methods[i] == NULL) {
                printf(" null");
            } else {
                err = jvmti_env->GetMethodName(methods[i],
                    &name, &sig, &generic);
                if (err == JVMTI_ERROR_NONE) {
                    printf(" \"%s%s\"", name, sig);
                } else {
                    printf(" ???");
                }
            }
        }
        printf("\n");
        printf(">>>   %d fields:", inf.fcount);
        for (i = 0; i < inf.fcount; i++) {
            if (i > 0) printf(",");
            if (fields[i] == NULL) {
                printf(" null");
            } else {
                err = jvmti_env->GetFieldName(cls, fields[i],
                    &name, &sig, &generic);
                if (err == JVMTI_ERROR_NONE) {
                    printf(" \"%s, %s\"", name, sig);
                } else {
                    printf(" ???");
                }
            }
        }
        printf("\n");
        printf(">>>   %d interfaces:", inf.icount);
        for (i = 0; i < inf.icount; i++) {
            if (i > 0) printf(",");
            if (interfaces[i] == NULL) {
                printf(" null");
            } else {
                err = jvmti_env->GetClassSignature(
                    interfaces[i], &sig, &generic);
                if (err == JVMTI_ERROR_NONE) {
                    printf(" \"%s\"", sig);
                } else {
                    printf(" ???");
                }
            }
        }
        printf("\n");
    }

    size_t expectedClassIdx = findClass(inf.sig, classes, sizeof(classes)/sizeof(class_info));
    // Test classes loading may cause system classes loading - skip them.
    if (expectedClassIdx == NOT_FOUND) {
        size_t unexpectedClassIdx = findClass(inf.sig, unexpectedClasses,
                                              sizeof(unexpectedClasses)/sizeof(class_info));
        if (unexpectedClassIdx != NOT_FOUND) {
            printf("# wrong class: \"%s\"\n", inf.sig);
            result = STATUS_FAILED;
        }
        return;
    }

    if (eventsCount != expectedClassIdx) {
        printf("(#%" PRIuPTR ") unexpected order: %" PRIuPTR ", expected: %" PRIuPTR "\n",
               eventsCount, expectedClassIdx, eventsCount);
        result = STATUS_FAILED;
        return;
    }

    if (inf.status != classes[eventsCount].status) {
        printf("(#%" PRIuPTR ") wrong status: ", eventsCount);
        printStatus(inf.status);
        printf("     expected: ");
        printStatus(classes[eventsCount].status);
        result = STATUS_FAILED;
    }
    if (inf.mcount != classes[eventsCount].mcount) {
        printf("(#%" PRIuPTR ") wrong number of methods: 0x%x",
               eventsCount, inf.mcount);
        printf(", expected: 0x%x\n", classes[eventsCount].mcount);
        result = STATUS_FAILED;
    }
    if (inf.fcount != classes[eventsCount].fcount) {
        printf("(#%" PRIuPTR ") wrong number of fields: 0x%x",
               eventsCount, inf.fcount);
        printf(", expected: 0x%x\n", classes[eventsCount].fcount);
        result = STATUS_FAILED;
    }
    if (inf.icount != classes[eventsCount].icount) {
        printf("(#%" PRIuPTR ") wrong number of interfaces: 0x%x",
               eventsCount, inf.icount);
        printf(", expected: 0x%x\n", classes[eventsCount].icount);
        result = STATUS_FAILED;
    }
    eventsCount++;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_classprep001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_classprep001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_classprep001(JavaVM *jvm, char *options, void *reserved) {
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

    callbacks.ClassPrepare = &ClassPrepare;
    err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    if (err != JVMTI_ERROR_NONE) {
        printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_ClassPrepare_classprep001_getReady(JNIEnv *env, jclass cls, jthread thread) {
    jvmtiError err;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_CLASS_PREPARE, thread);
    if (err == JVMTI_ERROR_NONE) {
        eventsExpected = sizeof(classes)/sizeof(class_info);
    } else {
        printf("Failed to enable JVMTI_EVENT_CLASS_PREPARE: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_ClassPrepare_classprep001_check(JNIEnv *env, jclass cls, jthread thread) {
    jvmtiError err;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_DISABLE,
            JVMTI_EVENT_CLASS_PREPARE, thread);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to disable JVMTI_EVENT_CLASS_PREPARE: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (eventsCount != eventsExpected) {
        printf("Wrong number of class prepare events: %" PRIuPTR ", expected: %" PRIuPTR "\n",
            eventsCount, eventsExpected);
        result = STATUS_FAILED;
    }
    return result;
}

}
