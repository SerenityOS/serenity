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
#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include <jvmti.h>
#include <aod.h>
#include <jvmti_aod.h>

extern "C" {

/*
 * In this test the same agent library is attached 3 times, but with
 * different options. In such scenario functions nsk_jvmti_aod_addMultiagentsOptions and
 * nsk_jvmti_aod_getMultiagentsOptions are used.
 *
 * Each agent from ClassLoad event handler tries to redefine class ClassToRedefine and
 * finishes work.
 */

#define REDEFINED_CLASS_NAME "Lnsk/jvmti/AttachOnDemand/attach046/ClassToRedefine;"
#define REDEFINED_CLASS_FILE_NAME "nsk/jvmti/AttachOnDemand/attach046/ClassToRedefine"

void JNICALL  classLoadHandler(
        jvmtiEnv *jvmti,
        JNIEnv* jni,
        jthread thread,
        jclass klass) {
    char className[MAX_STRING_LENGTH];
    const char* agentName;
    Options* options;

    options = nsk_jvmti_aod_getMultiagentsOptions(jvmti);
    if (!NSK_VERIFY(options != NULL)) {
        NSK_COMPLAIN0("Failed to get agents's options\n");
        nsk_jvmti_aod_disableEvent(jvmti, JVMTI_EVENT_CLASS_LOAD);
        // can't call nsk_aod_agentFinished because of without options can't get agent's name
        return;
    }

    agentName = nsk_aod_getOptionValue(options, NSK_AOD_AGENT_NAME_OPTION);

    if (!nsk_jvmti_aod_getClassName(jvmti, klass, className)) {
        nsk_jvmti_aod_disableEventAndFinish(agentName, JVMTI_EVENT_CLASS_LOAD, 0, jvmti, jni);
        return;
    }

    NSK_DISPLAY2("%s: ClassLoad event was received for class '%s'\n", agentName, className);

    if (!strcmp(className, REDEFINED_CLASS_NAME)) {

        NSK_DISPLAY1("%s: redefining class\n", agentName);

        if (!NSK_VERIFY(nsk_jvmti_aod_redefineClass(options, jvmti, klass, REDEFINED_CLASS_FILE_NAME))) {
            NSK_COMPLAIN1("%s: failed to redefine class\n", agentName);
            nsk_jvmti_aod_disableEventAndFinish(agentName, JVMTI_EVENT_CLASS_LOAD, 0, jvmti, jni);
        } else {
            nsk_jvmti_aod_disableEventAndFinish(agentName, JVMTI_EVENT_CLASS_LOAD, 1, jvmti, jni);
        }
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNI_OnLoad_attach046Agent00(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif

JNIEXPORT jint JNICALL
#ifdef STATIC_BUILD
Agent_OnAttach_attach046Agent00(JavaVM *vm, char *optionsString, void *reserved)
#else
Agent_OnAttach(JavaVM *vm, char *optionsString, void *reserved)
#endif
{
    jvmtiEventCallbacks eventCallbacks;
    jvmtiCapabilities caps;
    jvmtiEnv* jvmti = NULL;
    JNIEnv* jni = NULL;
    Options* options;
    const char* agentName;

    options = (Options*) nsk_aod_createOptions(optionsString);
    if (!NSK_VERIFY(options != NULL))
        return JNI_ERR;

    agentName = nsk_aod_getOptionValue(options, NSK_AOD_AGENT_NAME_OPTION);

    jni = (JNIEnv*) nsk_aod_createJNIEnv(vm);
    if (jni == NULL)
        return NSK_FALSE;

    jvmti = nsk_jvmti_createJVMTIEnv(vm, reserved);
    if (!NSK_VERIFY(jvmti != NULL))
        return JNI_ERR;

    memset(&caps, 0, sizeof(caps));
    caps.can_redefine_classes = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
        return JNI_ERR;
    }

    memset(&eventCallbacks,0, sizeof(eventCallbacks));
    eventCallbacks.ClassLoad = classLoadHandler;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks)))) {
        return JNI_ERR;
    }

    if (!(nsk_jvmti_aod_enableEvent(jvmti, JVMTI_EVENT_CLASS_LOAD))) {
        return JNI_ERR;
    }

    if (!NSK_VERIFY(nsk_jvmti_aod_addMultiagentsOptions(jvmti, options))) {
        return JNI_ERR;
    }

    NSK_DISPLAY1("%s: initialization was done\n", agentName);

    if (!NSK_VERIFY(nsk_aod_agentLoaded(jni, agentName)))
        return JNI_ERR;

    return JNI_OK;
}

}
