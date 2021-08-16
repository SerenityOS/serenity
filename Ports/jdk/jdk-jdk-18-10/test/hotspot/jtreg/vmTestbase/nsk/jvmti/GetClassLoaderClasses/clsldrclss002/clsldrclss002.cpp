/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include <string.h>
#include "jni_tools.h"
#include "agent_common.h"
#include "jvmti_tools.h"

extern "C" {

/* ========================================================================== */

/* scaffold objects */
static jlong timeout = 0;

/* test objects */
static jobject testedClassLoader = NULL;
static jclass testedClass = NULL;
static jfieldID testedFieldID = NULL;

static const char* CLASS_SIG =
    "Lnsk/jvmti/GetClassLoaderClasses/clsldrclss002;";
static const char* CLASS_SIG_A =
    "Lnsk/jvmti/GetClassLoaderClasses/clsldrclss002a;";
static const char* CLASS_SIG_E =
    "Lnsk/jvmti/GetClassLoaderClasses/clsldrclss002e;";
static const char* CLASS_SIG_I =
    "Lnsk/jvmti/GetClassLoaderClasses/clsldrclss002i;";

/* ========================================================================== */

static int prepare(JNIEnv* jni) {
    const char* CLASS_NAME = "nsk/jvmti/GetClassLoaderClasses/clsldrclss002";
    const char* FIELD_NAME = "testedClassLoader";
    const char* FIELD_SIGNATURE = "Ljava/lang/ClassLoader;";

    NSK_DISPLAY0("Obtain tested object from a static field of debugee class\n");

    NSK_DISPLAY1("Find class: %s\n", CLASS_NAME);
    if (!NSK_JNI_VERIFY(jni, (testedClass = jni->FindClass(CLASS_NAME)) != NULL))
        return NSK_FALSE;

    if (!NSK_JNI_VERIFY(jni, (testedClass = (jclass) jni->NewGlobalRef(testedClass)) != NULL))
        return NSK_FALSE;

    NSK_DISPLAY2("Find field: %s:%s\n", FIELD_NAME, FIELD_SIGNATURE);
    if (!NSK_JNI_VERIFY(jni, (testedFieldID =
            jni->GetStaticFieldID(testedClass, FIELD_NAME, FIELD_SIGNATURE)) != NULL))
        return NSK_FALSE;

    return NSK_TRUE;
}

static int lookup(jvmtiEnv* jvmti,
        jint classCount, jclass *classes, const char *exp_sig) {
    char *signature, *generic;
    int found = NSK_FALSE;
    jint i;

    for (i = 0; i < classCount && !found; i++) {
        if (!NSK_JVMTI_VERIFY(jvmti->GetClassSignature(classes[i], &signature, &generic)))
            break;

        if (signature != NULL && strcmp(signature, exp_sig) == 0) {
            NSK_DISPLAY1("Expected class found: %s\n", exp_sig);
            found = NSK_TRUE;
        }

        if (signature != NULL)
            jvmti->Deallocate((unsigned char*)signature);

        if (generic != NULL)
            jvmti->Deallocate((unsigned char*)generic);
    }

    return found;
}

/* ========================================================================== */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    jclass *classes;
    jint classCount;

    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!prepare(jni)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    NSK_DISPLAY0("Testcase #1: check on default classloader\n");
    if (!NSK_JNI_VERIFY(jni, (testedClassLoader =
            jni->GetStaticObjectField(testedClass, testedFieldID)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }
    if (!NSK_JVMTI_VERIFY(jvmti->GetClassLoaderClasses(testedClassLoader, &classCount, &classes))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    if (!NSK_VERIFY(classCount != 0)) {
        nsk_jvmti_setFailStatus();
        return;
    }
    if (!NSK_VERIFY(classes != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }
    if (!lookup(jvmti, classCount, classes, CLASS_SIG)) {
        NSK_COMPLAIN1("Cannot find class in the list: %s\n", CLASS_SIG);
        nsk_jvmti_setFailStatus();
        return;
    }
    if (classes != NULL)
        jvmti->Deallocate((unsigned char*)classes);

    if (!nsk_jvmti_resumeSync())
        return;
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    NSK_DISPLAY0("Testcase #2: check on custom classloader\n");
    if (!NSK_JNI_VERIFY(jni, (testedClassLoader =
            jni->GetStaticObjectField(testedClass, testedFieldID)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }
    if (!NSK_JVMTI_VERIFY(jvmti->GetClassLoaderClasses(testedClassLoader, &classCount, &classes))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    if (!NSK_VERIFY(classCount != 0)) {
        nsk_jvmti_setFailStatus();
        return;
    }
    if (!NSK_VERIFY(classes != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }
    if (!lookup(jvmti, classCount, classes, CLASS_SIG_A)) {
        NSK_COMPLAIN1("Cannot find class in the list: %s\n", CLASS_SIG_A);
        nsk_jvmti_setFailStatus();
    }
    if (!lookup(jvmti, classCount, classes, CLASS_SIG_I)) {
        NSK_COMPLAIN1("Cannot find class in the list: %s\n", CLASS_SIG_I);
        nsk_jvmti_setFailStatus();
    }
    if (!lookup(jvmti, classCount, classes, CLASS_SIG_E)) {
        NSK_COMPLAIN1("Cannot find class in the list: %s\n", CLASS_SIG_E);
        nsk_jvmti_setFailStatus();
    }
    if (classes != NULL)
        jvmti->Deallocate((unsigned char*)classes);

    NSK_TRACE(jni->DeleteGlobalRef(testedClass));

    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_clsldrclss002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_clsldrclss002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_clsldrclss002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;

    NSK_DISPLAY0("Agent_OnLoad\n");

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

}
