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
 * Expected agent work scenario:
 *  - receive ClassLoad event for class FirstLoadedClass, from handler for this event
 *  disable ClassLoad events for all threads except thread loading FirstLoadedClass
 * (after these ClassLoad events should come only from this thread)
 *  - receive ClassLoad event for class LastLoadedClass and finish work
 */

static Options* options = NULL;
static const char* agentName;

#define FIRST_LOADED_CLASS  "Lnsk/jvmti/AttachOnDemand/attach009/FirstLoadedClass;"
#define LAST_LOADED_CLASS  "Lnsk/jvmti/AttachOnDemand/attach009/LastLoadedClass;"

static int disabledForOthers = 0;

static volatile int success = 1;

void JNICALL
classLoadHandler(jvmtiEnv *jvmti,
        JNIEnv* jni,
        jthread thread,
        jclass klass) {
    static char mainThreadName[MAX_STRING_LENGTH];
    char loadedClassName[MAX_STRING_LENGTH];
    char threadName[MAX_STRING_LENGTH];

    if (!nsk_jvmti_aod_getThreadName(jvmti, thread, threadName)) {
        nsk_jvmti_aod_disableEventAndFinish(agentName, JVMTI_EVENT_CLASS_LOAD, 0, jvmti, jni);
        return;
    }

    if (!nsk_jvmti_aod_getClassName(jvmti, klass, loadedClassName)) {
        nsk_jvmti_aod_disableEventAndFinish(agentName, JVMTI_EVENT_CLASS_LOAD, 0, jvmti, jni);
        return;
    }

    NSK_DISPLAY2("Class '%s' was loaded by thread '%s'\n", loadedClassName, threadName);

    /*
     * When class FIRST_LOADED_CLASS was loaded try to disable events for all threads
     * except main target application thread
     */
    if (strcmp(loadedClassName, FIRST_LOADED_CLASS) == 0) {
        strcpy(mainThreadName, threadName);

        if (!nsk_jvmti_aod_disableEvent(jvmti, JVMTI_EVENT_CLASS_LOAD)) {
            NSK_COMPLAIN0("Failed to disable events\n");
            nsk_aod_agentFinished(jni, agentName, 0);
            return;
        }

        if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_LOAD, thread))) {
            NSK_COMPLAIN1("Failed to enable events for thread '%s'\n", mainThreadName);
            nsk_aod_agentFinished(jni, agentName, 0);
            return;
        }

        NSK_DISPLAY1("ClassLoad events are enabled only for thread '%s'", mainThreadName);

        disabledForOthers = 1;

        return;
    }

    if (disabledForOthers) {
        if (strcmp(threadName, mainThreadName) != 0) {
            success = 0;
            NSK_COMPLAIN1("ClassLoad event was erroneously generated for thread '%s'\n", threadName);
        }
    }

    /*
     * Stop agent when LAST_LOADED_CLASS was loaded
     */
    if (strcmp(loadedClassName, LAST_LOADED_CLASS) == 0) {
        nsk_jvmti_aod_disableEventAndFinish(agentName, JVMTI_EVENT_CLASS_LOAD, success, jvmti, jni);
    }
}


#ifdef STATIC_BUILD
JNIEXPORT jint JNI_OnLoad_attach009Agent00(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif

JNIEXPORT jint JNICALL
#ifdef STATIC_BUILD
Agent_OnAttach_attach009Agent00(JavaVM *vm, char *optionsString, void *reserved)
#else
Agent_OnAttach(JavaVM *vm, char *optionsString, void *reserved)
#endif
{
    jvmtiEnv* jvmti;
    jvmtiEventCallbacks eventCallbacks;
    JNIEnv* jni;

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

    memset(&eventCallbacks,0, sizeof(eventCallbacks));
    eventCallbacks.ClassLoad = classLoadHandler;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks)))) {
        return JNI_ERR;
    }

    if (!(nsk_jvmti_aod_enableEvent(jvmti, JVMTI_EVENT_CLASS_LOAD))) {
        return JNI_ERR;
    }

    NSK_DISPLAY1("%s: initialization was done\n", agentName);

    if (!NSK_VERIFY(nsk_aod_agentLoaded(jni, agentName)))
        return JNI_ERR;

    return JNI_OK;
}

}
