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

#define FLDS_NUM 12 /* overall number of tested fields */

#define STATUS_FAILED 2
#define PASSED 0

/* expected field signatures are below */
static const char *fld_sig[][FLDS_NUM] = {
    { "_getfldnm005St", "static",
      "Lnsk/jvmti/GetFieldName/getfldnm005;", "NULL" },

    { "_getfldnm005b", "instance",
      "Lnsk/jvmti/GetFieldName/getfldnm005b;",
      "Lnsk/jvmti/GetFieldName/getfldnm005b<Ljava/lang/String;>;" },
    { "_getfldnm005bSt", "static",
      "Lnsk/jvmti/GetFieldName/getfldnm005b;",
      "Lnsk/jvmti/GetFieldName/getfldnm005b<Ljava/lang/String;>;" },

    { "_getfldnm005c", "instance",
      "Lnsk/jvmti/GetFieldName/getfldnm005c;",
      "Lnsk/jvmti/GetFieldName/getfldnm005c<Ljava/lang/Boolean;Ljava/lang/Integer;>;" },
    { "_getfldnm005cSt", "static",
      "Lnsk/jvmti/GetFieldName/getfldnm005c;",
      "Lnsk/jvmti/GetFieldName/getfldnm005c<Ljava/lang/Boolean;Ljava/lang/Integer;>;" },

    { "_getfldnm005e", "instance",
      "Lnsk/jvmti/GetFieldName/getfldnm005e;",
      "NULL" },
    { "_getfldnm005eSt", "static",
      "Lnsk/jvmti/GetFieldName/getfldnm005e;",
      "NULL" },

    { "_getfldnm005if", "instance",
      "Lnsk/jvmti/GetFieldName/getfldnm005if;",
      "Lnsk/jvmti/GetFieldName/getfldnm005if<Ljava/lang/Object;>;" },
    { "_getfldnm005ifSt", "static",
      "Lnsk/jvmti/GetFieldName/getfldnm005if;",
      "Lnsk/jvmti/GetFieldName/getfldnm005if<Ljava/lang/Object;>;" },

    { "_getfldnm005g", "instance",
      "Lnsk/jvmti/GetFieldName/getfldnm005g;",
      "Lnsk/jvmti/GetFieldName/getfldnm005g<Lnsk/jvmti/GetFieldName/getfldnm005f;>;" },
    { "_getfldnm005gSt", "static",
      "Lnsk/jvmti/GetFieldName/getfldnm005g;",
      "Lnsk/jvmti/GetFieldName/getfldnm005g<Lnsk/jvmti/GetFieldName/getfldnm005f;>;" },

    { "_getfldnm005gArr", "instance",
      "[Lnsk/jvmti/GetFieldName/getfldnm005g;",
      "NULL" }
};

static jvmtiEnv *jvmti = NULL;

static int checkSig(JNIEnv *jni_env, jclass testedCls,
        jfieldID testedFld, int instance, int idx) {
    int totRes = PASSED;
    char *name;
    char *sign;
    char *gen_sign;

    if (!NSK_JVMTI_VERIFY(jvmti->GetFieldName(testedCls, testedFld, &name, &sign, &gen_sign))) {
        NSK_COMPLAIN1("TEST FAILED: unable to get field name & signature for \"%s\"\n\n",
            fld_sig[idx][0]);
        return STATUS_FAILED;
    } else {
        NSK_DISPLAY1("Checking signatures for \"%s\" ...\n",
            name);

        if (strcmp(fld_sig[idx][2], sign) != 0 ||
                strcmp(fld_sig[idx][3], (gen_sign == NULL) ? "NULL" : gen_sign) != 0) {
            NSK_COMPLAIN6(
                "TEST FAILED: %s field \"%s\" has\n"
                "\tsignature: \"%s\"\n"
                "\tgeneric signature: \"%s\"\n\n"
                "\tExpected: \"%s\"\n"
                "\t\t\"%s\"\n\n",
               (instance == 0) ? "instance" : "static",
                fld_sig[idx][0],
                sign, (gen_sign == NULL) ? "NULL" : gen_sign,
                fld_sig[idx][2], fld_sig[idx][3]);
            totRes = STATUS_FAILED;
        }
        else
            NSK_DISPLAY2("CHECK PASSED: signature: \"%s\",\n\tgeneric signature: \"%s\"\n",
                sign, (gen_sign == NULL) ? "NULL" : gen_sign);

        NSK_DISPLAY0("Deallocating name & signature arrays\n");
        if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*) name)))
            totRes = STATUS_FAILED;
        if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*) sign)))
            totRes = STATUS_FAILED;
        if (gen_sign != NULL)
            if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*) gen_sign)))
                totRes = STATUS_FAILED;
    }

    return totRes;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetFieldName_getfldnm005_check(
        JNIEnv *jni, jobject obj) {
    int res = PASSED, i, instance;
    jfieldID testedFld = NULL;
    jclass objCls = jni->GetObjectClass(obj);

    for (i=0; i<FLDS_NUM; i++) {
        instance = strcmp(fld_sig[i][1], "instance");

        NSK_DISPLAY2(">>> Finding %s field: %s ...\n",
            (instance == 0) ? "instance" : "static",
             fld_sig[i][0]);
        if (instance == 0) {
            if (!NSK_JNI_VERIFY(jni, (testedFld = jni->GetFieldID(objCls, fld_sig[i][0], fld_sig[i][2])) != NULL)) {
                NSK_COMPLAIN1("TEST FAILERE: unable to get field ID for \"%s\"\n\n",
                    fld_sig[i][0]);
                res = STATUS_FAILED;
                continue;
            }
        }
        else
            if (!NSK_JNI_VERIFY(jni, (testedFld = jni->GetStaticFieldID(objCls, fld_sig[i][0], fld_sig[i][2])) != NULL)) {
                NSK_COMPLAIN1("TEST FAILERE: unable to get field ID for \"%s\"\n\n",
                    fld_sig[i][0]);
                res = STATUS_FAILED;
                continue;
            }

        NSK_DISPLAY1("\t... got fieldID: 0x%p\n", (void*) testedFld);

        if (checkSig(jni, objCls, testedFld, instance, i) == STATUS_FAILED)
            res = STATUS_FAILED;

        NSK_DISPLAY0("<<<\n");
    }

    return res;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getfldnm005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getfldnm005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getfldnm005(JavaVM *jvm, char *options, void *reserved) {
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
