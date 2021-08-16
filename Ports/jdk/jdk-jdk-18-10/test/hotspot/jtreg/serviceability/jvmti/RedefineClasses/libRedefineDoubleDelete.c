/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

static jvmtiEnv *jvmti = NULL;

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


static jint newClassDataLen = 0;
static unsigned char* newClassData = NULL;

static jint
getBytecodes(jvmtiEnv *jvmti_env,
             jint class_data_len, const unsigned char* class_data) {
    int i;
    jint res;

    newClassDataLen = class_data_len;
    res = (*jvmti_env)->Allocate(jvmti_env, newClassDataLen, &newClassData);
    if (res != JNI_OK) {
        printf("    Unable to allocate bytes\n");
        return JNI_ERR;
    }
    for (i = 0; i < newClassDataLen; i++) {
        newClassData[i] = class_data[i];
        // Rewrite oo in class to aa
        if (i > 0 && class_data[i] == 'o' && class_data[i-1] == 'o') {
            newClassData[i] = newClassData[i-1] = 'a';
        }
    }
    printf("  ... copied bytecode: %d bytes\n", (int)newClassDataLen);
    return JNI_OK;
}


static void JNICALL
Callback_ClassFileLoadHook(jvmtiEnv *jvmti_env, JNIEnv *env,
                           jclass class_being_redefined,
                           jobject loader, const char* name, jobject protection_domain,
                           jint class_data_len, const unsigned char* class_data,
                           jint *new_class_data_len, unsigned char** new_class_data) {
    if (name != NULL && strcmp(name, "RedefineDoubleDelete$B") == 0) {
        if (newClassData == NULL) {
            jint res = getBytecodes(jvmti_env, class_data_len, class_data);
            if (res == JNI_ERR) {
              printf(">>>    ClassFileLoadHook event: class name %s FAILED\n", name);
              return;
            }
            // Only change for first CFLH event.
            *new_class_data_len = newClassDataLen;
            *new_class_data = newClassData;
        }
        printf(">>>    ClassFileLoadHook event: class name %s\n", name);
    }
}

static
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res, size;
    jvmtiCapabilities caps;
    jvmtiEventCallbacks callbacks;
    jvmtiError err;

    res = JNI_ENV_PTR(jvm)->GetEnv(JNI_ENV_ARG(jvm, (void **) &jvmti),
        JVMTI_VERSION_9);
    if (res != JNI_OK || jvmti == NULL) {
        printf("    Error: wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    printf("Enabling following capabilities: can_generate_all_class_hook_events, "
           "can_retransform_classes, can_redefine_classes");
    memset(&caps, 0, sizeof(caps));
    caps.can_generate_all_class_hook_events = 1;
    caps.can_retransform_classes = 1;
    caps.can_redefine_classes = 1;
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

#ifdef __cplusplus
}
#endif
