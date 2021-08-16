/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

static const char *EXC_CNAME = "java/lang/AssertionError";

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;

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
    jint res;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    res = JNI_ENV_PTR(jvm)->GetEnv(JNI_ENV_ARG(jvm, (void **) &jvmti),
        JVMTI_VERSION_9);
    if (res != JNI_OK || jvmti == NULL) {
        printf("    Error: wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    return JNI_OK;
}

static
jclass find_class(JNIEnv *env, const char* cname) {
    jclass cls = JNI_ENV_PTR(env)->FindClass(JNI_ENV_ARG(env, cname));

    if (cls == NULL) {
        printf("find_class: Error: FindClass(env, \"%s\") returned NULL\n", cname);
    }
    return cls;
}

static
jint throw_exc(JNIEnv *env, char *msg) {
    jclass exc_class = find_class(env, EXC_CNAME);

    if (exc_class == NULL) {
        printf("throw_exc: Error in find_class(env, \"%s\")\n", EXC_CNAME);
        return -1;
    }
    return JNI_ENV_PTR(env)->ThrowNew(JNI_ENV_ARG(env, exc_class), msg);
}

static jobject get_module_by_class_name(JNIEnv *env, const char* cname) {
    jobject module = NULL;
    jclass cls = find_class(env, cname);

    printf(">>> getting module by class name: \"%s\"\n", cname);
    if (cls == NULL) {
        printf("get_module_by_class_name: Error in find_class(env, \"%s\")\n", cname);
        return NULL;
    }
    module = JNI_ENV_PTR(env)->GetModule(JNI_ENV_ARG(env, cls));
    if (module == NULL) {
        printf("get_module_by_class_name: Error in GetModule for class \"%s\"\n", cname);
    }
    return module;
}

static
jint check_is_modifiable_error_codes(jobject module, jobject not_a_module) {
    jvmtiError err = JVMTI_ERROR_NONE;
    jboolean is_modifiable = JNI_FALSE;

    printf(">>> passing a bad module argument to JVMTI IsModifiableModule\n");
    err = (*jvmti)->IsModifiableModule(jvmti, not_a_module, &is_modifiable);
    if (err != JVMTI_ERROR_INVALID_MODULE) {
        printf("    Error #EC0: Did not get expected INVALID_MODULE error code from"
               " IsModifiableModule: %s (%d)\n", TranslateError(err), err);
        return FAILED;
    }
    printf(">>> passing NULL module argument to JVMTI IsModifiableModule\n");
    err = (*jvmti)->IsModifiableModule(jvmti, NULL, &is_modifiable);
    if (err != JVMTI_ERROR_NULL_POINTER) {
        printf("    Error #EC1: Did not get expected NULL_POINTER error code from"
               " IsModifiableModule: %s (%d)\n", TranslateError(err), err);
        return FAILED;
    }
    printf(">>> passing NULL status pointer to JVMTI IsModifiableModule\n");
    err = (*jvmti)->IsModifiableModule(jvmti, module, NULL);
    if (err != JVMTI_ERROR_NULL_POINTER) {
        printf("    Error #EC2: Did not get expected NULL_POINTER error code from"
               " IsModifiableModule: %s (%d)\n", TranslateError(err), err);
        return FAILED;
    }
    return PASSED;
}

static
jint check_is_modifiable(jobject module) {
    jvmtiError err = JVMTI_ERROR_NONE;
    jboolean is_modifiable = JNI_FALSE;

    printf(">>> checking module %p is modifiable\n", module);
    err = (*jvmti)->IsModifiableModule(jvmti, module, &is_modifiable);
    if (err != JVMTI_ERROR_NONE) {
        printf("    Error in IsModifiableModule for module %p: %s (%d)\n",
               module, TranslateError(err), err);
        return FAILED;
    }
    if (is_modifiable == JNI_FALSE) {
        printf("    unexpected non-modifiable status for module: %p\n", module);
        return FAILED;
    }
    return PASSED;
}

JNIEXPORT jint JNICALL
Java_MyPackage_IsModifiableModuleTest_check(JNIEnv *env, jclass cls) {
    jobject module = NULL;

    if (jvmti == NULL) {
        throw_exc(env, "JVMTI client was not properly loaded!\n");
        return FAILED;
    }

    printf("\n*** Testing IsModifiableModule ***\n\n");

    if (check_is_modifiable_error_codes(module, cls) == FAILED) {
        throw_exc(env, "check #MM0: failed to return expected error code from "
                      "a bad call to JVMTI IsModifiableModule");
        return FAILED;
    }

    module = get_module_by_class_name(env, "java/lang/Class");
    if (check_is_modifiable(module) == FAILED) {
        throw_exc(env, "check #MM1: failed to return modifiable module status");
        return FAILED;
    }

    module = get_module_by_class_name(env, "com/sun/jdi/VirtualMachine");
    if (check_is_modifiable(module) == FAILED) {
        throw_exc(env, "check #MM2: failed to return modifiable module status");
        return FAILED;
    }

    module = get_module_by_class_name(env, "MyPackage/IsModifiableModuleTest");
    if (check_is_modifiable(module) == FAILED) {
        throw_exc(env, "check #MM3: failed to return modifiable module status");
        return FAILED;
    }

    return PASSED;
}

#ifdef __cplusplus
}
#endif
