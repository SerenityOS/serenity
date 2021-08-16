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
#include <inttypes.h>
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
static jboolean printdump = JNI_FALSE;
static jfieldID actual_fid = NULL;
static jfieldID fids[4] = { NULL, NULL, NULL, NULL };

void JNICALL FieldAccess(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method,
        jlocation location, jclass field_klass, jobject obj, jfieldID field) {
    if (printdump == JNI_TRUE) {
        printf(">>> FieldAccess, field: 0x%p\n", field);
    }
    actual_fid = field;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setfldw004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setfldw004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setfldw004(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv !\n");
        return JNI_ERR;
    }

    err = jvmti->GetCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
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

    if (caps.can_generate_field_access_events) {
        callbacks.FieldAccess = &FieldAccess;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: FieldAccess watch is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_SetFieldAccessWatch_setfldw004_getReady(JNIEnv *env, jclass cls) {
    jvmtiError err;
    size_t i;

    if (!caps.can_generate_field_access_events) {
        return;
    }

    fids[0] = env->GetStaticFieldID(cls, "fld0", "I");
    fids[1] = env->GetStaticFieldID(cls, "fld1", "I");
    fids[2] = env->GetFieldID(cls, "fld2", "I");
    fids[3] = env->GetFieldID(cls, "fld3", "I");
    for (i = 0; i < sizeof(fids) / sizeof(jfieldID); i++) {
        if (fids[i] == NULL) {
            printf("Unable to set field access watch on fld%" PRIuPTR ", fieldID=0", i);
        } else {
            if (printdump == JNI_TRUE) {
                printf(">>> setting access watch on fld%" PRIuPTR, i);
                printf(", FieldID=0x%p\n", fids[i]);
            }
            err = jvmti->SetFieldAccessWatch(cls, fids[i]);
            if (err != JVMTI_ERROR_NONE) {
                printf("(SetFieldAccessWatch#%" PRIuPTR ") unexpected error: %s (%d)\n",
                       i, TranslateError(err), err);
                result = STATUS_FAILED;
            }
        }
    }
    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_FIELD_ACCESS, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("(SetEventNotificationMode) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_SetFieldAccessWatch_setfldw004_check(JNIEnv *env,
        jclass cls, jint ind) {
    if (printdump == JNI_TRUE) {
        printf(">>> checking on fld%d\n", ind);
    }
    if (actual_fid != fids[ind]) {
        result = STATUS_FAILED;
        printf("Field %d: thrown field ID expected=0x%p, actual=0x%p\n",
               ind, fids[ind], actual_fid);
    }
    actual_fid = NULL;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_SetFieldAccessWatch_setfldw004_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
