/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <jni.h>
#include <jvmti.h>
#include <aod.h>
#include <jvmti_aod.h>
#include "ExceptionCheckingJniEnv.hpp"

extern "C" {

/*
 * Expected agent work scenario:
 * - during initialization agent registers native methods used be target application and enables ObjectFree events
 * - target application using native method and agent's jvmti environment tags object and provokes collection
 * of this object
 * - agent receives ObjectFree event for tagged object
 * - target application using native method calls nsk_aod_agentFinished and agent finishes work
 * (agent can't call nsk_aod_agentFinished from ObjectFree handler, nsk_aod_agentFinished calls
 * JNI functions and it is prohibited in ObjectFree handler)
 *
 */

#define TAG_VALUE (jlong)777
#define ATTACH021_TARGET_APP_CLASS_NAME "nsk/jvmti/AttachOnDemand/attach021/attach021Target"

static jvmtiEnv* jvmti;

static Options* options = NULL;
static const char* agentName;

// agent should set success status from objectFreeHandler
volatile int success = 0;

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_AttachOnDemand_attach021_attach021Target_setTagFor(JNIEnv * jni,
        jclass klass, jobject obj) {
    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(obj, TAG_VALUE))) {
        return JNI_FALSE;
    }

    NSK_DISPLAY2("%s: object is tagged (tag: %ld)\n", agentName, TAG_VALUE);

    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_AttachOnDemand_attach021_attach021Target_shutdownAgent(JNIEnv * jni,
        jclass klass) {

    /* Flush any pending ObjectFree events, which will set global success variable to 1
       for any pending ObjectFree events. */
    if (jvmti->SetEventNotificationMode(JVMTI_DISABLE,
                                        JVMTI_EVENT_OBJECT_FREE,
                                        NULL) != JVMTI_ERROR_NONE) {
        success = 0;
    }

    nsk_aod_agentFinished(jni, agentName, success);
}

void JNICALL objectFreeHandler(jvmtiEnv *jvmti, jlong tag) {
    NSK_DISPLAY2("%s: object free event for object %ld\n", agentName, tag);

    if (tag != TAG_VALUE) {
        success = 0;
        NSK_COMPLAIN2("%s: unexpected tag value, expected is  %ld\n", agentName, TAG_VALUE);
    } else {
        success = 1;
    }

    /*
     * Can't use JNI functions from ObjectFree event handler, in this test target application calls
     * function nsk_aod_agentFinished
     */
}

void registerNativeMethods(JNIEnv* jni_env) {
    ExceptionCheckingJniEnvPtr ec_jni(jni_env);
    jclass appClass;
    JNINativeMethod nativeMethods[] = {
            { (char*) "setTagFor", (char*) "(Ljava/lang/Object;)Z", (void*) Java_nsk_jvmti_AttachOnDemand_attach021_attach021Target_setTagFor },
            { (char*) "shutdownAgent", (char*) "()V", (void*) Java_nsk_jvmti_AttachOnDemand_attach021_attach021Target_shutdownAgent } };
    jint nativeMethodsNumber = 2;

    appClass = ec_jni->FindClass(ATTACH021_TARGET_APP_CLASS_NAME, TRACE_JNI_CALL);
    ec_jni->RegisterNatives(appClass, nativeMethods, nativeMethodsNumber, TRACE_JNI_CALL);
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNI_OnLoad_attach021Agent00(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif

JNIEXPORT jint JNICALL
#ifdef STATIC_BUILD
Agent_OnAttach_attach021Agent00(JavaVM *vm, char *optionsString, void *reserved)
#else
Agent_OnAttach(JavaVM *vm, char *optionsString, void *reserved)
#endif
{
    jvmtiEventCallbacks eventCallbacks;
    jvmtiCapabilities caps;
    JNIEnv* jni;

    options = (Options*) nsk_aod_createOptions(optionsString);
    if (!NSK_VERIFY(options != NULL))
        return JNI_ERR;

    agentName = nsk_aod_getOptionValue(options, NSK_AOD_AGENT_NAME_OPTION);

    jni = (JNIEnv*) nsk_aod_createJNIEnv(vm);
    if (jni == NULL)
        return JNI_ERR;

    jvmti = nsk_jvmti_createJVMTIEnv(vm, reserved);
    if (!NSK_VERIFY(jvmti != NULL))
        return JNI_ERR;

    registerNativeMethods(jni);

    memset(&caps, 0, sizeof(caps));
    caps.can_tag_objects = 1;
    caps.can_generate_object_free_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
        return JNI_ERR;
    }

    memset(&eventCallbacks,0, sizeof(eventCallbacks));
    eventCallbacks.ObjectFree = objectFreeHandler;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks)))) {
        return JNI_ERR;
    }

    if (!(nsk_jvmti_aod_enableEvent(jvmti, JVMTI_EVENT_OBJECT_FREE))) {
        return JNI_ERR;
    }

    NSK_DISPLAY1("%s: initialization was done\n", agentName);

    if (!NSK_VERIFY(nsk_aod_agentLoaded(jni, agentName)))
        return JNI_ERR;

    return JNI_OK;
}

}
