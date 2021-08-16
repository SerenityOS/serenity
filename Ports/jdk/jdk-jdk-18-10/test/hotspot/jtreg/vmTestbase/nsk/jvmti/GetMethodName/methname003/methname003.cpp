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

/* number of tested methods in particular class */
#define METH_NUM 2

/* overall number of all tested methods */
#define TOT_NUM 5

#define STATUS_FAILED 2
#define PASSED 0

/* expected class signatures are below */
static const char *meth_sig[][METH_NUM][TOT_NUM] = {
    { { "methname003bMeth", "instance",
        "(Lnsk/jvmti/GetMethodName/methname003b;)Lnsk/jvmti/GetMethodName/methname003b;",
        "<L:Ljava/lang/String;>(Lnsk/jvmti/GetMethodName/methname003b<TL;>;)Lnsk/jvmti/GetMethodName/methname003b<Ljava/lang/String;>;" },
     { "methname003bMethSt", "static",
       "(Lnsk/jvmti/GetMethodName/methname003b;)Lnsk/jvmti/GetMethodName/methname003b;",
       "<T:Ljava/lang/String;>(Lnsk/jvmti/GetMethodName/methname003b<TT;>;)Lnsk/jvmti/GetMethodName/methname003b<Ljava/lang/String;>;" } },

    { { "methname003cMeth", "instance",
        "(Ljava/lang/Class;)Ljava/lang/Object;",
        "<U:Ljava/lang/Object;>(Ljava/lang/Class<TU;>;)TU;" },
     { "methname003cMethSt", "static",
       "(Ljava/lang/Class;)Ljava/lang/Object;",
       "<U:Ljava/lang/Object;>(Ljava/lang/Class<TU;>;)TU;" } },

    { { "methname003eMeth", "instance",
        "(Lnsk/jvmti/GetMethodName/methname003e;)V",
        "NULL" },
     { "methname003eMethSt", "static",
       "(Lnsk/jvmti/GetMethodName/methname003e;)V",
       "NULL" } },

    { { "methname003ifMeth", "instance",
        "()I",
        "NULL" },
     { "methname003ifMeth2", "instance",
       "(Ljava/lang/Object;)I",
       "<T:Ljava/lang/Object;>(TT;)I" } },

    { { "methname003gMeth", "instance",
        "(Ljava/lang/Byte;Ljava/lang/Double;[Ljava/lang/Class;)V",
        "<A:Ljava/lang/Byte;B:Ljava/lang/Double;>(TA;TB;[Ljava/lang/Class<*>;)V" },
     { "methname003gMethSt", "static",
       "(Ljava/lang/Byte;Ljava/lang/Double;)V",
       "<A:Ljava/lang/Byte;B:Ljava/lang/Double;>(TA;TB;)V" } }
};

static jvmtiEnv *jvmti = NULL;

static int checkSig(JNIEnv *jni_env, jmethodID testedMeth,
        int instance, int clsIdx, int methIdx) {
    int totRes = PASSED;
    char *name;
    char *sign;
    char *gen_sign;

    if (!NSK_JVMTI_VERIFY(jvmti->GetMethodName(testedMeth, &name, &sign, &gen_sign))) {
        NSK_COMPLAIN1("TEST FAILED: unable to get class signature for \"%s\"\n\n",
            meth_sig[clsIdx][methIdx][0]);
        return STATUS_FAILED;
    } else {
        NSK_DISPLAY1("Checking signatures for \"%s\" ...\n",
            meth_sig[clsIdx][methIdx][0]);

        if (strcmp(meth_sig[clsIdx][methIdx][2], sign) != 0 ||
                strcmp(meth_sig[clsIdx][methIdx][3], (gen_sign == NULL) ? "NULL" : gen_sign) != 0) {
            NSK_COMPLAIN5("TEST FAILED: class: \"%s\" \
has\n\tsignature: \"%s\"\n\tgeneric signature: \"%s\"\n\n\tExpected: \"%s\"\n\t\t\"%s\"\n\n",
                meth_sig[clsIdx][methIdx][0],
                sign, (gen_sign == NULL) ? "NULL" : gen_sign,
                meth_sig[clsIdx][methIdx][2], meth_sig[clsIdx][methIdx][3]);
            totRes = STATUS_FAILED;
        }
        else
            NSK_DISPLAY2("CHECK PASSED: signature: \"%s\",\n\tgeneric signature: \"%s\"\n",
                sign, (gen_sign == NULL) ? "NULL" : gen_sign);

        NSK_DISPLAY0("Deallocating name & signature arrays\n");
        if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*) name))) {
            totRes = STATUS_FAILED;
        }
        if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*) sign))) {
            totRes = STATUS_FAILED;
        }
        if (gen_sign != NULL)
            if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*) gen_sign))) {
                totRes = STATUS_FAILED;
            }
    }

    return totRes;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetMethodName_methname003_check(
        JNIEnv *jni, jobject obj, jobject testedObj, jint clsIdx) {
    int res = PASSED, i, instance;
    jmethodID testedMeth = NULL;
    jclass objCls = jni->GetObjectClass(testedObj);

    for (i=0; i<METH_NUM; i++) {
        instance = strcmp(meth_sig[clsIdx][i][1], "instance");

        NSK_DISPLAY2(">>> Finding %s method: %s ...\n",
            (instance == 0) ? "instance" : "static",
             meth_sig[clsIdx][i][0]);
        if (instance == 0) {
            if (!NSK_JNI_VERIFY(jni, (testedMeth = jni->GetMethodID(objCls, meth_sig[clsIdx][i][0], meth_sig[clsIdx][i][2])) != NULL)) {
                NSK_COMPLAIN2("TEST FAILERE: unable to get method ID for \"%s\" \"%s\"\n\n",
                    meth_sig[clsIdx][i][0], meth_sig[clsIdx][i][2]);
                res = STATUS_FAILED;
                continue;
            }
        }
        else
            if (!NSK_JNI_VERIFY(jni, (testedMeth = jni->GetStaticMethodID(objCls, meth_sig[clsIdx][i][0], meth_sig[clsIdx][i][2])) != NULL)) {
                NSK_COMPLAIN2("TEST FAILERE: unable to get method ID for \"%s\" \"%s\"\n\n",
                    meth_sig[clsIdx][i][0], meth_sig[clsIdx][i][2]);
                res = STATUS_FAILED;
                continue;
            }

        NSK_DISPLAY1("\t... got methodID: 0x%p\n", (void*) testedMeth);

        if (checkSig(jni, testedMeth, instance, clsIdx, i) == STATUS_FAILED)
            res = STATUS_FAILED;

        NSK_DISPLAY0("<<<\n");
    }

    return res;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_methname003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_methname003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_methname003(JavaVM *jvm, char *options, void *reserved) {
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
