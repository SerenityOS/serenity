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


#define PASSED 0
#define STATUS_FAILED 2
#define CLASS_SIGNATURE "Lnsk/jvmti/GetClassSignature/getclsig005;"

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getclsig005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getclsig005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getclsig005(JavaVM *jvm, char *options, void *reserved) {
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

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetClassSignature_getclsig005_check(JNIEnv *env, jclass cls) {
    jvmtiError err;
    char *sig, *generic;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> invalid class check ...\n");
    }
    err = jvmti->GetClassSignature(NULL, &sig, &generic);
    if (err != JVMTI_ERROR_INVALID_CLASS) {
        printf("Error expected: JVMTI_ERROR_INVALID_CLASS,\n");
        printf("\tactual: %s (%d)\n", TranslateError(err), err);
        result = STATUS_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> (signature_ptr) null pointer check ...\n");
    }
    err = jvmti->GetClassSignature(cls, NULL, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(signature_ptr) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    } else {
        if (printdump == JNI_TRUE) {
            printf(">>> generic = \"%s\"\n", generic);
        }
    }

    if (printdump == JNI_TRUE) {
        printf(">>> (generic_ptr) null pointer check ...\n");
    }
    err = jvmti->GetClassSignature(cls, &sig, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("(generic_ptr) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    } else {
        if (printdump == JNI_TRUE) {
            printf(">>> sig = \"%s\"\n", sig);
        }
        if (sig == NULL || strcmp(sig, CLASS_SIGNATURE) != 0) {
            printf("Wrong class sig: \"%s\", expected: \"%s\"\n",
                   CLASS_SIGNATURE, sig);
            result = STATUS_FAILED;
        }
    }

    if (printdump == JNI_TRUE) {
        printf(">>> ... done\n");
    }

    return result;
}

}
