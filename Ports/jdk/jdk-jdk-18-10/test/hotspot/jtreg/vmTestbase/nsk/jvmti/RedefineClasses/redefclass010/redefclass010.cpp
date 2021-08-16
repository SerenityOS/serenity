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

/* line number matrix of original methods */
static int orig_ln[METH_NUM][8] = {
    { 34, 0, 0, 0, 0, 0, 0, 0 }, /* <init> */
    { 40,41,43, 0, 0, 0, 0, 0 }, /* checkIt */
    { 55, 0, 0, 0, 0, 0, 0, 0 }, /* finMethod */
    { 48,50,51,50,52, 0, 0, 0 }  /* statMethod */
};

/* line number matrix of redefined methods */
static int redf_ln[METH_NUM][8] = {
    { 38,39,40,41,42,43,44,46 }, /* <init> */
    { 51,53,55, 0, 0, 0, 0, 0 }, /* checkIt */
    { 64,66,67,68,69,70,72, 0 }, /* finMethod */
    { 60, 0, 0, 0, 0, 0, 0, 0 }  /* statMethod */
};

typedef struct {   /* line numbers of a method */
    int inst;      /* type of a method: 0- static; 1- instance */
    char *m_name;  /* a method name */
    char *m_sign;  /* JVM signature of a method */
    int lcount;    /* line numbers quantity */
    jmethodID mid; /* JNI's method ID */
} methInfo;

/* list of original methods */
static methInfo origMethInfo[] = {
    { 1, (char*) "<init>", (char*) "()V", 1, NULL },
    { 1, (char*) "checkIt", (char*) "(Ljava/io/PrintStream;Z)I", 3, NULL },
    { 1, (char*) "finMethod", (char*) "(CJIJ)V", 1, NULL },
    { 0, (char*) "statMethod", (char*) "(III)D", 5, NULL }
};

/* list of redefined methods */
static methInfo redefMethInfo[] = {
    { 1, (char*) "<init>", (char*) "()V", 8, NULL },
    { 1, (char*) "checkIt", (char*) "(Ljava/io/PrintStream;Z)I", 3, NULL },
    { 1, (char*) "finMethod", (char*) "(CJIJ)V", 7, NULL },
    { 0, (char*) "statMethod", (char*) "(III)D", 1, NULL }
};

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_redefclass010(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_redefclass010(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_redefclass010(JavaVM *jvm, char *options, void *reserved) {
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

    if (!caps.can_get_line_numbers) {
        printf("Warning: no access to line number info\n");
    }

    return JNI_OK;
}

int checkAttr(JNIEnv *env, jclass redefCls, methInfo methodsInfo[], jint vrb) {
    jvmtiError err;
    int i, j, chkval = 0;
    int totRes = PASSED;
    jint count = -1;
    jvmtiLineNumberEntry *ln_table;

    if (!caps.can_get_line_numbers) {
        return PASSED;
    }

    for (i=0; i<METH_NUM; i++) {
        /* get the JNI method ID for a method with name m_name and
           signature m_sign */
        if (methodsInfo[i].inst) { /* an instance method */
            methodsInfo[i].mid = env->GetMethodID(redefCls,
                    methodsInfo[i].m_name, methodsInfo[i].m_sign);
        } else {                   /* a static method */
            methodsInfo[i].mid = env->GetStaticMethodID(redefCls,
                    methodsInfo[i].m_name, methodsInfo[i].m_sign);
        }
        if (methodsInfo[i].mid == NULL) {
            printf("%s: Failed to get the method ID for the%s%s method \"%s\", signature \"%s\"\n",
                __FILE__, (vrb == 2) ? " original " : " ",
                methodsInfo[i].inst ? "instance" : "static",
                methodsInfo[i].m_name, methodsInfo[i].m_sign);
            return STATUS_FAILED;
        }

        /* get the LineNumberTable attribute */
        err = jvmti->GetLineNumberTable(methodsInfo[i].mid, &count, &ln_table);
        if (err != JVMTI_ERROR_NONE) {
            printf("%s: Failed to call GetLineNumberTable(): error=%d: %s\n",
                __FILE__, err, TranslateError(err));
            printf("\tfor the%s%s method \"%s\", signature \"%s\"\n\n",
                (vrb == 2) ? " original " : " ",
                methodsInfo[i].inst ? "instance" : "static",
                methodsInfo[i].m_name, methodsInfo[i].m_sign);
            return STATUS_FAILED;
        } else {
            if (count != methodsInfo[i].lcount) {
                printf(
                    "TEST %s %s method \"%s\", signature \"%s\": found %d lines in the LineNumberTable, expected %d\n",
                    (vrb == 2) ? "BUG: original " : "FAILED:",
                    methodsInfo[i].inst ? "instance" : "static",
                    methodsInfo[i].m_name, methodsInfo[i].m_sign,
                    count, methodsInfo[i].lcount);
                totRes = STATUS_FAILED;
                continue;
            }
            else if (vrb == 1)
                printf(
                    "\nChecking line numbers in the LineNumberTable of the %s method \"%s\", signature \"%s\" ...\n"
                    "\toverall number of lines: %d as expected\n",
                    methodsInfo[i].inst ? "instance" : "static",
                    methodsInfo[i].m_name, methodsInfo[i].m_sign, count);

            for (j=0; j<count; j++) {
                if (vrb == 2)
                    chkval = orig_ln[i][j];
                else
                    chkval = redf_ln[i][j];

                if (ln_table[j].line_number != chkval) {
                    printf(
                        "TEST %s %s method \"%s\", signature \"%s\": "
                        "entry #%d has value %d in the LineNumberTable, expected %d\n",
                        (vrb == 2) ? "BUG: original" : "FAILED:",
                        methodsInfo[i].inst ? "instance" : "static",
                        methodsInfo[i].m_name, methodsInfo[i].m_sign,
                        j, ln_table[j].line_number, chkval);
                    totRes = STATUS_FAILED;
                    break;
                }
                else if (vrb == 1)
                    printf("\tentry #%d has value %d as expected\n",
                        j, ln_table[j].line_number);
            }
        }
    }
    return totRes;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RedefineClasses_redefclass010_checkOrigAttr(JNIEnv *env,
        jclass cls, jobject redefObj) {
    jclass redefCls = env->GetObjectClass(redefObj);
    return checkAttr(env, redefCls, origMethInfo, 2);
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RedefineClasses_redefclass010_makeRedefinition(JNIEnv *env,
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
Java_nsk_jvmti_RedefineClasses_redefclass010_getResult(JNIEnv *env,
    jclass cls, jint vrb, jobject redefObj) {
    jclass redefCls = env->GetObjectClass(redefObj);
    return checkAttr(env, redefCls, redefMethInfo, vrb);
}

}
