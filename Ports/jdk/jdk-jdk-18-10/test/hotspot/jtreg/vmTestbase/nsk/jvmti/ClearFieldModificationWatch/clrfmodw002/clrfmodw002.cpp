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

static jvmtiEnv *jvmti;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;
static jint result = PASSED;

void JNICALL FieldModification(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thd, jmethodID mid, jlocation loc,
        jclass field_klass, jobject obj, jfieldID field,
        char sig, jvalue new_value) {
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_clrfmodw002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_clrfmodw002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_clrfmodw002(JavaVM *jvm, char *options, void *reserved) {
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

JNIEXPORT void JNICALL
Java_nsk_jvmti_ClearFieldModificationWatch_clrfmodw002_check(JNIEnv *env,
        jclass cls) {
    jvmtiError err;
    jfieldID fid1, fid2;

    fid1 = env->GetStaticFieldID(cls, "fld1", "I");
    fid2 = env->GetStaticFieldID(cls, "fld2", "I");

    if (!caps.can_generate_field_modification_events) {
        printf("Warning: ClearFieldModificationWatch is not implemented\n");
        err = jvmti->ClearFieldModificationWatch(cls, fid1);
        if (err != JVMTI_ERROR_MUST_POSSESS_CAPABILITY) {
            result = STATUS_FAILED;
            printf("Failed to return MUST_POSSESS_CAPABILITY: %s (%d)\n",
                   TranslateError(err), err);
        }
    } else {
        err = jvmti->ClearFieldModificationWatch(NULL, fid2);
        if (err != JVMTI_ERROR_INVALID_CLASS) {
            result = STATUS_FAILED;
            printf("Failed to return JVMTI_ERROR_INVALID_CLASS: %s (%d)\n",
                   TranslateError(err), err);
        }

        err = jvmti->ClearFieldModificationWatch(cls, NULL);
        if (err != JVMTI_ERROR_INVALID_FIELDID) {
            result = STATUS_FAILED;
            printf("Failed to return INVALID_FIELDID: %s (%d)\n",
                   TranslateError(err), err);
        }

        err = jvmti->ClearFieldModificationWatch(cls, fid2);
        if (err != JVMTI_ERROR_NOT_FOUND) {
            result = STATUS_FAILED;
            printf("Failed to return NOT_FOUND: %s (%d)\n",
                   TranslateError(err), err);
        }
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_ClearFieldModificationWatch_clrfmodw002_getRes(JNIEnv *env,
        jclass cls) {
    return result;
}

}
