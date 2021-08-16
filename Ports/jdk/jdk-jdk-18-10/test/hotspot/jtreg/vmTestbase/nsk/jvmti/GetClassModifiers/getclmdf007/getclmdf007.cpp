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
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

#define ACC_PUBLIC      0x0001
#define ACC_PRIVATE     0x0002
#define ACC_PROTECTED   0x0004
#define ACC_STATIC      0x0008
#define ACC_FINAL       0x0010
#define ACC_SUPER       0x0020
#define ACC_INTERFACE   0x0200
#define ACC_ABSTRACT    0x0400

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getclmdf007(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getclmdf007(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getclmdf007(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    return JNI_OK;
}

void printModifiers(jint mod) {
    if (mod & ACC_PUBLIC) printf(" PUBLIC");
    if (mod & ACC_PRIVATE) printf(" PRIVATE");
    if (mod & ACC_PROTECTED) printf(" PROTECTED");
    if (mod & ACC_STATIC) printf(" STATIC");
    if (mod & ACC_FINAL) printf(" FINAL");
    if (mod & ACC_SUPER) printf(" SUPER");
    if (mod & ACC_INTERFACE) printf(" INTERFACE");
    if (mod & ACC_ABSTRACT) printf(" ABSTRACT");
    printf(" (0x%0x)\n", mod);
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetClassModifiers_getclmdf007_check(JNIEnv *env, jclass cls, jint i, jclass arr, jclass comp) {
    jvmtiError err;
    jint ArrayModifiers;
    jint ComponentModifiers;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->GetClassModifiers(arr, &ArrayModifiers);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassModifiers#%d, arr) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> %d:", i);
        printModifiers(ArrayModifiers);
    }

    if ((ArrayModifiers & ACC_FINAL) == 0) {
        printf("(%d) ACC_FINAL bit should be set\n", i);
        result = STATUS_FAILED;
    }

    if ((ArrayModifiers & ACC_INTERFACE) != 0) {
        printf("(%d) ACC_INTERFACE bit should be clear\n", i);
        result = STATUS_FAILED;
    }

    err = jvmti->GetClassModifiers(comp, &ComponentModifiers);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassModifiers#%d, comp) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    ArrayModifiers &= (ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);
    ComponentModifiers &= (ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED);
    if (ArrayModifiers != ComponentModifiers) {
        printf("(%d) access bits of array do not match component ones\n", i);
        printf("      array:");
        printModifiers(ArrayModifiers);
        printf("  component:");
        printModifiers(ComponentModifiers);
        result = STATUS_FAILED;
    }
}

JNIEXPORT int JNICALL Java_nsk_jvmti_GetClassModifiers_getclmdf007_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
