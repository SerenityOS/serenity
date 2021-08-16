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
jint throw_exc(JNIEnv *env, char *msg) {
    jclass exc_class = JNI_ENV_PTR(env)->FindClass(JNI_ENV_ARG(env, EXC_CNAME));

    if (exc_class == NULL) {
        printf("throw_exc: Error in FindClass(env, %s)\n", EXC_CNAME);
        return -1;
    }
    return JNI_ENV_PTR(env)->ThrowNew(JNI_ENV_ARG(env, exc_class), msg);
}

static
jobject get_class_loader(jclass cls) {
    jvmtiError err = JVMTI_ERROR_NONE;
    jobject loader = NULL;

    if (printdump == JNI_TRUE) {
        printf(">>> getting class loader ...\n");
    }
    err = (*jvmti)->GetClassLoader(jvmti, cls, &loader);
    if (err != JVMTI_ERROR_NONE) {
        printf("    Error in GetClassLoader: %s (%d)\n", TranslateError(err), err);
    }
    return loader;
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
jobject get_module_loader(JNIEnv *env, jobject module) {
    static jmethodID cl_method = NULL;
    jobject loader = NULL;

    if (cl_method == NULL) {
        cl_method = get_method(env, jlM(env), "getClassLoader", "()Ljava/lang/ClassLoader;");
    }
    loader = (jobject)JNI_ENV_PTR(env)->CallObjectMethod(JNI_ENV_ARG(env, module), cl_method);
    return loader;
}

static
const char* get_module_name(JNIEnv *env, jobject module) {
    static jmethodID method = NULL;
    jobject loader = NULL;
    jstring jstr = NULL;
    const char *name = NULL;
    const char *nstr = NULL;

    if (method == NULL) {
        method = get_method(env, jlM(env), "getName", "()Ljava/lang/String;");
    }
    jstr = (jstring)JNI_ENV_PTR(env)->CallObjectMethod(JNI_ENV_ARG(env, module), method);
    if (jstr != NULL) {
        name = JNI_ENV_PTR(env)->GetStringUTFChars(JNI_ENV_ARG(env, jstr), NULL);
    }
    loader = get_module_loader(env, module);
    nstr = (name == NULL) ? "<UNNAMED>" : name;
    printf("    loader: %p, module: %p, name: %s\n", loader, module, nstr);
    return name;
}

static
jvmtiError get_module(JNIEnv *env,
                      jobject loader,
                      const char* pkg_name,
                      jobject* module_ptr,
                      const char** mod_name_ptr) {
    jvmtiError err = JVMTI_ERROR_NONE;
    const char* name = (pkg_name == NULL) ? "<NULL>" : pkg_name;

    printf(">>> getting module by loader %p and package \"%s\"\n", loader, name);
    *mod_name_ptr = NULL;
    err = (*jvmti)->GetNamedModule(jvmti, loader, pkg_name, module_ptr);
    if (err != JVMTI_ERROR_NONE) {
        printf("    Error in GetNamedModule for package \"%s\": %s (%d)\n",
               name, TranslateError(err), err);
        return err;
    }
    printf("    returned module: %p\n", *module_ptr);
    if (*module_ptr == NULL) { // named module was not found
        return err;
    }
    *mod_name_ptr = get_module_name(env, *module_ptr);
    return err;
}

static
jint get_all_modules(JNIEnv *env) {
    jvmtiError err;
    jint cnt = -1;
    jint idx = 0;
    jobject* modules;

    printf(">>> Inspecting modules with GetAllModules\n");
    err = (*jvmti)->GetAllModules(jvmti, &cnt, &modules);
    if (err != JVMTI_ERROR_NONE) {
        printf("Error in GetAllModules: %d\n", err);
        return -1;
    }
    for (idx = 0; idx < cnt; ++idx) {
        get_module_name(env, modules[idx]);
    }
    return cnt;
}

static
jint check_bad_loader(JNIEnv *env, jobject loader) {
    jvmtiError err = JVMTI_ERROR_NONE;
    jobject module = NULL;
    const char* mod_name = NULL;

    err = get_module(env, loader, "", &module, &mod_name);
    if (err != JVMTI_ERROR_ILLEGAL_ARGUMENT) {
        return FAILED;
    }
    printf("    got expected JVMTI_ERROR_ILLEGAL_ARGUMENT for bad loader\n");
    return PASSED;
}

static
jint check_system_loader(JNIEnv *env, jobject loader) {
    jvmtiError err = JVMTI_ERROR_NONE;
    jobject module = NULL;
    const char* exp_name = NULL;
    const char* mod_name = NULL;

    // NULL pointer for package name
    err = get_module(env, loader, NULL, &module, &mod_name);
    if (err != JVMTI_ERROR_NULL_POINTER) {
        throw_exc(env, "check #SN1: failed to return JVMTI_ERROR_NULL_POINTER for NULL package");
        return FAILED;
    }

    // NULL pointer for module_ptr
    err = (*jvmti)->GetNamedModule(jvmti, loader, "", NULL);
    if (err != JVMTI_ERROR_NULL_POINTER) {
        throw_exc(env, "check #SN2: failed to return JVMTI_ERROR_NULL_POINTER for NULL module_ptr");
        return FAILED;
    }

    // Unnamed/default package ""
    err = get_module(env, loader, "", &module, &mod_name);
    if (err != JVMTI_ERROR_NONE) {
        throw_exc(env, "check #S1: failed to return JVMTI_ERROR_NONE for default package");
        return FAILED;
    }
    if (module != NULL || mod_name != NULL) {
        throw_exc(env, "check #S2: failed to return NULL-module for default package");
        return FAILED;
    }

    // Test package: MyPackage
    err = get_module(env, loader, "MyPackage", &module, &mod_name);
    if (err != JVMTI_ERROR_NONE) {
        throw_exc(env, "check #S3: failed to return JVMTI_ERROR_NONE for MyPackage");
        return FAILED;
    }
    if (module != NULL || mod_name != NULL) {
        throw_exc(env, "check #S4: failed to return NULL-module for MyPackage");
        return FAILED;
    }

    // Package: com/sun/jdi
    exp_name = "jdk.jdi";
    err = get_module(env, loader, "com/sun/jdi", &module, &mod_name);
    if (err != JVMTI_ERROR_NONE) {
        throw_exc(env, "check #S5: failed to return JVMTI_ERROR_NONE for test package");
        return FAILED;
    }
    if (module == NULL || mod_name == NULL) {
        throw_exc(env, "check #S6: failed to return named module for com/sun/jdi package");
        return FAILED;
    }
    if (strcmp(mod_name, exp_name) != 0) {
        printf("check #S7: failed to return right module, expected: %s, returned: %s\n",
               exp_name, mod_name);
        throw_exc(env, "check #S7: failed to return jdk.jdi module for com/sun/jdi package");
        return FAILED;
    }

    // Non-existing package: "bad/package/name"
    err = get_module(env, loader, "bad/package/name", &module, &mod_name);
    if (err != JVMTI_ERROR_NONE) {
        throw_exc(env, "check #S8: failed to return JVMTI_ERROR_NONE for bad package");
        return FAILED;
    }
    if (module != NULL || mod_name != NULL) {
        throw_exc(env, "check #S9: failed to return NULL-module for bad package");
        return FAILED;
    }
    return PASSED;
}

static
jint check_bootstrap_loader(JNIEnv *env, jobject loader) {
    jvmtiError err = JVMTI_ERROR_NONE;
    jobject module = NULL;
    const char* exp_name = NULL;
    const char* mod_name = NULL;

    // NULL pointer for package name
    err = get_module(env, loader, NULL, &module, &mod_name);
    if (err != JVMTI_ERROR_NULL_POINTER) {
        throw_exc(env, "check #BN1: failed to return JVMTI_ERROR_NULL_POINTER for NULL package");
        return FAILED;
    }

    // NULL pointer for module_ptr
    err = (*jvmti)->GetNamedModule(jvmti, loader, "", NULL);
    if (err != JVMTI_ERROR_NULL_POINTER) {
        throw_exc(env, "check #BN2: failed to return JVMTI_ERROR_NULL_POINTER for NULL module_ptr");
        return FAILED;
    }

    // Unnamed/default package ""
    err = get_module(env, loader, "", &module, &mod_name);
    if (err != JVMTI_ERROR_NONE) {
        throw_exc(env, "check #B1: failed to return JVMTI_ERROR_NONE for default package");
        return FAILED;
    }
    if (module != NULL || mod_name != NULL) {
        throw_exc(env, "check #B2: failed to return NULL-module for default package");
        return FAILED;
    }

    // Normal package from java.base module: "java/lang"
    exp_name = "java.base";
    err = get_module(env, loader, "java/lang", &module, &mod_name);
    if (err != JVMTI_ERROR_NONE) {
        throw_exc(env, "check #B3: failed to return JVMTI_ERROR_NONE for java/lang package");
        return FAILED;
    }
    if (module == NULL || mod_name == NULL) {
        throw_exc(env, "check #B4: failed to return named module for java/lang package");
        return FAILED;
    }
    if (strcmp(exp_name, mod_name) != 0) {
        printf("check #B5: failed to return right module, expected: %s, returned: %s\n",
               exp_name, mod_name);
        throw_exc(env, "check #B5: failed to return expected module for java/lang package");
        return FAILED;
    }

    // Non-existing package: "bad/package/name"
    err = get_module(env, loader, "bad/package/name", &module, &mod_name);
    if (err != JVMTI_ERROR_NONE) {
        throw_exc(env, "check #B6: failed to return JVMTI_ERROR_NONE for bad package");
        return FAILED;
    }
    if (module != NULL || mod_name != NULL) {
        throw_exc(env, "check #B7: failed to return NULL-module for bad package");
        return FAILED;
    }
    return PASSED;
}

JNIEXPORT jint JNICALL
Java_MyPackage_GetNamedModuleTest_check(JNIEnv *env, jclass cls) {
    jobject loader = NULL;

    if (jvmti == NULL) {
        throw_exc(env, "JVMTI client was not properly loaded!\n");
        return FAILED;
    }

    get_all_modules(env);

    printf("\n*** Check for bad ClassLoader ***\n\n");
    result = check_bad_loader(env, (jobject)cls);
    if (result != PASSED) {
        throw_exc(env, "check #L1: failed to return JVMTI_ERROR_ILLEGAL_ARGUMENT for bad loader");
        return result;
    }

    loader = get_class_loader(cls);
    if (loader == NULL) {
        throw_exc(env, "check #L2: failed to return non-NULL loader for valid test class");
        return FAILED;
    }

    printf("\n*** Checks for System ClassLoader ***\n\n");
    result = check_system_loader(env, loader);
    if (result != PASSED) {
        return result;
    }

    printf("\n*** Checks for Bootstrap ClassLoader ***\n\n");
    result = check_bootstrap_loader(env, NULL);

    return result;
}

#ifdef __cplusplus
}
#endif
