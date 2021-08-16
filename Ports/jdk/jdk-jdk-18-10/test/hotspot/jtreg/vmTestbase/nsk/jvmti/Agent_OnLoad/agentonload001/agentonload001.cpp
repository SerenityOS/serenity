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

static volatile int status = STATUS_FAILED;

/* ============================================================================= */

/** Check status of JVM_OnLoad() invocation. */
JNIEXPORT jint JNICALL
Java_nsk_jvmti_Agent_1OnLoad_agentonload001_checkLoadStatus(JNIEnv* jni, jobject obj) {
    return status;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_agentonload001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_agentonload001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_agentonload001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    status = PASSED;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    NSK_DISPLAY0("JVM_OnLoad is invoked with parameters:\n");
    NSK_DISPLAY1("    vm:       0x%p\n", (void*)jvm);
    NSK_DISPLAY1("    options:  \"%s\"\n", nsk_null_string(options));
    NSK_DISPLAY1("    reserved: 0x%p\n", (void*)jvm);

    if (jvm == NULL) {
        NSK_COMPLAIN1("First parameter 'vm' in JVM_OnLoad() is NULL: 0x%p\n", (void*)jvm);
        status = STATUS_FAILED;
    }

    if (options == NULL) {
        NSK_COMPLAIN1("Second parameter 'options' in JVM_OnLoad() is NULL: 0x%p\n", (void*)options);
        status = STATUS_FAILED;
    }

    if (status != PASSED) {
        return JNI_ERR;
    }

    return JNI_OK;
}

/* ============================================================================= */

}
