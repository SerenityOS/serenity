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
#define DEBUGEE_CLASS_NAME      "nsk/jvmti/ClassFileLoadHook/classfloadhk009"
#define TESTED_CLASS_NAME       "nsk/jvmti/ClassFileLoadHook/classfloadhk009r"
#define TESTED_CLASS_SIG        "L" TESTED_CLASS_NAME ";"

#define BYTECODE_FIELD_SIG      "[B"
#define REDEF_BYTECODE_FIELD_NAME  "redefClassBytes"
#define NEW_BYTECODE_FIELD_NAME    "newClassBytes"

#define TESTED_CLASS_FIELD_NAME "testedClass"
#define TESTED_CLASS_FIELD_SIG  "Ljava/lang/Class;"

static jclass testedClass = NULL;

static jint redefClassSize = 0;
static unsigned char* redefClassBytes = NULL;

static jint newClassSize = 0;
static unsigned char* newClassBytes = NULL;

static volatile int eventsCount = 0;

/* ============================================================================= */

/** Get classfile bytecode from a static field of given class. */
static int getBytecode(jvmtiEnv* jvmti, JNIEnv* jni, jclass cls,
                                    const char fieldName[], const char fieldSig[],
                                    jint* size, unsigned char* *bytes) {

    jfieldID fieldID = NULL;
    jbyteArray array = NULL;
    jbyte* elements;
    int i;

    NSK_DISPLAY1("Find static field: %s\n", fieldName);
    if (!NSK_JNI_VERIFY(jni, (fieldID =
            jni->GetStaticFieldID(cls, fieldName, fieldSig)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got fieldID: 0x%p\n", (void*)fieldID);

    NSK_DISPLAY1("Get classfile bytes array from static field: %s\n", fieldName);
    if (!NSK_JNI_VERIFY(jni, (array = (jbyteArray)
            jni->GetStaticObjectField(cls, fieldID)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got array object: 0x%p\n", (void*)array);

    if (!NSK_JNI_VERIFY(jni, (*size = jni->GetArrayLength(array)) > 0)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got array size: %d bytes\n", (int)*size);

    {
        jboolean isCopy;
        if (!NSK_JNI_VERIFY(jni, (elements = jni->GetByteArrayElements(array, &isCopy)) != NULL)) {
            nsk_jvmti_setFailStatus();
        return NSK_FALSE;
        }
    }
    NSK_DISPLAY1("  ... got elements list: 0x%p\n", (void*)elements);

    if (!NSK_JVMTI_VERIFY(jvmti->Allocate(*size, bytes))) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... created bytes array: 0x%p\n", (void*)*bytes);

    for (i = 0; i < *size; i++) {
        (*bytes)[i] = (unsigned char)elements[i];
    }
    NSK_DISPLAY1("  ... copied bytecode: %d bytes\n", (int)*size);

    NSK_DISPLAY1("Release elements list: 0x%p\n", (void*)elements);
    NSK_TRACE(jni->ReleaseByteArrayElements(array, elements, JNI_ABORT));
    NSK_DISPLAY0("  ... released\n");

    return NSK_TRUE;
}

/** Get global reference to object from a static field of given class. */
static jobject getObject(jvmtiEnv* jvmti, JNIEnv* jni, jclass cls,
                                    const char fieldName[], const char fieldSig[]) {

    jfieldID fieldID = NULL;
    jobject obj = NULL;

    NSK_DISPLAY1("Find static field: %s\n", fieldName);
    if (!NSK_JNI_VERIFY(jni, (fieldID =
            jni->GetStaticFieldID(cls, fieldName, fieldSig)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NULL;
    }
    NSK_DISPLAY1("  ... got fieldID: 0x%p\n", (void*)fieldID);

    NSK_DISPLAY1("Get object from static field: %s\n", fieldName);
    if (!NSK_JNI_VERIFY(jni, (obj = jni->GetStaticObjectField(cls, fieldID)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NULL;
    }
    NSK_DISPLAY1("  ... got object: 0x%p\n", (void*)obj);

    NSK_DISPLAY1("Make global reference to object: 0x%p\n", obj);
    if (!NSK_JNI_VERIFY(jni, (obj = jni->NewGlobalRef(obj)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NULL;
    }
    NSK_DISPLAY1("  ... got global ref: 0x%p\n", (void*)obj);

    return obj;
}

/** Redefine given class with new bytecode. */
static int redefineClass(jvmtiEnv* jvmti, jclass klass, const char className[],
                                                    jint size, unsigned char bytes[]) {
    jvmtiClassDefinition classDef;

    classDef.klass = klass;
    classDef.class_byte_count = size;
    classDef.class_bytes = bytes;

    NSK_DISPLAY1("Redefine class: %s\n", className);
    if (!NSK_JVMTI_VERIFY(jvmti->RedefineClasses(1, &classDef))) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("   ... redefined with bytecode: %d bytes\n", (int)size);

    return NSK_TRUE;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    NSK_DISPLAY0("Wait for debuggee to load original class\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    /* perform testing */
    {
        {
            jclass debugeeClass = NULL;

            NSK_DISPLAY0(">>> Obtain debuggee class\n");
            NSK_DISPLAY1("Find debugee class: %s\n", DEBUGEE_CLASS_NAME);
            if (!NSK_JNI_VERIFY(jni, (debugeeClass =
                    jni->FindClass(DEBUGEE_CLASS_NAME)) != NULL)) {
                nsk_jvmti_setFailStatus();
                return;
            }
            NSK_DISPLAY1("  ... found class: 0x%p\n", (void*)debugeeClass);

            NSK_DISPLAY0(">>> Obtain tested class object\n");
            if (!NSK_VERIFY((testedClass = (jclass)
                    getObject(jvmti, jni, debugeeClass, TESTED_CLASS_FIELD_NAME,
                                                        TESTED_CLASS_FIELD_SIG)) != NULL))
                return;

            NSK_DISPLAY0(">>> Obtain redefined bytecode of tested class\n");
            if (!NSK_VERIFY(getBytecode(jvmti, jni, debugeeClass,
                                        REDEF_BYTECODE_FIELD_NAME,
                                        BYTECODE_FIELD_SIG,
                                        &redefClassSize, &redefClassBytes)))
                return;

            NSK_DISPLAY0(">>> Obtain new instrumented bytecode of tested class\n");
            if (!NSK_VERIFY(getBytecode(jvmti, jni, debugeeClass,
                                        NEW_BYTECODE_FIELD_NAME,
                                        BYTECODE_FIELD_SIG,
                                        &newClassSize, &newClassBytes)))
                return;
        }

        NSK_DISPLAY0(">>> Redefine tested class\n");
        {
            if (!NSK_VERIFY(redefineClass(jvmti, testedClass, TESTED_CLASS_NAME,
                                                        redefClassSize, redefClassBytes)))
                return;
        }

        NSK_DISPLAY0(">>> Testcase #1: Redefine class and check CLASS_FILE_LOAD_HOOK event\n");
        {
            jvmtiEvent event = JVMTI_EVENT_CLASS_FILE_LOAD_HOOK;

            NSK_DISPLAY1("Enable event: %s\n", "CLASS_FILE_LOAD_HOOK");
            if (!NSK_VERIFY(nsk_jvmti_enableEvents(JVMTI_ENABLE, 1, &event, NULL)))
                return;
            NSK_DISPLAY0("  ... event enabled\n");

            NSK_VERIFY(redefineClass(jvmti, testedClass, TESTED_CLASS_NAME,
                                                        redefClassSize, redefClassBytes));

            NSK_DISPLAY1("Disable event: %s\n", "CLASS_FILE_LOAD_HOOK");
            if (NSK_VERIFY(nsk_jvmti_enableEvents(JVMTI_DISABLE, 1, &event, NULL))) {
                NSK_DISPLAY0("  ... event disabled\n");
            }

            NSK_DISPLAY1("Check if event was received: %s\n", "CLASS_FILE_LOAD_HOOK");
            if (eventsCount != 1) {
                NSK_COMPLAIN3("Unexpected number of %s events received for tested class:\n"
                              "#   received: %d events\n"
                              "#   expected: %d events\n",
                                "CLASS_FILE_LOAD_HOOK", eventsCount, 1);
                nsk_jvmti_setFailStatus();
            } else {
                NSK_DISPLAY1("  ... received: %d events\n", eventsCount);
            }
        }

        NSK_DISPLAY0(">>> Clean used data\n");
        {
            NSK_DISPLAY1("Delete global reference to tested class object: 0x%p\n", (void*)testedClass);
            jni->DeleteGlobalRef(testedClass);

            NSK_DISPLAY1("Deallocate redefined bytecode array: 0x%p\n", (void*)redefClassBytes);
            if (!NSK_JVMTI_VERIFY(jvmti->Deallocate(redefClassBytes))) {
                nsk_jvmti_setFailStatus();
            }
        }
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

/* ============================================================================= */

/** Callback for CLASS_FILE_LOAD_HOOK event **/
static void JNICALL
callbackClassFileLoadHook(jvmtiEnv *jvmti, JNIEnv *jni,
                            jclass class_being_redefined,
                            jobject loader, const char* name, jobject protection_domain,
                            jint class_data_len, const unsigned char* class_data,
                            jint *new_class_data_len, unsigned char** new_class_data) {

    NSK_DISPLAY5("  <CLASS_FILE_LOAD_HOOK>: name: %s, loader: 0x%p, redefined: 0x%p, bytecode: 0x%p:%d\n",
                        nsk_null_string(name), (void*)loader, (void*)class_being_redefined,
                        (void*)class_data, (int)class_data_len);

    if (name != NULL && (strcmp(name, TESTED_CLASS_NAME) == 0)) {
        NSK_DISPLAY1("SUCCESS! CLASS_FILE_LOAD_HOOK for tested class: %s\n", TESTED_CLASS_NAME);
        eventsCount++;

        NSK_DISPLAY2("Received redefined bytecode of redefined class: 0x%p:%d\n",
                                        (void*)class_data, (int)class_data_len);
        if (nsk_getVerboseMode()) {
            nsk_printHexBytes("   ", 16, class_data_len, class_data);
        }

        NSK_DISPLAY1("Check pointer to new_class_data_len: 0x%p\n", (void*)new_class_data_len);
        if (new_class_data_len == NULL) {
            NSK_COMPLAIN1("NULL new_class_data_len pointer passed to CLASS_FILE_LOAD_HOOK: 0x%p\n",
                                                    (void*)new_class_data_len);
            nsk_jvmti_setFailStatus();
        }

        NSK_DISPLAY1("Check pointer to new_class_data: 0x%p\n", (void*)new_class_data);
        if (new_class_data == NULL) {
            NSK_COMPLAIN1("NULL new_class_data pointer passed to CLASS_FILE_LOAD_HOOK: 0x%p\n",
                                                    (void*)new_class_data);
            nsk_jvmti_setFailStatus();
        }

        if (new_class_data_len != NULL && new_class_data != NULL) {
            NSK_DISPLAY2("Replace with new instrumented bytecode: 0x%p:%d\n",
                                        (void*)newClassBytes, (int)newClassSize);
            if (nsk_getVerboseMode()) {
                nsk_printHexBytes("   ", 16, newClassSize, newClassBytes);
            }

            *new_class_data_len = newClassSize;
            *new_class_data = newClassBytes;
        }
    }
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_classfloadhk009(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_classfloadhk009(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_classfloadhk009(JavaVM *jvm, char *options, void *reserved) {
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

    NSK_DISPLAY1("Add required capabilities: %s\n", "can_generate_eraly_class_hook_events, can_redefine_classes");
    {
        jvmtiCapabilities caps;

        memset(&caps, 0, sizeof(caps));
        caps.can_generate_all_class_hook_events = 1;
        caps.can_redefine_classes = 1;
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
            return JNI_ERR;
        }
    }
    NSK_DISPLAY0("  ... added\n");

    NSK_DISPLAY1("Set callback for event: %s\n", "CLASS_FILE_LOAD_HOOK");
    {
        jvmtiEventCallbacks callbacks;
        jint size = (jint)sizeof(callbacks);

        memset(&callbacks, 0, sizeof(callbacks));
        callbacks.ClassFileLoadHook = callbackClassFileLoadHook;
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, size))) {
            return JNI_ERR;
        }
    }
    NSK_DISPLAY0("  ... set\n");

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
