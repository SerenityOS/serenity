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
    const char sig;
    const jboolean stat;
    const jint val;
    jfieldID fid;
} field;

static jvmtiEnv *jvmti;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jfieldID actual_fid = NULL;
static char actual_sig = '\0';
static jint actual_val = 0;
static field flds[] = {
    { "fld0", 'I', JNI_TRUE, 42, NULL },
    { "fld1", 'I', JNI_TRUE, 43, NULL },
    { "fld2", 'I', JNI_FALSE, 44, NULL },
    { "fld3", 'I', JNI_FALSE, 45, NULL }
};

void JNICALL FieldModification(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method, jlocation location, jclass field_klass, jobject obj,
        jfieldID field, char sig, jvalue new_value) {
    if (printdump == JNI_TRUE) {
        printf(">>> FieldModification, field: 0x%p", field);
        printf(", signature: '%c', new value: %d\n", sig, new_value.i);
    }
    actual_fid = field;
    actual_sig = sig;
    actual_val = new_value.i;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setfmodw003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setfmodw003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setfmodw003(JavaVM *jvm, char *options, void *reserved) {
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
    } else {
        printf("Warning: FieldModification watch is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_SetFieldModificationWatch_setfmodw003_getReady(JNIEnv *env,
        jclass cls) {
    jvmtiError err;
    size_t i;

    if (!caps.can_generate_field_modification_events) {
        return;
    }

    for (i = 0; i < sizeof(flds) / sizeof(field); i++) {
        if (flds[i].stat == JNI_TRUE) {
            flds[i].fid = env->GetStaticFieldID(cls, flds[i].name, "I");
        } else {
            flds[i].fid = env->GetFieldID(cls, flds[i].name, "I");
        }
        if (flds[i].fid == NULL) {
            printf("Unable to set field modif. watch on fld%" PRIuPTR ", fieldID=0", i);
        } else {
            if (printdump == JNI_TRUE) {
                printf(">>> setting modification watch on fld%" PRIuPTR, i);
                printf(", fieldID=0x%p\n", flds[i].fid);
            }
            err = jvmti->SetFieldModificationWatch(cls, flds[i].fid);
            if (err != JVMTI_ERROR_NONE) {
                printf("(SetFieldModificationWatch#%" PRIuPTR ") unexpected error:", i);
                printf(" %s (%d)\n", TranslateError(err), err);
                result = STATUS_FAILED;
            }
        }
    }
    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_FIELD_MODIFICATION, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("(SetEventNotificationMode) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_SetFieldModificationWatch_setfmodw003_check(JNIEnv *env,
        jclass cls, jint ind) {
    if (!caps.can_generate_field_modification_events) {
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> checking on fld%d\n", ind);
    }
    if (actual_fid != flds[ind].fid) {
        result = STATUS_FAILED;
        printf("Field %s: thrown field ID expected=0x%p, actual=0x%p\n",
               flds[ind].name, flds[ind].fid, actual_fid);
    }
    if (actual_sig != flds[ind].sig) {
        result = STATUS_FAILED;
        printf("Field %s: thrown sygnature type expected='%c', actual='%c'\n",
               flds[ind].name, flds[ind].sig, actual_sig);
    }
    if (actual_val != flds[ind].val) {
        result = STATUS_FAILED;
        printf("Field %s: thrown new value expected=%d, actual=%d\n",
               flds[ind].name, flds[ind].val, actual_val);
    }
    actual_fid = NULL;
    actual_sig = '\0';
    actual_val = 0;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_SetFieldModificationWatch_setfmodw003_getRes(JNIEnv *env,
        jclass cls) {
    return result;
}

}
