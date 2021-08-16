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

#include <string.h>
#include "jvmti.h"
#include "agent_common.h"
#include "jni_tools.h"
#include "jvmti_tools.h"

extern "C" {

/* ============================================================================= */

/* scaffold objects */
static jlong timeout = 0;

/* constant names */
#define DEBUGEE_CLASS_NAME    "nsk/jvmti/IsMethodObsolete/isobsolete001"
#define TESTED_CLASS_NAME     "nsk/jvmti/IsMethodObsolete/isobsolete001r"
#define TESTED_CLASS_SIG      "L" TESTED_CLASS_NAME ";"
#define TESTED_THREAD_NAME    "testedThread"
#define CLASSFILE_FIELD_NAME  "classfileBytes"
#define CLASSFILE_FIELD_SIG   "[B"

#define STATIC_METHOD_NAME    "testedStaticMethod"
#define STATIC_METHOD_SIG     "(I" TESTED_CLASS_SIG ")I"
#define INSTANCE_METHOD_NAME  "testedInstanceMethod"
#define INSTANCE_METHOD_SIG   "(I)I"

/* constants */
#define MAX_STACK_DEPTH       64

/* ============================================================================= */

/** Check is method obsolete is as expected. */
static void checkMethodObsolete(jvmtiEnv* jvmti, jmethodID method, const char name[],
                                                const char kind[], jboolean expected) {

    jboolean obsolete = JNI_FALSE;

    NSK_DISPLAY3("Call IsObsolete() for %s method: %p (%s)\n", kind, (void*)method, name);
    if (!NSK_JVMTI_VERIFY(
            jvmti->IsMethodObsolete(method, &obsolete))) {
        nsk_jvmti_setFailStatus();
    }
    NSK_DISPLAY1("  ... got obsolete: %d\n", (int)obsolete);
    if (obsolete != expected) {
        NSK_COMPLAIN4("IsObsolete() returns unexpected value for %s method: %s\n"
                      "#   return value: %d\n"
                      "#   expected:     %d\n",
                      kind, name,
                      (int)obsolete, (int)expected);
        nsk_jvmti_setFailStatus();
    }
}

/** Check is obsolete for methods on the stack are as expected. */
static void checkStackMethodsObsolete(jvmtiEnv* jvmti, jthread thread,
                                            const char kind[], jboolean expected) {

    jvmtiFrameInfo frameStack[MAX_STACK_DEPTH];
    jint frameCount = 0;

    NSK_DISPLAY1("Get stack frames for thread: %p\n", (void*)thread);
    if (!NSK_JVMTI_VERIFY(
            jvmti->GetStackTrace(thread, 0, MAX_STACK_DEPTH, frameStack, &frameCount))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY1("  ... got frames: %d\n", (int)frameCount);

    NSK_DISPLAY1("Check methods of each frame: %d frames\n", (int)frameCount);
    {
        int found = 0;
        int i;

        for (i = 0; i < frameCount; i++) {
            char* name = NULL;
            char* signature = NULL;
            char* generic = NULL;
            char* kind = NULL;

            NSK_DISPLAY1("  frame #%i:\n", i);
            NSK_DISPLAY1("     methodID:  %p\n", (void*)frameStack[i].method);
            if (!NSK_JVMTI_VERIFY(
                    jvmti->GetMethodName(frameStack[i].method, &name, &signature, &generic))) {
                nsk_jvmti_setFailStatus();
                continue;
            }
            NSK_DISPLAY1("     name:      %s\n", nsk_null_string(name));
            NSK_DISPLAY1("     signature: %s\n", nsk_null_string(signature));
            NSK_DISPLAY1("     generic:   %s\n", nsk_null_string(generic));
            if (name != NULL
                    && (strcmp(STATIC_METHOD_NAME, name) == 0
                        || strcmp(INSTANCE_METHOD_NAME, name) == 0)) {
                found++;
                NSK_DISPLAY1("SUCCESS: found redefined method on stack: %s\n", name);
                checkMethodObsolete(jvmti, frameStack[i].method, name,
                                                    "obsolete redefined", expected);
            }

            if (!NSK_JVMTI_VERIFY(
                    jvmti->Deallocate((unsigned char*)name))) {
                nsk_jvmti_setFailStatus();
            }
            if (!NSK_JVMTI_VERIFY(
                    jvmti->Deallocate((unsigned char*)signature))) {
                nsk_jvmti_setFailStatus();
            }
            if (!NSK_JVMTI_VERIFY(
                    jvmti->Deallocate((unsigned char*)generic))) {
                nsk_jvmti_setFailStatus();
            }
        }

        if (found < 2) {
            NSK_COMPLAIN3("Not all %s methods found on stack:\n"
                          "#   found methods: %d\n"
                          "#   expected:      %d\n",
                          kind, found, 2);
            nsk_jvmti_setFailStatus();
        }
    }
}

/** Redefine class with given bytecode. */
static int redefineClass(jvmtiEnv* jvmti, jclass klass, const char className[],
                                                    jint size, unsigned char bytes[]) {
    jvmtiClassDefinition classDef;

    classDef.klass = klass;
    classDef.class_byte_count = size;
    classDef.class_bytes = bytes;

    NSK_DISPLAY1("Redefine class: %s\n", className);
    if (!NSK_JVMTI_VERIFY(
            jvmti->RedefineClasses(1, &classDef))) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("   ... redefined with classfile: %d bytes\n", (int)size);

    return NSK_TRUE;
}

/** Get classfile bytes to redefine. */
static int getClassfileBytes(JNIEnv* jni, jvmtiEnv* jvmti,
                                    jint* size, unsigned char* *bytes) {
    jclass debugeeClass = NULL;
    jfieldID fieldID = NULL;
    jbyteArray array = NULL;
    jbyte* elements;
    int i;

    NSK_DISPLAY1("Find debugee class: %s\n", DEBUGEE_CLASS_NAME);
    if (!NSK_JNI_VERIFY(jni, (debugeeClass =
            jni->FindClass(DEBUGEE_CLASS_NAME)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... found class: %p\n", (void*)debugeeClass);

    NSK_DISPLAY1("Find static field: %s\n", CLASSFILE_FIELD_NAME);
    if (!NSK_JNI_VERIFY(jni, (fieldID =
            jni->GetStaticFieldID(debugeeClass, CLASSFILE_FIELD_NAME, CLASSFILE_FIELD_SIG)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got fieldID: %p\n", (void*)fieldID);

    NSK_DISPLAY1("Get classfile bytes array from static field: %s\n", CLASSFILE_FIELD_NAME);
    if (!NSK_JNI_VERIFY(jni, (array = (jbyteArray)
            jni->GetStaticObjectField(debugeeClass, fieldID)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got array object: %p\n", (void*)array);

    if (!NSK_JNI_VERIFY(jni, (*size =
            jni->GetArrayLength(array)) > 0)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got array size: %d bytes\n", (int)*size);

    {
        jboolean isCopy;
        if (!NSK_JNI_VERIFY(jni, (elements =
                jni->GetByteArrayElements(array, &isCopy)) != NULL)) {
            nsk_jvmti_setFailStatus();
        return NSK_FALSE;
        }
    }
    NSK_DISPLAY1("  ... got elements list: %p\n", (void*)elements);

    if (!NSK_JVMTI_VERIFY(
            jvmti->Allocate(*size, bytes))) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... created bytes array: %p\n", (void*)*bytes);

    for (i = 0; i < *size; i++) {
        (*bytes)[i] = (unsigned char)elements[i];
    }
    NSK_DISPLAY1("  ... copied bytecode: %d bytes\n", (int)*size);

    NSK_DISPLAY1("Release elements list: %p\n", (void*)elements);
    NSK_TRACE(jni->ReleaseByteArrayElements(array, elements, JNI_ABORT));
    NSK_DISPLAY0("  ... released\n");

    return NSK_TRUE;
}


/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    NSK_DISPLAY0("Wait for tested methods to run\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    /* perform testing */
    {
        jclass testedClass = NULL;
        jobject testedThread = NULL;
        jmethodID staticMethodID = NULL;
        jmethodID instanceMethodID = NULL;
        unsigned char* classfileBytes = NULL;
        jint classfileSize = 0;

        NSK_DISPLAY0(">>> Obtain bytes for class file redefinition\n");
        {
            if (!NSK_VERIFY(getClassfileBytes(jni, jvmti, &classfileSize, &classfileBytes)))
                return;
        }

        NSK_DISPLAY0(">>> Find tested methods and running thread\n");
        {
            NSK_DISPLAY1("Find tested class: %s\n", TESTED_CLASS_NAME);
            if (!NSK_JNI_VERIFY(jni, (testedClass =
                    jni->FindClass(TESTED_CLASS_NAME)) != NULL)) {
                nsk_jvmti_setFailStatus();
                return;
            }
            NSK_DISPLAY1("  ... found class: %p\n", (void*)testedClass);

            NSK_DISPLAY1("Make global reference for class object: %p\n", (void*)testedClass);
            if (!NSK_JNI_VERIFY(jni, (testedClass = (jclass)
                    jni->NewGlobalRef(testedClass)) != NULL)) {
                nsk_jvmti_setFailStatus();
                return;
            }
            NSK_DISPLAY1("  ... got reference: %p\n", (void*)testedClass);

            NSK_DISPLAY1("Get static methodID: %s\n", STATIC_METHOD_NAME);
            if (!NSK_JNI_VERIFY(jni, (staticMethodID =
                    jni->GetStaticMethodID(testedClass, STATIC_METHOD_NAME, STATIC_METHOD_SIG)) != NULL)) {
                nsk_jvmti_setFailStatus();
                return;
            }
            NSK_DISPLAY1("  ... got methodID: %p\n", (void*)staticMethodID);

            NSK_DISPLAY1("Get instance methodID: %s\n", INSTANCE_METHOD_NAME);
            if (!NSK_JNI_VERIFY(jni, (instanceMethodID =
                    jni->GetMethodID(testedClass, INSTANCE_METHOD_NAME, INSTANCE_METHOD_SIG)) != NULL)) {
                nsk_jvmti_setFailStatus();
                return;
            }
            NSK_DISPLAY1("  ... got methodID: %p\n", (void*)instanceMethodID);

            NSK_DISPLAY1("Find thread with running methods by name: %s\n", TESTED_THREAD_NAME);
            if (!NSK_VERIFY((testedThread =
                    nsk_jvmti_threadByName(TESTED_THREAD_NAME)) != NULL)) {
                nsk_jvmti_setFailStatus();
                return;
            }
            NSK_DISPLAY1("  ... got thread reference: %p\n", (void*)testedThread);
        }

        NSK_DISPLAY0(">>> Testcase #1: check IsObsolete() for methods before class redefinition\n");
        {
            checkMethodObsolete(jvmti, staticMethodID, STATIC_METHOD_NAME,
                                                    "not yet redefined", JNI_FALSE);
            checkMethodObsolete(jvmti, instanceMethodID, INSTANCE_METHOD_NAME,
                                                    "not yet redefined", JNI_FALSE);
        }

        NSK_DISPLAY0(">>> Testcase #2: check IsObsolete() for methods on stack before class redefinition\n");
        {
            checkStackMethodsObsolete(jvmti, testedThread, "not yet redefined", JNI_FALSE);
        }

        NSK_DISPLAY0(">>> Redefine class while methods are on the stack\n");
        {
            if (!NSK_VERIFY(redefineClass(jvmti, testedClass, TESTED_CLASS_NAME,
                                                        classfileSize, classfileBytes)))
                return;
        }

        NSK_DISPLAY0(">>> Testcase #3: check IsObsolete() for methods after class redefinition\n");
        {
            checkMethodObsolete(jvmti, staticMethodID, STATIC_METHOD_NAME,
                                                        "redefined", JNI_FALSE);
            checkMethodObsolete(jvmti, instanceMethodID, INSTANCE_METHOD_NAME,
                                                        "redefined", JNI_FALSE);
        }

        NSK_DISPLAY0(">>> Testcase #4: check IsObsolete() for obsoleted methods on stack after class redefinition\n");
        {
            checkStackMethodsObsolete(jvmti, testedThread, "obsolete redefined", JNI_TRUE);
        }

        NSK_DISPLAY0(">>> Clean used data\n");
        {
            NSK_DISPLAY1("Deallocate classfile bytes array: %p\n", (void*)classfileBytes);
            if (!NSK_JVMTI_VERIFY(
                        jvmti->Deallocate(classfileBytes))) {
                nsk_jvmti_setFailStatus();
            }

            NSK_DISPLAY1("Delete global eference to thread: %p\n", (void*)testedThread);
            NSK_TRACE(jni->DeleteGlobalRef(testedThread));

            NSK_DISPLAY1("Delete global reference to class: %p\n", (void*)testedClass);
            NSK_TRACE(jni->DeleteGlobalRef(testedClass));
        }
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_isobsolete001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_isobsolete001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_isobsolete001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* add required capabilities */
    {
        jvmtiCapabilities caps;

        memset(&caps, 0, sizeof(caps));
        caps.can_redefine_classes = 1;
        if (!NSK_JVMTI_VERIFY(
                jvmti->AddCapabilities(&caps))) {
            return JNI_ERR;
        }
    }

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
