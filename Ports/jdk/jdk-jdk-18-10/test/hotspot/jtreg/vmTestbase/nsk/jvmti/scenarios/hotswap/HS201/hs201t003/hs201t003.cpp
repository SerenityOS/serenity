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

static const char *expHSMethod = "entryMethod2";
static const char *expHSSignature = "()V";

#define EVENTS_COUNT 4
static jvmtiEvent eventsList[EVENTS_COUNT] = {
    JVMTI_EVENT_EXCEPTION,
    JVMTI_EVENT_METHOD_ENTRY,
    JVMTI_EVENT_METHOD_EXIT,
    JVMTI_EVENT_FRAME_POP
};

static jvmtiEnv *jvmti = NULL;
static jlong timeout = 0;

static jint bytesCount; /* number of bytes of a redefining class */
static jbyte *clsBytes; /* bytes defining a redefining class */

static jint redefMethBytesCount; /* number of bytes of a redefined method */
static unsigned char *redefMethBytes; /* bytes defining a redefined method */

JNIEXPORT void JNICALL Java_nsk_jvmti_scenarios_hotswap_HS201_hs201t003_storeClassBytes
        (JNIEnv *jni_env, jclass cls, jbyteArray classBytes) {
    jboolean isCopy;

    bytesCount = jni_env->GetArrayLength(classBytes);
    clsBytes =
        jni_env->GetByteArrayElements(classBytes, &isCopy);
}

static int expectedMeth(jvmtiEnv *jvmti_env, const char *event,
        jmethodID method, const char *expMeth, const char *expSig) {
    char *name;
    char *sig;
    int methFound = 0;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &name, &sig, NULL))) {
        nsk_jvmti_setFailStatus();
        return 0;
    }

    if ((strcmp(name, expMeth) == 0) && (strcmp(sig, expSig) == 0)) {
        NSK_DISPLAY4(
            "===== %s event received for the tested method:\n"
            "\tID=0x%p name=\"%s\" signature=\"%s\"\n",
            event, (void*) method, name, sig);
        methFound = 1;
    }
    else
        methFound = 0;

    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*) name)))
        nsk_jvmti_setFailStatus();
    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*) sig)))
        nsk_jvmti_setFailStatus();

    return methFound;
}

static void doHotSwap(jvmtiEnv *jvmti_env,
        jmethodID tMethodID, const char *event) {
    jclass decl_cls;
    char *cls_sig;
    jvmtiClassDefinition classDef;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodDeclaringClass(tMethodID, &decl_cls))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(decl_cls, &cls_sig, NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    else
        NSK_DISPLAY2("[%s] tested method class signature: \"%s\"\n\n",
            event, cls_sig);

    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*) cls_sig)))
        nsk_jvmti_setFailStatus();

    /* fill the structure jvmtiClassDefinition */
    classDef.klass = decl_cls;
    classDef.class_byte_count = bytesCount;
    classDef.class_bytes = (unsigned char*) clsBytes;

    NSK_DISPLAY2(
        "[%s] >>>>> Invoke RedefineClasses():\n"
        "\tnew class byte count=%d\n",
        event, classDef.class_byte_count);
    if (!NSK_JVMTI_VERIFY(jvmti->RedefineClasses(1, &classDef))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY1("[%s] <<<<< RedefineClasses() is successfully done\n",
        event);
}

static void doChecks(jvmtiEnv *jvmti_env,
        jmethodID tMethodID, jboolean expected, const char *event) {
    jboolean isObsolete = JNI_FALSE;
    char *name;
    char *sig;
    jint methBytesCount; /* number of bytes of a method */
    unsigned char *methBytes; /* bytes defining a method */

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(tMethodID, &name, &sig, NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY4("[%s] method ID=0x%p name=\"%s\" signature=\"%s\"\n",
        event, (void*) tMethodID, name, sig);

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetBytecodes(tMethodID, &methBytesCount, &methBytes))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY3(
        "[%s] method bytes count=%d\n"
        "\tbytes count of the redefined method=%d\n",
        event, methBytesCount, redefMethBytesCount);
    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*) methBytes)))
        nsk_jvmti_setFailStatus();

    if (!NSK_JVMTI_VERIFY(jvmti_env->IsMethodObsolete(tMethodID, &isObsolete))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    if ((methBytesCount == redefMethBytesCount && isObsolete == JNI_TRUE) ||
            (methBytesCount != redefMethBytesCount && isObsolete == JNI_FALSE)) {
        NSK_DISPLAY3("[%s] CHECK PASSED: IsMethodObsolete = %d(%s) as expected\n",
            event, (int)isObsolete,
            (isObsolete == JNI_TRUE) ? "TRUE" : "FALSE");
    }
    else {
        nsk_jvmti_setFailStatus();
        NSK_COMPLAIN4("[%s] TEST FAILED: IsMethodObsolete = %d(%s), expected: %s\n",
            event, (int)isObsolete,
            (isObsolete == JNI_TRUE) ? "TRUE" : "FALSE",
            (methBytesCount == redefMethBytesCount) ? "TRUE" : "FALSE");
    }
}

/** callback functions **/
void JNICALL MethodEntry(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method) {

    if (expectedMeth(jvmti_env, "MethodEntry",
            method, expHSMethod, expHSSignature) == 1) {
        if (!NSK_JVMTI_VERIFY(jvmti_env->GetBytecodes(method, &redefMethBytesCount, &redefMethBytes)))
            nsk_jvmti_setFailStatus();
        else {
            NSK_DISPLAY2("[MethodEntry] thread=0x%p method bytes count=%d\n",
                thr, redefMethBytesCount);

            if (!NSK_JVMTI_VERIFY(jvmti_env->NotifyFramePop(thr, 0)))
                nsk_jvmti_setFailStatus();
        }

        if (!NSK_JVMTI_VERIFY(jvmti_env->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_METHOD_ENTRY, NULL)))
            nsk_jvmti_setFailStatus();
    }
}

void JNICALL
Exception(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thr,
        jmethodID method, jlocation location, jobject exception,
        jmethodID catch_method, jlocation catch_location) {

    if (expectedMeth(jvmti_env, "Exception",
            method, expHSMethod, expHSSignature) == 1) {
        NSK_DISPLAY1("[Exception] thread=0x%p\n", thr);

        doHotSwap(jvmti_env, method, "Exception");
        doChecks(jvmti_env, method, JNI_TRUE, "Exception");
    }
}

void JNICALL
MethodExit(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method,
        jboolean was_poped_by_exc, jvalue return_value) {

    if (expectedMeth(jvmti_env, "MethodExit",
            method, expHSMethod, expHSSignature) == 1) {
        NSK_DISPLAY1("[MethodExit] thread=0x%p\n", thr);

        doHotSwap(jvmti_env, method, "MethodExit");
        doChecks(jvmti_env, method, JNI_TRUE, "MethodExit");
    }
}

void JNICALL
FramePop(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method, jboolean wasPopedByException) {
    if (expectedMeth(jvmti_env, "FramePop",
            method, expHSMethod, expHSSignature) == 1) {
        NSK_DISPLAY1("[FramePop] thread=0x%p\n", thr);

        doHotSwap(jvmti_env, method, "FramePop");
        doChecks(jvmti_env, method, JNI_TRUE, "FramePop");
    }
}
/************************/

/** agent's procedure **/
static void JNICALL
agentProc(jvmtiEnv* jvmti_env, JNIEnv* jni_env, void* arg) {
    int tries = 0;

    /* testing sync */
    NSK_DISPLAY1("agentProc: waiting for the debuggee start for %d msecs...\n\n",
        (int) timeout);
    if (!nsk_jvmti_waitForSync(timeout))
        return;
    NSK_DISPLAY0("agentProc: resuming the debuggee ...\n\n");
    if (!nsk_jvmti_resumeSync())
        return;

    /* testing sync */
    NSK_DISPLAY1("agentProc: waiting for the debuggee finish for %d msecs...\n\n",
        (int) timeout);
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    /* deallocating used memory */
    if (!NSK_JVMTI_VERIFY(jvmti_env->Deallocate((unsigned char*) redefMethBytes)))
        nsk_jvmti_setFailStatus();

    NSK_DISPLAY0("agentProc: final resuming of the debuggee ...\n\n");
    if (!nsk_jvmti_resumeSync())
        return;

    NSK_DISPLAY0("agentProc: finished\n\n");
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_hs201t003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_hs201t003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_hs201t003(JavaVM *jvm, char *options, void *reserved) {
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
    caps.can_get_bytecodes = 1;
    caps.can_generate_exception_events = 1;
    caps.can_generate_method_entry_events = 1;
    caps.can_generate_method_exit_events = 1;
    caps.can_generate_frame_pop_events = 1;
    caps.can_redefine_classes = 1;
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    /* set event callback */
    NSK_DISPLAY0("setting event callbacks ...\n");
    (void) memset(&callbacks, 0, sizeof(callbacks));
    callbacks.Exception = &Exception;
    callbacks.MethodEntry = &MethodEntry;
    callbacks.MethodExit = &MethodExit;
    callbacks.FramePop = &FramePop;
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
