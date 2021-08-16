/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <string.h>
#include <jvmti.h>

static jvmtiEnv* jvmti = NULL;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT
void JNICALL Java_DynamicCodeGeneratedTest_changeEventNotificationMode(JNIEnv* jni, jclass cls) {
  while (true) {
    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_DYNAMIC_CODE_GENERATED, NULL);
    jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_DYNAMIC_CODE_GENERATED, NULL);
  }
}

#ifdef __cplusplus
}
#endif

void JNICALL DynamicCodeGenerated(jvmtiEnv* jvmti, const char* name, const void* address, jint length) {

}

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM* vm, char* options, void* reserved) {
    vm->GetEnv((void**)&jvmti, JVMTI_VERSION_1_0);
    jvmtiEventCallbacks callbacks;
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.DynamicCodeGenerated = DynamicCodeGenerated;
    jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));

    return 0;
}
