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
#include <stdlib.h>
#include <string.h>
#include <jvmti.h>
#include "agent_common.h"

#include "nsk_tools.h"
#include "JVMTITools.h"
#include "jvmti_tools.h"
#include "jni_tools.h"

extern "C" {

#define STATUS_FAILED 2
#define PASSED 0

#define MEM_SIZE 1024

static const char *CLASS_SIG =
    "Lnsk/jvmti/ObjectFree/objfree001u;";

static jvmtiEnv *jvmti = NULL;
static jvmtiEventCallbacks callbacks;

static volatile jint result = PASSED;
static volatile int objfree = 0;
static volatile jboolean clsUnloaded = JNI_FALSE;

unsigned char *mem;

typedef struct _LocalStorage {
    unsigned char data[MEM_SIZE];
} LocalStorage;

static LocalStorage stor;

static void rawMonitorFunc(jvmtiEnv *jvmti_env, const char *msg) {
    jrawMonitorID _lock;

    NSK_DISPLAY1("%s: creating a raw monitor ...\n",
        msg);
    if (!NSK_JVMTI_VERIFY(jvmti_env->CreateRawMonitor("_lock", &_lock))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: %s: unable to create a raw monitor\n\n",
            msg);
        return;
    }
    NSK_DISPLAY1("CHECK PASSED: %s: raw monitor created\n",
        msg);

    NSK_DISPLAY1("%s: entering the raw monitor ...\n",
        msg);
    if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorEnter(_lock))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: %s: unable to enter the raw monitor\n\n",
            msg);
    }
    else {
        NSK_DISPLAY1("CHECK PASSED: %s: the raw monitor entered\n",
            msg);

        NSK_DISPLAY1("%s: exiting the raw monitor ...\n",
            msg);
        if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorExit(_lock))) {
            result = STATUS_FAILED;
            NSK_COMPLAIN1("TEST FAILED: %s: unable to exit the raw monitor\n\n",
                msg);
        }
        NSK_DISPLAY1("CHECK PASSED: %s: the raw monitor exited\n",
            msg);
    }

    NSK_DISPLAY1("%s: destroying the raw monitor ...\n",
        msg);
    if (!NSK_JVMTI_VERIFY(jvmti_env->DestroyRawMonitor(_lock))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: %s: unable to destroy a raw monitor\n",
            msg);
        return;
    }
    NSK_DISPLAY1("CHECK PASSED: %s: the raw monitor destroyed\n",
        msg);
}

static void memoryFunc(jvmtiEnv *jvmti_env, const char *msg) {
    NSK_DISPLAY1("%s: allocating memory ...\n",
        msg);
    if (!NSK_JVMTI_VERIFY(jvmti_env->Allocate(MEM_SIZE, &mem))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: %s: unable to allocate memory\n\n",
            msg);
        return;
    }
    else
        NSK_DISPLAY1("CHECK PASSED: %s: memory has been allocated successfully\n",
            msg);

    NSK_DISPLAY1("%s: deallocating memory ...\n",
        msg);
    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate(mem))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: %s: unable to deallocate memory\n\n",
            msg);
    }
    else
        NSK_DISPLAY1("CHECK PASSED: %s: memory has been deallocated successfully\n",
            msg);
}

static void envStorageFunc(jvmtiEnv *jvmti_env, const char *msg) {
    LocalStorage* obtainedData = NULL;
    LocalStorage* storedData = &stor;

    NSK_DISPLAY2("%s: setting an environment local storage 0x%p ...\n",
        msg, (void*) &stor);
    if (!NSK_JVMTI_VERIFY(jvmti_env->SetEnvironmentLocalStorage((const void*) &stor))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: %s: unable to set an environment local storage\n\n",
            msg);
        return;
    }
    else
        NSK_DISPLAY1("CHECK PASSED: %s: environment local storage has been set successfully\n",
            msg);

    NSK_DISPLAY1("%s: getting an environment local storage ...\n",
        msg);
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetEnvironmentLocalStorage((void**) &obtainedData))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: %s: unable to get an environment local storage\n\n",
            msg);
        return;
    }
    else {
        if (obtainedData != storedData) {
            result = STATUS_FAILED;
            NSK_COMPLAIN3(
                "TEST FAILED: %s: obtained an environment local storage has unexpected pointer:\n"
                "got: 0x%p\texpected: 0x%p\n\n",
                msg, (void*) obtainedData, (void*) storedData);
        }
        else
            NSK_DISPLAY2("CHECK PASSED: %s: environment local storage 0x%p obtained successfully\n",
                msg, (void*) obtainedData);
    }
}

/** callback functions **/
void JNICALL
ObjectFree(jvmtiEnv *jvmti_env, jlong tag) {
    NSK_DISPLAY1(">>>> ObjectFree event received for an object with tag %ld\n",
        (long) tag);

    if (tag != 1) {
        result = STATUS_FAILED;
        NSK_COMPLAIN1("TEST FAILED: unexpected ObjectFree event for an object with unknown tag %ld\n",
            (long) tag);
    }
    else {
        objfree++;
        NSK_DISPLAY0("CHECK PASSED: ObjectFree event received for previously tagged object\n");
    }

    rawMonitorFunc(jvmti_env, "ObjectFree");
    memoryFunc(jvmti_env, "ObjectFree");
    envStorageFunc(jvmti_env, "ObjectFree");

    NSK_DISPLAY0("<<<<\n\n");
}

void JNICALL
VMDeath(jvmtiEnv *jvmti_env, JNIEnv *env) {
    NSK_DISPLAY0("VMDeath event received\n");

    if (clsUnloaded == JNI_TRUE) {
        if (objfree == 0) {
            NSK_DISPLAY1(
                "Warning: no ObjectFree events for a tagged object\n"
                "\twhich class \"%s\" has been detected for unloading\n\n",
                CLASS_SIG);
        } else {
            NSK_DISPLAY2(
                "CHECK PASSED: %d ObjectFree event(s) received for a tagged object\n"
                "\twhich class \"%s\" has been detected for unloading\n\n",
                objfree, CLASS_SIG);
        }
    } else {
         NSK_DISPLAY1(
             "Warning: unloading of the tested class \"%s\" has not been detected,\n"
             "\tso the test has no results\n",
             CLASS_SIG);
    }

    if (result == STATUS_FAILED)
        exit(95 + STATUS_FAILED);
}
/************************/

JNIEXPORT void JNICALL
Java_nsk_jvmti_ObjectFree_objfree001_setTag(JNIEnv *jni_env, jobject obj, jobject objToTag) {
    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(objToTag, (jlong) 1))) {
        result = STATUS_FAILED;
        NSK_COMPLAIN0("TEST FAILED: unable to set tag for a tested object\n");
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_ObjectFree_objfree001_inform(
        JNIEnv *env, jobject obj, jboolean unLoaded) {
    clsUnloaded = unLoaded;
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_objfree001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_objfree001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_objfree001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiCapabilities caps;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* add capability to generate compiled method events */
    memset(&caps, 0, sizeof(jvmtiCapabilities));
    caps.can_generate_object_free_events = 1;
    caps.can_tag_objects = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    if (!caps.can_generate_object_free_events)
        NSK_DISPLAY0("Warning: generation of object free events is not implemented\n");
    if (!caps.can_tag_objects)
        NSK_DISPLAY0("Warning: tagging objects is not implemented\n");

    /* set event callback */
    NSK_DISPLAY0("setting event callbacks ...\n");
    (void) memset(&callbacks, 0, sizeof(callbacks));
    callbacks.VMDeath = &VMDeath;
    callbacks.ObjectFree = &ObjectFree;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    NSK_DISPLAY0("setting event callbacks done\nenabling JVMTI events ...\n");
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                                          JVMTI_EVENT_VM_DEATH,
                                                          NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                                          JVMTI_EVENT_OBJECT_FREE,
                                                          NULL)))
        return JNI_ERR;
    NSK_DISPLAY0("enabling the events done\n\n");

    return JNI_OK;
}

}
