/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <stdarg.h>
#include <stdlib.h>
#include <string.h>

#include <jvmti.h>
#include "agent_common.h"

#include "nsk_tools.h"
#include "JVMTITools.h"
#include "jvmti_tools.h"

extern "C" {

#define PASSED  0
#define STATUS_FAILED  2

/* classes which must have the class load event */
static const char *expSigs[] = {
    "Lnsk/jvmti/ClassLoad/classload001;",
    "Lnsk/jvmti/ClassLoad/classload001$TestedClass;"
};
#define EXP_SIG_NUM (sizeof(expSigs)/sizeof(char*))

/* classes which must not have the class load event */
static const char *unexpSigs[] = {
    "Z", /* boolean */
    "B", /* byte */
    "C", /* char */
    "D", /* double */
    "F", /* float */
    "I", /* integer */
    "J", /* long */
    "S", /* short */

    "[Z", /* boolean array */
    "[B", /* byte array */
    "[C", /* char array */
    "[D", /* double array */
    "[F", /* float array */
    "[I", /* integer array */
    "[J", /* long array */
    "[S", /* short array */
    "[Lnsk/jvmti/ClassLoad/classload001$TestedClass;"
};
#define UNEXP_SIG_NUM (sizeof(unexpSigs)/sizeof(char*))

static volatile int clsEvents[EXP_SIG_NUM];
static volatile int primClsEvents[UNEXP_SIG_NUM];

static jint result = PASSED;
static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;
static jrawMonitorID countLock;

static void initCounters() {
    size_t i;

    for (i=0; i<EXP_SIG_NUM; i++)
        clsEvents[i] = 0;

    for (i=0; i<UNEXP_SIG_NUM; i++)
        primClsEvents[i] = 0;
}

static int findSig(char *sig, int expected) {
    unsigned int i;

    for (i=0; i<((expected == 1) ? EXP_SIG_NUM : UNEXP_SIG_NUM); i++)
        if (sig != NULL &&
                strcmp(((expected == 1) ? expSigs[i] : unexpSigs[i]), sig) == 0)
            return i; /* the signature found, return index */

    return -1; /* the signature not found */
}

static void lock(jvmtiEnv *jvmti_env, JNIEnv *jni_env) {
    if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorEnter(countLock)))
        jni_env->FatalError("failed to enter a raw monitor\n");
}

static void unlock(jvmtiEnv *jvmti_env, JNIEnv *jni_env) {
    if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorExit(countLock)))
        jni_env->FatalError("failed to exit a raw monitor\n");
}

/** callback functions **/
void JNICALL
ClassLoad(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread, jclass klass) {
    int i = 0;
    char *sig, *generic;

    lock(jvmti_env, env);

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(klass, &sig, &generic))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILURE: unable to obtain a class signature\n");
        unlock(jvmti_env, env);
        return;
    }

    i = findSig(sig, 1);
    if (i != -1) {
        clsEvents[i]++;
        NSK_DISPLAY1("CHECK PASSED: ClassLoad event received for the class \"%s\" as expected\n",
            sig);
    }
    else {
      i = findSig(sig, 0);
      if (i != -1) {
        result = STATUS_FAILED;
        primClsEvents[i]++;
        NSK_COMPLAIN1(
            "TEST FAILED: JVMTI_EVENT_CLASS_LOAD event received for\n"
            "\t a primitive class/array of primitive types with the signature \"%s\"\n",
            sig);
      }
    }

    unlock(jvmti_env, env);
}
/************************/

JNIEXPORT jint JNICALL
Java_nsk_jvmti_ClassLoad_classload001_check(
        JNIEnv *env, jobject obj) {
    size_t i;

    for (i=0; i<EXP_SIG_NUM; i++)
        if (clsEvents[i] != 1) {
            result = STATUS_FAILED;
            NSK_COMPLAIN2("TEST FAILED: wrong number of JVMTI_EVENT_CLASS_LOAD events for \"%s\":\n\tgot: %d\texpected: 1\n",
                expSigs[i], clsEvents[i]);
        }

    for (i=0; i<UNEXP_SIG_NUM; i++)
        if (primClsEvents[i] != 0)
            NSK_COMPLAIN0("TEST FAILED: there are JVMTI_EVENT_CLASS_LOAD events for the primitive classes\n");

    return result;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_classload001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_classload001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_classload001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    initCounters();

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("_counter_lock", &countLock)))
        return JNI_ERR;

    NSK_DISPLAY0("setting event callbacks ...\n");
    (void) memset(&callbacks, 0, sizeof(callbacks));
    callbacks.ClassLoad = &ClassLoad;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    NSK_DISPLAY0("setting event callbacks done\nenabling ClassLoad event ...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_LOAD, NULL)))
        return JNI_ERR;
    NSK_DISPLAY0("the event enabled\n");

    return JNI_OK;
}

}
