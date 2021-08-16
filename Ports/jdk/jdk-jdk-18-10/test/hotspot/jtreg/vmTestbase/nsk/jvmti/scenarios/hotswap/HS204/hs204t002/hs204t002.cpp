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
#include <jvmti.h>
#include "agent_common.h"
#include <jni.h>
#include <string.h>
#include "jvmti_tools.h"
#include "JVMTITools.h"
/*
 *1. Enable event ClassPrepare.
 *2. Upon occurrence of ClassPrepare, set a breakpoint in class static
 * initializer.
 *3. Upon reaching the breakpoint, redefine the class and pop
 *a currently executed frame of the static initializer.
*/
extern "C" {
#define FILE_NAME "nsk/jvmti/scenarios/hotswap/HS204/hs204t002/MyThread"

#define SEARCH_NAME "nsk/jvmti/scenarios/hotswap/HS204/hs204t002/MyThread"
#define CLASS_NAME "Lnsk/jvmti/scenarios/hotswap/HS204/hs204t002/MyThread;"
#define METHOD_NAME "<init>"
#define METHOD_SIGNATURE "()V"
static jint redefineNumber;
static jvmtiEnv * jvmti;

JNIEXPORT void JNICALL
callbackClassPrepare(jvmtiEnv *jvmti,
                                  JNIEnv* jni,
                                  jthread thread,
                                  jclass klass) {
    char * className;
    char * generic;
    redefineNumber=0;
    jvmti->GetClassSignature(klass, &className, &generic);
    /* printf("Agent::Class Name %s \n",className); */
    if ((strcmp(className, CLASS_NAME) == 0)) {
        jclass cls;
        cls = jni->FindClass(SEARCH_NAME);
        if (cls == NULL) {
            printf("Agent::CLS is null");
        } else {
            jmethodID method;
            method = jni->GetMethodID(cls, METHOD_NAME,METHOD_SIGNATURE);
            if (method == NULL) {
                printf("Agent::Method is null ");
            } else {
                jlocation start;
                jlocation end;
                jvmtiError err ;
                err=jvmti->GetMethodLocation(method, &start, &end);
                if (err != JVMTI_ERROR_NONE) {
                    printf("Agent::Errors in finding start and end for the method \n");
                } else {
                    printf("Agent Start = %" LL "d and end = %" LL "d \n", start , end);
                    printf("Agent::setting break points..");
                    err= jvmti->SetBreakpoint(method, start+1);
                    if (err == JVMTI_ERROR_DUPLICATE) {
                        printf("Agent::JVMTI_ERROR_DUPLICATE");
                    } else if (err == JVMTI_ERROR_INVALID_METHODID) {
                        printf("Agent::JVMTI_ERROR_INVALID_METHODID ");
                    } else if (err == JVMTI_ERROR_INVALID_LOCATION) {
                        printf("Agent::JVMTI_ERROR_INVALID_LOCATION ");
                    } else if (err == JVMTI_ERROR_NONE) {
                        printf("Agent::NO ERRORS ");
                    } else {
                        printf("Agent::VERY VERY INVALID STATE ");
                    }
                }
            }
        }
    }
    return;
}
void JNICALL callbackBreakpoint(jvmtiEnv *jvmti_env,
        JNIEnv* jni,
        jthread thread,
        jmethodID method,
        jlocation loc) {
    jclass cls;
    char fileName[512];

    nsk_jvmti_getFileName(redefineNumber, FILE_NAME, fileName,
                        sizeof(fileName)/sizeof(char));
    cls = jni->FindClass(SEARCH_NAME);
    printf("Agent::  Break Pont Reached..");
    if (nsk_jvmti_redefineClass(jvmti, cls, fileName)) {
        nsk_printf("\nMyClass :: Successfully redefined..\n");
    } else {
        nsk_printf("\nMyClass :: Failed to redefine ..\n");
    }
}


#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_hs204t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_hs204t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_hs204t002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *vm, char *options, void *reserved) {

    jint rc ;
    printf("Agent:: VM.. Started..\n");
    rc=vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_1);
    if (rc != JNI_OK) {
        printf("Agent:: Could not load JVMTI interface \n");
        return JNI_ERR;
    } else {
        jvmtiCapabilities caps;
        jvmtiEventCallbacks eventCallbacks;
        memset(&caps, 0, sizeof(caps));
        if (!nsk_jvmti_parseOptions(options)) {
            nsk_printf(" NSK Failed to parse..");
            return JNI_ERR;
        }
        /*
           required to set a prepareClassLoad not required call
           and setBreakPoint is required.
           and redefine is required..
         */
        caps.can_redefine_classes = 1;
        caps.can_generate_breakpoint_events=1;
        caps.can_redefine_classes = 1;
        jvmti->AddCapabilities(&caps);
        memset(&eventCallbacks, 0, sizeof(eventCallbacks));
        eventCallbacks.ClassPrepare = callbackClassPrepare;
        eventCallbacks.Breakpoint = callbackBreakpoint;
        rc=jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks));

        if (rc != JVMTI_ERROR_NONE) {
            printf("Agent:: Error occured while setting event call back \n");
            return JNI_ERR;
        }

        nsk_jvmti_enableNotification(jvmti, JVMTI_EVENT_CLASS_PREPARE , NULL);
        nsk_jvmti_enableNotification(jvmti, JVMTI_EVENT_BREAKPOINT, NULL);
    }
    return JNI_OK;
}

}
