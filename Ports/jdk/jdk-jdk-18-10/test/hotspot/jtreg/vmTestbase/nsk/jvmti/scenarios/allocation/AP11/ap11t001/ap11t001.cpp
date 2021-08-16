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

#include <string.h>
#include "jvmti.h"
#include "agent_common.h"
#include "jni_tools.h"
#include "jvmti_tools.h"

extern "C" {

/* ========================================================================== */

/* scaffold objects */
static jlong timeout = 0;

/* ========================================================================== */

static int lookup(jvmtiEnv* jvmti,
        jint classCount, jclass *classes, const char *exp_sig) {
    char *signature, *generic;
    int found = NSK_FALSE;
    jint i;

    for (i = 0; i < classCount && !found; i++) {
        if (!NSK_JVMTI_VERIFY(jvmti->GetClassSignature(classes[i], &signature, &generic)))
            break;

        if (signature != NULL && strcmp(signature, exp_sig) == 0) {
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

JNIEXPORT void JNICALL
VMObjectAlloc(jvmtiEnv *jvmti, JNIEnv* jni, jthread thread, jobject object,
              jclass object_klass, jlong size) {

    char *signature, *generic;
    jvmtiThreadInfo threadInfo;

    /* Check that event is received in Live phase
    */
    {
        jvmtiPhase phase;
        if (!NSK_JVMTI_VERIFY(jvmti->GetPhase(&phase))) {
            nsk_jvmti_setFailStatus();
            return;
        }
        if (phase != JVMTI_PHASE_LIVE) {
            NSK_COMPLAIN1("VMObjectAlloc event was received in wrong phase: %s\n", TranslatePhase(phase));
            return;
        }
    }

    do {
        if (!NSK_JVMTI_VERIFY(jvmti->GetClassSignature(object_klass, &signature, &generic))) {
            nsk_jvmti_setFailStatus();
            break;
        }

        if (!NSK_JVMTI_VERIFY(jvmti->GetThreadInfo(thread, &threadInfo))) {
            nsk_jvmti_setFailStatus();
            break;
        }

        NSK_DISPLAY2("VMObjectAlloc in \"%s\" thread: \"%s\"\n", threadInfo.name, signature);
    } while (0);

    /* Check that event's thread is live thread
    */
    do {
        jint threadCount;
        jthread *threads;
        jint i;
        jboolean found = JNI_FALSE;

        if (!NSK_JVMTI_VERIFY(jvmti->GetAllThreads(&threadCount, &threads))) {
            nsk_jvmti_setFailStatus();
            break;
        }

        for (i = 0; i < threadCount && !found; i++) {
            found = jni->IsSameObject(threads[i], thread);
            if (found == JNI_TRUE) {
                break;
            }
        }

        if (!found) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN1("VMObjectAlloc: event's thread was found in the list of live threads: %s\n\n", threadInfo.name);
        }
    } while (0);


    /* Check that object_klass is loaded class
    */
    do {
        jint classCount;
        jclass *classes;

        if (!NSK_JVMTI_VERIFY(jvmti->GetLoadedClasses(&classCount, &classes))) {
            nsk_jvmti_setFailStatus();
            break;
        }

        if (!lookup(jvmti, classCount, classes, signature)) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN1("VMObjectAlloc: object_klass is not found in the list of loaded classes: %s\n", signature);
            return;
        }

        if (classes != NULL)
            jvmti->Deallocate((unsigned char*)classes);
    } while (0);


    /* Check for object_klass
    */
    {
        jclass klass;
        klass = jni->GetObjectClass(object);
        if (!(jni->IsSameObject(object_klass, klass))) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN1("VMObjectAlloc: unexpected object_klass : \"%s\"\n\n", signature);
        }
    }

    /* Check for object size
    */

    do {
        jlong objSize;
        if (!NSK_JVMTI_VERIFY(jvmti->GetObjectSize(object, &objSize))) {
            nsk_jvmti_setFailStatus();
            break;
        }

        if (objSize != size) {
            nsk_jvmti_setFailStatus();
            NSK_COMPLAIN2("VMObjectAlloc: inconsistent object size data\n\t"
               " size passed in to callback: %d\n\t size returned by GetObjectSize: %d\n\n", (long)size, (long)objSize);
        }
    } while (0);

    if (signature != NULL)
        jvmti->Deallocate((unsigned char*)signature);

    if (generic != NULL)
        jvmti->Deallocate((unsigned char*)generic);
}

/* ========================================================================== */

/* agent algorithm */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    /* wait for debuggee start */
    if (!nsk_jvmti_waitForSync(timeout))
        return;
    /* resume debugee after last sync */
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/* agent library initialization */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_ap11t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_ap11t001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_ap11t001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;
    jvmtiCapabilities caps;
    jvmtiEventCallbacks callbacks;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60000;
    NSK_DISPLAY1("Timeout: %d msc\n", (int)timeout);

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    memset(&caps, 0, sizeof(caps));
    caps.can_generate_vm_object_alloc_events = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
        return JNI_ERR;
    }

    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.VMObjectAlloc= &VMObjectAlloc;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    /* enable VMObjectAlloc event */
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_OBJECT_ALLOC, NULL)))
        return JNI_ERR;

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

}
