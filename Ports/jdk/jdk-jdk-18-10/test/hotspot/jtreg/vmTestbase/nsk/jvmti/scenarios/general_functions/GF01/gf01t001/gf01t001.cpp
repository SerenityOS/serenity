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

/* the highly recommended system properties are below */
#define PROP_NUM 6
static const char *expected_props[PROP_NUM] = {
    "java.vm.vendor",
    "java.vm.version",
    "java.vm.name",
    "java.vm.info",
    "java.library.path",
    "java.class.path"
};

static jvmtiEventCallbacks callbacks;
static jint result = PASSED;

static int findProp(char *prop) {
    int i;

    for (i=0; i<PROP_NUM; i++) {
        if (strcmp(expected_props[i], prop) == 0) {
            NSK_DISPLAY1("CHECK PASSED: found highly recommended system property \"%s\" as expected\n",
                expected_props[i]);
            return 1; /* the property found */
        }
    }

    NSK_DISPLAY1("\tsystem property \"%s\" not found among highly recommended ones\n",
        prop);
    return 0; /* the property not found */
}

static void checkProps(jvmtiEnv *jvmti_env, const char *stepMsg) {
    jint count;
    char **propKeys;
    char *prop;
    int i;
    int foundProps = 0;

    NSK_DISPLAY1("%s: Getting system property keys ...\n",
        stepMsg);
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetSystemProperties(&count, &propKeys))) {
        result = STATUS_FAILED;
        return;
    }
    NSK_DISPLAY1("%d keys obtained\n",
        count);

    if (count < PROP_NUM) {
        result = STATUS_FAILED;
        NSK_COMPLAIN2("TEST FAILED: GetSystemProperties() returns %d system property keys\n\texpected at least %d",
            count, PROP_NUM);
    }

    for (i=0; i< count; i++) {
        NSK_DISPLAY2("%d) getting property for the key \"%s\":\n",
            i+1, propKeys[i]);
       if (!NSK_JVMTI_VERIFY(jvmti_env->GetSystemProperty((const char*) propKeys[i], &prop))) {
           result = STATUS_FAILED;
           return;
        }
        NSK_DISPLAY1("\tproperty=\"%s\"\n",
            prop);

        foundProps += findProp(propKeys[i]);

        NSK_DISPLAY0("\tdeallocating system property\n");
        if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*) prop))) {
            result = STATUS_FAILED;
            return;
        }

        NSK_DISPLAY0("\tdeallocating the system property key\n\n");
        if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*) propKeys[i]))) {
            result = STATUS_FAILED;
            return;
        }
    }

/*    NSK_DISPLAY0("Deallocating the property key array ...\n");
    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*) &propKeys))) {
        result = STATUS_FAILED;
        return;
    }*/

    if (foundProps != PROP_NUM) {
        result = STATUS_FAILED;
        NSK_COMPLAIN2("TEST FAILED: only %d highly recommended system properties found\n\tinstead of %d as expected\n",
            foundProps, PROP_NUM);
    }
}

/** callback functions **/
void JNICALL
VMInit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr) {
    NSK_DISPLAY0("VMInit event received\n");

    checkProps(jvmti_env, ">>> b) TEST CASE \"VMInit\"");
}

void JNICALL
VMDeath(jvmtiEnv *jvmti_env, JNIEnv *env) {
    NSK_DISPLAY0("VMDeath event received\n");

    checkProps(jvmti_env, ">>> c) TEST CASE \"VMDeath\"");

    if (result == STATUS_FAILED)
        exit(STATUS_FAILED);
}
/************************/

JNIEXPORT jint JNICALL
Java_nsk_jvmti_scenarios_general_1functions_GF01_gf01t001_check(JNIEnv *env, jobject obj) {
    return result;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_gf01t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_gf01t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_gf01t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv *jvmti;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    NSK_DISPLAY0("setting event callbacks ...\n");

    (void) memset(&callbacks, 0, sizeof(callbacks));
    callbacks.VMInit = &VMInit;
    callbacks.VMDeath = &VMDeath;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    NSK_DISPLAY0("setting event callbacks done\nenabling events ...\n");

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL)))
        return JNI_ERR;

    NSK_DISPLAY0("enabling events done\n\n");

    checkProps(jvmti, ">>> a) TEST CASE \"OnLoad\"");

    return JNI_OK;
}

}
