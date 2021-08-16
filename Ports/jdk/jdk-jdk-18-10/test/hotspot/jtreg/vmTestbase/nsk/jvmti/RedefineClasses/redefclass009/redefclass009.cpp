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
#include "JVMTITools.h"

extern "C" {


#define METH_NUM 4 /* overall number of methods */

#define STATUS_FAILED 2
#define PASSED 0

typedef struct {   /* local variable info */
    char *v_name;  /* a variable name */
    char *v_sign;  /* JVM type signature */
} localVar;

/* local variables of redefined methods */
static localVar constr_lv[] = { /* constructor's local variables */
    { (char*) "this", (char*) "Lnsk/jvmti/RedefineClasses/redefclass009r;" },
    { (char*) "constr_i", (char*) "I" },
    { (char*) "constr_l", (char*) "J" },
    { (char*) "constr_d", (char*) "D" },
    { (char*) "constr_f", (char*) "F" },
    { (char*) "constr_c", (char*) "C" }
};
static localVar checkIt_lv[] = { /* checkIt()'s local variables */
    { (char*) "this", (char*) "Lnsk/jvmti/RedefineClasses/redefclass009r;" },
    { (char*) "out", (char*) "Ljava/io/PrintStream;" },
    { (char*) "DEBUG_MODE", (char*) "Z" }
};
static localVar finMeth_lv[] = { /* finMethod()'s local variables */
    { (char*) "this", (char*) "Lnsk/jvmti/RedefineClasses/redefclass009r;" },
    { (char*) "fin_c", (char*) "C" },
    { (char*) "fin_i", (char*) "J" },
    { (char*) "fin_j", (char*) "I" },
    { (char*) "fin_k", (char*) "J" },
    { (char*) "fin_l", (char*) "J" },
    { (char*) "fin_f", (char*) "F" }
};
static localVar statMeth_lv[] = { /* statMethod()'s local variables */
    { (char*) "stat_x", (char*) "I" },
    { (char*) "stat_y", (char*) "I" },
    { (char*) "stat_z", (char*) "I" },
    { (char*) "stat_j", (char*) "D" },
    { (char*) "stat_i", (char*) "I" }
};

typedef struct {    /* local variables of a method */
    int inst;       /* type of a method: 0- static; 1- instance */
    char *m_name;   /* a method name */
    char *m_sign;   /* JVM signature of a method */
    int vcount;     /* overall number of local variables */
    localVar *vars;
    jmethodID mid;  /* JNI's method ID */
} methInfo;

/* list of original methods with NULL pointers to localVar */
static methInfo origMethInfo[] = {
    { 1, (char*) "<init>", (char*) "()V", 1, NULL, NULL },
    { 1, (char*) "checkIt", (char*) "(Ljava/io/PrintStream;Z)I", 4, NULL, NULL },
    { 1, (char*) "finMethod", (char*) "(CJIJ)V", 5, NULL, NULL },
    { 0, (char*) "statMethod", (char*) "(III)D", 3, NULL, NULL }
};

/* list of redefined methods */
static methInfo redefMethInfo[] = {
    { 1, (char*) "<init>", (char*) "()V", 6, constr_lv, NULL },
    { 1, (char*) "checkIt", (char*) "(Ljava/io/PrintStream;Z)I", 3, checkIt_lv, NULL },
    { 1, (char*) "finMethod", (char*) "(CJIJ)V", 7, finMeth_lv, NULL },
    { 0, (char*) "statMethod", (char*) "(III)D", 5, statMeth_lv, NULL }
};

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_redefclass009(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_redefclass009(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_redefclass009(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *vm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    res = vm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK) {
        printf("%s: Failed to call GetEnv: error=%d\n", __FILE__, res);
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

    if (!caps.can_redefine_classes) {
        printf("Warning: RedefineClasses is not implemented\n");
    }

    if (!caps.can_access_local_variables) {
        printf("Warning: Access to local variables is not implemented\n");
    }

    return JNI_OK;
}

int checkAttr(JNIEnv *env, jclass redefCls, methInfo methodsInfo[],
        jint vrb, int full) {
    jvmtiError err;
    int i, j, k;
    int totRes = PASSED;
    jint count = -1;
    jvmtiLocalVariableEntry *lv_table;

    if (!caps.can_access_local_variables) {
        return PASSED;
    }

    for (i=0; i<METH_NUM; i++) {
      /* get the JNI method ID for a method with name m_name and
         signature m_sign */
        if (methodsInfo[i].inst) { /* an instance method */
            methodsInfo[i].mid = env->GetMethodID(redefCls,
                methodsInfo[i].m_name, methodsInfo[i].m_sign);
        } else {                    /* a static method */
            methodsInfo[i].mid = env->GetStaticMethodID(redefCls,
                methodsInfo[i].m_name, methodsInfo[i].m_sign);
        }
        if (methodsInfo[i].mid == NULL) {
            printf("%s: Failed to get the method ID for the%s%s method \"%s\", signature \"%s\"\n",
                __FILE__, full ? " " : " original ", methodsInfo[i].inst ? "instance":"static",
                methodsInfo[i].m_name, methodsInfo[i].m_sign);
            return STATUS_FAILED;
        }

        /* get the LocalVariableTable attribute */
        err = jvmti->GetLocalVariableTable(methodsInfo[i].mid, &count, &lv_table);
        if (err != JVMTI_ERROR_NONE) {
            printf("%s: Failed to call GetLocalVariableTable(): error=%d: %s\n",
                __FILE__, err, TranslateError(err));
            printf("\tfor the%s%s method \"%s\", signature \"%s\"\n\n",
                full ? " " : " original ", methodsInfo[i].inst ? "instance":"static",
                methodsInfo[i].m_name, methodsInfo[i].m_sign);
            return STATUS_FAILED;
        } else {
            if (count != methodsInfo[i].vcount) {
                printf(
                    "TEST FAILED: %s%s method \"%s\", signature \"%s\": "
                    "found %d vars in the LocalVariableTable, expected %d\n",
                    full ? " " : " original ", methodsInfo[i].inst ? "instance":"static",
                    methodsInfo[i].m_name, methodsInfo[i].m_sign,
                    count, methodsInfo[i].vcount);
                totRes = STATUS_FAILED;
                continue;
            }
            else if (vrb)
                printf(
                    "\nChecking vars in the LocalVariableTable of the %s method \"%s\", "
                    "signature \"%s\" ...\n"
                    "\tfound %d local vars as expected\n",
                    methodsInfo[i].inst ? "instance" : "static",
                    methodsInfo[i].m_name, methodsInfo[i].m_sign, count);

            if (full) {
                for (j=0; j<count; j++) {
                    for (k=0; k<count; k++) {
                        if (strcmp(lv_table[j].name, methodsInfo[i].vars[k].v_name) == 0) {
                            if (strcmp(lv_table[j].signature, methodsInfo[i].vars[k].v_sign) != 0) {
                                printf(
                                    "TEST FAILED: %s method \"%s\", signature \"%s\": var \"%s\" "
                                    "has signature \"%s\" in the LocalVariableTable, expected \"%s\"\n",
                                    methodsInfo[i].inst ? "instance" : "static",
                                    methodsInfo[i].m_name, methodsInfo[i].m_sign,
                                    lv_table[j].name, lv_table[j].signature,
                                    methodsInfo[i].vars[k].v_sign);
                                totRes = STATUS_FAILED;
                                break;
                            }
                            else if (vrb)
                                printf("\tfound var \"%s\", signature \"%s\" as expected\n",
                                    lv_table[j].name, lv_table[j].signature);
                        }
                    }
                }
            }
        }
    }
    return totRes;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RedefineClasses_redefclass009_checkOrigAttr(JNIEnv *env,
        jclass cls, jobject redefObj) {
    jclass redefCls = env->GetObjectClass(redefObj);
    /* check only the number of local variables */
    return checkAttr(env, redefCls, origMethInfo, 0, 0);
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RedefineClasses_redefclass009_makeRedefinition(JNIEnv *env,
        jclass cls, jint vrb, jclass redefCls, jbyteArray classBytes) {
    jvmtiError err;
    jvmtiClassDefinition classDef;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    if (!caps.can_redefine_classes) {
        return PASSED;
    }

    /* fill the structure jvmtiClassDefinition */
    classDef.klass = redefCls;
    classDef.class_byte_count = env->GetArrayLength(classBytes);
    classDef.class_bytes = (unsigned char *) env->GetByteArrayElements(classBytes, NULL);

    if (vrb)
        printf("\n>>>>>>>> Invoke RedefineClasses():\n\tnew class byte count=%d\n",
            classDef.class_byte_count);
    err = jvmti->RedefineClasses(1, &classDef);
    if (err != JVMTI_ERROR_NONE) {
        printf("%s: Failed to call RedefineClasses(): error=%d: %s\n",
            __FILE__, err, TranslateError(err));
        printf("\tFor more info about this error see the JVMTI spec.\n");
        return JNI_ERR;
    }
    if (vrb)
        printf("<<<<<<<< RedefineClasses() is successfully done\n\n");

    return PASSED;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RedefineClasses_redefclass009_getResult(JNIEnv *env,
        jclass cls, jint vrb, jobject redefObj) {
    jclass redefCls = env->GetObjectClass(redefObj);
    return checkAttr(env, redefCls, redefMethInfo, vrb, 1);
}

}
