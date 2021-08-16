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
1. Enable event ClassPrepare.
2. Upon ClassPrepare occurrence, enable FieldAccessWatch for a field to be initialized by the initializer.
3. Upon accessing the field by the initializer, redefine the class and pop a currently executed
 frame of the initializer within incoming FieldAccess callback.
*/
extern "C" {

#define METHOD_NAME "loadClass"
#define SIGNATURE "(Ljava/lang/String;)Ljava/lang/Class;"
#define FILE_NAME "nsk/jvmti/scenarios/hotswap/HS204/hs204t004/MyClassLoader"
#define CLASS_LOADER_CLASS_NAME "Lnsk/jvmti/scenarios/hotswap/HS204/hs204t004/MyClassLoader;"
/* for redefine..*/
static jint redefineNumber;
static jvmtiEnv * jvmti;
static jclass cloader;

JNIEXPORT void JNICALL
callbackClassLoad(jvmtiEnv *jvmti_env,
                        JNIEnv* jni,
                        jthread thread,
                        jclass klass) {
    char * className;
    char * generic;
    redefineNumber=0;
    jvmti->GetClassSignature(klass, &className, &generic);
    if (strcmp(className, CLASS_LOADER_CLASS_NAME) == 0) {
        if (klass != NULL) {
            jmethodID method;
            cloader = klass;
            method = jni->GetMethodID(klass,METHOD_NAME,SIGNATURE);
            if (method != NULL) {
                jlocation start;
                jlocation end;
                jvmtiError err ;
                err=jvmti->GetMethodLocation(method, &start, &end);
                if (err == JVMTI_ERROR_NONE) {
                    nsk_printf("Agent:: NO ERRORS FOUND \n");
                    err= jvmti->SetBreakpoint(method, start+1);
                    if (err == JVMTI_ERROR_NONE) {
                        nsk_printf("Agent:: Breakpoint set \n");
                    } else {
                        nsk_printf("Agent:: ***ERROR OCCURED ... in SET BREAK POINT ERROR \n");
                    }
                } else {
                    nsk_printf("Agent:: ***ERROR OCCURED .. in METHOD LOCATION FINDER \n");
                }
            } else {
                nsk_printf("Agent:: ***ERROR OCCURED .. COUND NOT FIND THE METHOD AND SIGNATURE SPECIFIED \n");
            }
        } else {
            nsk_printf("Agent:: ***ERROR OCCURED .. CLASS SPECIFIED WAS NOT FOUND \n");
        }
    }
}

void JNICALL callbackBreakpoint(jvmtiEnv *jvmti_env,
        JNIEnv* jni,
        jthread thread,
        jmethodID method,
        jlocation loc) {
    jvmtiError err ;
    jclass clas;
    char fileName[512];
    clas = jni->FindClass(CLASS_LOADER_CLASS_NAME);
    nsk_printf("Agent::  Break Pont Reached..\n");
    /* Redefine the class loader and then pop the
       frame and resume the thread..*/
    nsk_jvmti_getFileName(redefineNumber, FILE_NAME, fileName, sizeof(fileName)/sizeof(char));
    if (nsk_jvmti_redefineClass(jvmti_env, clas, fileName)) {
        nsk_printf("\nMyClass :: Successfully redefined..\n");
    } else {
        nsk_printf("\nMyClass :: Failed to redefine ..\n");
    }
    nsk_printf(" End of REDEFINE CLASS LOADER \n");
    err=jvmti->SuspendThread(thread);
    if (err == JVMTI_ERROR_NONE) {
        nsk_printf("Agent:: Succeded in suspending..\n");
    } else if (err == JVMTI_ERROR_THREAD_SUSPENDED) {
        nsk_printf("Agent:: JVMTI_ERROR_THREAD_SUSPENDED \n");
    } else if (err == JVMTI_ERROR_INVALID_THREAD) {
        nsk_printf("Agent:: JVMTI_ERROR_INVALID_THREAD \n");
    } else if (err == JVMTI_ERROR_THREAD_NOT_ALIVE) {
        nsk_printf("Agent:: JVMTI_ERROR_THREAD_NOT_ALIVE \n");
    } else {
        nsk_printf(" Else error ");
    }
}


#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_hs204t004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_hs204t004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_hs204t004(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint   Agent_Initialize(JavaVM *vm, char *options, void *reserved) {
      jint rc ;
      nsk_printf("Agent:: VM.. Started..\n");
      rc=vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_1);
      if (rc != JNI_OK) {
          nsk_printf("Agent:: Could not load JVMTI interface \n");
          return JNI_ERR;
      } else {
          jvmtiCapabilities caps;
          jvmtiEventCallbacks eventCallbacks;
          memset(&caps, 0, sizeof(caps));
          if (!nsk_jvmti_parseOptions(options)) {
              nsk_printf(" NSK Failed to parse..");
              return JNI_ERR;
          }
          caps.can_redefine_classes = 1;
          caps.can_suspend=1;
          caps.can_pop_frame=1;
          caps.can_generate_breakpoint_events=1;
          caps.can_generate_all_class_hook_events=1;
          jvmti->AddCapabilities(&caps);
          memset(&eventCallbacks, 0, sizeof(eventCallbacks));
          eventCallbacks.ClassLoad =callbackClassLoad;
          eventCallbacks.Breakpoint = callbackBreakpoint;
          rc=jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks));
          if (rc != JVMTI_ERROR_NONE) {
              nsk_printf(" Agent:: Error occured while setting event call back \n");
              return JNI_ERR;
          }
          nsk_jvmti_enableNotification(jvmti,JVMTI_EVENT_CLASS_LOAD, NULL);
          nsk_jvmti_enableNotification(jvmti,JVMTI_EVENT_BREAKPOINT, NULL);
      }
      return JNI_OK;
  }

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS204_hs204t004_hs204t004_popFrame(JNIEnv * jni,
        jclass clas,
        jthread thread) {
    jvmtiError err ;
    jboolean retvalue;
    jint state;
    nsk_printf("Agent:: POPING THE FRAME....\n");
    retvalue = JNI_FALSE;
    jvmti->GetThreadState(thread, &state);
    if (state & JVMTI_THREAD_STATE_IN_NATIVE) nsk_printf("JVMTI_THREAD_STATE_IN_NATIVE");
    if (state & JVMTI_THREAD_STATE_INTERRUPTED) nsk_printf("JVMTI_THREAD_STATE_INTERRUPTED");
    if (state & JVMTI_THREAD_STATE_WAITING) nsk_printf(" JVMTI_THREAD_STATE_WAITING");
    if (state & JVMTI_THREAD_STATE_SUSPENDED) {
        nsk_printf("Agent:: Thread state .. JVMTI_THREAD_STATE_SUSPENDED \n");
        err = jvmti->PopFrame(thread);
        if (err == JVMTI_ERROR_NONE) {
            nsk_printf("Agent:: NO Errors poped very well ..\n");
            err = jvmti->ResumeThread(thread);
            if (err == JVMTI_ERROR_NONE) {
                retvalue = JNI_TRUE;
            } else {
                nsk_printf("Agent:: Error occured in resuming a thread..\n");
            }
        } else {
            nsk_printf("Agent:: some other error ..%s \n",TranslateError(err));
        }
    } else {
        nsk_printf("Agent:: Thread is not suspended ..\n");
    }
    return retvalue;
}


}
