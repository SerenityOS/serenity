/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

static const char *EXPECTED_NAME = "java/util/Collections";
static const char *EXC_CNAME = "java/lang/Exception";

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;

static jboolean with_early_vm_start_capability = JNI_FALSE;
static jboolean with_early_class_hook_capability = JNI_FALSE;

static jboolean found_class_in_vm_start = JNI_FALSE;
static jboolean found_class_in_primordial = JNI_FALSE;
static jboolean found_class_in_cflh_events = JNI_FALSE;

static int cflh_events_primordial_count = 0;
static int cflh_events_vm_start_count = 0;

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
Callback_ClassFileLoadHook(jvmtiEnv *jvmti_env, JNIEnv *env,
                           jclass class_being_redefined,
                           jobject loader, const char* name, jobject protection_domain,
                           jint class_data_len, const unsigned char* class_data,
                           jint *new_class_data_len, unsigned char** new_class_data) {
    jvmtiPhase phase;
    jvmtiError err;

    err = (*jvmti)->GetPhase(jvmti_env, &phase);
    if (err != JVMTI_ERROR_NONE) {
        printf("ClassFileLoadHook event: GetPhase error: %s (%d)\n", TranslateError(err), err);
        result = FAILED;
        return;
    }

    if (phase == JVMTI_PHASE_PRIMORDIAL || phase == JVMTI_PHASE_START) {
        if (phase == JVMTI_PHASE_START) {
            cflh_events_vm_start_count++;
            if(strcmp(name, EXPECTED_NAME) == 0) {
                found_class_in_vm_start = JNI_TRUE;
            }
        } else {
            cflh_events_primordial_count++;
            if(strcmp(name, EXPECTED_NAME) == 0) {
                found_class_in_primordial = JNI_TRUE;
            }
        }
    }

    if(strcmp(name, EXPECTED_NAME) == 0) {
        found_class_in_cflh_events = JNI_TRUE;
    }

    if (printdump == JNI_TRUE) {
        printf(">>>    ClassFileLoadHook event: phase(%d), class name %s\n", phase, name);
    }
}

static
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res, size;
    jvmtiCapabilities caps;
    jvmtiEventCallbacks callbacks;
    jvmtiError err;

    printf("agent options: %s\n", options);
    if (options != NULL) {
        if (strstr(options, "with_early_vmstart") != NULL) {
            with_early_vm_start_capability = JNI_TRUE;
        }
        if (strstr(options, "with_early_class_hook") != NULL) {
            with_early_class_hook_capability = JNI_TRUE;
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

    printf("Enabling following capabilities: can_generate_all_class_hook_events");
    memset(&caps, 0, sizeof(caps));
    caps.can_generate_all_class_hook_events = 1;
    if (with_early_vm_start_capability == JNI_TRUE) {
        printf(", can_generate_early_vmstart");
        caps.can_generate_early_vmstart = 1;
    }
    if (with_early_class_hook_capability == JNI_TRUE) {
        printf(", can_generate_early_class_hook_events");
        caps.can_generate_early_class_hook_events = 1;
    }
    printf("\n");

    err = (*jvmti)->AddCapabilities(jvmti, &caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("    Error in AddCapabilites: %s (%d)\n", TranslateError(err), err);
        return JNI_ERR;
    }

    size = (jint)sizeof(callbacks);

    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.ClassFileLoadHook = Callback_ClassFileLoadHook;

    err = (*jvmti)->SetEventCallbacks(jvmti, &callbacks, size);
    if (err != JVMTI_ERROR_NONE) {
        printf("    Error in SetEventCallbacks: %s (%d)\n", TranslateError(err), err);
        return JNI_ERR;
    }

    err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("    Error in SetEventNotificationMode: %s (%d)\n", TranslateError(err), err);
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_MAAClassFileLoadHook_check(JNIEnv *env, jclass cls) {
    jobject loader = NULL;

    if (jvmti == NULL) {
        throw_exc(env, "JVMTI client was not properly loaded!\n");
        return FAILED;
    }

    /*
     * Expecting ClassFileLoadHook events in the VM Start phase if early_vm_start is enabled.
     */
    if (with_early_vm_start_capability == JNI_TRUE && cflh_events_vm_start_count == 0) {
        throw_exc(env, "Didn't get ClassFileLoadHook events in start phase!\n");
        return FAILED;
    }

    if (with_early_class_hook_capability == JNI_TRUE) {
       /*
        * Expecting that we get ClassFileLoadHook events in the Primordial phase
        * when can_generate_all_class_hook_events and can_generate_early_class_hook_events
        * capabilities are enabled.
        */
        if (cflh_events_primordial_count == 0) {
            throw_exc(env, "Didn't get ClassFileLoadHook events in primordial phase!\n");
            return FAILED;
        }
    } else {
       /*
        * Expecting that we don't get ClassFileLoadHook events in the Primordial phase
        * when can_generate_early_class_hook_events capability is disabled.
        */
        if (cflh_events_primordial_count != 0) {
            throw_exc(env, "Get ClassFileLoadHook events in primordial phase!\n");
            return FAILED;
        }
    }


    if (with_early_vm_start_capability == JNI_TRUE) {
        /*
         * Expecting that "java/util/Collections" class from java.base module is present in the
         * ClassFileLoadHook events during VM Start phase when can_generate_early_vmstart
         * capability is enabled.
         */
        printf("Expecting to find '%s' class in ClassFileLoadHook events during VM early start phase.\n", EXPECTED_NAME);
        if (found_class_in_vm_start == JNI_FALSE) {
            throw_exc(env, "Unable to find expected class in ClassLoad events during VM early start phase!\n");
            return FAILED;
        }
    } else if (with_early_class_hook_capability == JNI_TRUE) {
        /*
         * Expecting that "java/util/Collections" class from java.base module is present in the
         * ClassFileLoadHook events during Primordial phase when can_generate_all_class_hook_events
         * and can_generate_early_class_hook_events capabilities are enabled and can_generate_early_vmstart
         * capability is disabled.
         */
        printf("Expecting to find '%s' class in ClassFileLoadHook events during VM primordial phase.\n", EXPECTED_NAME);
        if (found_class_in_primordial == JNI_FALSE) {
            throw_exc(env, "Unable to find expected class in ClassFileLoadHook events during primordial phase!\n");
            return FAILED;
        }
    } else {
        /*
         * Expecting that "java/util/Collections" class from java.base module is not present in the
         * ClassFileLoadHook events when can_generate_all_class_hook_events, can_generate_early_class_hook_events
         * and can_generate_early_vmstart capabilities are disabled.
         */
        printf("Expecting that '%s' class is absent in ClassLoadHook events.\n", EXPECTED_NAME);
        if (found_class_in_cflh_events == JNI_TRUE) {
            throw_exc(env, "Class is found in ClassFileLoadHook events!\n");
            return FAILED;
        }
    }

    return result;
}

#ifdef __cplusplus
}
#endif
