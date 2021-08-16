/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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


#define PASSED 0
#define STATUS_FAILED 2

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getclstat007(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getclstat007(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getclstat007(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetClassStatus_getclstat007_check(JNIEnv *env, jclass cls,
        jint i, jclass klass, jboolean is_array) {
    jvmtiError err;
    jint status;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    err = jvmti->GetClassStatus(klass, &status);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassStatus#%d) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> %d: 0x%0x\n", i, status);
    }

    if (i == 0) {
        if ((status & JVMTI_CLASS_STATUS_VERIFIED) == 0) {
            printf("(%d) JVMTI_CLASS_STATUS_VERIFIED bit should be set\n", i);
            result = STATUS_FAILED;
        }
        if ((status & JVMTI_CLASS_STATUS_PREPARED) == 0) {
            printf("(%d) JVMTI_CLASS_STATUS_PREPARED bit should be set\n", i);
            result = STATUS_FAILED;
        }
        if ((status & JVMTI_CLASS_STATUS_INITIALIZED) == 0) {
            printf("(%d) JVMTI_CLASS_STATUS_INITIALIZED bit should be set\n", i);
            result = STATUS_FAILED;
        }
        if ((status & JVMTI_CLASS_STATUS_ERROR) != 0) {
            printf("(%d) JVMTI_CLASS_STATUS_ERROR bit should be clear\n", i);
            result = STATUS_FAILED;
        }
        if ((status & JVMTI_CLASS_STATUS_ARRAY) != 0) {
            printf("(%d) JVMTI_CLASS_STATUS_ARRAY bit should be clear\n", i);
            result = STATUS_FAILED;
        }
        if ((status & JVMTI_CLASS_STATUS_PRIMITIVE) != 0) {
            printf("(%d) JVMTI_CLASS_STATUS_PRIMITIVEbit should be clear\n", i);
            result = STATUS_FAILED;
        }
    } else if (is_array == JNI_TRUE) {
        if ((status & JVMTI_CLASS_STATUS_ARRAY) == 0) {
            printf("(%d) JVMTI_CLASS_STATUS_ARRAY bit should be set\n", i);
            result = STATUS_FAILED;
        }
        if ((status & (~JVMTI_CLASS_STATUS_ARRAY)) != 0) {
            printf("(%d) not JVMTI_CLASS_STATUS_ARRAY bits should be clear: 0x%0x\n",
                i, status);
            result = STATUS_FAILED;
        }
    } else {
        if ((status & JVMTI_CLASS_STATUS_PRIMITIVE) == 0) {
            printf("(%d) JVMTI_CLASS_STATUS_PRIMITIVE bit should be set\n", i);
            result = STATUS_FAILED;
        }
        if ((status & (~JVMTI_CLASS_STATUS_PRIMITIVE)) != 0) {
            printf("(%d) not JVMTI_CLASS_STATUS_PRIMITIVE bits should be clear: 0x%0x\n",
                i, status);
            result = STATUS_FAILED;
        }
    }
}

JNIEXPORT int JNICALL
Java_nsk_jvmti_GetClassStatus_getclstat007_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
