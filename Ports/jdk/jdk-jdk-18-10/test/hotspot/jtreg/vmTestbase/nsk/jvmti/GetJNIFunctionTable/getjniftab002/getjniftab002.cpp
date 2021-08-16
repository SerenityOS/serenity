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

#include <stdio.h>
#include <string.h>
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {

#define PASSED  0
#define STATUS_FAILED  2

static JavaVM *vm;
static jvmtiEnv *jvmti = NULL;
static jniNativeInterface *orig_jni_functions = NULL;

static int verbose = 0;
static jint result = PASSED;

void redirect(JNIEnv *env, jvmtiError exError) {
    jvmtiError err;

    if (verbose)
        printf("\ntrying to get the JNI function table expecting the error %s to be returned ...\n",
            TranslateError(exError));

    err = jvmti->GetJNIFunctionTable((exError == JVMTI_ERROR_NULL_POINTER) ? NULL : &orig_jni_functions);
    if (err != exError) {
        result = STATUS_FAILED;
        printf("(%s,%d): TEST FAILED: GetJNIFunctionTable() returns %s instead of %s as expected\n",
            __FILE__, __LINE__, TranslateError(err), TranslateError(exError));
        return;
    }
    else if (verbose)
        printf("CHECK PASSED: GetJNIFunctionTable() returns %s as expected\n",
            TranslateError(err));
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetJNIFunctionTable_getjniftab002_check(JNIEnv *env, jobject obj) {
    jint err;
    JNIEnv *nextEnv = NULL;

    if (jvmti == NULL) {
        printf("(%s,%d): TEST FAILURE: JVMTI client was not properly loaded\n",
            __FILE__, __LINE__);
        return STATUS_FAILED;
    }

    /* a) Verifying the error JVMTI_ERROR_NULL_POINTER */
    if (verbose)
        printf("\na) Checking the function with the parameter JVMTI_ERROR_NULL_POINTER ...\n");
    redirect(env, JVMTI_ERROR_NULL_POINTER);

    /* b) Verifying the error JVMTI_ERROR_UNATTACHED_THREAD
       Note: the JNI spec says that the main thread can be detached from the VM
       only since JDK 1.2 */
    if (verbose)
        printf("\nb) Checking the function with the detached thread ...\n\ndetaching the main thread ...\n");
    err = vm->DetachCurrentThread();
    if (err != 0) {
        printf(
            "(%s,%d): Warning: DetachCurrentThread() returns: %d\n"
            "\tcheck with the detached main thread skipped\n",
            __FILE__, __LINE__, err);
    } else {
        redirect(env, JVMTI_ERROR_UNATTACHED_THREAD);

        if (verbose)
            printf("\nattaching the main thread back ...\n");
        err = vm->AttachCurrentThread((void **) &nextEnv, (void *) 0);
        if (err != 0) {
            printf("(%s,%d): TEST FAILURE: waitingThread: AttachCurrentThread() returns: %d\n",
                __FILE__, __LINE__, err);
            return STATUS_FAILED;
        }
    }

    return result;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getjniftab002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getjniftab002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getjniftab002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;

    if (options != NULL && strcmp(options, "-verbose") == 0)
        verbose = 1;

    if (verbose)
        printf("verbose mode on\n");

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("(%s,%d): Failed to call GetEnv\n", __FILE__, __LINE__);
        return JNI_ERR;
    }

    vm = jvm;

    return JNI_OK;
}

}
