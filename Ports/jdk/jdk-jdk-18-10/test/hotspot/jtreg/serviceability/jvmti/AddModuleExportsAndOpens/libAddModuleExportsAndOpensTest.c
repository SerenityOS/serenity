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
jboolean is_exported(JNIEnv *env, jobject module, const char* pkg, jboolean open) {
    static jmethodID mIsExported = NULL;
    jstring jstr = NULL;
    jboolean res = JNI_FALSE;

    if (mIsExported == NULL) {
        const char* sign = "(Ljava/lang/String;)Z";
        const char* name = open ? "isOpen" : "isExported";
        mIsExported = get_method(env, jlM(env), name, sign);
    }
    jstr = JNI_ENV_PTR(env)->NewStringUTF(JNI_ENV_ARG(env, pkg));
    res = JNI_ENV_PTR(env)->CallBooleanMethod(JNI_ENV_ARG(env, module),
                                              mIsExported, jstr);
    return res;
}

static
jboolean is_exported_to(JNIEnv *env, jobject module, const char* pkg, jobject to_module,
                        jboolean open) {
    static jmethodID mIsExportedTo = NULL;
    jstring jstr = NULL;
    jboolean res = JNI_FALSE;

    if (mIsExportedTo == NULL) {
        const char* sign = "(Ljava/lang/String;Ljava/lang/Module;)Z";
        const char* name = open ? "isOpen" : "isExported";
        mIsExportedTo = get_method(env, jlM(env), name, sign);
    }
    jstr = JNI_ENV_PTR(env)->NewStringUTF(JNI_ENV_ARG(env, pkg));
    res = JNI_ENV_PTR(env)->CallBooleanMethod(JNI_ENV_ARG(env, module),
                                              mIsExportedTo, jstr, to_module);
    return res;
}

static
jvmtiError add_module_exports(jobject baseModule, const char* pkg, jobject thisModule,
                              jboolean open) {
    jvmtiError err = JVMTI_ERROR_NONE;
    if (open) {
        err = (*jvmti)->AddModuleOpens(jvmti, baseModule, pkg, thisModule);
    } else {
        err = (*jvmti)->AddModuleExports(jvmti, baseModule, pkg, thisModule);
    }
    return err;
}

static
jint check_add_module_exports(JNIEnv *env,
                              jclass  cls,
                              jobject baseModule,
                              jobject thisModule,
                              jboolean open) {
    static char strbuf[128] = { '\0' };
    jvmtiError err = JVMTI_ERROR_NONE;
    const char* pkg = open ? "jdk.internal.math"
                           : "jdk.internal.misc";
    const char* bad_pkg = "my.bad.pkg";
    const char* jvmti_fn = open ? "AddModuleOpens"
                                : "AddModuleExports";
    jboolean exported = JNI_FALSE;

    // Export from NULL module
    printf("Check #N1:\n");
    err = add_module_exports(NULL, pkg, thisModule, open);
    if (err != JVMTI_ERROR_NULL_POINTER) {
        printf("#N1: jvmtiError from %s: %d\n", jvmti_fn, err);
        throw_exc(env, "Check #N1: failed to return JVMTI_ERROR_NULL_POINTER for module==NULL");
        return FAILED;
    }

    // Export NULL package
    printf("Check #N2:\n");
    err = add_module_exports(baseModule, NULL, thisModule, open);
    if (err != JVMTI_ERROR_NULL_POINTER) {
        printf("#N2: jvmtiError from %s: %d\n", jvmti_fn, err);
        throw_exc(env, "Check #N2: failed to return JVMTI_ERROR_NULL_POINTER for pkg==NULL");
        return FAILED;
    }

    // Export to NULL module
    printf("Check #N3:\n");
    err = add_module_exports(baseModule, pkg, NULL, open);
    if (err != JVMTI_ERROR_NULL_POINTER) {
        printf("#N3: jvmtiError from %s: %d\n", jvmti_fn, err);
        throw_exc(env, "Check #N3: failed to return JVMTI_ERROR_NULL_POINTER for to_module==NULL");
        return FAILED;
    }

    // Export a bad package
    printf("Check #I0:\n");
    err = add_module_exports(baseModule, bad_pkg, thisModule, open);
    if (err != JVMTI_ERROR_ILLEGAL_ARGUMENT) {
        printf("#I0: jvmtiError from %s: %d\n", jvmti_fn, err);
        throw_exc(env, "Check #I0: did not get expected JVMTI_ERROR_ILLEGAL_ARGUMENT for invalid package");
        return FAILED;
    }

    // Export from invalid module (cls)
    printf("Check #I1:\n");
    err = add_module_exports((jobject)cls, pkg, thisModule, open);
    if (err != JVMTI_ERROR_INVALID_MODULE) {
        printf("#I1: jvmtiError from %s: %d\n", jvmti_fn, err);
        throw_exc(env, "Check #I1: did not get expected JVMTI_ERROR_INVALID_MODULE for invalid module");
        return FAILED;
    }

    // Export to invalid module (cls)
    printf("Check #I2:\n");
    err = add_module_exports(baseModule, pkg, (jobject)cls, open);
    if (err != JVMTI_ERROR_INVALID_MODULE) {
        printf("#I2: jvmtiError from %s: %d\n", jvmti_fn, err);
        throw_exc(env, "Check #I2: did not get expected JVMTI_ERROR_INVALID_MODULE for invalid to_module");
        return FAILED;
    }

    // Check the pkg is not exported from baseModule to thisModule
    printf("Check #C0:\n");
    exported = is_exported_to(env, baseModule, pkg, thisModule, open);
    if (exported != JNI_FALSE) {
        sprintf(strbuf, "Check #C0: unexpected export of %s from base to this", pkg);
        throw_exc(env, strbuf);
        return FAILED;
    }

    // Add export of the pkg from baseModule to thisModule
    printf("Check #C1:\n");
    err = add_module_exports(baseModule, pkg, thisModule, open);
    if (err != JVMTI_ERROR_NONE) {
        printf("#C1: jvmtiError from %s: %d\n", jvmti_fn, err);
        sprintf(strbuf, "Check #C1: error in add export of %s from base to this", pkg);
        throw_exc(env, strbuf);
        return FAILED;
    }

    // Check the pkg is exported from baseModule to thisModule
    printf("Check #C2:\n");
    exported = is_exported_to(env, baseModule, pkg, thisModule, open);
    if (exported == JNI_FALSE) {
        sprintf(strbuf, "Check #C2: failed to export %s from base to this", pkg);
        throw_exc(env, strbuf);
        return FAILED;
    }

    // Check the pkg is not exported to all modules
    printf("Check #C3:\n");
    exported = is_exported(env, baseModule, pkg, open);
    if (exported != JNI_FALSE) {
        sprintf(strbuf, "Check #C3: unexpected export of %s from base to all modules", pkg);
        throw_exc(env, strbuf);
        return FAILED;
    }
    return PASSED;
}

JNIEXPORT jint JNICALL
Java_MyPackage_AddModuleExportsAndOpensTest_check(JNIEnv *env,
                                                  jclass cls,
                                                  jobject baseModule,
                                                  jobject thisModule) {
    if (jvmti == NULL) {
        throw_exc(env, "JVMTI client was not properly loaded!\n");
        return FAILED;
    }

    printf("\n*** Checks for JVMTI AddModuleExports ***\n\n");
    result = check_add_module_exports(env, cls, baseModule, thisModule, JNI_FALSE);
    if (result != PASSED) {
        return result;
    }

    printf("\n*** Checks for JVMTI AddModuleOpens ***\n\n");
    result = check_add_module_exports(env, cls, baseModule, thisModule, JNI_TRUE);
    if (result != PASSED) {
        return result;
    }
    return result;
}

#ifdef __cplusplus
}
#endif
