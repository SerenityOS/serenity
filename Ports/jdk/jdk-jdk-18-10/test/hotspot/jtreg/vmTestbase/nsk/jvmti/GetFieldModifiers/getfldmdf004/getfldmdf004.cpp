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
#define ACC_VOLATILE    0x0040
#define ACC_TRANSIENT   0x0080
#define ACC_NATIVE      0x0100
#define ACC_INTERFACE   0x0200
#define ACC_ABSTRACT    0x0400

typedef struct {
    const char *name;
    const char *sig;
    jboolean is_static;
} field_info;

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static field_info fields[] = {
    { "field0", "I", JNI_FALSE },
    { "field1", "I", JNI_FALSE },
    { "field2", "I", JNI_FALSE },
    { "field3", "I", JNI_FALSE },
    { "field4", "I", JNI_FALSE },
    { "field5", "I", JNI_FALSE },
    { "field6", "I", JNI_FALSE },
    { "field7", "I", JNI_FALSE },
    { "field8", "I", JNI_FALSE },
    { "field9", "I", JNI_FALSE },
    { "field10", "I", JNI_FALSE },
    { "field11", "I", JNI_FALSE },
    { "field12", "I", JNI_FALSE },
    { "field13", "I", JNI_FALSE },
    { "field14", "I", JNI_FALSE },
    { "field15", "I", JNI_FALSE },
    { "field16", "I", JNI_FALSE },
    { "field17", "I", JNI_FALSE },
    { "field18", "I", JNI_FALSE },
    { "field19", "I", JNI_FALSE },
    { "field20", "I", JNI_FALSE },
    { "field21", "I", JNI_FALSE },
    { "field22", "I", JNI_FALSE },
    { "field23", "I", JNI_FALSE },

    { "field24", "I", JNI_TRUE },
    { "field25", "I", JNI_TRUE },
    { "field26", "I", JNI_TRUE },
    { "field27", "I", JNI_TRUE },
    { "field28", "I", JNI_TRUE },
    { "field29", "I", JNI_TRUE },
    { "field30", "I", JNI_TRUE },
    { "field31", "I", JNI_TRUE },
    { "field32", "I", JNI_TRUE },
    { "field33", "I", JNI_TRUE },
    { "field34", "I", JNI_TRUE },
    { "field35", "I", JNI_TRUE },
    { "field36", "I", JNI_TRUE },
    { "field37", "I", JNI_TRUE },
    { "field38", "I", JNI_TRUE },
    { "field39", "I", JNI_TRUE },
    { "field40", "I", JNI_TRUE },
    { "field41", "I", JNI_TRUE },
    { "field42", "I", JNI_TRUE },
    { "field43", "I", JNI_TRUE },
    { "field44", "I", JNI_TRUE },
    { "field45", "I", JNI_TRUE },
    { "field46", "I", JNI_TRUE },
    { "field47", "I", JNI_TRUE }
};

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getfldmdf004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getfldmdf004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getfldmdf004(JavaVM *jvm, char *options, void *reserved) {
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
    if (mod & ACC_VOLATILE) printf(" VOLATILE");
    if (mod & ACC_TRANSIENT) printf(" TRANSIENT");
    if (mod & ACC_NATIVE) printf(" NATIVE");
    if (mod & ACC_INTERFACE) printf(" INTERFACE");
    if (mod & ACC_ABSTRACT) printf(" ABSTRACT");
    printf(" (0x%0x)\n", mod);
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetFieldModifiers_getfldmdf004_check(JNIEnv *env,
        jclass cls, jint i, jint mod) {
    jvmtiError err;
    jfieldID fid;
    jint modifiers;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (fields[i].is_static == JNI_TRUE) {
        fid = env->GetStaticFieldID(cls, fields[i].name, fields[i].sig);
    } else {
        fid = env->GetFieldID(cls, fields[i].name, fields[i].sig);
    }
    if (fid == NULL) {
        printf("(%d) cannot get field ID for %s:\"%s\"\n",
               i, fields[i].name, fields[i].sig);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->GetFieldModifiers(cls, fid, &modifiers);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetFieldModifiers#%d) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> %2d:", i);
        printModifiers(modifiers);
    }

    if (modifiers != mod) {
        printf("(%2d) access flags expected:", i);
        printModifiers(mod);
        printf("\t\t    actual:");
        printModifiers(modifiers);
        result = STATUS_FAILED;
    }
}

JNIEXPORT int JNICALL
Java_nsk_jvmti_GetFieldModifiers_getfldmdf004_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
