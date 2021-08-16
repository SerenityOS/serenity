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
#include "jni_tools.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define STATUS_PASSED 0
#define STATUS_FAILED 2

static jvmtiEnv *jvmti = NULL;
static jint result = STATUS_PASSED;
static jboolean printdump = JNI_FALSE;

void print_LocalVariableEntry(jvmtiLocalVariableEntry *lvt_elem) {
  printf("\n Var name: %s, slot: %d", lvt_elem->name, lvt_elem->slot);
  printf(", start_bci: %" LL "d", lvt_elem->start_location);
  printf(", end_bci: %" LL "d",   lvt_elem->start_location + lvt_elem->length);
  printf(", signature: %s\n", lvt_elem->signature);
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getlocal004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getlocal004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getlocal004(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;
    static jvmtiCapabilities caps;

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

    if (!caps.can_access_local_variables) {
        printf("Warning: Access to local variables is not implemented\n");
        return JNI_ERR;
    }
    if (!caps.can_access_local_variables) {
        printf("Warning: Access to local variables is not implemented\n");
        return JNI_ERR;
    }
    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_GetLocalVariable_getlocal004_getMeth(JNIEnv *env, jclass cls) {
    jmethodID mid = NULL;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    mid = env->GetStaticMethodID(cls, "staticMeth", "(I)I");
    if (mid == NULL) {
        printf("Cannot find Method ID for staticMeth\n");
        result = STATUS_FAILED;
        return;
    }
    fflush(stdout);
}

void checkErrorCodeIn(jvmtiError err, jint slot) {
    if (err != JVMTI_ERROR_NONE) {
        printf(" FAILURE: JVMTI_ERROR_NONE is expected, slot: %d\n\n", slot);
        result = STATUS_FAILED;
    } else {
        printf(" success: JVMTI_ERROR_NONE as expected, slot: %d\n\n", slot);
    }
}

void checkErrorCodeOut(jvmtiError err, jint slot) {
    if (err != JVMTI_ERROR_INVALID_SLOT) {
        printf(" FAILURE: JVMTI_ERROR_INVALID_SLOT is expected, slot: %d\n\n", slot);
        result = STATUS_FAILED;
    } else {
        printf(" success: JVMTI_ERROR_INVALID_SLOT as expected, slot: %d\n\n", slot);
    }
}

#define CHECK_ERROR_CODE(scope_no, err, slot) \
    if (scope_no == 1) {                      \
        checkErrorCodeOut(err, slot);         \
    } else {                                  \
        checkErrorCodeIn(err, slot);          \
    }

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_GetLocalVariable_getlocal004_checkLoc(JNIEnv *env,
        jclass cls, jthread thr, jint scope_no) {
    jvmtiError err       = JVMTI_ERROR_NONE;
    jint       slot      = 0;
    jint       locInt    = 0;
    jlong      locLong   = 0L;
    jdouble    locDouble = 0.0f;

    if (jvmti == NULL) {
        return;
    }
    printf("\n ----------------- checkLoc: %d -----------------\n\n", scope_no);

    /* Check for slots which has to be available in general */
    for (slot = 3; slot < 5; slot++) {
        err = jvmti->GetLocalInt(thr, 1, slot, &locInt);
        printf(" GetLocalInt: %s (%d)\n", TranslateError(err), err);
        CHECK_ERROR_CODE(scope_no, err, slot);

        if (err == JVMTI_ERROR_NONE) {
            printf(" slot%d: %d\n", slot, locInt);
        }

        err = jvmti->GetLocalLong(thr, 1, slot, &locLong);
        printf(" GetLocalLong: %s (%d)\n", TranslateError(err), err);
        CHECK_ERROR_CODE(scope_no, err, slot);

        err = jvmti->GetLocalDouble(thr, 1, slot, &locDouble);
        printf(" GetLocalDouble: %s (%d)\n", TranslateError(err), err);
        CHECK_ERROR_CODE(scope_no, err, slot);
    }

    /* Slot 5 is special: it's not for 64 bit values! */
    slot = 5; {
        err = jvmti->GetLocalInt(thr, 1, slot, &locInt);
        printf(" GetLocalInt: %s (%d)\n", TranslateError(err), err);
        CHECK_ERROR_CODE(scope_no, err, slot);

        if (err == JVMTI_ERROR_NONE) {
            printf(" slot%d: %d\n", slot, locInt);
        }

        err = jvmti->GetLocalLong(thr, 1, slot, &locLong);
        printf(" GetLocalLong: %s (%d)\n", TranslateError(err), err);
        checkErrorCodeOut(err, slot);

        err = jvmti->GetLocalDouble(thr, 1, slot, &locDouble);
        printf(" GetLocalDouble: %s (%d)\n", TranslateError(err), err);
        checkErrorCodeOut(err, slot);
    }

    /* Check for slots which has to be unavailable in general */
    for (slot = 6; slot < 8; slot++) {
        err = jvmti->GetLocalInt(thr, 1, slot, &locInt);
        printf(" GetLocalInt: %s (%d)\n", TranslateError(err), err);
        checkErrorCodeOut(err, slot);

        if (err == JVMTI_ERROR_NONE) {
            printf(" slot%d: %d\n", slot, locInt);
        }

        err = jvmti->GetLocalLong(thr, 1, slot, &locLong);
        printf(" GetLocalLong: %s (%d)\n", TranslateError(err), err);
        checkErrorCodeOut(err, slot);

        err = jvmti->GetLocalDouble(thr, 1, slot, &locDouble);
        printf(" GetLocalDouble: %s (%d)\n", TranslateError(err), err);
        checkErrorCodeOut(err, slot);
    }

    fflush(stdout);
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_GetLocalVariable_getlocal004_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
