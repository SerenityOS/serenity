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

typedef struct {
    const char *name;
    const char *sig;
    const jboolean stat;
    jfieldID fid;
    const char *descr;
} field;

static jvmtiEnv *jvmti;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jfieldID actual_fid = NULL;
static field fields[] = {
    { "fld0", "J", JNI_TRUE, NULL, "static long" },
    { "fld1", "J", JNI_FALSE, NULL, "long" },
    { "fld2", "F", JNI_TRUE, NULL, "static float" },
    { "fld3", "F", JNI_FALSE, NULL, "float" },
    { "fld4", "D", JNI_TRUE, NULL, "static double" },
    { "fld5", "D", JNI_FALSE, NULL, "double" },
    { "fld6", "Ljava/lang/Object;", JNI_TRUE, NULL, "static Object" },
    { "fld7", "Ljava/lang/Object;", JNI_FALSE, NULL, "Object" },
    { "fld8", "Z", JNI_TRUE, NULL, "static boolean" },
    { "fld9", "Z", JNI_FALSE, NULL, "boolean" },
    { "fld10", "B", JNI_TRUE, NULL, "static byte" },
    { "fld11", "B", JNI_FALSE, NULL, "byte" },
    { "fld12", "S", JNI_TRUE, NULL, "static short" },
    { "fld13", "S", JNI_FALSE, NULL, "short" },
    { "fld14", "C", JNI_TRUE, NULL, "static char" },
    { "fld15", "C", JNI_FALSE, NULL, "char" }
};

void JNICALL FieldAccess(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method,
        jlocation location, jclass field_klass, jobject obj, jfieldID field) {
    if (printdump == JNI_TRUE) {
        printf(">>> FieldAccess, field: 0x%p\n", field);
    }
    actual_fid = field;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setfldw005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setfldw005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setfldw005(JavaVM *jvm, char *options, void *reserved) {
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
Java_nsk_jvmti_SetFieldAccessWatch_setfldw005_getReady(JNIEnv *env, jclass cls) {
    jvmtiError err;
    size_t i;

    if (!caps.can_generate_field_access_events) {
        return;
    }

    for (i = 0; i < sizeof(fields) / sizeof(field); i++) {
        if (fields[i].stat == JNI_TRUE) {
            fields[i].fid = env->GetStaticFieldID(cls, fields[i].name, fields[i].sig);
        } else {
            fields[i].fid = env->GetFieldID(cls, fields[i].name, fields[i].sig);
        }
        if (fields[i].fid == NULL) {
            printf("Unable to set access watch on %s fld%" PRIuPTR ", fieldID=0",
                   fields[i].descr, i);
        } else {
            if (printdump == JNI_TRUE) {
                printf(">>> setting access watch on %s fld%" PRIuPTR ", fieldID=0x%p\n",
                       fields[i].descr, i, fields[i].fid);
            }
            err = jvmti->SetFieldAccessWatch(cls, fields[i].fid);
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
Java_nsk_jvmti_SetFieldAccessWatch_setfldw005_check(JNIEnv *env,
        jclass cls, jint ind) {
    if (printdump == JNI_TRUE) {
        printf(">>> checking on %s fld%d\n", fields[ind].descr, ind);
    }
    if (actual_fid != fields[ind].fid) {
        result = STATUS_FAILED;
        printf("Field %s fld%d: thrown field ID expected=0x%p, actual=0x%p\n",
               fields[ind].descr, ind, fields[ind].fid, actual_fid);
    }
    actual_fid = NULL;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_SetFieldAccessWatch_setfldw005_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
