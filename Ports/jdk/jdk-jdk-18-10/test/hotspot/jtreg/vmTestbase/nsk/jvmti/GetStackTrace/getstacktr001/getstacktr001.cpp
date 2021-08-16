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


#define PASSED 0
#define STATUS_FAILED 2

typedef struct {
    const char *cls;
    const char *name;
    const char *sig;
} frame_info;

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static frame_info frames[] = {
    { "Lnsk/jvmti/GetStackTrace/getstacktr001;", "check",
     "(Ljava/lang/Thread;)V" },
    { "Lnsk/jvmti/GetStackTrace/getstacktr001;", "dummy", "()V" },
    { "Lnsk/jvmti/GetStackTrace/getstacktr001;", "chain", "()I" },
    { "Lnsk/jvmti/GetStackTrace/getstacktr001;", "run",
     "([Ljava/lang/String;Ljava/io/PrintStream;)I" },
    { "Lnsk/jvmti/GetStackTrace/getstacktr001;", "main",
     "([Ljava/lang/String;)V" }
};

#define NUMBER_OF_STACK_FRAMES ((int) (sizeof(frames)/sizeof(frame_info)))

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getstacktr001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getstacktr001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getstacktr001(JavaVM *jvm, char *options, void *reserved) {
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
Java_nsk_jvmti_GetStackTrace_getstacktr001_chain(JNIEnv *env, jclass cls) {
    jmethodID mid;

    mid = env->GetStaticMethodID(cls, "dummy", "()V");
    env->CallStaticVoidMethod(cls, mid);

    return result;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetStackTrace_getstacktr001_check(JNIEnv *env, jclass cls, jthread thread) {
    jvmtiError err;
    jvmtiFrameInfo f[NUMBER_OF_STACK_FRAMES + 1];
    jclass callerClass;
    char *sigClass, *name, *sig, *generic;
    jint i, count;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->GetStackTrace(thread, 0,
        NUMBER_OF_STACK_FRAMES + 1, f, &count);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetStackTrace) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    if (count != NUMBER_OF_STACK_FRAMES) {
        printf("Wrong number of frames: %d, expected: %d\n",
               count, NUMBER_OF_STACK_FRAMES);
        result = STATUS_FAILED;
    }
    for (i = 0; i < count; i++) {
        if (printdump == JNI_TRUE) {
            printf(">>> checking frame#%d ...\n", i);
        }
        err = jvmti->GetMethodDeclaringClass(
            f[i].method, &callerClass);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetMethodDeclaringClass#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
            continue;
        }
        err = jvmti->GetClassSignature(callerClass,
            &sigClass, &generic);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetClassSignature#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
            continue;
        }
        err = jvmti->GetMethodName(f[i].method, &name, &sig, &generic);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetMethodName#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
            continue;
        }
        if (printdump == JNI_TRUE) {
            printf(">>>   class:  \"%s\"\n", sigClass);
            printf(">>>   method: \"%s%s\"\n", name, sig);
            printf(">>>   %d ... done\n", i);
        }
        if (i < NUMBER_OF_STACK_FRAMES) {
            if (sigClass == NULL || strcmp(sigClass, frames[i].cls) != 0) {
                printf("(frame#%d) wrong class sig: \"%s\", expected: \"%s\"\n",
                       i, sigClass, frames[i].cls);
                result = STATUS_FAILED;
            }
            if (name == NULL || strcmp(name, frames[i].name) != 0) {
                printf("(frame#%d) wrong method name: \"%s\", expected: \"%s\"\n",
                       i, name, frames[i].name);
                result = STATUS_FAILED;
            }
            if (sig == NULL || strcmp(sig, frames[i].sig) != 0) {
                printf("(frame#%d) wrong method sig: \"%s\", expected: \"%s\"\n",
                       i, sig, frames[i].sig);
                result = STATUS_FAILED;
            }
        }
    }
}

}
