/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

#ifdef __cplusplus
extern "C" {
#endif

#ifndef JNI_ENV_ARG

#ifdef __cplusplus
#define JNI_ENV_ARG(x, y) y
#define JNI_ENV_PTR(x) x
#else
#define JNI_ENV_ARG(x,y) x, y
#define JNI_ENV_PTR(x) (*x)
#endif

#endif

#define TranslateError(err) "JVMTI error"

#define PASSED 0
#define FAILED 2

static const char *EXPECTED_SIGNATURE = "Ljava/util/Collections;";
static const char *EXC_CNAME = "java/lang/Exception";

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;

static jboolean with_early_vm_start_capability = JNI_FALSE;

static jboolean class_in_class_load_events_vm_start = JNI_FALSE;
static jboolean class_in_class_load_events_vm_live = JNI_FALSE;
static jboolean class_in_class_prepare_events_vm_start = JNI_FALSE;
static jboolean class_in_class_prepare_events_vm_live = JNI_FALSE;

static int class_load_events_vm_start_count = 0;
static int class_prepare_events_vm_start_count = 0;

static jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved);

JNIEXPORT
jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT
jint JNICALL Agent_OnAttach(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT
jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    return JNI_VERSION_9;
}

static
jint throw_exc(JNIEnv *env, char *msg) {
    jclass exc_class = JNI_ENV_PTR(env)->FindClass(JNI_ENV_ARG(env, EXC_CNAME));

    if (exc_class == NULL) {
        printf("throw_exc: Error in FindClass(env, %s)\n", EXC_CNAME);
        return -1;
    }
    return JNI_ENV_PTR(env)->ThrowNew(JNI_ENV_ARG(env, exc_class), msg);
}

static void JNICALL
Callback_ClassFileLoad(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread, jclass klass) {
    jvmtiPhase phase;
    char *sig, *generic;
    jvmtiError err;

    err = (*jvmti)->GetPhase(jvmti_env,&phase);
    if (err != JVMTI_ERROR_NONE) {
        printf("ClassLoad event: GetPhase error: %s (%d)\n", TranslateError(err), err);
        result = FAILED;
        return;
    }

    if (phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE) {

        err = (*jvmti)->GetClassSignature(jvmti_env, klass, &sig, &generic);

        if (err != JVMTI_ERROR_NONE) {
            printf("ClassLoad event: GetClassSignature error: %s (%d)\n", TranslateError(err), err);
            result = FAILED;
            return;
        }

        if (phase == JVMTI_PHASE_START) {
            class_load_events_vm_start_count++;
            if(strcmp(sig, EXPECTED_SIGNATURE) == 0) {
                class_in_class_load_events_vm_start = JNI_TRUE;
            }
        } else {
            if(strcmp(sig, EXPECTED_SIGNATURE) == 0) {
                class_in_class_load_events_vm_live = JNI_TRUE;
            }
        }

        if (printdump == JNI_TRUE) {
            printf(">>>    ClassLoad event: phase(%d), class signature %s\n", phase, sig == NULL ? "null": sig);
        }
    } else {
        printf("ClassLoad event: get event in unexpected phase(%d)\n", phase);
        result = FAILED;
    }
}

static void JNICALL
Callback_ClassFilePrepare(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread, jclass klass) {
    jvmtiPhase phase;
    char *sig, *generic;
    jvmtiError err;

    err = (*jvmti)->GetPhase(jvmti_env,&phase);
    if (err != JVMTI_ERROR_NONE) {
        printf("ClassPrepare event: GetPhase error: %s (%d)\n", TranslateError(err), err);
        result = FAILED;
        return;
    }

    if (phase == JVMTI_PHASE_START || phase == JVMTI_PHASE_LIVE) {

        err = (*jvmti)->GetClassSignature(jvmti_env, klass, &sig, &generic);

        if (err != JVMTI_ERROR_NONE) {
            printf("ClassPrepare event: GetClassSignature error: %s (%d)\n", TranslateError(err), err);
            result = FAILED;
            return;
        }

        if (phase == JVMTI_PHASE_START) {
            class_prepare_events_vm_start_count++;
            if(strcmp(sig, EXPECTED_SIGNATURE) == 0) {
                class_in_class_prepare_events_vm_start = JNI_TRUE;
            }
        } else {
            if(strcmp(sig, EXPECTED_SIGNATURE) == 0) {
                class_in_class_prepare_events_vm_live = JNI_TRUE;
            }
        }

        if (printdump == JNI_TRUE) {
            printf(">>>    ClassPrepare event: phase(%d), class signature %s\n", phase, sig == NULL ? "null": sig);
        }
    } else {
        printf("ClassPrepare event: get event in unexpected phase(%d)\n", phase);
        result = FAILED;
    }
}

static
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res, size;
    jvmtiCapabilities caps;
    jvmtiEventCallbacks callbacks;
    jvmtiError err;

    if (options != NULL) {
        if (strstr(options, "with_early_vmstart") != NULL) {
            with_early_vm_start_capability = JNI_TRUE;
        }
        if (strstr(options, "printdump") != NULL) {
            printdump = JNI_TRUE;
        }
    }

    res = JNI_ENV_PTR(jvm)->GetEnv(JNI_ENV_ARG(jvm, (void **) &jvmti),
        JVMTI_VERSION_9);
    if (res != JNI_OK || jvmti == NULL) {
        printf("    Error: wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    if (with_early_vm_start_capability == JNI_TRUE) {
        printf("Enabling following capability: can_generate_early_vmstart\n");
        memset(&caps, 0, sizeof(caps));
        caps.can_generate_early_vmstart = 1;

        err = (*jvmti)->AddCapabilities(jvmti, &caps);
        if (err != JVMTI_ERROR_NONE) {
            printf("    Error in AddCapabilites: %s (%d)\n", TranslateError(err), err);
            return JNI_ERR;
        }
    }

    size = (jint)sizeof(callbacks);

    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.ClassLoad = Callback_ClassFileLoad;
    callbacks.ClassPrepare = Callback_ClassFilePrepare;

    err = (*jvmti)->SetEventCallbacks(jvmti, &callbacks, size);
    if (err != JVMTI_ERROR_NONE) {
        printf("    Error in SetEventCallbacks: %s (%d)\n", TranslateError(err), err);
        return JNI_ERR;
    }

    err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_CLASS_LOAD, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("    Error in SetEventNotificationMode: %s (%d)\n", TranslateError(err), err);
        return JNI_ERR;
    }

    err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE, NULL);

    if (err != JVMTI_ERROR_NONE) {
        printf("    Error in SetEventNotificationMode: %s (%d)\n", TranslateError(err), err);
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_MAAClassLoadPrepare_check(JNIEnv *env, jclass cls) {
    jobject loader = NULL;

    if (jvmti == NULL) {
        throw_exc(env, "JVMTI client was not properly loaded!\n");
        return FAILED;
    }

    if (with_early_vm_start_capability == JNI_TRUE) {
        /*
         * Expecting that "java/util/Collections" class from java.base module is present in the
         * ClassLoad and ClassPrepare events during VM Start phase when can_generate_early_vmstart
         * capability is enabled.
         * Expecting that ClassLoad and ClassPrepare events are sent in the VM early start phase
         * when can_generate_early_vmstart is enabled(JDK-8165681).
         */
        if (class_load_events_vm_start_count == 0) {
            throw_exc(env, "Didn't get ClassLoad events in start phase!\n");
            return FAILED;
        }

        printf("Expecting to find '%s' class in ClassLoad events during VM early start phase.\n", EXPECTED_SIGNATURE);
        if (class_in_class_load_events_vm_start == JNI_FALSE) {
            throw_exc(env, "Unable to find expected class in ClassLoad events during early start phase!\n");
            return FAILED;
        }

        if (class_prepare_events_vm_start_count == 0) {
            throw_exc(env, "Didn't get ClassPrepare events in start phase!\n");
            return FAILED;
        }

        printf("Expecting to find '%s' class in ClassPrepare events during VM early start phase.\n", EXPECTED_SIGNATURE);
        if (class_in_class_prepare_events_vm_start == JNI_FALSE) {
            throw_exc(env, "Unable to find expected class in ClassPrepare events during early start phase!\n");
            return FAILED;
        }
    } else {
        /*
         * Expecting that "java/util/Collections" class from java.base module is not present in the
         * ClassLoad and ClassPrepare events during VM Start phase when can_generate_early_vmstart
         * capability is disabled.
         */
        printf("Expecting that '%s' class is absent in ClassLoad events during normal VM start phase.\n", EXPECTED_SIGNATURE);
        if (class_in_class_prepare_events_vm_start == JNI_TRUE) {
            throw_exc(env, "Class is found in ClassLoad events during normal VM start phase!\n");
            return FAILED;
        }

        printf("Expecting that '%s' class is absent in ClassPrepare events during normal VM start phase.\n", EXPECTED_SIGNATURE);
        if (class_in_class_prepare_events_vm_start == JNI_TRUE) {
            throw_exc(env, "Class is found in ClassPrepare events during normal VM start phase!\n");
            return FAILED;
        }
    }

    /*
     * In any case, we not expect to see "java/util/Collections" class from java.base module
     * in the ClassLoad and ClassPrepare events during VM Live phase.
     */
    if (class_in_class_prepare_events_vm_live == JNI_TRUE) {
        throw_exc(env, "Class is found in ClassLoad events during VM Live phase!\n");
        return FAILED;
    }

    if (class_in_class_prepare_events_vm_live == JNI_TRUE) {
        throw_exc(env, "Class is found in ClassPrepare events during VM Live phase!\n");
        return FAILED;
    }

    return result;
}

#ifdef __cplusplus
}
#endif
