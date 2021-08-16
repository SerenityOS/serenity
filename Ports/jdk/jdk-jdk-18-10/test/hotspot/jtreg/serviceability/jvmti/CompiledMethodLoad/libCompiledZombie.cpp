/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "jvmti.h"
#include "jni.h"

#ifdef __cplusplus
extern "C" {
#endif

static int events;
static int total_events = 0;

void JNICALL CompiledMethodLoad(jvmtiEnv* jvmti, jmethodID method,
                                jint code_size, const void* code_addr,
                                jint map_length, const jvmtiAddrLocationMap* map,
                                const void* compile_info) {
    events++;
    total_events++;
}

// Continuously generate CompiledMethodLoad events for all currently compiled methods
void JNICALL GenerateEventsThread(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_LOAD, NULL);
    int count = 0;

    while (true) {
        events = 0;
        jvmti->GenerateEvents(JVMTI_EVENT_COMPILED_METHOD_LOAD);
        if (events != 0 && ++count == 200) {
            printf("Generated %d events\n", events);
            count = 0;
        }
    }
}

// As soon as VM starts, run a separate Agent thread that will generate CompiledMethodLoad events
void JNICALL VMInit(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread) {
    jclass thread_class = jni->FindClass("java/lang/Thread");
    jmethodID thread_constructor = jni->GetMethodID(thread_class, "<init>", "()V");
    jthread agent_thread = jni->NewObject(thread_class, thread_constructor);

    jvmti->RunAgentThread(agent_thread, GenerateEventsThread, NULL, JVMTI_THREAD_NORM_PRIORITY);
}

JNIEXPORT
jint JNICALL Agent_OnLoad(JavaVM* vm, char* options, void* reserved) {
    jvmtiEnv* jvmti;
    vm->GetEnv((void**)&jvmti, JVMTI_VERSION_1_0);

    jvmtiCapabilities capabilities;
    memset(&capabilities, 0, sizeof(capabilities));

    capabilities.can_generate_compiled_method_load_events = 1;
    jvmti->AddCapabilities(&capabilities);

    jvmtiEventCallbacks callbacks;
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.VMInit = VMInit;
    callbacks.CompiledMethodLoad = CompiledMethodLoad;
    jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);

    return 0;
}

#ifdef __cplusplus
}
#endif

