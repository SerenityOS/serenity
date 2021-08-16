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
#include "jvmti_tools.h"
#include "JVMTITools.h"
#include "jni_tools.h"

extern "C" {

#define DIR_NAME "newclass"
#define PATH_FORMAT "%s%02d/%s"
#define FILE_NAME "nsk/jvmti/scenarios/hotswap/HS104/hs104t002/MyThread"
#define SEARCH_NAME "nsk/jvmti/scenarios/hotswap/HS104/hs104t002/MyThread"

static jvmtiEnv * jvmti;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_hs104t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_hs104t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_hs104t002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *vm, char *options, void *reserved) {
    if (!NSK_VERIFY (JNI_OK == vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_1))) {
        nsk_printf("#error Agent :: Could not load JVMTI interface.\n");
        return JNI_ERR;
    } else {
        jvmtiCapabilities caps;
        if (!nsk_jvmti_parseOptions(options)) {
            nsk_printf("# error agent Failed to parse options \n");
            return JNI_ERR;
        }
        memset(&caps, 0, sizeof(caps));
        caps.can_redefine_classes = 1;
        if (!NSK_JVMTI_VERIFY (jvmti->AddCapabilities(&caps))) {
            nsk_printf("#error Agent :: occured while adding capabilities.\n");
            return JNI_ERR;
        }
    }
    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_scenarios_hotswap_HS104_hs104t002_hs104t002_redefineClasses(
    JNIEnv *jni, jclass classObject) {
    jclass cla;
    char fileName[512];

    if (!NSK_JNI_VERIFY(jni, (cla = jni->FindClass(SEARCH_NAME)) != NULL)) {
        nsk_printf(" Agent :: Failed to get class.\n");
        nsk_jvmti_agentFailed();
        return;
    }
    nsk_jvmti_getFileName(0, FILE_NAME, fileName,
            sizeof(fileName)/sizeof(char));
    if (nsk_jvmti_redefineClass(jvmti, cla, fileName)) {
        nsk_printf(" Agent :: Redefine successfull.\n");
    } else {
        nsk_printf("# error Agent :: Redefine failed.\n");
        nsk_jvmti_agentFailed();
    }
}

}
