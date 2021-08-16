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

#define ACC_PUBLIC        0x001
#define ACC_PRIVATE       0x002
#define ACC_PROTECTED     0x004
#define ACC_STATIC        0x008
#define ACC_FINAL         0x010
#define ACC_SYNCHRONIZED  0x020
#define ACC_NATIVE        0x100
#define ACC_ABSTRACT      0x400

static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;

void printModifiers(jint mod) {
    if (mod & ACC_PUBLIC) printf(" PUBLIC");
    if (mod & ACC_PRIVATE) printf(" PRIVATE");
    if (mod & ACC_PROTECTED) printf(" PROTECTED");
    if (mod & ACC_STATIC) printf(" STATIC");
    if (mod & ACC_FINAL) printf(" FINAL");
    if (mod & ACC_SYNCHRONIZED) printf(" SYNCHRONIZED");
    if (mod & ACC_NATIVE) printf(" NATIVE");
    if (mod & ACC_ABSTRACT) printf(" ABSTRACT");
    printf(" (0x%0x)\n", mod);
}

void checkMeth(jvmtiEnv *jvmti_env, JNIEnv *env, jclass cl,
        const char *name, const char *sig, int stat, int flags) {
    jvmtiError err;
    jmethodID mid;
    jint modifiers;

    if (stat) {
        mid = env->GetStaticMethodID(cl, name, sig);
    } else {
        mid = env->GetMethodID(cl, name, sig);
    }
    if (mid == NULL) {
        printf("Cannot find MethodID for \"%s%s\"\n", name, sig);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti_env->GetMethodModifiers(mid, &modifiers);
    if (err != JVMTI_ERROR_NONE) {
        printf("\"%s%s\"\n", name, sig);
        printf("(GetClassModifiers) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> %s%s" , name, sig);
        printModifiers(modifiers);
    }

    if (modifiers != flags) {
        printf("\"%s%s\" access flags expected:", name, sig);
        printModifiers(flags);
        printf("\t       actual:");
        printModifiers(modifiers);
        result = STATUS_FAILED;
    }
}

void JNICALL
ClassLoad(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr, jclass cls) {
    jvmtiError err;
    char *sig, *generic;

    err = jvmti_env->GetClassSignature(cls, &sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassSignature) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return;
    }
    if (strcmp(sig, "Lnsk/jvmti/GetMethodModifiers/methmod001;") == 0) {
        checkMeth(jvmti_env, (JNIEnv *)env, cls,
            "run", "([Ljava/lang/String;Ljava/io/PrintStream;)I",
            1, ACC_PUBLIC |  ACC_STATIC);
        checkMeth(jvmti_env, (JNIEnv *)env, cls,
            "meth_stat", "(ILjava/lang/String;)[F",
            1, ACC_PROTECTED |  ACC_STATIC |  ACC_FINAL);
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_methmod001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_methmod001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_methmod001(JavaVM *jvm, char *options, void *reserved) {
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

    callbacks.ClassLoad = &ClassLoad;
    err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    if (err != JVMTI_ERROR_NONE) {
        printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_CLASS_LOAD, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable event JVMTI_EVENT_CLASS_LOAD: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL Java_nsk_jvmti_GetMethodModifiers_methmod001_check(JNIEnv *env, jclass cls) {
    jclass clsId;

    checkMeth(jvmti, env, cls, "<init>", "()V", 0,  ACC_PUBLIC);
    checkMeth(jvmti, env, cls, "meth_1", "(C)C", 0,  ACC_PRIVATE);
    checkMeth(jvmti, env, cls, "check", "()I", 1,  ACC_NATIVE |  ACC_STATIC);
    clsId = env->FindClass("nsk/jvmti/GetMethodModifiers/methmod001a");
    checkMeth(jvmti, env, clsId, "meth_new", "()Lnsk/jvmti/GetMethodModifiers/methmod001;", 0,  ACC_SYNCHRONIZED);
    checkMeth(jvmti, env, clsId, "meth_abs", "()V", 0,  ACC_ABSTRACT);
    clsId = env->FindClass("nsk/jvmti/GetMethodModifiers/methmod001$Inn");
    checkMeth(jvmti, env, clsId, "meth_inn", "(Ljava/lang/String;)V", 0, ACC_PUBLIC |  ACC_SYNCHRONIZED |  ACC_FINAL);

    return result;
}

}
