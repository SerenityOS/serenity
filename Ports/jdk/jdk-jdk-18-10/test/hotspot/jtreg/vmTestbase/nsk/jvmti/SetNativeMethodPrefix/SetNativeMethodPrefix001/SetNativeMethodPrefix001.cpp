/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "jni_tools.h"

extern "C" {

#define FOO 1
#define WRAPPED_FOO 2

/* ============================================================================= */

JNIEXPORT jint JNICALL
Java_nsk_jvmti_SetNativeMethodPrefix_AutomaticResolution1_foo (
        JNIEnv *jni
        , jclass klass
    )
{
    NSK_DISPLAY0(" >>> AutomaticResolution1.foo()\n");
    return FOO;
}

/* ============================================================================= */

JNIEXPORT jint JNICALL
Java_nsk_jvmti_SetNativeMethodPrefix_AutomaticResolution1_wrapped_1foo (
        JNIEnv *jni
        , jclass klass
    )
{
    NSK_DISPLAY0(" >>> AutomaticResolution1.wrapped_foo()\n");
    return WRAPPED_FOO;
}

/* ============================================================================= */

JNIEXPORT jint JNICALL
Java_nsk_jvmti_SetNativeMethodPrefix_AutomaticResolution2_foo (
        JNIEnv *jni
        , jclass klass
    )
{
    NSK_DISPLAY0(" >>> AutomaticResolution2.foo()\n");
    return FOO;
}

/* ============================================================================= */

JNIEXPORT jint JNICALL
Java_nsk_jvmti_SetNativeMethodPrefix_AutomaticResolution3_foo (
        JNIEnv *jni
        , jclass klass
    )
{
    NSK_DISPLAY0(" >>> AutomaticResolution3.foo()\n");
    return FOO;
}
/* ============================================================================= */

JNIEXPORT jint JNICALL foo (JNIEnv *jni, jclass klass)
{
    NSK_DISPLAY0(" >>> ::foo()\n");
    return FOO;
}

/* ============================================================================= */

JNIEXPORT jint JNICALL wrapped_foo (JNIEnv *jni, jclass klass)
{
    NSK_DISPLAY0(" >>> ::wrapped_foo()\n");
    return WRAPPED_FOO;
}

/* ============================================================================= */

#define METHODS_COUNT 2
static const void *METHODS [METHODS_COUNT] = {
    (void *) &foo,
    (void *) &wrapped_foo,
};

/* ============================================================================= */

static jvmtiEnv *jvmti = NULL;

/* ============================================================================= */

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_SetNativeMethodPrefix_Binder_setMethodPrefix (
        JNIEnv *jni
        , jclass klass
        , jstring prefix
        )
{
    jboolean result = JNI_TRUE;
    char *str = NULL;

    if (prefix != NULL) {
        if (!NSK_VERIFY((str = (char *) jni->GetStringUTFChars(prefix, 0)) != NULL))
        { result = JNI_FALSE; goto finally; }
    }

    if (!NSK_JVMTI_VERIFY(jvmti->SetNativeMethodPrefix(str)))
    { result = JNI_FALSE; goto finally; }

    if (str != NULL) {
        NSK_DISPLAY1("New PREFIX is set: %s\n"
                , str
                );
    } else {
        NSK_DISPLAY0("Old PREFIX is reset\n");
    }

finally:
    if (str != NULL) {
        jni->ReleaseStringUTFChars(prefix, str);
    }

    return JNI_TRUE;
}

/* ============================================================================= */

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_SetNativeMethodPrefix_Binder_setMultiplePrefixes (
        JNIEnv *jni
        , jclass klass
        , jstring prefix
        )
{
    jboolean result = JNI_TRUE;
    char *str = NULL;

    if (prefix != NULL) {
        if (!NSK_VERIFY((str = (char *) jni->GetStringUTFChars(prefix, 0)) != NULL))
        { result = JNI_FALSE; goto finally; }

        if (!NSK_JVMTI_VERIFY(jvmti->SetNativeMethodPrefixes(1, (char **) &str)))
        { result = JNI_FALSE; goto finally; }

        NSK_DISPLAY1("MultiplePrefixes: New PREFIX is set: %s\n"
                , str
                );
    } else {
        char* prefixes[1];
        prefixes[0] = NULL;

        if (!NSK_JVMTI_VERIFY(jvmti->SetNativeMethodPrefixes(0, (char **)&prefixes)))
        { result = JNI_FALSE; goto finally; }

        NSK_DISPLAY0("Old PREFIX is reset\n");
    }

finally:
    if (str != NULL) {
        jni->ReleaseStringUTFChars(prefix, str);
    }

    return JNI_TRUE;
}

/* ============================================================================= */

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_SetNativeMethodPrefix_Binder_registerMethod (
        JNIEnv *jni
        , jclass klass
        , jclass bound_klass
        , jstring method_name_obj
        , jstring method_sig_obj
        , jint native_method_number
        )
{
    JNINativeMethod method;
    jboolean result = JNI_FALSE;

    if (native_method_number < 0 || native_method_number >= METHODS_COUNT) {
        NSK_DISPLAY2("Method index is out of the bound: %d of %d"
                , native_method_number
                , METHODS_COUNT
                );
        return JNI_FALSE;
    }

    if (!NSK_VERIFY((method.name = (char *) jni->GetStringUTFChars(method_name_obj, 0)) != NULL)) {
        goto finally;
    }

    if (!NSK_VERIFY((method.signature = (char *) jni->GetStringUTFChars(method_sig_obj, 0)) != NULL)) {
        goto finally;
    }

    method.fnPtr = (void *) METHODS[native_method_number];

    NSK_DISPLAY2(">>>> Register native method: %s %s\n"
            , method.name
            , method.signature
            );

    if (jni->RegisterNatives(bound_klass, (const JNINativeMethod*) &method, 1) != 0)
    {
        if (jni->ExceptionOccurred() != NULL) {
            jni->ExceptionClear();
        }

        goto finally;
    }

    NSK_DISPLAY0("<<<< Finished native method registration\n");

    result = JNI_TRUE;
finally:
    if (method.name != NULL) {
        jni->ReleaseStringUTFChars(method_name_obj, method.name);
    }

    if (method.signature != NULL) {
        jni->ReleaseStringUTFChars(method_sig_obj, method.signature);
    }

    return result;
}

/* ============================================================================= */

/* Agent initialization procedure */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_SetNativeMethodPrefix001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_SetNativeMethodPrefix001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_SetNativeMethodPrefix001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *vm, char *options, void *reserved)
{
    jvmtiCapabilities caps;

    if (!NSK_VERIFY(
                nsk_jvmti_parseOptions(options)
                )
       )
        return JNI_ERR;

    if (!NSK_VERIFY(
                (jvmti = nsk_jvmti_createJVMTIEnv(vm, reserved)) != NULL
                )
       )
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    // Register all necessary JVM capabilities
    caps.can_set_native_method_prefix = 1;

    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    return JNI_OK;
}

}
