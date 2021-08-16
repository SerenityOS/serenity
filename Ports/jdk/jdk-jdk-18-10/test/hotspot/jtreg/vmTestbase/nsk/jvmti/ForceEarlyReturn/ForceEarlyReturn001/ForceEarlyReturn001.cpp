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
#include "jvmti.h"
#include <jvmti_tools.h>
#include "JVMTITools.h"
#include "agent_common.h"

extern "C" {

/* ============================================================================= */

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;

/* ============================================================================= */

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_ForceEarlyReturn_ForceEarlyReturn001_suspendThread (
        JNIEnv *env
        , jclass cls
        , jobject earlyReturnThread
        )
{
    if (!NSK_JVMTI_VERIFY(jvmti->SuspendThread(earlyReturnThread)))
         return JNI_FALSE;

    return JNI_TRUE;
}

/* ============================================================================= */

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_ForceEarlyReturn_ForceEarlyReturn001_resumeThread (
        JNIEnv *env
        , jclass klass
        , jobject earlyReturnThread
        )
{
    if (!NSK_JVMTI_VERIFY(jvmti->ResumeThread(earlyReturnThread)))
         return JNI_FALSE;

    return JNI_TRUE;
}

/* ============================================================================= */

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_ForceEarlyReturn_ForceEarlyReturn001_doForceEarlyReturnObject (
        JNIEnv *env
        , jclass klass
        , jthread earlyReturnThread
        , jobject valueToReturn
        )
{
    if (!NSK_JVMTI_VERIFY(jvmti->ForceEarlyReturnObject(earlyReturnThread, valueToReturn)))
        return JNI_FALSE;

    return JNI_TRUE;
}

/* ============================================================================= */

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_ForceEarlyReturn_ForceEarlyReturn001_doForceEarlyReturnInt(
        JNIEnv *env
        , jclass klass
        , jthread earlyReturnThread
        , jint valueToReturn
        )
{
    if (!NSK_JVMTI_VERIFY(jvmti->ForceEarlyReturnInt(earlyReturnThread, valueToReturn)))
        return JNI_FALSE;

    return JNI_TRUE;
}

/* ============================================================================= */

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_ForceEarlyReturn_ForceEarlyReturn001_doForceEarlyReturnLong (
        JNIEnv *env
        , jclass klass
        , jthread earlyReturnThread
        , jlong valueToReturn
        )
{
    if (!NSK_JVMTI_VERIFY(jvmti->ForceEarlyReturnLong(earlyReturnThread, valueToReturn)))
        return JNI_FALSE;

    return JNI_TRUE;
}

/* ============================================================================= */

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_ForceEarlyReturn_ForceEarlyReturn001_doForceEarlyReturnFloat (
        JNIEnv *env
        , jclass klass
        , jthread earlyReturnThread
        , jfloat valueToReturn
        )
{
    if (!NSK_JVMTI_VERIFY(jvmti->ForceEarlyReturnFloat(earlyReturnThread, valueToReturn)))
        return JNI_FALSE;

    return JNI_TRUE;
}

/* ============================================================================= */

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_ForceEarlyReturn_ForceEarlyReturn001_doForceEarlyReturnDouble (
        JNIEnv *env
        , jclass klass
        , jthread earlyReturnThread
        , jdouble valueToReturn
        )
{
    if (!NSK_JVMTI_VERIFY(jvmti->ForceEarlyReturnDouble(earlyReturnThread, valueToReturn)))
        return JNI_FALSE;

    return JNI_TRUE;
}

/* ============================================================================= */

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_ForceEarlyReturn_ForceEarlyReturn001_doForceEarlyReturnVoid (
        JNIEnv *env
        , jclass klass
        , jthread earlyReturnThread
        )
{
    if (!NSK_JVMTI_VERIFY(jvmti->ForceEarlyReturnVoid(earlyReturnThread)))
        return JNI_FALSE;

    return JNI_TRUE;
}

/* ============================================================================= */

/* Agent initialization procedure */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ForceEarlyReturn001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ForceEarlyReturn001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ForceEarlyReturn001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *vm, char *options, void *reserved)
{
    jvmtiCapabilities caps;

    if (!NSK_VERIFY((jvmti = nsk_jvmti_createJVMTIEnv(vm, reserved)) != NULL))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    // Register all necessary JVM capabilities
    caps.can_force_early_return = 1;
    caps.can_suspend = 1;

    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    return JNI_OK;
}

}
