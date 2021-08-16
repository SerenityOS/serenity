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
#include "JVMTITools.h"
#include "jvmti_tools.h"

extern "C" {

#define METH_NUM 3 /* overall number of methods */

#define STATUS_FAILED 2
#define PASSED 0

typedef struct {      /* local variable info */
    const char *v_name;     /* a variable name */
    const char *v_sign;     /* JVM type signature */
    const char *v_gen_sign; /* JVM type generic signature */
} localVar;

/* expected local variable info are below */
static localVar constr_lv[] = { /* constructor's local variables */
    { "this", "Lnsk/jvmti/GetLocalVariableTable/localtab005a;", "NULL" },
    { "constr_b", "Lnsk/jvmti/GetLocalVariableTable/localtab005b;",
      "Lnsk/jvmti/GetLocalVariableTable/localtab005b<Ljava/lang/String;>;" },
    { "constr_i", "I", "NULL" },
    { "constr_c", "Lnsk/jvmti/GetLocalVariableTable/localtab005c;",
      "Lnsk/jvmti/GetLocalVariableTable/localtab005c<Ljava/lang/Boolean;Ljava/lang/Integer;>;" },
    { "constr_f", "F", "NULL" },
    { "constr_ch", "C", "NULL" },
    { "constr_if", "Lnsk/jvmti/GetLocalVariableTable/localtab005if;",
      "Lnsk/jvmti/GetLocalVariableTable/localtab005if<Ljava/lang/Object;>;" }
};

static localVar insMeth_lv[] = { /* finMethod()'s local variables */
    { "this", "Lnsk/jvmti/GetLocalVariableTable/localtab005a;", "NULL" },
    { "ins_c", "C", "NULL" },
    { "ins_i", "J", "NULL" },
    { "ltab005d", "Lnsk/jvmti/GetLocalVariableTable/localtab005d;",
      "Lnsk/jvmti/GetLocalVariableTable/localtab005d<Ljava/lang/Object;>;" },
    { "ins_k", "J", "NULL" },
    { "ins_l", "J", "NULL" },
    { "ins_g", "Lnsk/jvmti/GetLocalVariableTable/localtab005g;",
      "Lnsk/jvmti/GetLocalVariableTable/localtab005g<Lnsk/jvmti/GetLocalVariableTable/localtab005f;>;" }
};

static localVar statMeth_lv[] = { /* statMethod()'s local variables */
    { "stat_x", "I", "NULL" },
    { "stat_y", "I", "NULL" },
    { "stat_z", "I", "NULL" },
    { "stat_j", "D", "NULL" },
    { "stat_d", "Lnsk/jvmti/GetLocalVariableTable/localtab005d;",
      "Lnsk/jvmti/GetLocalVariableTable/localtab005d<Ljava/lang/Byte;>;" }
};

typedef struct {    /* local variables of a method */
    int inst;       /* type of a method: 0- static; 1- instance */
    char *m_name;   /* a method name */
    char *m_sign;   /* JVM signature of a method */
    int vcount;     /* overall number of local variables */
    localVar *vars;
    jmethodID mid;  /* JNI's method ID */
} methodInfo;

/* list of tested methods */
static methodInfo methInfo[] = {
    { 1, (char*) "<init>", (char*) "()V", 7, constr_lv, NULL },
    { 1, (char*) "insMethod", (char*) "(CJLnsk/jvmti/GetLocalVariableTable/localtab005d;J)V", 7, insMeth_lv, NULL },
    { 0, (char*) "statMethod", (char*) "(III)D", 5, statMeth_lv, NULL }
};

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;

static int checkAttr(JNIEnv *jni_env, jclass testedCls) {
    int i, j, k;
    int totRes = PASSED;
    jint count = -1;
    jvmtiLocalVariableEntry *lv_table;

    for (i=0; i<METH_NUM; i++) {
/* get the JNI method ID for a method with name m_name and
   signature m_sign */
        if (methInfo[i].inst) /* an instance method */
            methInfo[i].mid = jni_env->GetMethodID(testedCls, methInfo[i].m_name, methInfo[i].m_sign);
        else                   /* a static method */
            methInfo[i].mid = jni_env->GetStaticMethodID(testedCls, methInfo[i].m_name, methInfo[i].m_sign);
        if (methInfo[i].mid == NULL) {
            NSK_COMPLAIN3("TEST FAILURE: unable to get the method ID for the %s method \"%s\", signature \"%s\"\n\n",
                methInfo[i].inst ? "instance" : "static",
                methInfo[i].m_name, methInfo[i].m_sign);
            return STATUS_FAILED;
        }

/* get the LocalVariableTable attribute */
        if (!NSK_JVMTI_VERIFY(jvmti->GetLocalVariableTable(methInfo[i].mid, &count, &lv_table))) {
            NSK_COMPLAIN3("TEST FAILED: unable to get local variable table\n\tfor the %s method \"%s\", signature \"%s\"\n\n",
                methInfo[i].inst ? "instance" : "static",
                methInfo[i].m_name, methInfo[i].m_sign);
            return STATUS_FAILED;
        } else {
            if (count != methInfo[i].vcount) {
                totRes = STATUS_FAILED;
                NSK_COMPLAIN5(
                    "TEST FAILED: %s method \"%s\", signature \"%s\" : "
                    "found %d vars in the LocalVariableTable, expected %d\n"
                    "\tHere are the found vars:\n",
                    methInfo[i].inst ? "instance" : "static",
                    methInfo[i].m_name, methInfo[i].m_sign,
                    count, methInfo[i].vcount);
                for (j=0; j<count; j++)
                    NSK_COMPLAIN4("\t%d) name: \"%s\"\n\tsignature: \"%s\"\n\tgeneric signature: \"%s\"\n",
                        j+1, lv_table[j].name,
                        lv_table[j].signature,
                       (lv_table[j].generic_signature == NULL) ? "NULL" : lv_table[j].generic_signature);
                NSK_COMPLAIN0("\n");

                continue;
            }
            else {
                NSK_DISPLAY4(
                    ">>> Checking vars in the LocalVariableTable of the %s method \"%s\","
                    "signature \"%s\" ...\n"
                    "\t%d local vars as expected\n",
                    methInfo[i].inst ? "instance" : "static",
                    methInfo[i].m_name, methInfo[i].m_sign, count);
            }

            for (j=0; j<count; j++) {
                for (k=0; k<count; k++) {
                    if (strcmp(lv_table[j].name, methInfo[i].vars[k].v_name) == 0) {
                        if ((strcmp(lv_table[j].signature, methInfo[i].vars[k].v_sign) != 0) ||
                            (strcmp((lv_table[j].generic_signature == NULL) ? "NULL" : lv_table[j].generic_signature,
                                methInfo[i].vars[k].v_gen_sign) != 0)) {
                            NSK_COMPLAIN8(
                                "TEST FAILED: %s method: \"%s\" \"%s\":\n"
                                "\tvar \"%s\" has signature \"%s\",\n"
                                "\tgeneric signature \"%s\"\n\n"
                                "\tExpected: \"%s\"\n\t\t\"%s\"\n\n",
                                methInfo[i].inst ? "instance" : "static",
                                methInfo[i].m_name, methInfo[i].m_sign,
                                lv_table[j].name, lv_table[j].signature,
                               (lv_table[j].generic_signature == NULL) ? "NULL" : lv_table[j].generic_signature,
                                methInfo[i].vars[k].v_sign,
                               (methInfo[i].vars[k].v_gen_sign == NULL) ? "NULL" : methInfo[i].vars[k].v_gen_sign);
                            totRes = STATUS_FAILED;
                            break;
                        }
                        else
                            NSK_DISPLAY3("CHECK PASSED: var: \"%s\",\n\tsignature: \"%s\",\n\tgeneric signature: \"%s\"\n",
                                lv_table[j].name,
                                lv_table[j].signature,
                               (lv_table[j].generic_signature == NULL) ? "NULL" : lv_table[j].generic_signature);
                    }
                }
            }
            NSK_DISPLAY0("Deallocating the local variable table entries\n");
            if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*) lv_table))) {
                totRes = STATUS_FAILED;
            }

            NSK_DISPLAY0("<<<\n");
        }
    }

    return totRes;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_GetLocalVariableTable_localtab005_check(
        JNIEnv *env, jobject obj, jobject testedObj) {
    jclass testedCls = env->GetObjectClass(testedObj);

    if (!caps.can_access_local_variables)
        return PASSED;

    return checkAttr(env, testedCls);
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_localtab005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_localtab005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_localtab005(JavaVM *jvm, char *options, void *reserved) {
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

    /* add capability to access local variables */
    memset(&caps, 0, sizeof(jvmtiCapabilities));
    caps.can_access_local_variables = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    if (!caps.can_access_local_variables)
        NSK_DISPLAY0("Warning: access to local variables is not implemented\n");

    return JNI_OK;
}

}
