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

/*
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
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_linetab004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_linetab004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_linetab004(JavaVM *jvm, char *options, void *reserved) {
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

    if (!caps.can_get_line_numbers) {
        printf("Warning: GetLineNumberTable is not implemented\n");
    }

    return JNI_OK;
}

void checkGetLineNumberTable(jmethodID mid, const char *methName,
        int abstract, jvmtiError exp) {
    jint entryCount = -1;
    jvmtiLineNumberEntry *table = NULL;
    jvmtiError err;
    int i;

    err = jvmti->GetLineNumberTable(mid, &entryCount, &table);
    if (printdump == JNI_TRUE) {
        printf("\n Method: %s%s\n", methName,
            (abstract == 0) ? "" : " (abstract)");
    }
    if (err != exp) {
        result = STATUS_FAILED;
        printf(" Error expected: %s (%d),\n", TranslateError(exp), exp);
        printf(" actual: %s (%d)\n", TranslateError(err), err);
        if (err == JVMTI_ERROR_NONE) {
            printf("  %s%s line number table (%d entries):%s\n",
                   methName, (abstract == 0) ? "" : " (abstract)",
                   entryCount, (entryCount == 0 ? " empty" : ""));
            for (i = 0; i < entryCount; i++) {
                printf("    start_location = 0x%x%08x,",
                       (jint)(table[i].start_location >> 32),
                       (jint)table[i].start_location);
                printf(" line_number = %d\n", table[i].line_number);
            }
        }
    } else if (printdump == JNI_TRUE) {
        printf(" Error code: %s (%d),\n", TranslateError(err), err);
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_GetLineNumberTable_linetab004_check(JNIEnv *env, jclass cls) {
    jmethodID mid;
    jclass abstr;
    jclass interf;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    if (!caps.can_get_line_numbers) {
        return result;
    }

    if (printdump == JNI_TRUE) {
        printf("\n Check methods of interface:\n");
    }
    interf = env->FindClass("nsk/jvmti/unit/GetLineNumberTable/Interface004");
    if (interf == NULL) {
        printf("Cannot get Interface class!\n");
        return STATUS_FAILED;
    }

    mid = env->GetMethodID(cls, "instanceMeth0", "()I");
    if (mid == NULL) {
        printf("Cannot get method ID!\n");
        return STATUS_FAILED;
    }
    checkGetLineNumberTable(mid, "instanceMeth0", 1,
        JVMTI_ERROR_ABSENT_INFORMATION);

    mid = env->GetMethodID(cls, "instanceMeth1", "()I");
    if (mid == NULL) {
        printf("Cannot get method ID!\n");
        return STATUS_FAILED;
    }
    checkGetLineNumberTable(mid, "instanceMeth1", 1,
        JVMTI_ERROR_ABSENT_INFORMATION);

    if (printdump == JNI_TRUE) {
        printf("\n Check methods of abstract class:\n");
    }
    abstr = env->GetSuperclass(cls);
    if (abstr == NULL) {
        printf("Cannot get super class!\n");
        return STATUS_FAILED;
    }

    mid = env->GetMethodID(abstr, "instanceMeth0", "()I");
    if (mid == NULL) {
        printf("Cannot get method ID!\n");
        return STATUS_FAILED;
    }
    checkGetLineNumberTable(mid, "instanceMeth0", 1,
        JVMTI_ERROR_ABSENT_INFORMATION);

    mid = env->GetMethodID(abstr, "instanceMeth1", "()I");
    if (mid == NULL) {
        printf("Cannot get method ID!\n");
        return STATUS_FAILED;
    }
    checkGetLineNumberTable(mid, "instanceMeth1", 0,
        JVMTI_ERROR_ABSENT_INFORMATION);

    if (printdump == JNI_TRUE) {
        printf("\n Check methods of regular class:\n");
    }
    mid = env->GetMethodID(cls, "instanceMeth0", "()I");
    if (mid == NULL) {
        printf("Cannot get method ID!\n");
        return STATUS_FAILED;
    }
    checkGetLineNumberTable(mid, "instanceMeth0", 0,
        JVMTI_ERROR_ABSENT_INFORMATION);

    mid = env->GetMethodID(cls, "instanceMeth1", "()I");
    if (mid == NULL) {
        printf("Cannot get method ID!\n");
        return STATUS_FAILED;
    }
    checkGetLineNumberTable(mid, "instanceMeth1", 0,
        JVMTI_ERROR_ABSENT_INFORMATION);

    mid = env->GetMethodID(cls, "instanceMeth2", "()I");
    if (mid == NULL) {
        printf("Cannot get method ID!\n");
        return STATUS_FAILED;
    }
    checkGetLineNumberTable(mid, "instanceMeth2", 0,
        JVMTI_ERROR_ABSENT_INFORMATION);

    if (printdump == JNI_TRUE) {
        printf("\n Check native methods of regular class:\n");
    }
    mid = env->GetMethodID(cls, "instanceNativeMeth", "()I");
    if (mid == NULL) {
        printf("Cannot get method ID!\n");
        return STATUS_FAILED;
    }
    checkGetLineNumberTable(mid, "instanceNativeMeth", 1,
        JVMTI_ERROR_NATIVE_METHOD);

    mid = env->GetStaticMethodID(cls, "staticNativeMeth", "()I");
    if (mid == NULL) {
        printf("Cannot get method ID!\n");
        return STATUS_FAILED;
    }
    checkGetLineNumberTable(mid, "staticNativeMeth", 1,
        JVMTI_ERROR_NATIVE_METHOD);

    if (printdump == JNI_TRUE) {
        printf(">>> ... done\n");
    }

    return result;
}

}
