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


#define PASSED 0
#define STATUS_FAILED 2

typedef struct {
    jfieldID fid;
    const char *f_name;
    const char *f_sig;
    jboolean is_static;
    jint expected;
    jint count;
} watch_info;

static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static int missesCount = 0;
static watch_info watches[] = {
    { NULL, "staticBoolean", "Z", JNI_TRUE, 0, 0 },
    { NULL, "staticByte", "B", JNI_TRUE, 0, 0 },
    { NULL, "staticShort", "S", JNI_TRUE, 0, 0 },
    { NULL, "staticInt", "I", JNI_TRUE, 0, 0 },
    { NULL, "staticLong", "J", JNI_TRUE, 0, 0 },
    { NULL, "staticFloat", "F", JNI_TRUE, 0, 0 },
    { NULL, "staticDouble", "D", JNI_TRUE, 0, 0 },
    { NULL, "staticChar", "C", JNI_TRUE, 0, 0 },
    { NULL, "staticObject", "Ljava/lang/Object;", JNI_TRUE, 0, 0 },
    { NULL, "staticArrInt", "[I", JNI_TRUE, 0, 0 },

    { NULL, "instanceBoolean", "Z", JNI_FALSE, 0, 0 },
    { NULL, "instanceByte", "B", JNI_FALSE, 0, 0 },
    { NULL, "instanceShort", "S", JNI_FALSE, 0, 0 },
    { NULL, "instanceInt", "I", JNI_FALSE, 0, 0 },
    { NULL, "instanceLong", "J", JNI_FALSE, 0, 0 },
    { NULL, "instanceFloat", "F", JNI_FALSE, 0, 0 },
    { NULL, "instanceDouble", "D", JNI_FALSE, 0, 0 },
    { NULL, "instanceChar", "C", JNI_FALSE, 0, 0 },
    { NULL, "instanceObject", "Ljava/lang/Object;", JNI_FALSE, 0, 0 },
    { NULL, "instanceArrInt", "[I", JNI_FALSE, 0, 0 }
};


void JNICALL FieldModification(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method, jlocation location, jclass field_klass, jobject obj,
        jfieldID field, char sig, jvalue new_value) {
    size_t i;

    for (i = 0; i < sizeof(watches)/sizeof(watch_info); i++) {
        if (field == watches[i].fid) {
            watches[i].count++;
            return;
        }
    }
    missesCount++;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setfmodw006(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setfmodw006(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setfmodw006(JavaVM *jvm, char *options, void *reserved) {
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
        printf("Wrong result of a valid call to GetEnv!\n");
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
Java_nsk_jvmti_SetFieldModificationWatch_setfmodw006_getReady(JNIEnv *env,
        jclass cls, jint n) {
    jvmtiError err;
    size_t i;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (!caps.can_generate_field_modification_events) {
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> setting field modification watches ...\n");
    }
    for (i = 0; i < sizeof(watches)/sizeof(watch_info); i++) {
        if (watches[i].is_static == JNI_TRUE) {
            watches[i].fid = env->GetStaticFieldID(cls, watches[i].f_name, watches[i].f_sig);
        } else {
            watches[i].fid = env->GetFieldID(cls, watches[i].f_name, watches[i].f_sig);
        }
        err = jvmti->SetFieldModificationWatch(cls, watches[i].fid);
        if (err == JVMTI_ERROR_NONE) {
            watches[i].expected = n;
        } else {
            printf("(SetFieldModificationWatch#%" PRIuPTR ") unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
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

JNIEXPORT jint JNICALL
Java_nsk_jvmti_SetFieldModificationWatch_setfmodw006_check(JNIEnv *env,
        jclass cls, jboolean flag) {
    size_t i;
    jint expected;

    for (i = 0; i < sizeof(watches)/sizeof(watch_info); i++) {
        if (printdump == JNI_TRUE && watches[i].count > 0) {
            printf(">>> field %s modifications: %d\n",
                watches[i].f_name, watches[i].count);
        }
        expected = (flag == JNI_TRUE) ? watches[i].expected : 0;
        if (watches[i].count != expected) {
            printf("(%s) wrong number of field modification events: %d,",
                watches[i].f_name, watches[i].count);
            printf(" expected: %d\n", expected);
            result = STATUS_FAILED;
        }
        watches[i].count = 0;
    }

    if (missesCount != 0) {
        printf("%d unexpected field modification catches\n", missesCount);
        missesCount = 0;
        result = STATUS_FAILED;
    }
    return result;
}

}
