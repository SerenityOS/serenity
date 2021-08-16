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

typedef struct {
    const char *klass;
    const char *name;
    const char *sig;
    int stat;
    jfieldID fid;
} field;

static jvmtiEnv *jvmti;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;
static jint result = PASSED;
static jfieldID thrown_fid = NULL;
static field fields[] = {
    { "nsk/jvmti/ClearFieldModificationWatch/clrfmodw001", "fld0", "I", 0, NULL },
    { "nsk/jvmti/ClearFieldModificationWatch/clrfmodw001", "fld1", "I", 1, NULL },
    { "nsk/jvmti/ClearFieldModificationWatch/clrfmodw001", "fld2",
      "Lnsk/jvmti/ClearFieldModificationWatch/clrfmodw001a;", 0, NULL },
    { "nsk/jvmti/ClearFieldModificationWatch/clrfmodw001a", "fld3", "[I", 0, NULL },
    { "nsk/jvmti/ClearFieldModificationWatch/clrfmodw001b", "fld4", "F", 0, NULL },
};

void switchWatch(JNIEnv *env, jint ind, jboolean on) {
    jvmtiError err;
    jclass cls;
    field fld = fields[ind];
    const char *msg;

    cls = env->FindClass(fld.klass);
    if (fld.fid == NULL) {
        if (fld.stat) {
            fields[ind].fid = env->GetStaticFieldID(cls, fld.name, fld.sig);
        } else {
            fields[ind].fid = env->GetFieldID(cls, fld.name, fld.sig);
        }
    }

    if (on == JNI_TRUE) {
        msg = "Set";
        err = jvmti->SetFieldModificationWatch(cls, fields[ind].fid);
    } else {
        msg = "Clear";
        err = jvmti->ClearFieldModificationWatch(cls, fields[ind].fid);
    }
    if (err == JVMTI_ERROR_MUST_POSSESS_CAPABILITY &&
            !caps.can_generate_field_modification_events) {
        /* Ok, it's expected */
    } else if (err != JVMTI_ERROR_NONE) {
        printf("(%sFieldModificationWatch#%d) unexpected error: %s (%d)\n",
               msg, ind, TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

void JNICALL FieldModification(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thd, jmethodID mid, jlocation loc, jclass field_klass, jobject obj,
        jfieldID field, char sig, jvalue new_value) {
    thrown_fid = field;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_clrfmodw001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_clrfmodw001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_clrfmodw001(JavaVM *jvm, char *options, void *reserved) {
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
Java_nsk_jvmti_ClearFieldModificationWatch_clrfmodw001_setWatch(JNIEnv *env, jclass cls, jint fld_ind) {
    switchWatch(env, fld_ind, JNI_TRUE);
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_ClearFieldModificationWatch_clrfmodw001_clearWatch(JNIEnv *env, jclass cls, jint fld_ind) {
    switchWatch(env, fld_ind, JNI_FALSE);
}

JNIEXPORT void JNICALL Java_nsk_jvmti_ClearFieldModificationWatch_clrfmodw001_touchfld0(JNIEnv *env, jobject obj) {
    env->SetIntField(obj, fields[0].fid, 2000);
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_ClearFieldModificationWatch_clrfmodw001_check(JNIEnv *env, jclass cls, jint fld_ind, jboolean flag) {
    if (caps.can_generate_field_modification_events) {
        if (flag == JNI_FALSE && thrown_fid != NULL) {
            result = STATUS_FAILED;
            printf("(Field %d) ", fld_ind);
            printf("FieldModification event without modification watch set\n");
        } else if (flag == JNI_TRUE && thrown_fid != fields[fld_ind].fid) {
            result = STATUS_FAILED;
            printf("(Field %d) thrown field ID expected: 0x%p, got: 0x%p\n",
                   fld_ind, fields[fld_ind].fid, thrown_fid);
        }
        thrown_fid = NULL;
    }
}

JNIEXPORT jint JNICALL Java_nsk_jvmti_ClearFieldModificationWatch_clrfmodw001_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
