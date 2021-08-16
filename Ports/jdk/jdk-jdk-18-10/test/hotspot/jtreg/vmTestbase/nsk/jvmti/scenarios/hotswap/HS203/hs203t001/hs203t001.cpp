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
#include <string.h>
#include <jvmti.h>
#include "agent_common.h"
#include <jni.h>
#include "jvmti_tools.h"
#include "JVMTITools.h"
/*
    README
        ******
        HS203: Hotswap + pop frame within events
        T001:
        1. Set a breakpoint.
        2. Upon reaching the breakpoint, enable SingleStep.
        3. Redefine a class within SingleStep callback. Stepping should
        be continued in obsolete method.
        4. Pop a currently executed frame. Stepping should be continued
        on invoke instruction.
*/
extern "C" {

#define FILE_NAME "nsk/jvmti/scenarios/hotswap/HS203/hs203t001/MyThread"

#define SEARCH_NAME "nsk/jvmti/scenarios/hotswap/HS203/hs203t001/MyThread"
#define CLASS_NAME "Lnsk/jvmti/scenarios/hotswap/HS203/hs203t001/MyThread;"
#define METHOD_NAME "doThisFunction"
#define METHOD_SIGN "()V"

static jint redefineNumber;
static jvmtiEnv * jvmti;

JNIEXPORT void JNICALL
    callbackClassLoad(jvmtiEnv *jvmti,
            JNIEnv* jni,
            jthread thread,
            jclass klass) {
        char * className;
        char * generic;
        redefineNumber=0;
        jvmti->GetClassSignature(klass, &className, &generic);
        if (strcmp(className,CLASS_NAME) == 0) {
            jmethodID method;
            method = jni->GetMethodID(klass,METHOD_NAME,METHOD_SIGN);
            if (method != NULL) {
                jlocation start;
                jlocation end;
                jvmtiError err ;
                err=jvmti->GetMethodLocation(method, &start, &end);
                if (err == JVMTI_ERROR_NONE) {
                    nsk_printf("Agent:: NO ERRORS FOUND \n");
                    err= jvmti->SetBreakpoint(method, start);
                    if (err == JVMTI_ERROR_NONE) {
                        nsk_printf(" Class Name %s \n", className);
                        nsk_printf("Agent:: Breakpoint set \n");
                    } else {
                        nsk_printf(" ## Error occured %s \n",TranslateError(err));
                    }
                } else {
                    nsk_printf("Agent:: ***ERROR OCCURED .. in METHOD LOCATION FINDER \n");
                }
            } else {
                nsk_printf("Agent:: ***ERROR OCCURED .. COUND NOT FIND THE METHOD AND SIGNATURE SPECIFIED \n");
            }
        }
    }

void JNICALL callbackSingleStep(jvmtiEnv *jvmti, JNIEnv* jni,
        jthread thread,
        jmethodID method,
        jlocation location) {
    jclass threadClass;
    jvmtiError err;
    char fileName[512];
    threadClass = jni->FindClass(SEARCH_NAME);
    nsk_jvmti_getFileName(redefineNumber, FILE_NAME, fileName,
                    sizeof(fileName)/sizeof(char));
    nsk_printf(" %d..",redefineNumber);
    if (nsk_jvmti_redefineClass(jvmti, threadClass, fileName)) {
        nsk_printf("\nMyClass :: Successfully redefined..\n");
    } else {
        nsk_printf("\nMyClass :: Failed to redefine ..\n");
    }
    nsk_printf(" End of REDEFINE CLASS LOADER \n");
    err=jvmti->SuspendThread(thread);
    if (err == JVMTI_ERROR_NONE) {
        nsk_printf("Agent:: Succeded in suspending..\n");
    } else {
        nsk_printf(" ## Error occured %s \n",TranslateError(err));
    }
}

void JNICALL
callbackBreakpoint(jvmtiEnv *jvmti,
        JNIEnv* jni,
        jthread thread,
        jmethodID method,
        jlocation location) {
    nsk_printf("Agent::... BreakPoint Reached..\n");
    if (nsk_jvmti_enableNotification(jvmti,JVMTI_EVENT_SINGLE_STEP,thread)) {
        nsk_printf(" ....   Enabled..\n");
    }
    return;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_hs203t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_hs203t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_hs203t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *vm, char *options, void *reserved) {
    jint rc ;
    nsk_printf("Agent:: VM.. Started..\n");
    rc=vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_1);
    if (rc != JNI_OK) {
        nsk_printf("Agent:: Could not load JVMTI interface \n");
        return JNI_ERR;
    } else {
        jvmtiCapabilities caps;
        jvmtiEventCallbacks eventCallbacks;
        if (!nsk_jvmti_parseOptions(options)) {
            nsk_printf("# error agent Failed to parse options \n");
            return JNI_ERR;
        }
        memset(&caps, 0, sizeof(caps));
        caps.can_redefine_classes = 1;
        caps.can_suspend=1;
        caps.can_pop_frame=1;
        caps.can_generate_breakpoint_events=1;
        caps.can_generate_all_class_hook_events=1;
        caps.can_generate_single_step_events=1;
        jvmti->AddCapabilities(&caps);
        memset(&eventCallbacks, 0, sizeof(eventCallbacks));
        eventCallbacks.ClassLoad =callbackClassLoad;
        eventCallbacks.Breakpoint = callbackBreakpoint;
        eventCallbacks.SingleStep =callbackSingleStep;
        rc=jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks));
        if (rc != JVMTI_ERROR_NONE) {
            nsk_printf(" Agent:: Error occured while setting event call back \n");
            return JNI_ERR;
        }
        if (nsk_jvmti_enableNotification(jvmti,JVMTI_EVENT_CLASS_LOAD, NULL) &&
                nsk_jvmti_enableNotification(jvmti,JVMTI_EVENT_BREAKPOINT,NULL)) {
            nsk_printf("Agent :: NOTIFICATIONS ARE ENABLED \n");
        } else {
            nsk_printf(" Error in Eanableing Notifications..");
        }
    }
    return JNI_OK;
}


JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS203_hs203t001_hs203t001_popThreadFrame(JNIEnv * jni,
        jclass clas,
        jthread thread) {
    jvmtiError err ;
    jboolean retvalue;
    jint state;
    nsk_printf("Agent:: POPING THE FRAME....\n");
    retvalue = JNI_FALSE;
    nsk_printf(" Here ");
    jvmti->GetThreadState(thread, &state);
    nsk_printf(" Here ");
    if (state & JVMTI_THREAD_STATE_SUSPENDED) {
        err = jvmti->PopFrame(thread);
        if (err == JVMTI_ERROR_NONE) {
            nsk_printf("Agent:: NO Errors poped very well ..\n");
            retvalue = JNI_TRUE;
        } else {
            nsk_printf(" Here -3");
            nsk_printf(" ## Error occured %s \n",TranslateError(err));
        }
    } else {
        nsk_printf("Agent:: Thread was not suspened.. check for capabilities, and java method signature ");
    }
    return retvalue;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS203_hs203t001_hs203t001_resumeThread(JNIEnv * jni,
        jclass clas,
        jthread thread) {
    jvmtiError err ;
    jboolean retvalue;
    retvalue = JNI_FALSE;
    err =jvmti->SetEventNotificationMode(JVMTI_DISABLE,JVMTI_EVENT_SINGLE_STEP,thread);
    if (err == JVMTI_ERROR_NONE) {
        nsk_printf(" Agent:: cleared Single Step event");
    } else {
        nsk_printf(" Agent :: Failed to clear Single Step Event");
    }
    err =jvmti->SetEventNotificationMode(JVMTI_DISABLE,JVMTI_EVENT_BREAKPOINT,thread);
    if (err == JVMTI_ERROR_NONE) {
        nsk_printf(" Agent:: cleared Break point event");
    } else {
        nsk_printf(" Agent :: Failed to clear Single Step Event");
    }
    err = jvmti->ResumeThread(thread);
    if (err == JVMTI_ERROR_NONE) {
        nsk_printf(" Agent:: Thread Resumed..");
    } else {
        nsk_printf(" Failed.. to Resume the thread.");
    }
    return retvalue;
}

}
