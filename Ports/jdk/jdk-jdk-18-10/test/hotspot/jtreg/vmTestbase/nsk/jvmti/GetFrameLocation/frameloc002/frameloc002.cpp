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
#include "jni_tools.h"
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

typedef struct {
    const char *cls_sig;
    const char *name;
    const char *sig;
    jlocation loc;
} frame_info;

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static frame_info fi =
    { "Lnsk/jvmti/GetFrameLocation/frameloc002;", "check",
      "(Ljava/lang/Thread;)I", -1 };

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_frameloc002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_frameloc002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_frameloc002(JavaVM *jvm, char *options, void *reserved) {
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

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetFrameLocation_frameloc002_check(JNIEnv *env, jclass cls, jthread thr) {
    jvmtiError err;
    jclass klass;
    jmethodID mid;
    jlocation loc;
    char *cls_sig, *name, *sig, *generic;
    char buffer[32];

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> acquiring frame location ...\n");
    }
    err = jvmti->GetFrameLocation(thr, 0, &mid, &loc);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetFrameLocation) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return result;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> retrieving class/method info ...\n");
    }
    err = jvmti->GetMethodDeclaringClass(mid, &klass);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodDeclaringClass) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return result;
    }
    err = jvmti->GetClassSignature(klass, &cls_sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassSignature) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return result;
    }
    err = jvmti->GetMethodName(mid, &name, &sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodName) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return result;
    }

    if (printdump == JNI_TRUE) {
        printf(">>>      class: \"%s\"\n", cls_sig);
        printf(">>>     method: \"%s%s\"\n", name, sig);
        printf(">>>   location: %s\n",
               jlong_to_string(loc, buffer));
    }

    if (cls_sig == NULL ||
            strcmp(cls_sig, fi.cls_sig) != 0) {
        printf("(GetFrameLocation) wrong class: \"%s\"", cls_sig);
        printf(", expected: \"%s\"\n", fi.cls_sig);
        result = STATUS_FAILED;
    }
    if (name == NULL ||
            strcmp(name, fi.name) != 0) {
        printf("(GetFrameLocation) wrong method name: \"%s\"", name);
        printf(", expected: \"%s\"\n", fi.name);
        result = STATUS_FAILED;
    }
    if (sig == NULL ||
            strcmp(sig, fi.sig) != 0) {
        printf("(GetFrameLocation) wrong method signature: \"%s\"", sig);
        printf(", expected: \"%s\"\n", fi.sig);
        result = STATUS_FAILED;
    }
    if (loc != fi.loc) {
        printf("(GetFrameLocation) wrong location: %s",
               jlong_to_string(loc, buffer));
        printf(", expected: %s\n",
               jlong_to_string(fi.loc, buffer));
        result = STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> ... done\n");
    }

    return result;
}

}
