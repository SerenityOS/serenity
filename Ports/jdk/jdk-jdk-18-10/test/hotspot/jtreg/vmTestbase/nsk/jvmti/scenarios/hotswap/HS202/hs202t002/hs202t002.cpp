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
#include <jni.h>
#include <jvmti.h>
#include "agent_common.h"
#include <string.h>
#include <stdarg.h>
#include "jvmti_tools.h"
#include "JVMTITools.h"

/*
hs202t002:
*/
extern "C" {

#define FILE_NAME "nsk/jvmti/scenarios/hotswap/HS202/hs202t002/MyThread"
#define CLASS_NAME "Lnsk/jvmti/scenarios/hotswap/HS202/hs202t002/MyThread;"

#define PATH_FORMAT "%s%02d/%s"
#define DIR_NAME "newclass"
#define METHOD_NAME "display"

static jint redefineNumber = 0;
static jvmtiEnv * jvmti = NULL;

static volatile bool thread_suspend_error = false;

void JNICALL callbackMethodExit(jvmtiEnv *jvmti_env,
                                JNIEnv* jni_env,
                                jthread thread,
                                jmethodID method,
                                jboolean was_popped_by_exception,
                                jvalue return_value) {
    if (was_popped_by_exception) {
        char * name;
        char * signature;
        char * generic ;
        jvmtiError err;
        err= JVMTI_ERROR_NONE;
        jvmti_env->GetMethodName(method, &name, &signature, &generic);
        if (strcmp(name,METHOD_NAME) == 0) {
            jclass cls;
            char fileName[512];
            nsk_jvmti_getFileName(redefineNumber, FILE_NAME, fileName,
                                  sizeof(fileName)/sizeof(char));
            jvmti_env->GetMethodDeclaringClass(method, &cls);
            if (nsk_jvmti_redefineClass(jvmti_env, cls,fileName)) {
                nsk_printf(" Agent:: redefine class success ..\n");
                nsk_printf("Agent::SUSPENDING>> \n");
                err=jvmti_env->SuspendThread(thread);
                if (err == JVMTI_ERROR_NONE) {
                  // we don't get here until we are resumed
                    nsk_printf("Agent:: Thread successfully suspended and was resumed\n");
                } else {
                    thread_suspend_error = true;
                    nsk_printf(" ## Error occured %s \n",TranslateError(err));
                }
            }
        }
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_hs202t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_hs202t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_hs202t002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *vm, char *options, void *reserved) {
    jint rc ;
    nsk_printf("Agent:: VM.. Started..\n");
    redefineNumber=0;
    rc=vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_1);
    if (rc != JNI_OK) {
        nsk_printf("Agent:: Could not load JVMTI interface \n");
        return JNI_ERR;
    } else {
        jvmtiCapabilities caps;
        jvmtiEventCallbacks eventCallbacks;
        memset(&caps, 0, sizeof(caps));
        if (!nsk_jvmti_parseOptions(options)) {
            nsk_printf("# error agent Failed to parse options \n");
            return JNI_ERR;
        }
        caps.can_redefine_classes = 1;
        caps.can_suspend = 1;
        caps.can_pop_frame = 1;
        caps.can_generate_method_exit_events = 1;
        jvmti->AddCapabilities(&caps);
        memset(&eventCallbacks, 0, sizeof(eventCallbacks));
        eventCallbacks.MethodExit = callbackMethodExit;
        rc=jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks));
        if (rc != JVMTI_ERROR_NONE) {
            nsk_printf(" Agent:: Error occured while setting event callbacks \n");
            return JNI_ERR;
        }
        if (nsk_jvmti_enableNotification(jvmti,JVMTI_EVENT_METHOD_EXIT, NULL)) {
            nsk_printf(" Agent :: NOTIFICATIONS ARE ENABLED \n");
        } else {
            nsk_printf(" Agent :: Error Enabling Notifications..");
        }
    }
    return JNI_OK;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS202_hs202t002_hs202t002_popThreadFrame(JNIEnv * jni,
                                                                          jclass clas,
                                                                          jthread thread) {
    jvmtiError err = JVMTI_ERROR_NONE;
    jboolean retvalue = JNI_FALSE;
    jint state;
    nsk_printf("Agent:: POPPING THE FRAME..\n");
    jvmti->GetThreadState(thread, &state);
    if (state & JVMTI_THREAD_STATE_SUSPENDED) {
        err = jvmti->PopFrame(thread);
        if (err == JVMTI_ERROR_NONE) {
            nsk_printf("Agent:: PopFrame succeeded..\n");
            return JNI_TRUE;
        } else  {
            nsk_printf(" ## Error occured %s \n",TranslateError(err));
        }
    } else {
        nsk_printf("Agent:: Thread was not suspened.. check for capabilities, and java method signature ");
    }
    return retvalue;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS202_hs202t002_hs202t002_resumeThread(JNIEnv * jni,
                                                                        jclass clas,
                                                                        jthread thread) {
    jvmtiError err = JVMTI_ERROR_NONE;
    jboolean retvalue = JNI_FALSE;

    // disable notifications before resuming thread
    // to avoid recursion on PopFrame issued reinvoke
    if (nsk_jvmti_disableNotification(jvmti,JVMTI_EVENT_METHOD_EXIT, NULL)) {
        nsk_printf("Agent :: nsk_jvmti_disabled notifications..\n");
    } else {
        nsk_printf("Agent :: Failed to disable notifications..");
        return JNI_FALSE;
    }

    err = jvmti->ResumeThread(thread);
    if (err == JVMTI_ERROR_NONE) {
        retvalue = JNI_TRUE;
        nsk_printf(" Agent:: Thread Resumed.. \n");
    } else {
        nsk_printf(" Agent:: Failed.. to Resume the thread.\n");
        retvalue = JNI_FALSE;
    }
    return retvalue;
}

JNIEXPORT jboolean JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS202_hs202t002_hs202t002_isThreadSuspended(JNIEnv* jni,
                                                                             jclass clas,
                                                                             jthread thread) {
    if (thread_suspend_error) {
        jclass ex_class = jni->FindClass("java/lang/IllegalThreadStateException");
        jni->ThrowNew(ex_class, "Thread has failed to self suspend");
        return JNI_FALSE;
    }

    // There is an inherent race here if the suspend fails for some reason but
    // thread_suspend_error is not yet set. But as long as we report the suspend
    // state correctly there is no problem as the Java code will simply loop and call
    // this again until we see thread_suspend_error is true.

    jint state = 0;
    // No errors possible here: thread is valid, and state is not NULL
    jvmti->GetThreadState(thread, &state);
    return (state & JVMTI_THREAD_STATE_SUSPENDED) != 0;
}

} // extern C
