/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "jvmti.h"
#include "agent_common.h"
#include "jvmti_tools.h"

extern "C" {

/* ============================================================================= */

#define STATUS_FAILED 2
#define PASSED 0

#define KEY_PHRASE      "KEY PHRASE: Agent_OnUnload() was invoked"

static volatile int status = PASSED;

/* ============================================================================= */

/** Check status of JVM_OnUnload() invocation. */
JNIEXPORT jint JNICALL
Java_nsk_jvmti_Agent_1OnUnload_agentonunload001_checkLoadStatus(JNIEnv* jni, jobject obj) {
    return status;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_agentonunload001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_agentonunload001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_agentonunload001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    NSK_DISPLAY0("Agent_OnLoad() is successfully invoked\n");
    return JNI_OK;
}

/** Agent library shutdown. */
JNIEXPORT void JNICALL
#ifdef STATIC_BUILD
Agent_OnUnload_agentonunload001(JavaVM *jvm)
#else
Agent_OnUnload(JavaVM *jvm)
#endif
{
    status = STATUS_FAILED;
    fprintf(stdout, "%s\n", KEY_PHRASE);
    fflush(stdout);
}

/* ============================================================================= */

}
