/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

#ifdef __cplusplus
extern "C" {
#endif

#ifndef JNI_ENV_ARG

#ifdef __cplusplus
#define JNI_ENV_ARG(x, y) y
#define JNI_ENV_PTR(x) x
#else
#define JNI_ENV_ARG(x,y) x, y
#define JNI_ENV_PTR(x) (*x)
#endif

#endif

#define TranslateError(err) "JVMTI error"

#define PASSED 0
#define FAILED 2

static const char *EXC_CNAME = "java/lang/Exception";
static const char* MOD_CNAME = "Ljava/lang/Module;";

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;

static jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved);

JNIEXPORT
jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT
jint JNICALL Agent_OnAttach(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT
jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    return JNI_VERSION_1_8;
}

static
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res = JNI_ENV_PTR(jvm)->GetEnv(JNI_ENV_ARG(jvm, (void **) &jvmti),
                                        JVMTI_VERSION_9);
    if (res != JNI_OK || jvmti == NULL) {
        printf("    Error: wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }
    return JNI_OK;
}

static
void throw_exc(JNIEnv *env, char *msg) {
    jclass exc_class = JNI_ENV_PTR(env)->FindClass(JNI_ENV_ARG(env, EXC_CNAME));
    jint rt = JNI_OK;

    if (exc_class == NULL) {
        printf("throw_exc: Error in FindClass(env, %s)\n", EXC_CNAME);
        return;
    }
    rt = JNI_ENV_PTR(env)->ThrowNew(JNI_ENV_ARG(env, exc_class), msg);
    if (rt == JNI_ERR) {
        printf("throw_exc: Error in JNI ThrowNew(env, %s)\n", msg);
    }
}

static
jclass jlM(JNIEnv *env) {
    jclass cls = NULL;

    cls = JNI_ENV_PTR(env)->FindClass(JNI_ENV_ARG(env, MOD_CNAME));
    if (cls == NULL) {
        printf("    Error in JNI FindClass: %s\n", MOD_CNAME);
    }
    return cls;
}

jmethodID
get_method(JNIEnv *env, jclass clazz, const char * name, const char *sig) {
    jmethodID method = NULL;

    method = JNI_ENV_PTR(env)->GetMethodID(JNI_ENV_ARG(env, clazz), name, sig);
    if (method == NULL) {
        printf("    Error in JNI GetMethodID %s with signature %s", name, sig);
    }
    return method;
}

static
jboolean can_module_read(JNIEnv *env, jobject module, jobject to_module) {
    static jmethodID mCanRead = NULL;
    jboolean res = JNI_FALSE;

    if (mCanRead == NULL) {
        const char* sign = "(Ljava/lang/Module;)Z";
        mCanRead = get_method(env, jlM(env), "canRead", sign);
    }
    res = JNI_ENV_PTR(env)->CallBooleanMethod(JNI_ENV_ARG(env, module),
                                              mCanRead, to_module);
    return res;
}

static
jint check_add_module_reads(JNIEnv *env,
                            jclass  cls,
                            jobject unnamedModule,
                            jobject baseModule,
                            jobject instrModule) {
    jvmtiError err = JVMTI_ERROR_NONE;
    jboolean can = JNI_FALSE;

    // Add an invalid read edge from NULL module
    printf("Check #N1:\n");
    err = (*jvmti)->AddModuleReads(jvmti, NULL, baseModule);
    if (err != JVMTI_ERROR_NULL_POINTER) {
        printf("#N1: jvmtiError from AddModuleReads: %d\n", err);
        throw_exc(env, "Check #N1: failed to return JVMTI_ERROR_NULL_POINTER for module==NULL");
        return FAILED;
    }

    // Add an invalid read edge to NULL module
    printf("Check #N2:\n");
    err = (*jvmti)->AddModuleReads(jvmti, baseModule, NULL);
    if (err != JVMTI_ERROR_NULL_POINTER) {
        printf("#N2: jvmtiError from AddModuleReads: %d\n", err);
        throw_exc(env, "Check #N2: failed to return JVMTI_ERROR_NULL_POINTER for to_module==NULL");
        return FAILED;
    }

    // Add an invalid read edge from invalid module (cls)
    printf("Check #I1:\n");
    err = (*jvmti)->AddModuleReads(jvmti, cls, baseModule);
    if (err != JVMTI_ERROR_INVALID_MODULE) {
        printf("#I1: jvmtiError from AddModuleReads: %d\n", err);
        throw_exc(env, "Check #I1: failed to return JVMTI_ERROR_INVALID_MODULE for module==cls");
        return FAILED;
    }

    // Add an invalid read edge to invalid module (cls)
    printf("Check #I2:\n");
    err = (*jvmti)->AddModuleReads(jvmti, baseModule, cls);
    if (err != JVMTI_ERROR_INVALID_MODULE) {
        printf("#I2: jvmtiError from AddModuleReads: %d\n", err);
        throw_exc(env, "Check #I2: failed to return JVMTI_ERROR_INVALID_MODULE for to_module==cls");
        return FAILED;
    }

    // Check the edge baseModule->instrModule is absent
    printf("Check #C0:\n");
    can = can_module_read(env, baseModule, instrModule);
    if (can != JNI_FALSE) {
        throw_exc(env, "Check #C0: read edge from base to instr is unexpected");
        return FAILED;
    }

    // Add read edge baseModule->instrModule
    printf("Check #C1:\n");
    err = (*jvmti)->AddModuleReads(jvmti, baseModule, instrModule);
    if (err != JVMTI_ERROR_NONE) {
        printf("#C1: jvmtiError from AddModuleReads: %d\n", err);
        throw_exc(env, "Check #C1: error in add reads from base to instr");
        return FAILED;
    }

    // Check the read edge baseModule->instrModule is present now
    printf("Check #C2:\n");
    can = can_module_read(env, baseModule, instrModule);
    if (can == JNI_FALSE) {
        throw_exc(env, "Check #C2: failed to add reads from base to instr");
        return FAILED;
    }

    // Check the read edge baseModule->unnamedModule is absent
    printf("Check #C3:\n");
    can = can_module_read(env, baseModule, unnamedModule);
    if (can != JNI_FALSE) {
        throw_exc(env, "Check #C3: got unexpected read edge from base to unnamed");
        return FAILED;
    }

    // Add read edge baseModule->unnamedModule
    printf("Check #C4:\n");
    err = (*jvmti)->AddModuleReads(jvmti, baseModule, unnamedModule);
    if (err != JVMTI_ERROR_NONE) {
        printf("#C4: jvmtiError from AddModuleReads: %d\n", err);
        throw_exc(env, "Check #C4: failed to ignore adding read edge from base to unnamed");
        return FAILED;
    }

    // Check the read edge baseModule->unnamedModule is present now
    printf("Check #C5:\n");
    can = can_module_read(env, baseModule, unnamedModule);
    if (can == JNI_FALSE) {
        throw_exc(env, "Check #C5: did not get expected read edge from base to unnamed");
        return FAILED;
    }

    // Check the read edge unnamedModule->instrModule is absent
    printf("Check #C6:\n");
    can = can_module_read(env, unnamedModule, instrModule);
    if (can == JNI_FALSE) {
        throw_exc(env, "Check #C6: did not get expected read edge from unnamed to instr");
        return FAILED;
    }

    // Add read edge unnamedModule->instrModule
    printf("Check #C7:\n");
    err = (*jvmti)->AddModuleReads(jvmti, unnamedModule, instrModule);
    if (err != JVMTI_ERROR_NONE) {
        printf("#C7: jvmtiError from AddModuleReads: %d\n", err);
        throw_exc(env, "Check #C7: failed to ignore adding read edge from unnamed to instr");
        return FAILED;
    }
    return PASSED;
}

JNIEXPORT jint JNICALL
Java_MyPackage_AddModuleReadsTest_check(JNIEnv *env,
                                        jclass cls,
                                        jobject unnamedModule,
                                        jobject baseModule,
                                        jobject instrModule) {
    if (jvmti == NULL) {
        throw_exc(env, "JVMTI client was not properly loaded!\n");
        return FAILED;
    }

    printf("\n*** Checks for JVMTI AddModuleReads ***\n\n");
    result = check_add_module_reads(env, cls, unnamedModule, baseModule, instrModule);
    if (result != PASSED) {
        return result;
    }
    return result;
}

#ifdef __cplusplus
}
#endif
