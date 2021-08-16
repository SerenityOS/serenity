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

/* ============================================================================= */

#define FOO 1

/* ============================================================================= */

static char *prefix = NULL;

/* ============================================================================= */

static jvmtiEnv *jvmti = NULL;

/* ============================================================================= */

JNIEXPORT int JNICALL
Java_nsk_jvmti_SetNativeMethodPrefix_SetNativeMethodPrefix002_foo (
        JNIEnv *jni
        , jclass klass
    )
{
    NSK_DISPLAY1(" >>> SetNativeMethodPrefix002.foo() (Library: SetNativeMethodPrefix002).\n", prefix);
    return FOO;
}

/* ============================================================================= */

static jboolean setMethodPrefix (char *prefix)
{
    if (!NSK_JVMTI_VERIFY(jvmti->SetNativeMethodPrefix(prefix)))
        return JNI_FALSE;

    return JNI_TRUE;
}

/* ============================================================================= */

/* Agent initialization procedure */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_SetNativeMethodPrefix002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_SetNativeMethodPrefix002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_SetNativeMethodPrefix002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *vm, char *options, void *reserved)
{
    jvmtiCapabilities caps;
    int apply;

    if (!NSK_VERIFY(
                nsk_jvmti_parseOptions(options)
                )
       )
        return JNI_ERR;

    // Parse additional parameters

    // Specify native method prefix
    prefix = (char *)nsk_jvmti_findOptionValue("prefix");
    if (prefix != NULL) {
        NSK_DISPLAY1("Prefix: %s\n", prefix);
    }

    // Specify whether prefix should be applied or not
    apply = nsk_jvmti_findOptionIntValue("apply", 1);

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


    if (apply) {
        if (!setMethodPrefix(prefix)) {
            NSK_COMPLAIN0("Can't specify prefix for native method lookup.");
            return JNI_ERR;
        }
    }

    return JNI_OK;
}

}
