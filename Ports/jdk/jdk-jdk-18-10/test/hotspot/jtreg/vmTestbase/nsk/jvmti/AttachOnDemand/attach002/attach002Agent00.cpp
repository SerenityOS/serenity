/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *  - receive ClassFileLoadHook event for class 'ClassToRedefine'
 *  - receive ClassLoad event for class 'ClassToRedefine' and redefine class from ClassLoad event handler
 *  - receive one more ClassFileLoadHook event for class 'ClassToRedefine'
 *  - receive ClassPrepare event for class 'ClassToRedefine' and finish work
 */

#define REDEFINED_CLASS_NAME "Lnsk/jvmti/AttachOnDemand/attach002/ClassToRedefine;"
#define REDEFINED_CLASS_FILE_NAME "nsk/jvmti/AttachOnDemand/attach002/ClassToRedefine"

// class name in the ClassFileLoadHook callback
#define REDEFINED_CLASS_NAME_INTERNAL "nsk/jvmti/AttachOnDemand/attach002/ClassToRedefine"

static Options* options = NULL;
static const char* agentName;

static volatile jboolean agentGotCapabilities = JNI_FALSE;

static jvmtiEvent testEvents[] = {
        JVMTI_EVENT_CLASS_LOAD,
        JVMTI_EVENT_CLASS_PREPARE,
        JVMTI_EVENT_CLASS_FILE_LOAD_HOOK };

static const int testEventsNumber = 3;

static volatile int classLoadReceived = 0;
static volatile int classFileLoadHookReceived = 0;

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_AttachOnDemand_attach002_attach002Target_agentGotCapabilities(JNIEnv * jni,
        jclass klass, jobject obj) {
    return agentGotCapabilities;
}

#define ATTACH002_TARGET_APP_CLASS_NAME "nsk/jvmti/AttachOnDemand/attach002/attach002Target"

void registerNativeMethods(JNIEnv* jni_env) {
    ExceptionCheckingJniEnvPtr ec_jni(jni_env);
    jclass appClass;
    JNINativeMethod nativeMethods[] = {
            { (char*) "agentGotCapabilities", (char*) "()Z", (void*) Java_nsk_jvmti_AttachOnDemand_attach002_attach002Target_agentGotCapabilities } };
    jint nativeMethodsNumber = 1;

    appClass = ec_jni->FindClass(ATTACH002_TARGET_APP_CLASS_NAME, TRACE_JNI_CALL);
    ec_jni->RegisterNatives(appClass, nativeMethods, nativeMethodsNumber, TRACE_JNI_CALL);
}

void JNICALL  classLoadHandler(
        jvmtiEnv *jvmti,
        JNIEnv* jni,
        jthread thread,
        jclass klass) {
    char className[MAX_STRING_LENGTH];

    if (!nsk_jvmti_aod_getClassName(jvmti, klass, className)) {
        nsk_jvmti_aod_disableEventsAndFinish(agentName, testEvents, testEventsNumber, 0, jvmti, jni);
        return;
    }

    NSK_DISPLAY2("%s: ClassLoad event was received for class '%s'\n", agentName, className);

    if (!strcmp(className, REDEFINED_CLASS_NAME)) {

        classLoadReceived = 1;

        NSK_DISPLAY1("%s: redefining class\n", agentName);

        if (!NSK_VERIFY(nsk_jvmti_aod_redefineClass(options, jvmti, klass, REDEFINED_CLASS_FILE_NAME))) {
            NSK_COMPLAIN1("%s: failed to redefine class\n", agentName);
            nsk_jvmti_aod_disableEventsAndFinish(agentName, testEvents, testEventsNumber, 0, jvmti, jni);
        }
    }
}

void JNICALL classPrepareHandler(
        jvmtiEnv *jvmti,
        JNIEnv* jni,
        jthread thread,
        jclass klass) {
    char className[MAX_STRING_LENGTH];

    if (!nsk_jvmti_aod_getClassName(jvmti, klass, className)) {
        nsk_jvmti_aod_disableEventsAndFinish(agentName, testEvents, testEventsNumber, 0, jvmti, jni);
        return;
    }

    NSK_DISPLAY2("%s: ClassPrepare event received for class '%s'\n", agentName, REDEFINED_CLASS_NAME);

    if (!strcmp(className, REDEFINED_CLASS_NAME)) {
        int success = 1;

        if (!classLoadReceived) {
            success = 0;
            NSK_COMPLAIN2("%s: expected ClassLoad event wasn't received for class '%s'\n", agentName, REDEFINED_CLASS_NAME);
        }

        /*
         * ClassFileLoadHook event should be received twice - when class is loaded and when class is redefined
         */
        if (classFileLoadHookReceived != 2) {
            success = 0;
            NSK_COMPLAIN2("%s: expected ClassFileLoadHook event wasn't received for class '%s'\n", agentName, REDEFINED_CLASS_NAME);
        }

        nsk_jvmti_aod_disableEventsAndFinish(agentName, testEvents, testEventsNumber, success, jvmti, jni);
    }
}

void JNICALL classFileLoadHoockHandler(
        jvmtiEnv * jvmti,
        JNIEnv * jni,
        jclass class_beeing_redefined,
        jobject loader,
        const char * name,
        jobject protection_domain,
        jint class_data_len,
        const unsigned char * class_data,
        jint * new_class_data_len,
        unsigned char** new_class_data) {

    if (name != NULL) {
        NSK_DISPLAY2("%s: ClassFileLoadHook event received for class '%s'\n", agentName, name);
        if (!strcmp(name, REDEFINED_CLASS_NAME_INTERNAL)) {
            classFileLoadHookReceived++;
        }
    } else {
        NSK_DISPLAY1("%s: ClassFileLoadHook event received for class with NULL name\n", agentName);
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNI_OnLoad_attach002Agent00(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif

JNIEXPORT jint JNICALL
#ifdef STATIC_BUILD
Agent_OnAttach_attach002Agent00(JavaVM *vm, char *optionsString, void *reserved)
#else
Agent_OnAttach(JavaVM *vm, char *optionsString, void *reserved)
#endif
{
    jvmtiEventCallbacks eventCallbacks;
    jvmtiCapabilities caps;
    jvmtiEnv* jvmti = NULL;
    JNIEnv* jni = NULL;

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

    registerNativeMethods(jni);

    memset(&caps, 0, sizeof(caps));
    caps.can_generate_all_class_hook_events = 1;
    caps.can_redefine_classes = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
        /*
         * If VM is run with -Xshare:on agent can't get required capabilities (see 6718407)
         */
        NSK_DISPLAY1("%s: warning: agent failed to get required capabilities, agent finishing\n", agentName);

        if (!NSK_VERIFY(nsk_aod_agentLoaded(jni, agentName)))
            return JNI_ERR;

        nsk_aod_agentFinished(jni, agentName, 1);
    } else {
        agentGotCapabilities = JNI_TRUE;

        memset(&eventCallbacks,0, sizeof(eventCallbacks));
        eventCallbacks.ClassLoad = classLoadHandler;
        eventCallbacks.ClassPrepare = classPrepareHandler;
        eventCallbacks.ClassFileLoadHook = classFileLoadHoockHandler;
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks)))) {
            return JNI_ERR;
        }

        if (!(nsk_jvmti_aod_enableEvents(jvmti, testEvents, testEventsNumber))) {
            return JNI_ERR;
        }

        NSK_DISPLAY1("%s: initialization was done\n", agentName);

        if (!NSK_VERIFY(nsk_aod_agentLoaded(jni, agentName)))
            return JNI_ERR;
    }

    return JNI_OK;
}

}
