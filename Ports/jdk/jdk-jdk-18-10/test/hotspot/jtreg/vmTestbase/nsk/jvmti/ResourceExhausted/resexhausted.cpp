/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include "jvmti_tools.h"
#include "agent_common.h"

#define PASSED 0
#define STATUS_FAILED 2

extern "C" {

static jvmtiEnv* gJvmti = NULL;
static volatile jint gEventFlags = 0;

void JNICALL
resourceExhausted(jvmtiEnv *jvmti_env,
                  JNIEnv* jni_env,
                  jint flags,
                  const void* reserved,
                  const char* description)
{
    NSK_DISPLAY1("Agent: ResourceExhausted detected: %s\n", description);
    if (flags & JVMTI_RESOURCE_EXHAUSTED_OOM_ERROR) NSK_DISPLAY0("Agent:    JVMTI_RESOURCE_EXHAUSTED_OOM_ERROR\n");
    if (flags & JVMTI_RESOURCE_EXHAUSTED_JAVA_HEAP) NSK_DISPLAY0("Agent:    JVMTI_RESOURCE_EXHAUSTED_JAVA_HEAP\n");
    if (flags & JVMTI_RESOURCE_EXHAUSTED_THREADS)   NSK_DISPLAY0("Agent:    JVMTI_RESOURCE_EXHAUSTED_THREADS\n");
    gEventFlags = flags;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_ResourceExhausted_Helper_getExhaustedEventFlags(JNIEnv* env, jclass cls)
{
    return gEventFlags;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_ResourceExhausted_Helper_resetExhaustedEvent(JNIEnv* env, jclass cls)
{
    gEventFlags = 0;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_resexhausted(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_resexhausted(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_resexhausted(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *vm, char *options, void *reserved)
{
    jvmtiEventCallbacks callbacks;
    jvmtiCapabilities capabilities;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    if (!NSK_VERIFY((gJvmti = nsk_jvmti_createJVMTIEnv(vm, reserved)) != NULL))
        return JNI_ERR;

    memset(&capabilities, 0, sizeof(jvmtiCapabilities));
    capabilities.can_generate_resource_exhaustion_heap_events = 1;
    capabilities.can_generate_resource_exhaustion_threads_events = 1;
    if (!NSK_JVMTI_VERIFY(gJvmti->AddCapabilities(&capabilities)))
        return JNI_ERR;

    memset((void *)&callbacks, 0, sizeof(jvmtiEventCallbacks));
    callbacks.ResourceExhausted = resourceExhausted;
    if (!NSK_JVMTI_VERIFY(gJvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(gJvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                                           JVMTI_EVENT_RESOURCE_EXHAUSTED,
                                                           NULL)))
        return JNI_ERR;

    return JNI_OK;
}

}
