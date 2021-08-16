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
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

typedef struct {
    int class_id;
    const char *name;
    const char *signature;
    jboolean is_static;
    jboolean is_synthetic;
} method_info;

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static method_info methods[] = {
    { 1, "meth_stat", "(ILjava/lang/String;)[F", JNI_TRUE, JNI_FALSE },
    { 1, "meth_1", "(CCC)C", JNI_FALSE, JNI_FALSE },
    { 1, "class$", "(Ljava/lang/String;)Ljava/lang/Class;", JNI_TRUE, JNI_TRUE },
    { 1, "access$000",
        "(Lnsk/jvmti/IsMethodSynthetic/issynth001a;)I", JNI_TRUE, JNI_TRUE },
    { 1, "nmeth", "()V", JNI_FALSE, JNI_FALSE },
    { 1, "check", "(Ljava/lang/Class;Ljava/lang/Class;)I", JNI_TRUE, JNI_FALSE },

    { 2, "<init>", "()V", JNI_FALSE, JNI_FALSE },
    { 2, "run",
        "([Ljava/lang/String;Ljava/io/PrintStream;)I", JNI_TRUE, JNI_FALSE },

    { 3, "meth_inn", "(Ljava/lang/String;J)V", JNI_FALSE, JNI_FALSE },
};

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_issynth001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_issynth001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_issynth001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv!\n");
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

    if (!caps.can_get_synthetic_attribute) {
        printf("Warning: IsMethodSynthetic is not implemented\n");
    }

    return JNI_OK;
}

char const *jbooleanToString(jboolean flag) {
    return ((flag == JNI_TRUE) ? "true" : "false");
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_IsMethodSynthetic_issynth001a_check(JNIEnv *env,
        jclass cls1, jclass cls2, jclass cls3) {
    jvmtiError err;
    jclass cl;
    jmethodID mid;
    jboolean is_synthetic;
    size_t i;

    if (!caps.can_get_synthetic_attribute) {
        return result;
    }

    for (i = 0; i < sizeof(methods)/sizeof(method_info); i++) {
        cl = ((methods[i].class_id == 1) ? cls1 :
                ((methods[i].class_id == 2) ? cls2 : cls3));
        if (methods[i].is_static == JNI_TRUE) {
            mid = env->GetStaticMethodID(cl, methods[i].name, methods[i].signature);
        } else {
            mid = env->GetMethodID(cl, methods[i].name, methods[i].signature);
        }
        if (mid == NULL) {
            printf("Cannot find MethodID for \"%s%s\"\n",
                methods[i].name, methods[i].signature);
            result = STATUS_FAILED;
            continue;
        }

        err = jvmti->IsMethodSynthetic(mid, &is_synthetic);
        if (err != JVMTI_ERROR_NONE) {
            printf("(IsMethodSynthetic) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            result = STATUS_FAILED;
        }

        if (printdump == JNI_TRUE) {
            printf(">>> %s%s - %s\n", methods[i].name, methods[i].signature,
                   jbooleanToString(is_synthetic));
        }

        if (is_synthetic != methods[i].is_synthetic) {
            printf("(%s%s) wrong is_synthetic value: %s, expected: %s\n",
                methods[i].name, methods[i].signature,
                jbooleanToString(is_synthetic),
                jbooleanToString(methods[i].is_synthetic));
            result = STATUS_FAILED;
        }
    }

    return result;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_IsMethodSynthetic_issynth001a_nmeth(JNIEnv *env, jclass cls) {
}

}
