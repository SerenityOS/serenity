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
#include <jvmti.h>
#include "agent_common.h"

#include "nsk_tools.h"
#include "jni_tools.h"
#include "JVMTITools.h"
#include "jvmti_tools.h"

extern "C" {

#define CLS_NUM 5 /* overall number of tested classes */

#define STATUS_FAILED 2
#define PASSED 0

/* expected class signatures are below */
static const char *class_sig[][CLS_NUM] = {
    { "getclsig006", "Lnsk/jvmti/GetClassSignature/getclsig006;", "NULL" },
    { "getclsig006b", "Lnsk/jvmti/GetClassSignature/getclsig006b;",
      "<L:Ljava/lang/String;>Ljava/lang/Object;" },
    { "getclsig006c", "Lnsk/jvmti/GetClassSignature/getclsig006c;",
      "<A:Ljava/lang/Object;B:Ljava/lang/Integer;>Ljava/lang/Object;" },
    { "getclsig006if", "Lnsk/jvmti/GetClassSignature/getclsig006if;",
      "<I:Ljava/lang/Object;>Ljava/lang/Object;" },
    { "getclsig006g", "Lnsk/jvmti/GetClassSignature/getclsig006g;",
      "<E:Lnsk/jvmti/GetClassSignature/getclsig006e;:Lnsk/jvmti/GetClassSignature/getclsig006if;>Ljava/lang/Object;" }
};

static jvmtiEnv *jvmti = NULL;

static int checkSig(JNIEnv *jni_env, jclass testedCls, int idx) {
    int totRes = PASSED;
    char *sign;
    char *gen_sign;

    if (!NSK_JVMTI_VERIFY(jvmti->GetClassSignature(testedCls, &sign, &gen_sign))) {
        NSK_COMPLAIN1("TEST FAILED: unable to get class signature for \"%s\"\n\n",
            class_sig[idx][0]);
        return STATUS_FAILED;
    } else {
        NSK_DISPLAY1(">>> Checking signatures for \"%s\" ...\n",
            class_sig[idx][0]);

        if (strcmp(class_sig[idx][1], sign) != 0 ||
                strcmp(class_sig[idx][2], (gen_sign == NULL) ? "NULL" : gen_sign) != 0) {
            NSK_COMPLAIN5(
                "TEST FAILED: class: \"%s\" has\n"
                "\tsignature: \"%s\"\n"
                "\tgeneric signature: \"%s\"\n\n"
                "\tExpected: \"%s\"\n"
                "\t\"%s\"\n\n",
                class_sig[idx][0],
                sign, (gen_sign == NULL) ? "NULL" : gen_sign,
                class_sig[idx][1], class_sig[idx][2]);
            totRes = STATUS_FAILED;
        }
        else
            NSK_DISPLAY2("CHECK PASSED: signature: \"%s\",\n\tgeneric signature: \"%s\"\n",
                sign, (gen_sign == NULL) ? "NULL" : gen_sign);

        NSK_DISPLAY0("Deallocating the signature array\n");
        if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*) sign))) {
            totRes = STATUS_FAILED;
        }
        if (gen_sign != NULL)
            if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*) gen_sign))) {
                totRes = STATUS_FAILED;
            }

        NSK_DISPLAY0("<<<\n");
    }

    return totRes;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetClassSignature_getclsig006_check(
        JNIEnv *jni, jobject obj) {
    int res = PASSED, i;
    jclass testedCls;

    for (i=0; i<CLS_NUM; i++) {
        if (!NSK_JNI_VERIFY(jni, (testedCls = jni->FindClass(class_sig[i][1])) != NULL)) {
            NSK_COMPLAIN1("TEST FAILURE: unable to find class \"%s\"\n\n",
                class_sig[i][0]);
            res = STATUS_FAILED;
            continue;
        }

        if (checkSig(jni, testedCls, i) == STATUS_FAILED)
            res = STATUS_FAILED;
    }

    return res;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getclsig006(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getclsig006(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getclsig006(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    return JNI_OK;
}

}
