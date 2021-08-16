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
static jvmtiCapabilities caps;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static frame_info frames[] = {
    { "Ljava/lang/Object;", "wait", "()V" },
    { "Lnsk/jvmti/GetStackTrace/getstacktr003;", "dummy", "()V" },
    { "Lnsk/jvmti/GetStackTrace/getstacktr003;", "chain", "()V" },
    { "Lnsk/jvmti/GetStackTrace/getstacktr003$TestThread;", "run", "()V" },
};

#define NUMBER_OF_STACK_FRAMES ((int) (sizeof(frames)/sizeof(frame_info)))
#define MAX_NUMBER_OF_FRAMES 32

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getstacktr003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getstacktr003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getstacktr003(JavaVM *jvm, char *options, void *reserved) {
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

    if (!caps.can_suspend) {
        printf("Warning: suspend/resume is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetStackTrace_getstacktr003_chain(JNIEnv *env, jclass cls) {
    jmethodID mid;

    mid = env->GetStaticMethodID(cls, "dummy", "()V");
    if (mid == NULL) {
        printf("Could not find method ID for dummy()V!\n");
    } else {
        env->CallStaticVoidMethod(cls, mid);
    }

    return;
}

JNIEXPORT int JNICALL
Java_nsk_jvmti_GetStackTrace_getstacktr003_check(JNIEnv *env, jclass cls, jthread thread) {
    jvmtiError err;
    jvmtiFrameInfo f[MAX_NUMBER_OF_FRAMES];
    jclass callerClass;
    char *sigClass, *name, *sig, *generic;
    jint i, count;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    if (!caps.can_suspend) {
        return result;
    }

    err = jvmti->SuspendThread(thread);
    if (err != JVMTI_ERROR_NONE) {
        printf("(SuspendThread) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return result;
    }

    err = jvmti->GetStackTrace(thread,
        0, MAX_NUMBER_OF_FRAMES, f, &count);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetStackTrace) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return result;
    }
    if (count < NUMBER_OF_STACK_FRAMES) {
        printf("Number of frames: %d is less then expected: %d\n",
               count, NUMBER_OF_STACK_FRAMES);
        result = STATUS_FAILED;
    }
    for (i = 0; i < count; i++) {
        if (printdump == JNI_TRUE) {
            printf(">>> checking frame#%d ...\n", count-1-i);
        }
        err = jvmti->GetMethodDeclaringClass(f[count-1-i].method,
            &callerClass);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetMethodDeclaringClass#%d) unexpected error: %s (%d)\n",
                   count-1-i, TranslateError(err), err);
            result = STATUS_FAILED;
            continue;
        }
        err = jvmti->GetClassSignature(callerClass,
            &sigClass, &generic);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetClassSignature#%d) unexpected error: %s (%d)\n",
                   count-1-i, TranslateError(err), err);
            result = STATUS_FAILED;
            continue;
        }
        err = jvmti->GetMethodName(f[count-1-i].method,
            &name, &sig, &generic);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetMethodName#%d) unexpected error: %s (%d)\n",
                   count-1-i, TranslateError(err), err);
            result = STATUS_FAILED;
            continue;
        }
        if (printdump == JNI_TRUE) {
            printf(">>>   class:  \"%s\"\n", sigClass);
            printf(">>>   method: \"%s%s\"\n", name, sig);
            printf(">>>   %d ... done\n", i);
        }
        if (i < NUMBER_OF_STACK_FRAMES) {
            if (sigClass == NULL || strcmp(sigClass, frames[NUMBER_OF_STACK_FRAMES-1-i].cls) != 0) {
                printf("(frame#%d) wrong class sig: \"%s\", expected: \"%s\"\n",
                       NUMBER_OF_STACK_FRAMES-1-i, sigClass, frames[NUMBER_OF_STACK_FRAMES-1-i].cls);
                result = STATUS_FAILED;
            }
            if (name == NULL || strcmp(name, frames[NUMBER_OF_STACK_FRAMES-1-i].name) != 0) {
                printf("(frame#%d) wrong method name: \"%s\", expected: \"%s\"\n",
                       NUMBER_OF_STACK_FRAMES-1-i, name, frames[NUMBER_OF_STACK_FRAMES-1-i].name);
                result = STATUS_FAILED;
            }
            if (sig == NULL || strcmp(sig, frames[NUMBER_OF_STACK_FRAMES-1-i].sig) != 0) {
                printf("(frame#%d) wrong method sig: \"%s\", expected: \"%s\"\n",
                       NUMBER_OF_STACK_FRAMES-1-i, sig, frames[NUMBER_OF_STACK_FRAMES-1-i].sig);
                result = STATUS_FAILED;
            }
        }
    }

    err = jvmti->ResumeThread(thread);
    if (err != JVMTI_ERROR_NONE) {
        printf("(ResumeThread) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    return result;
}

}
