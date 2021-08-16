/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "native_thread.h"
#include "JVMTITools.h"
#include "jvmti_tools.h"

extern "C" {

#define PASSED  0
#define STATUS_FAILED  2

#define MAX_ATTEMPTS 15

static const char *expHSMethod = "redefclass029HotMethod";
static const char *expHSSignature = "(I)V";

#define EVENTS_COUNT 2
static jvmtiEvent eventsList[EVENTS_COUNT] = {
    JVMTI_EVENT_COMPILED_METHOD_LOAD,
    JVMTI_EVENT_COMPILED_METHOD_UNLOAD
};

static jvmtiEnv *jvmti = NULL;
static jlong timeout = 0;
static volatile int fire = 0; /* CompiledMethodLoad received for hotspot method */
static jmethodID hsMethodID; /* hotspot method ID */

static volatile int enteredHotMethod = 0; /* "hot" method is entered */

static jint bytesCount; /* number of bytes of a redefining class */
static jbyte *clsBytes; /* bytes defining a redefining class */

/** pass info about redefinition to Java **/
JNIEXPORT jboolean JNICALL Java_nsk_jvmti_RedefineClasses_redefclass029_isRedefinitionOccurred
        (JNIEnv *jni_env, jclass cls)
{
    if (fire == 1) {
        NSK_DISPLAY0("isRedefinitionOccurred is called: fired!\n");
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

/** notify native agent from Java when "hot" method is executed **/
JNIEXPORT void JNICALL Java_nsk_jvmti_RedefineClasses_redefclass029_notifyNativeAgent
        (JNIEnv *jni_env, jclass cls)
{
    if (enteredHotMethod == 0) {
        NSK_DISPLAY0("notifyNativeAgent is called\n");
        enteredHotMethod = 1;
    }
}

JNIEXPORT void JNICALL Java_nsk_jvmti_RedefineClasses_redefclass029_storeClassBytes
        (JNIEnv *jni_env, jclass cls, jbyteArray classBytes) {
    jboolean isCopy;

    bytesCount = jni_env->GetArrayLength(classBytes);
    clsBytes =
        jni_env->GetByteArrayElements(classBytes, &isCopy);
}

/** callback functions **/
void JNICALL
CompiledMethodLoad(jvmtiEnv *jvmti_env, jmethodID method, jint code_size,
        const void* code_addr,  jint map_length, const jvmtiAddrLocationMap* map,
        const void* compile_info) {
    char *name;
    char *sig;

    NSK_DISPLAY0("CompiledMethodLoad event received for:\n");
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &name, &sig, NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY5("\tmethod: name=\"%s\" signature=\"%s\"\n"
                 "\tcompiled code size=%d\n"
                 "\tstarting native address=0x%p\n"
                 "\tnumber of address location map entries=%d\n",
                 name, sig, code_size, code_addr, map_length);

    if ((strcmp(name, expHSMethod) == 0) &&
            (strcmp(sig, expHSSignature) == 0)) {
        NSK_DISPLAY0("CompiledMethodLoad: a tested hotspot method found\n");

        // CR 6604375: check whether "hot" method was entered
        if (enteredHotMethod) {
            hsMethodID = method;
            fire = 1;
        } else {
            NSK_DISPLAY0("Compilation occured before method execution. Ignoring.\n");
        }
    }
}

JNIEXPORT void JNICALL
CompiledMethodUnload(jvmtiEnv* jvmti_env, jmethodID method,
        const void* code_addr) {
    char *name;
    char *sig;
    jvmtiError err;

    NSK_DISPLAY0("CompiledMethodUnload event received\n");
    // Check for the case that the class has been unloaded
    err = jvmti_env->GetMethodName(method, &name, &sig, NULL);
    if (err == JVMTI_ERROR_NONE) {
        NSK_DISPLAY3("for: \tmethod: name=\"%s\" signature=\"%s\"\n\tnative address=0x%p\n",
          name, sig, code_addr);
        jvmti_env->Deallocate((unsigned char*)name);
        jvmti_env->Deallocate((unsigned char*)sig);
    }
}
/************************/

/** agent's procedure **/
static void JNICALL
agentProc(jvmtiEnv* jvmti_env, JNIEnv* jni_env, void* arg) {
    int tries = 0;
    jclass decl_cls;
    char *cls_sig;
    jvmtiClassDefinition classDef;

    /* testing sync */
    NSK_DISPLAY1("agentProc: waiting for the debuggee start for %d msecs...\n\n",
        (int) timeout);
    if (!nsk_jvmti_waitForSync(timeout))
        return;
    NSK_DISPLAY0("agentProc: resuming the debuggee ...\n\n");
    if (!nsk_jvmti_resumeSync())
        return;

    /* at first, send all generated CompiledMethodLoad events */
    NSK_DISPLAY0("agentProc: sending all generated CompiledMethodLoad events ...\n\n");
    if (!NSK_JVMTI_VERIFY(jvmti->GenerateEvents(JVMTI_EVENT_COMPILED_METHOD_LOAD))) {
        nsk_jvmti_setFailStatus();
        nsk_jvmti_resumeSync();
        return;
    }

    NSK_DISPLAY0("agentProc: waiting for hotspot method compilation...\n\n");
    do {
        THREAD_sleep(1);
        tries++;
        if (tries > MAX_ATTEMPTS) {
            printf("WARNING: CompiledMethodLoad event is still not received for \"%s\" after %d attempts\n"
                   "\tThe test has no results\n\n",
                   expHSMethod, MAX_ATTEMPTS);
            nsk_jvmti_resumeSync();
            exit(95 + PASSED);
        }
    } while (fire == 0);

    NSK_DISPLAY0("agentProc: hotspot method compiled\n\n");

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodDeclaringClass(hsMethodID, &decl_cls))) {
        nsk_jvmti_setFailStatus();
        nsk_jvmti_resumeSync();
        return;
    }
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(decl_cls, &cls_sig, NULL))) {
        nsk_jvmti_setFailStatus();
        nsk_jvmti_resumeSync();
        return;
    } else {
        NSK_DISPLAY1("agentProc: hotspot method class signature: \"%s\"\n\n",
                     cls_sig);
    }

    /* fill the structure jvmtiClassDefinition */
    classDef.klass = decl_cls;
    classDef.class_byte_count = bytesCount;
    classDef.class_bytes = (unsigned char*) clsBytes;

    NSK_DISPLAY1("agentProc: >>>>>>>> Invoke RedefineClasses():\n"
                 "\tnew class byte count=%d\n",
                 classDef.class_byte_count);
    if (!NSK_JVMTI_VERIFY(jvmti->RedefineClasses(1, &classDef))) {
      nsk_jvmti_setFailStatus();
      nsk_jvmti_resumeSync();
      return;
    }
    NSK_DISPLAY0("agentProc: <<<<<<<< RedefineClasses() is successfully done\n");

    /* testing sync */
    NSK_DISPLAY1("agentProc: waiting for the debuggee finish for %d msecs...\n\n",
        (int) timeout);
    if (!nsk_jvmti_waitForSync(timeout))
        return;
    NSK_DISPLAY0("agentProc: final resuming of the debuggee ...\n\n");
    if (!nsk_jvmti_resumeSync())
        return;

    NSK_DISPLAY0("agentProc: finished\n\n");
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_redefclass029(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_redefclass029(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_redefclass029(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiCapabilities caps;
    jvmtiEventCallbacks callbacks;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    /* obtain WAITTIME parameter */
    timeout = nsk_jvmti_getWaitTime() * 60000;
    NSK_DISPLAY1("waittime=%d msecs\n", (int) timeout);

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* add required capabilities */
    memset(&caps, 0, sizeof(jvmtiCapabilities));
    caps.can_generate_compiled_method_load_events = 1;
    caps.can_redefine_classes = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    /* set event callback */
    NSK_DISPLAY0("setting event callbacks ...\n");
    (void) memset(&callbacks, 0, sizeof(callbacks));
    callbacks.CompiledMethodLoad = &CompiledMethodLoad;
    callbacks.CompiledMethodUnload = &CompiledMethodUnload;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    NSK_DISPLAY0("setting event callbacks done\nenabling events ...\n");
    if (!nsk_jvmti_enableEvents(JVMTI_ENABLE, EVENTS_COUNT,
            eventsList, NULL))
        return JNI_ERR;
    NSK_DISPLAY0("enabling the events done\n\n");

    /* register agent proc */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

}
