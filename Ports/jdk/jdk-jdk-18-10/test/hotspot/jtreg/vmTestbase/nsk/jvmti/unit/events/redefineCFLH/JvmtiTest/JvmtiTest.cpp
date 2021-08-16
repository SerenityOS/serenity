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
#include <jvmti.h>
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define STATUS_FAILED 2
#define PASSED 0

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;

const char* CLASS_NAME = "nsk/jvmti/unit/events/redefineCFLH/JvmtiTestr";

void JNICALL
VMInit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread) {
    if (printdump == JNI_TRUE) {
        printf("VMInit event received\n");
    }
}

void JNICALL
ClassFileLoadHook(jvmtiEnv *jvmti_env, JNIEnv *env,
                  jclass redefined_class,
                  jobject loader, const char* name,
                  jobject protection_domain,
                  jint class_data_len,
                  const unsigned char* class_data,
                  jint* new_class_data_len,
                  unsigned char** new_class_data) {

    jvmtiError err;
    int len = sizeof(jint);
    char *sig;
    char *gen;

    if (name != NULL && (strcmp(name, CLASS_NAME) == 0)) {
        if (printdump == JNI_TRUE) {
            printf("Received class file load hook event for class %s\n", name);
        }

        if (redefined_class != NULL) {
            err = jvmti->GetClassSignature(redefined_class, &sig, &gen);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetClassSignature) unexpected error: %s (%d)\n",
                       TranslateError(err), err);
                result = STATUS_FAILED;
            } else if (printdump == JNI_TRUE) {
                printf("redefined class name signature is %s\n", sig);
            }
        }

        err = jvmti->Allocate(class_data_len, new_class_data);
        if (err != JVMTI_ERROR_NONE) {
            printf("(Allocate) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        } else {
            *new_class_data_len = class_data_len;
            memcpy(*new_class_data, class_data, class_data_len);
        }
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_JvmtiTest(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_JvmtiTest(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_JvmtiTest(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *vm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    res = vm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK) {
        printf("%s: Failed to call GetEnv: error=%d\n", __FILE__, res);
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

    callbacks.VMInit = &VMInit;
    callbacks.ClassFileLoadHook = &ClassFileLoadHook;
    err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    if (err != JVMTI_ERROR_NONE) {
        printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable event JVMTI_EVENT_VM_INIT: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable event JVMTI_EVENT_CLASS_FILE_LOAD_HOOK: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_events_redefineCFLH_JvmtiTest_makeRedefinition(JNIEnv *env,
        jclass cls, jint fl, jclass redefCls, jbyteArray classBytes) {
    jvmtiClassDefinition classDef;
    jvmtiError err;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    if (!caps.can_redefine_classes) {
        return PASSED;
    }

/* filling the structure jvmtiClassDefinition */
    classDef.klass = redefCls;
    classDef.class_byte_count = env->GetArrayLength(classBytes);
    classDef.class_bytes = (unsigned char *) env->GetByteArrayElements(classBytes, NULL);

    if (fl == 2) {
        printf(">>>>>>>> Invoke RedefineClasses():\n");
        printf("\tnew class byte count=%d\n", classDef.class_byte_count);
    }
    err = jvmti->RedefineClasses(1, &classDef);
    if (err != JVMTI_ERROR_NONE) {
        printf("%s: Failed to call RedefineClasses():\n", __FILE__);
        printf("\tthe function returned error %d: %s\n",
            err, TranslateError(err));
        printf("\tFor more info about this error see the JVMTI spec.\n");
        return STATUS_FAILED;
    }
    if (fl == 2)
        printf("<<<<<<<< RedefineClasses() is successfully done\n");

    return PASSED;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_events_redefineCFLH_JvmtiTest_GetResult(JNIEnv *env, jclass cls) {
    return result;
}

}
