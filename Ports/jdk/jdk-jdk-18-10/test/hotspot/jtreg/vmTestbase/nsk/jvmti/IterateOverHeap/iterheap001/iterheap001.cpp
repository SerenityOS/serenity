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

static jlong timeout = 0;

#define INFO_NONE       0x00
#define INFO_ALL        0xFF
#define INFO_OBJREF     0x01
#define INFO_STACKREF   0x02
#define INFO_HEAPROOT   0x04
#define INFO_HEAPOBJ    0x08
#define INFO_TAGGED     0x10

static unsigned int info = INFO_NONE;

#define DEBUGEE_CLASS_NAME      "nsk/jvmti/IterateOverHeap/iterheap001"
#define ROOT_OBJECT_CLASS_NAME  "nsk/jvmti/IterateOverHeap/iterheap001RootTestedClass"
#define ROOT_OBJECT_CLASS_SIG   "L" ROOT_OBJECT_CLASS_NAME ";"
#define CHAIN_OBJECT_CLASS_NAME "nsk/jvmti/IterateOverHeap/iterheap001TestedClass"
#define CHAIN_OBJECT_CLASS_SIG  "L" CHAIN_OBJECT_CLASS_NAME ";"

#define OBJECT_FIELD_NAME               "object"
#define REACHABLE_CHAIN_FIELD_NAME      "reachableChain"
#define UNREACHABLE_CHAIN_FIELD_NAME    "unreachableChain"
#define TAIL_FIELD_NAME                 "tail"

#define DEFAULT_CHAIN_LENGTH 4

typedef struct ObjectDescStruct {
    jlong tag;
    jint found;
    int collected;
} ObjectDesc;

static int chainLength = 0;
static int objectsCount = 0;

static ObjectDesc* objectDescList = NULL;

static volatile int foundUntagged = 0;

static int fakeUserData = 0;
static int userDataError = 0;

/* ============================================================================= */

/** Obtain chain of tested objects and tag them recursively. */
static int getChainObjects(jvmtiEnv* jvmti, JNIEnv* jni, jobject firstObject,
                                    jfieldID firstField, const char firstFieldName[],
                                    jfieldID nextField, const char nextFieldName[],
                                    int count, ObjectDesc objectDescList[],
                                    jlong tag, int reachable) {

    int success = NSK_TRUE;
    jobject obj = NULL;
    jlong objTag = (!reachable ? -tag : (tag % 2 != 0) ? tag : (jlong)0);

    if (count <= 0)
        return NSK_TRUE;

    count--;
    tag++;

    if (!NSK_JNI_VERIFY(jni, (obj =
            jni->GetObjectField(firstObject, firstField)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }

    objectDescList[count].tag = objTag;
    if (!NSK_JVMTI_VERIFY(
            jvmti->SetTag(obj, objTag))) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY2("        tag=%-5ld object=0x%p\n", (long)objTag, (void*)obj);

    if (!getChainObjects(jvmti, jni, obj, nextField, nextFieldName,
                                nextField, nextFieldName,
                                count, objectDescList, tag, reachable)) {
        return NSK_FALSE;
    }

    NSK_TRACE(jni->DeleteLocalRef(obj));
    return success;
}

/** Obtain all tested objects from debugee class and tag them recursively. */
static int getTestedObjects(jvmtiEnv* jvmti, JNIEnv* jni, int chainLength,
                                    int *objectsCount, ObjectDesc* *objectDescList) {
    jclass debugeeClass = NULL;
    jclass rootObjectClass = NULL;
    jclass chainObjectClass = NULL;

    jfieldID objectField = NULL;
    jfieldID reachableChainField = NULL;
    jfieldID unreachableChainField = NULL;
    jfieldID tailField = NULL;

    jobject rootObject = NULL;

    jlong chainObjectTag = 100;

    *objectsCount = 2 * chainLength;

    NSK_DISPLAY1("Allocate memory for objects list: %d objects\n", *objectsCount);
    if (!NSK_JVMTI_VERIFY(jvmti->Allocate((*objectsCount * sizeof(ObjectDesc)), (unsigned char**)objectDescList))) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... allocated array: 0x%p\n", (void*)objectDescList);

    {
        int k;
        for (k = 0; k < *objectsCount; k++) {
            (*objectDescList)[k].tag = 0;
            (*objectDescList)[k].found = 0;
            (*objectDescList)[k].collected = 0;
        }
    }

    NSK_DISPLAY1("Find debugee class: %s\n", DEBUGEE_CLASS_NAME);
    if (!NSK_JNI_VERIFY(jni, (debugeeClass =
            jni->FindClass(DEBUGEE_CLASS_NAME)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... found class: 0x%p\n", (void*)debugeeClass);

    NSK_DISPLAY1("Find root object class: %s\n", ROOT_OBJECT_CLASS_NAME);
    if (!NSK_JNI_VERIFY(jni, (rootObjectClass =
            jni->FindClass(ROOT_OBJECT_CLASS_NAME)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... found class: 0x%p\n", (void*)rootObjectClass);

    NSK_DISPLAY1("Find chain object class: %s\n", CHAIN_OBJECT_CLASS_NAME);
    if (!NSK_JNI_VERIFY(jni, (chainObjectClass =
            jni->FindClass(CHAIN_OBJECT_CLASS_NAME)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... found class: 0x%p\n", (void*)chainObjectClass);

    NSK_DISPLAY1("Find static field in debugee class: %s\n", OBJECT_FIELD_NAME);
    if (!NSK_JNI_VERIFY(jni, (objectField =
            jni->GetStaticFieldID(debugeeClass, OBJECT_FIELD_NAME, ROOT_OBJECT_CLASS_SIG)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got fieldID: 0x%p\n", (void*)objectField);

    NSK_DISPLAY1("Find instance field in root object class: %s\n", REACHABLE_CHAIN_FIELD_NAME);
    if (!NSK_JNI_VERIFY(jni, (reachableChainField =
            jni->GetFieldID(rootObjectClass, REACHABLE_CHAIN_FIELD_NAME, CHAIN_OBJECT_CLASS_SIG)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got fieldID: 0x%p\n", (void*)reachableChainField);

    NSK_DISPLAY1("Find instance field in root object class: %s\n", UNREACHABLE_CHAIN_FIELD_NAME);
    if (!NSK_JNI_VERIFY(jni, (unreachableChainField =
            jni->GetFieldID(rootObjectClass, UNREACHABLE_CHAIN_FIELD_NAME, CHAIN_OBJECT_CLASS_SIG)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got fieldID: 0x%p\n", (void*)unreachableChainField);

    NSK_DISPLAY1("Find instance field in chain object class: %s\n", TAIL_FIELD_NAME);
    if (!NSK_JNI_VERIFY(jni, (tailField =
            jni->GetFieldID(chainObjectClass, TAIL_FIELD_NAME, CHAIN_OBJECT_CLASS_SIG)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got fieldID: 0x%p\n", (void*)tailField);

    NSK_DISPLAY1("Get root object from static field: %s\n", OBJECT_FIELD_NAME);
    if (!NSK_JNI_VERIFY(jni, (rootObject =
            jni->GetStaticObjectField(debugeeClass, objectField)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    NSK_DISPLAY1("  ... got object: 0x%p\n", (void*)rootObject);

    NSK_DISPLAY0("Obtain and tag chain objects:\n");

    NSK_DISPLAY1("    reachable objects chain: %d objects\n", chainLength);
    if (!getChainObjects(jvmti, jni, rootObject,
                                reachableChainField, REACHABLE_CHAIN_FIELD_NAME,
                                tailField, TAIL_FIELD_NAME,
                                chainLength, *objectDescList,
                                chainObjectTag, NSK_TRUE)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }

    NSK_DISPLAY1("    unreachable objects chain: %d objects\n", chainLength);
    if (!getChainObjects(jvmti, jni, rootObject,
                                unreachableChainField, UNREACHABLE_CHAIN_FIELD_NAME,
                                tailField, TAIL_FIELD_NAME,
                                chainLength, *objectDescList + chainLength,
                                chainObjectTag, NSK_FALSE)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }

    return NSK_TRUE;
}

/** Check if tagged objects were iterated. */
static int checkTestedObjects(jvmtiEnv* jvmti, JNIEnv* jni,
                                int chainLength, ObjectDesc objectDescList[]) {
    int success = NSK_TRUE;
    int expectedUntagged = 0;
    int i;

    NSK_DISPLAY0("Following tagged/untagged objects were iterated:\n");

    NSK_DISPLAY0("    reachable objects:\n");
    for (i = 0; i < chainLength; i++) {
        NSK_DISPLAY2("        tag=%-5ld iterated=%d times\n",
                        (long)objectDescList[i].tag, objectDescList[i].found);

        if (objectDescList[i].found <= 0
                    && objectDescList[i].tag != 0) {
            NSK_COMPLAIN2("Reachable tagged object was not iterated:\n"
                          "#   tag:      %ld\n"
                          "#   iterated: %d times\n",
                            (long)objectDescList[i].tag,
                            objectDescList[i].found);
            nsk_jvmti_setFailStatus();
        }

        if (objectDescList[i].tag == 0) {
            expectedUntagged++;
        }
    }

    NSK_DISPLAY0("    unreachable objects:\n");
    for (i = 0; i < chainLength; i++) {
        NSK_DISPLAY3("        tag=%-5ld iterated=%-3d collected=%d times\n",
                        (long)objectDescList[i + chainLength].tag,
                        objectDescList[i + chainLength].found,
                        objectDescList[i + chainLength].collected);

        if (objectDescList[i + chainLength].found <= 0
                    && objectDescList[i + chainLength].collected <= 0) {
            NSK_COMPLAIN2("Not collected unreachable tagged object was not iterated:\n"
                          "#   tag:      %ld\n"
                          "#   iterated: %d times\n",
                            (long)objectDescList[i + chainLength].tag,
                            objectDescList[i + chainLength].found);
            nsk_jvmti_setFailStatus();
        }
    }

    NSK_DISPLAY0("    untagged objects:\n");
    NSK_DISPLAY2("        minimum=%-3d iterated=%d objects\n",
                                    expectedUntagged, foundUntagged);
    if (foundUntagged < expectedUntagged) {
        NSK_COMPLAIN2("Unexpected number of untagged objects were iterated:\n"
                      "#   iterated untagged objects: %d\n"
                      "#   expected at least:         %d\n",
                        foundUntagged, expectedUntagged);
        nsk_jvmti_setFailStatus();
    }

    return NSK_TRUE;
}

/** Release references to the tested objects and free allocated memory. */
static int releaseTestedObjects(jvmtiEnv* jvmti, JNIEnv* jni, int chainLength,
                                                    ObjectDesc* objectDescList) {
    if (objectDescList != NULL) {
        NSK_DISPLAY1("Deallocate objects list: 0x%p\n", (void*)objectDescList);
        if (!NSK_JVMTI_VERIFY(
            jvmti->Deallocate((unsigned char*)objectDescList))) {
            nsk_jvmti_setFailStatus();
        }
    }

    return NSK_TRUE;
}

/* ============================================================================= */

/** heapRootCallback for heap iterator. */
jvmtiIterationControl JNICALL
heapObjectCallback(jlong class_tag, jlong size, jlong* tag_ptr, void* user_data) {

    if (info & INFO_HEAPOBJ) {
        NSK_DISPLAY3("  heapObjectCallback: class_tag=%-3ld size=%-3ld *tag_ptr=%-5ld\n",
                        (long)class_tag, (long)size,
                        (long)(tag_ptr == NULL ? (jlong)0 : *tag_ptr));
    } else if ((info & INFO_TAGGED) != 0 &&
                    tag_ptr != NULL && *tag_ptr != 0) {
        NSK_DISPLAY3("  heapObjectCallback: class_tag=%-3ld size=%-3ld *tag_ptr=%-5ld\n",
                        (long)class_tag, (long)size,
                        (long)*tag_ptr);
    }

    if (class_tag != 0) {
        NSK_COMPLAIN3("Unexpected class_tag passed to heapObjectCallback:\n"
                      "#   object tag:     %ld\n"
                      "#   class_tag:      %ld\n"
                      "#   size:           %ld\n",
                        (long)*tag_ptr,
                        (long)class_tag,
                        (long)size);
        nsk_jvmti_setFailStatus();
    }

    if (tag_ptr == NULL) {
        NSK_COMPLAIN3("NULL tag_ptr is passed to heapObjectCallback:\n"
                      "#   tag_ptr:        0x%p\n"
                      "#   class_tag:      %ld\n"
                      "#   size:           %ld\n",
                        (void*)tag_ptr,
                        (long)class_tag,
                        (long)size);
        nsk_jvmti_setFailStatus();
    } else {
        if (*tag_ptr == 0) {
            foundUntagged++;
        } else {
            int found = 0;
            int i;

            for (i = 0; i < objectsCount; i++) {
                if (*tag_ptr == objectDescList[i].tag) {
                    found++;
                    objectDescList[i].found++;
                    break;
                }
            }

            if (found <= 0) {
                NSK_COMPLAIN3("Unknown tagged object passed to heapObjectCallback:\n"
                              "#   tag:            %ld\n"
                              "#   class_tag:      %ld\n"
                              "#   size:           %ld\n",
                                (long)*tag_ptr,
                                (long)class_tag,
                                (long)size);
                nsk_jvmti_setFailStatus();
            }
        }
    }

    if (user_data != &fakeUserData && !userDataError) {
       NSK_COMPLAIN2("Unexpected user_data is passed to heapObjectCallback:\n"
                      "#   expected:       0x%p\n"
                      "#   actual:         0x%p\n",
                      user_data,
                      &fakeUserData);
        nsk_jvmti_setFailStatus();
        userDataError++;
    }

    return JVMTI_ITERATION_CONTINUE;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    NSK_DISPLAY0("Wait for tested objects created\n");
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
        return;

    {
        NSK_DISPLAY0(">>> Obtain and tag tested objects from debugee class\n");
        {
            if (!NSK_VERIFY(getTestedObjects(jvmti, jni, chainLength,
                                                &objectsCount, &objectDescList)))
                return;
        }

        NSK_DISPLAY0(">>> Enable OBJECT_FREE event and let debugee to clean links to unreachable objects\n");
        {
            jvmtiEvent event = JVMTI_EVENT_OBJECT_FREE;
            if (!NSK_VERIFY(nsk_jvmti_enableEvents(JVMTI_ENABLE, 1, &event, NULL)))
                return;

            if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
                return;
            if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout)))
                return;
        }

        NSK_DISPLAY0(">>> Iterate over all object in heap with filter JVMTI_HEAP_OBJECT_EITHER\n");
        {
            if (!NSK_JVMTI_VERIFY(
                    jvmti->IterateOverHeap(JVMTI_HEAP_OBJECT_EITHER, heapObjectCallback, &fakeUserData))) {
                nsk_jvmti_setFailStatus();
                return;
            }
        }

        NSK_DISPLAY0(">>> Disable OBJECT_FREE event and check if tagged/untagged objects were iterated:\n");
        {
            jvmtiEvent event = JVMTI_EVENT_OBJECT_FREE;
            if (!NSK_VERIFY(nsk_jvmti_enableEvents(JVMTI_DISABLE, 1, &event, NULL)))
                return;

            if (!checkTestedObjects(jvmti, jni, chainLength, objectDescList)) {
                nsk_jvmti_setFailStatus();
            }
        }

        NSK_DISPLAY0(">>> Clean used data\n");
        {
            if (!NSK_VERIFY(releaseTestedObjects(jvmti, jni, chainLength, objectDescList)))
                return;
        }
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

/* ============================================================================= */

JNIEXPORT void JNICALL
callbackObjectFree(jvmtiEnv* jvmti, jlong tag) {
    int i;

    if (info & INFO_HEAPOBJ) {
        NSK_DISPLAY1("  <ObjectFree>: tag=%-5ld\n", tag);
    }

    if (tag != 0) {
        for (i = 0; i < objectsCount; i++) {
            if (tag == objectDescList[i].tag) {
                objectDescList[i].collected++;
                break;
            }
        }
    }
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_iterheap001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_iterheap001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_iterheap001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    {
        const char* infoOpt = nsk_jvmti_findOptionValue("info");
        if (infoOpt != NULL) {
            if (strcmp(infoOpt, "none") == 0)
                info = INFO_NONE;
            else if (strcmp(infoOpt, "all") == 0)
                info = INFO_ALL;
            else if (strcmp(infoOpt, "objref") == 0)
                info = INFO_OBJREF;
            else if (strcmp(infoOpt, "stackref") == 0)
                info = INFO_STACKREF;
            else if (strcmp(infoOpt, "heaproot") == 0)
                info = INFO_HEAPROOT;
            else if (strcmp(infoOpt, "heapobj") == 0)
                info = INFO_HEAPOBJ;
            else if (strcmp(infoOpt, "tagged") == 0)
                info = INFO_TAGGED;
            else {
                NSK_COMPLAIN1("Unknown option value: info=%s\n", infoOpt);
                return JNI_ERR;
            }
        }
    }

    chainLength = nsk_jvmti_findOptionIntValue("objects", DEFAULT_CHAIN_LENGTH);
    if (!NSK_VERIFY(chainLength > 0))
        return JNI_ERR;

    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    {
        jvmtiCapabilities caps;

        memset(&caps, 0, sizeof(caps));
        caps.can_tag_objects = 1;
        caps.can_generate_object_free_events = 1;
        if (!NSK_JVMTI_VERIFY(
                jvmti->AddCapabilities(&caps))) {
            return JNI_ERR;
        }
    }

    {
        jvmtiEventCallbacks eventCallbacks;
        memset(&eventCallbacks, 0, sizeof(eventCallbacks));
        eventCallbacks.ObjectFree = callbackObjectFree;
        if (!NSK_JVMTI_VERIFY(
                jvmti->SetEventCallbacks(&eventCallbacks, sizeof(eventCallbacks))))
            return JNI_ERR;
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
