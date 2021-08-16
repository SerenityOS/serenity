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

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jbyteArray classBytes;

void JNICALL
Exception(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread,
        jmethodID method, jlocation location, jobject exception,
        jmethodID c_method, jlocation c_location) {
    jvmtiError err;
    jclass klass;
    jvmtiClassDefinition classDef;

    err = jvmti->SetEventNotificationMode(JVMTI_DISABLE,
            JVMTI_EVENT_EXCEPTION, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to disable JVMTI_EVENT_EXCEPTION: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }

    err = jvmti_env->GetMethodDeclaringClass(method, &klass);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodDeclaringClass) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    classDef.klass = klass;
    classDef.class_byte_count = env->GetArrayLength(classBytes);
    classDef.class_bytes = (unsigned char *) env->GetByteArrayElements(classBytes, NULL);

    if (printdump == JNI_TRUE) {
        printf(">>> about to call RedefineClasses\n");
    }

    err = jvmti->RedefineClasses(1, &classDef);
    if (err != JVMTI_ERROR_NONE) {
        printf("(RedefineClasses) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_redefclass018(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_redefclass018(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_redefclass018(JavaVM *jvm, char *options, void *reserved) {
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

    if (caps.can_generate_exception_events) {
        callbacks.Exception = &Exception;
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
Java_nsk_jvmti_RedefineClasses_redefclass018_getReady(JNIEnv *env, jclass cls,
        jclass clazz, jbyteArray bytes) {
    jvmtiError err;
    jthread thread;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (!caps.can_redefine_classes ||
        !caps.can_generate_exception_events) return;

    classBytes = (jbyteArray) env->NewGlobalRef(bytes);

    err = jvmti->GetCurrentThread(&thread);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to get current thread: %s (%d)\n", TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
            JVMTI_EVENT_EXCEPTION, thread);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable JVMTI_EVENT_EXCEPTION_CATCH: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RedefineClasses_redefclass018_check(JNIEnv *env, jclass cls) {
    return result;
}

}
