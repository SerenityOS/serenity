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
#include "JVMTITools.h"

extern "C" {


#define PASSED  0
#define STATUS_FAILED  2

static jvmtiEnv *jvmti;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;
static jint result = PASSED;

void JNICALL FieldModification(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method, jlocation location, jclass field_klass, jobject obj,
        jfieldID field, char sig, jvalue new_value) {
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setfmodw002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setfmodw002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setfmodw002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv !\n");
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

    if (caps.can_generate_field_modification_events) {
        callbacks.FieldModification = &FieldModification;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }

        err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                JVMTI_EVENT_FIELD_MODIFICATION, NULL);
        if (err != JVMTI_ERROR_NONE) {
            printf("Failed to enable JVMTI_EVENT_FIELD_MODIFICATION: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: FieldModification watch is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_SetFieldModificationWatch_setfmodw002_check(JNIEnv *env,
        jclass cls) {
    jvmtiError err;
    jfieldID fid;

    fid = env->GetStaticFieldID(cls, "fld1", "I");
    if (fid == NULL) {
      printf("(GetStaticFieldID) returns NULL");
      result = STATUS_FAILED;
      return result;
    }

    if (!caps.can_generate_field_modification_events) {
        err = jvmti->SetFieldModificationWatch(cls, fid);
        if (err != JVMTI_ERROR_MUST_POSSESS_CAPABILITY) {
            result = STATUS_FAILED;
            printf("Failed to return JVMTI_ERROR_MUST_POSSESS_CAPABILITY:");
            printf(" %s (%d)\n", TranslateError(err), err);
        }
    } else {
        err = jvmti->SetFieldModificationWatch(NULL, fid);
        if (err != JVMTI_ERROR_INVALID_CLASS) {
            result = STATUS_FAILED;
            printf("Failed to return JVMTI_ERROR_INVALID_CLASS: %s (%d)\n",
                   TranslateError(err), err);
        }

        err = jvmti->SetFieldModificationWatch(cls, NULL);
        if (err != JVMTI_ERROR_INVALID_FIELDID) {
            result = STATUS_FAILED;
            printf("Failed to return JVMTI_ERROR_INVALID_FIELDID: %s (%d)\n",
                   TranslateError(err), err);
        }

        err = (jvmti->SetFieldModificationWatch(cls, fid));
        if (err != JVMTI_ERROR_NONE) {
            result = STATUS_FAILED;
            printf("(SetFieldModificationWatch) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
        } else {
            err = jvmti->SetFieldModificationWatch(cls, fid);
            if (err != JVMTI_ERROR_DUPLICATE) {
                result = STATUS_FAILED;
                printf("Failed to return JVMTI_ERROR_DUPLICATE: %s (%d)\n",
                       TranslateError(err), err);
            }
        }
    }

    return result;
}

}
