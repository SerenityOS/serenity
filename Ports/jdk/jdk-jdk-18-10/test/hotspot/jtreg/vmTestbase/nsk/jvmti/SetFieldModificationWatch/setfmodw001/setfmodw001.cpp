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
#include <ctype.h>
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define PASSED  0
#define STATUS_FAILED  2

typedef struct {
    const char *klass;
    const char *name;
    const char *sig;
    int stat;
    jfieldID fid;
    jfieldID thrown_fid;
} field;

static jvmtiEnv *jvmti;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;
static jint result = PASSED;
static field fields[] = {
    { "nsk/jvmti/SetFieldModificationWatch/setfmodw001", "fld0", "I", 0, NULL, NULL },
    { "nsk/jvmti/SetFieldModificationWatch/setfmodw001", "fld1", "I", 1, NULL, NULL },
    { "nsk/jvmti/SetFieldModificationWatch/setfmodw001", "fld2",
      "Lnsk/jvmti/SetFieldModificationWatch/setfmodw001a;", 0, NULL, NULL },
    { "nsk/jvmti/SetFieldModificationWatch/setfmodw001a", "fld3", "[I", 0, NULL, NULL },
    { "nsk/jvmti/SetFieldModificationWatch/setfmodw001b", "fld4", "F", 0, NULL, NULL },
};

void setWatch(JNIEnv *env, jint ind) {
    jclass cls;
    jvmtiError err;

    cls = env->FindClass(fields[ind].klass);
    if (fields[ind].fid == NULL) {
        if (fields[ind].stat) {
            fields[ind].fid = env->GetStaticFieldID(cls, fields[ind].name, fields[ind].sig);
        } else {
            fields[ind].fid = env->GetFieldID(cls, fields[ind].name, fields[ind].sig);
        }
    }

    err = jvmti->SetFieldModificationWatch(cls, fields[ind].fid);
    if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY &&
            !caps.can_generate_field_modification_events) {
        /* Ok, it's expected */
    } else if (err != JVMTI_ERROR_NONE) {
        printf("(SetFieldModificationWatch#%d) unexpected error: %s (%d)\n",
               ind, TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

void JNICALL FieldModification(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method, jlocation location, jclass field_klass, jobject obj,
        jfieldID field, char sig, jvalue new_value) {

    char *fld_name = NULL;
    jint fld_ind = 0;
    size_t len = 0;
    jvmtiError err = jvmti_env->GetFieldName(field_klass, field,
                                                &fld_name, NULL, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Error in GetFieldName: %s (%d)\n", TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }
    if (fld_name == NULL) {
        printf("GetFieldName returned NULL field name\n");
        result = STATUS_FAILED;
        return;
    }
    len = strlen(fld_name);
    /* All field names have the same length and end with a digit. */
    if (len != strlen(fields[0].name) || !isdigit(fld_name[len - 1])) {
        printf("GetFieldName returned unexpected field name: %s\n", fld_name);
        result = STATUS_FAILED;
        return;
    }
    fld_ind = (int)(fld_name[len - 1] - '0'); /* last digit is index in the array */
    fields[fld_ind].thrown_fid = field;
    jvmti_env->Deallocate((unsigned char*) fld_name);
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setfmodw001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setfmodw001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setfmodw001(JavaVM *jvm, char *options, void *reserved) {
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
Java_nsk_jvmti_SetFieldModificationWatch_setfmodw001_setWatch(JNIEnv *env,
        jclass cls, jint fld_ind) {
    setWatch(env, fld_ind);
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_SetFieldModificationWatch_setfmodw001_touchfld0(JNIEnv *env,
        jclass cls) {
    setWatch(env, (jint)0);
    env->SetIntField(cls, fields[0].fid, (jint)2000);
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_SetFieldModificationWatch_setfmodw001_check(JNIEnv *env,
        jclass cls, jint fld_ind, jboolean flag) {
    jfieldID thrown_fid = fields[fld_ind].thrown_fid;

    if (caps.can_generate_field_modification_events) {
        if (flag == JNI_FALSE && thrown_fid != NULL) {
            result = STATUS_FAILED;
            printf("(Field %d) FieldModification without modification watch set\n",
                   fld_ind);
        } else if (flag == JNI_TRUE && thrown_fid != fields[fld_ind].fid) {
            result = STATUS_FAILED;
            printf("(Field %d) thrown field ID expected: 0x%p, got: 0x%p\n",
                   fld_ind, fields[fld_ind].fid, thrown_fid);
        }
    }
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_SetFieldModificationWatch_setfmodw001_getRes(JNIEnv *env,
        jclass cls) {
    return result;
}

}
