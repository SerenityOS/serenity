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
jboolean can_use_service(JNIEnv *env, jobject module, jclass service) {
    static jmethodID mCanUse = NULL;
    jboolean res = JNI_FALSE;

    if (mCanUse == NULL) {
        const char* sign = "(Ljava/lang/Class;)Z";
        mCanUse = get_method(env, jlM(env), "canUse", sign);
    }
    res = JNI_ENV_PTR(env)->CallBooleanMethod(JNI_ENV_ARG(env, module),
                                              mCanUse, service);
    return res;
}

JNIEXPORT jint JNICALL
Java_MyPackage_AddModuleUsesAndProvidesTest_checkUses(JNIEnv *env,
                                                      jclass  cls,
                                                      jobject baseModule,
                                                      jclass  service) {
    jvmtiError err = JVMTI_ERROR_NONE;
    jboolean used = JNI_FALSE;

    // Add a service to use to NULL module
    printf("Check #UN1:\n");
    err = (*jvmti)->AddModuleUses(jvmti, NULL, service);
    if (err != JVMTI_ERROR_NULL_POINTER) {
        printf("#UN1: jvmtiError from AddModuleUses: %d\n", err);
        throw_exc(env, "Check #UN1: failed to return JVMTI_ERROR_NULL_POINTER for module==NULL");
        return FAILED;
    }

    // Add NULL service to use to baseModule
    printf("Check #UN2:\n");
    err = (*jvmti)->AddModuleUses(jvmti, baseModule, NULL);
    if (err != JVMTI_ERROR_NULL_POINTER) {
        printf("#UN2: jvmtiError from AddModuleUses: %d\n", err);
        throw_exc(env, "Check #UN2: failed to return JVMTI_ERROR_NULL_POINTER for service==NULL");
        return FAILED;
    }

    // Add service to use to invalid module (cls)
    printf("Check #UI1:\n");
    err = (*jvmti)->AddModuleUses(jvmti, (jobject)cls, service);
    if (err != JVMTI_ERROR_INVALID_MODULE) {
        printf("#UI1: jvmtiError from AddModuleUses: %d\n", err);
        throw_exc(env, "Check #UI1: did not get expected JVMTI_ERROR_INVALID_MODULE for invalid module");
        return FAILED;
    }

    // Add invalid service (thisModule) to use to baseModule
    printf("Check #UI2:\n");
    err = (*jvmti)->AddModuleUses(jvmti, baseModule, baseModule);
    if (err != JVMTI_ERROR_INVALID_CLASS) {
        printf("#UI2: jvmtiError from AddModuleUses: %d\n", err);
        throw_exc(env, "Check #UI2: did not get expected JVMTI_ERROR_INVALID_CLASS for invalid service");
        return FAILED;
    }

    // Check if the service can not be used
    printf("Check #UC1:\n");
    used = can_use_service(env, baseModule, service);
    if (used != JNI_FALSE) {
        throw_exc(env, "Check #UC1: unexpected use of service");
        return FAILED;
    }

    // Add uses of a correct service
    printf("Check #UC2:\n");
    err = (*jvmti)->AddModuleUses(jvmti, baseModule, service);
    if (err != JVMTI_ERROR_NONE) {
        printf("#UC2: jvmtiError from AddModuleUses: %d\n", err);
        throw_exc(env, "Check #UC2: got unexpected JVMTI error");
        return FAILED;
    }

    // Check if the service can not be used
    printf("Check #UC3:\n");
    used = can_use_service(env, baseModule, service);
    if (used == JNI_FALSE) {
        throw_exc(env, "Check #UC3: service can not be used unexpectedly");
        return FAILED;
    }
    fflush(0);
    return PASSED;
}

JNIEXPORT jint JNICALL
Java_MyPackage_AddModuleUsesAndProvidesTest_checkProvides(JNIEnv *env,
                                                          jclass  cls,
                                                          jobject baseModule,
                                                          jclass  service,
                                                          jclass  serviceImpl) {
   jvmtiError err = JVMTI_ERROR_NONE;
   jboolean provided = JNI_FALSE;

    // Add provides to NULL module
    printf("Check #PN1:\n");
    err = (*jvmti)->AddModuleProvides(jvmti, NULL, service, serviceImpl);
    if (err != JVMTI_ERROR_NULL_POINTER) {
        printf("#PN1: jvmtiError from AddModuleProvides: %d\n", err);
        throw_exc(env, "Check #PN1: failed to return JVMTI_ERROR_NULL_POINTER for module==NULL");
        return FAILED;
    }

    // Add provides with NULL service
    printf("Check #PN2:\n");
    err = (*jvmti)->AddModuleProvides(jvmti, baseModule, NULL, serviceImpl);
    if (err != JVMTI_ERROR_NULL_POINTER) {
        printf("#PN2: jvmtiError from AddModuleProvides: %d\n", err);
        throw_exc(env, "Check #PN2: failed to return JVMTI_ERROR_NULL_POINTER for service==NULL");
        return FAILED;
    }

    // Add provides with NULL serviceImpl
    printf("Check #PN3:\n");
    err = (*jvmti)->AddModuleProvides(jvmti, baseModule, service, NULL);
    if (err != JVMTI_ERROR_NULL_POINTER) {
        printf("#PN3: jvmtiError from AddModuleProvides: %d\n", err);
        throw_exc(env, "Check #PN3: failed to return JVMTI_ERROR_NULL_POINTER for serviceImpl==NULL");
        return FAILED;
    }

    // Add provides to invalid module (cls)
    printf("Check #PI1:\n");
    err = (*jvmti)->AddModuleProvides(jvmti, (jobject)cls, service, serviceImpl);
    if (err != JVMTI_ERROR_INVALID_MODULE) {
        printf("#PI1: jvmtiError from AddModuleProvides: %d\n", err);
        throw_exc(env, "Check #PI1: did not get expected JVMTI_ERROR_INVALID_MODULE for invalid module");
        return FAILED;
    }

    // Add provides with invalid service (baseModule)
    printf("Check #PI2:\n");
    err = (*jvmti)->AddModuleProvides(jvmti, baseModule, baseModule, serviceImpl);
    if (err != JVMTI_ERROR_INVALID_CLASS) {
        printf("#PI2: jvmtiError from AddModuleProvides: %d\n", err);
        throw_exc(env, "Check #PI2: did not get expected JVMTI_ERROR_INVALID_CLASS for invalid service");
        return FAILED;
    }

    // Add provides with invalid serviceImpl (baseModule)
    printf("Check #PI3:\n");
    err = (*jvmti)->AddModuleProvides(jvmti, baseModule, service, baseModule);
    if (err != JVMTI_ERROR_INVALID_CLASS) {
        printf("#PI3: jvmtiError from AddModuleProvides: %d\n", err);
        throw_exc(env, "Check #PI3: did not get expected JVMTI_ERROR_INVALID_CLASS for invalid serviceImpl");
        return FAILED;
    }

    // Add provides to baseModule with correct service and serviceImpl
    printf("Check #PC2:\n");
    err = (*jvmti)->AddModuleProvides(jvmti, baseModule, service, serviceImpl);
    if (err != JVMTI_ERROR_NONE) {
        printf("#PC2: jvmtiError from AddModuleExports: %d\n", err);
        throw_exc(env, "Check #PC2: error in add provides to baseModule with correct service and serviceImpl");
        return FAILED;
    }
    fflush(0);
    return PASSED;
}

#ifdef __cplusplus
}
#endif
