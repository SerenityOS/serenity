/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

#define DEBUGEE_CLASS_NAME      "nsk/jvmti/unit/FollowReferences/followref002"
#define ROOT_OBJECT_CLASS_NAME  "nsk/jvmti/unit/FollowReferences/followref002RootTestedClass"
#define ROOT_OBJECT_CLASS_SIG   "L" ROOT_OBJECT_CLASS_NAME ";"
#define CHAIN_OBJECT_CLASS_NAME "nsk/jvmti/unit/FollowReferences/followref002TestedClass"
#define CHAIN_OBJECT_CLASS_SIG  "L" CHAIN_OBJECT_CLASS_NAME ";"

#define OBJECT_FIELD_NAME               "object"
#define REACHABLE_CHAIN_FIELD_NAME      "reachableChain"
#define UNREACHABLE_CHAIN_FIELD_NAME    "unreachableChain"
#define TAIL_FIELD_NAME                 "tail"


#define DEFAULT_CHAIN_LENGTH 3

typedef struct ObjectDescStruct {
    jlong tag;           /* Tag of the object */
    jlong exp_class_tag; /* Expected tag of the object class */
    jlong class_tag;     /* Reported tag of the object class */
    jint  exp_found;     /* Expected number of iterations through the object */
    jint  found;         /* Reported number of iterations through the object */
} ObjectDesc;

static int chainLength   = 0;
static int objectsCount  = 0;
static int fakeUserData  = 0;
static int userDataError = 0;

static ObjectDesc* objectDescList = NULL;

static jlong rootClassTag   = 9;
static jlong chainClassTag  = 99;
static jlong rootObjectTag  = 10;
static jlong chainObjectTag = 100;

static jvmtiHeapCallbacks heapCallbacks;

static const char* ref_kind_str[28] = {
   "unknown_0",
   "JVMTI_HEAP_REFERENCE_CLASS",
   "JVMTI_HEAP_REFERENCE_FIELD",
   "JVMTI_HEAP_REFERENCE_ARRAY_ELEMENT",
   "JVMTI_HEAP_REFERENCE_CLASS_LOADER",
   "JVMTI_HEAP_REFERENCE_SIGNERS",
   "JVMTI_HEAP_REFERENCE_PROTECTION_DOMAIN",
   "JVMTI_HEAP_REFERENCE_INTERFACE",
   "JVMTI_HEAP_REFERENCE_STATIC_FIELD",
   "JVMTI_HEAP_REFERENCE_CONSTANT_POOL",
   "unknown_10", "unknown_11", "unknown_12",
   "unknown_13", "unknown_14", "unknown_15", "unknown_16",
   "unknown_17", "unknown_18", "unknown_19", "unknown_20",
   "JVMTI_HEAP_REFERENCE_JNI_GLOBAL",
   "JVMTI_HEAP_REFERENCE_SYSTEM_CLASS",
   "JVMTI_HEAP_REFERENCE_MONITOR",
   "JVMTI_HEAP_REFERENCE_STACK_LOCAL",
   "JVMTI_HEAP_REFERENCE_JNI_LOCAL",
   "JVMTI_HEAP_REFERENCE_THREAD",
   "JVMTI_HEAP_REFERENCE_OTHER"
};

#define DEREF(ptr) (((ptr) == NULL ? 0 : *(ptr)))

/* ============================================================================= */

/** Obtain chain of tested objects and tag them recursively. */
static int getAndTagChainObjects(
    jvmtiEnv*  jvmti,
    JNIEnv*    jni,
    jobject    firstObject,
    jfieldID   firstField,
    const char firstFieldName[],
    jfieldID   nextField,
    const char nextFieldName[],
    int        count,
    ObjectDesc objectDescList[],
    jlong      tag,
    int        reachable)
{
    jobject obj = NULL;
    jlong objTag = (reachable ? tag : -tag);

    if (count <= 0)
        return NSK_TRUE;

    count--;
    tag++;

    if (!NSK_JNI_VERIFY(jni, (obj = jni->GetObjectField(firstObject, firstField)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }

    objectDescList[count].tag = objTag;
    if (reachable) {
        objectDescList[count].exp_found++;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(obj, objTag))) {
        nsk_jvmti_setFailStatus();
    }
    printf("        tag=%-5ld object=0x%p\n", (long)objTag, (void*)obj);
    fflush(0);
    if (!getAndTagChainObjects(jvmti, jni, obj,
                               nextField,
                               nextFieldName,
                               nextField,
                               nextFieldName,
                               count,
                               objectDescList,
                               tag,
                               reachable)) {
        return NSK_FALSE;
    }

    NSK_TRACE(jni->DeleteLocalRef(obj));
    return NSK_TRUE;
} /* getAndTagChainObjects */

/** Obtain all tested objects from debugee class and tag them recursively. */
static int getAndTagTestedObjects(
    jvmtiEnv*    jvmti,
    JNIEnv*      jni,
    int          chainLength,
    int*         objectsCount,
    ObjectDesc** objectDescList,
    jobject*     rootObject)
{
    jclass debugeeClass = NULL;
    jclass rootObjectClass = NULL;
    jclass chainObjectClass = NULL;

    jfieldID objectField = NULL;
    jfieldID reachableChainField = NULL;
    jfieldID unreachableChainField = NULL;
    jfieldID tailField = NULL;

    /* root object + reachable and unreachable object chains */
    *objectsCount = 1 + 2 * chainLength;

    printf("Allocate memory for objects list: %d objects\n", *objectsCount);
    fflush(0);
    if (!NSK_JVMTI_VERIFY(jvmti->Allocate((*objectsCount * sizeof(ObjectDesc)),
                                          (unsigned char**)objectDescList))) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    printf("  ... allocated array: 0x%p\n", (void*)objectDescList);
    fflush(0);

    {
        int k;
        for (k = 0; k < *objectsCount; k++) {
            (*objectDescList)[k].tag = 0;
            (*objectDescList)[k].exp_class_tag = chainClassTag;
            (*objectDescList)[k].exp_found = 0;
            (*objectDescList)[k].found = 0;
        }
    }
    (*objectDescList)[0].exp_class_tag = rootClassTag;

    printf("Find debugee class: %s\n", DEBUGEE_CLASS_NAME);
    fflush(0);
    if (!NSK_JNI_VERIFY(jni, (debugeeClass = jni->FindClass(DEBUGEE_CLASS_NAME)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    printf("  ... found class: 0x%p\n", (void*)debugeeClass);

    printf("Find root object class: %s\n", ROOT_OBJECT_CLASS_NAME);
    fflush(0);
    if (!NSK_JNI_VERIFY(jni, (rootObjectClass = jni->FindClass(ROOT_OBJECT_CLASS_NAME)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    printf("  ... found class: 0x%p\n", (void*)rootObjectClass);

    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(rootObjectClass, rootClassTag))) {
        nsk_jvmti_setFailStatus();
    }
    printf("        tag=%-5ld rootClass=0x%p\n", (long)rootClassTag, (void*)rootObjectClass);

    printf("Find chain object class: %s\n", CHAIN_OBJECT_CLASS_NAME);
    fflush(0);
    if (!NSK_JNI_VERIFY(jni, (chainObjectClass =
            jni->FindClass(CHAIN_OBJECT_CLASS_NAME)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    printf("  ... found class: 0x%p\n", (void*)chainObjectClass);

    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(chainObjectClass, chainClassTag))) {
        nsk_jvmti_setFailStatus();
    }
    printf("        tag=%-5ld chainClass=0x%p\n", (long)chainClassTag, (void*)chainObjectClass);

    printf("Find static field in debugee class: %s\n", OBJECT_FIELD_NAME);
    fflush(0);
    if (!NSK_JNI_VERIFY(jni, (objectField =
            jni->GetStaticFieldID(debugeeClass, OBJECT_FIELD_NAME, ROOT_OBJECT_CLASS_SIG)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    printf("  ... got fieldID: 0x%p\n", (void*)objectField);

    printf("Find instance field in root object class: %s\n", REACHABLE_CHAIN_FIELD_NAME);
    fflush(0);
    if (!NSK_JNI_VERIFY(jni, (reachableChainField =
            jni->GetFieldID(rootObjectClass, REACHABLE_CHAIN_FIELD_NAME, CHAIN_OBJECT_CLASS_SIG)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    printf("  ... got fieldID: 0x%p\n", (void*)reachableChainField);

    printf("Find instance field in root object class: %s\n", UNREACHABLE_CHAIN_FIELD_NAME);
    fflush(0);
    if (!NSK_JNI_VERIFY(jni, (unreachableChainField =
            jni->GetFieldID(rootObjectClass, UNREACHABLE_CHAIN_FIELD_NAME, CHAIN_OBJECT_CLASS_SIG)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    printf("  ... got fieldID: 0x%p\n", (void*)unreachableChainField);

    printf("Find instance field in chain object class: %s\n", TAIL_FIELD_NAME);
    fflush(0);
    if (!NSK_JNI_VERIFY(jni, (tailField =
            jni->GetFieldID(chainObjectClass, TAIL_FIELD_NAME, CHAIN_OBJECT_CLASS_SIG)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    printf("  ... got fieldID: 0x%p\n", (void*)tailField);

    printf("Get root object from static field: %s\n", OBJECT_FIELD_NAME);
    fflush(0);
    if (!NSK_JNI_VERIFY(jni, (*rootObject =
            jni->GetStaticObjectField(debugeeClass, objectField)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    printf("  ... got object: 0x%p\n", (void*)*rootObject);
    fflush(0);

    if (!NSK_JNI_VERIFY(jni, (*rootObject = jni->NewGlobalRef(*rootObject)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    printf("  ... global ref: 0x%p\n", (void*)*rootObject);

    printf("Obtain and tag chain objects:\n");

    printf("    root tested object:\n");
    fflush(0);
    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(*rootObject, rootObjectTag))) {
        nsk_jvmti_setFailStatus();
    }
    printf("        tag=%-5ld object=0x%p\n", (long)rootObjectTag, (void*)*rootObject);

    (*objectDescList)[0].tag = rootObjectTag;

    /* Root object must be referenced 1 time */
    (*objectDescList)[0].exp_found = 1;

    /* Object with tag=101 must be referenced 2 times */
    (*objectDescList)[chainLength].exp_found = 1;

    printf("    reachable objects chain: %d objects\n", chainLength);
    fflush(0);
    if (!getAndTagChainObjects(jvmti, jni, *rootObject,
                               reachableChainField,
                               REACHABLE_CHAIN_FIELD_NAME,
                               tailField,
                               TAIL_FIELD_NAME,
                               chainLength,
                               (*objectDescList) + 1,
                               chainObjectTag,
                               NSK_TRUE)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }

    printf("    unreachable objects chain: %d objects\n", chainLength);
    if (!getAndTagChainObjects(jvmti, jni, *rootObject,
                               unreachableChainField,
                               UNREACHABLE_CHAIN_FIELD_NAME,
                               tailField,
                               TAIL_FIELD_NAME,
                               chainLength,
                               (*objectDescList) + 1 + chainLength,
                               chainObjectTag,
                               NSK_FALSE)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }

    return NSK_TRUE;
} /* getAndTagTestedObjects */

/** Check if tagged objects were iterated. */
static int checkTestedObjects(jvmtiEnv* jvmti, JNIEnv* jni,
                                int chainLength, ObjectDesc objectDescList[]) {
    int success = NSK_TRUE;
    int i, idx;

    printf("Following tagged objects were iterated:\n");

    printf("Root tested object:\n");
    printf("   tag:                 %ld\n"
           "   expected to iterate: %d times\n"
           "   iterated:            %d times\n",
           (long) objectDescList[0].tag,
                  objectDescList[0].exp_found,
                  objectDescList[0].found);
    if (objectDescList[0].found != objectDescList[0].exp_found) {
        NSK_COMPLAIN1("Root tested object unexpectedly iterated %d times\n",
                      objectDescList[0].found);
        nsk_jvmti_setFailStatus();
    }

    printf("\nReachable objects:\n");
    fflush(0);
    for (i = 0; i < chainLength; i++) {
        idx = i + 1;
        printf("Reachable object:\n"
               "   tag:                 %-3ld\n"
               "   expected to iterate: %d times\n"
               "   iterated:            %d times\n",
                (long) objectDescList[idx].tag,
                       objectDescList[idx].exp_found,
                       objectDescList[idx].found);
        if (objectDescList[i + 1].found <= 0 && objectDescList[i + 1].exp_found > 0) {
            NSK_COMPLAIN0("Reachable object was not iterated\n");
            nsk_jvmti_setFailStatus();
        }
        if (objectDescList[idx].found != objectDescList[idx].exp_found) {
            NSK_COMPLAIN0("Reachable object was iterated unexpected number of times\n");
            nsk_jvmti_setFailStatus();
        }
    }

    printf("\nUnreachable objects:\n");
    for (i = 0; i < chainLength; i++) {
        idx = i + 1 + chainLength;

        printf("Unreachable object:\n"
               "   tag:                 %ld\n"
               "   expected to iterate: %d times\n"
               "   iterated:            %d times\n",
                (long) objectDescList[idx].tag,
                       objectDescList[idx].exp_found,
                       objectDescList[idx].found);
        if (objectDescList[idx].found > 0) {
            NSK_COMPLAIN0("Unreachable object was iterated\n");
            nsk_jvmti_setFailStatus();
        }
        fflush(0);
    }

    return NSK_TRUE;
} /* checkTestedObjects */

/** Release references to the tested objects and free allocated memory. */
static int releaseTestedObjects(jvmtiEnv* jvmti, JNIEnv* jni, int chainLength,
                                ObjectDesc* objectDescList, jobject rootObject) {
    if (rootObject != NULL) {
        printf("Release object reference to root tested object: 0x%p\n", rootObject);
        NSK_TRACE(jni->DeleteGlobalRef(rootObject));
    }

    if (objectDescList != NULL) {
        printf("Deallocate objects list: 0x%p\n", (void*)objectDescList);
        if (!NSK_JVMTI_VERIFY(jvmti->Deallocate((unsigned char*)objectDescList))) {
            nsk_jvmti_setFailStatus();
        }
    }

    fflush(0);
    return NSK_TRUE;
} /* releaseTestedObjects */

/* ============================================================================= */

/** heapReferenceCallback for heap iterator. */
jint JNICALL heapReferenceCallback(
     jvmtiHeapReferenceKind        reference_kind,
     const jvmtiHeapReferenceInfo* reference_info,
     jlong                         class_tag,
     jlong                         referrer_class_tag,
     jlong                         size,
     jlong*                        tag_ptr,
     jlong*                        referrer_tag_ptr,
     jint                          length,
     void*                         user_data)
{
    jint referrer_index = 0;
    jlong tag     = DEREF(tag_ptr);
    jlong ref_tag = DEREF(referrer_tag_ptr);

    switch (reference_kind) {
        case JVMTI_HEAP_REFERENCE_CONSTANT_POOL:
            referrer_index = reference_info->constant_pool.index;
            break;
        case JVMTI_HEAP_REFERENCE_FIELD:
        case JVMTI_HEAP_REFERENCE_STATIC_FIELD:
            referrer_index = reference_info->field.index;
            break;
        case JVMTI_HEAP_REFERENCE_ARRAY_ELEMENT:
            referrer_index = reference_info->array.index;
            break;
        case JVMTI_HEAP_REFERENCE_STACK_LOCAL:
            referrer_index = reference_info->stack_local.slot;
            /* Fall through */
        case JVMTI_HEAP_REFERENCE_JNI_LOCAL:
            referrer_index |= reference_info->stack_local.depth << 16;
            break;
        default:
            // TODO: check that realy should be done w/ other jvmtiHeapReferenceKind
            break;
    }

    printf("     heapReferenceCallback: ref=%s, class_tag=%-3ld, tag=%-3ld,"
           " size=%-3ld, ref_tag=%-3ld, ref_idx=%#x\n",
           ref_kind_str[reference_kind],
           (long) class_tag,
           (long) tag,
           (long) size,
           (long) ref_tag,
           (int) referrer_index);
    fflush(0);

    if (tag_ptr == NULL) {
        NSK_COMPLAIN1("NULL tag_ptr is passed to heapReferenceCallback:"
                      " tag_ptr=0x%p\n", (void*)tag_ptr);
        nsk_jvmti_setFailStatus();
    }

    if (tag_ptr != NULL && *tag_ptr != 0) {
        int found = 0;
        int i;

        for (i = 0; i < objectsCount; i++) {
            if (*tag_ptr == objectDescList[i].tag) {
                found++;
                objectDescList[i].found++;

                if (*tag_ptr < 0) {
                    NSK_COMPLAIN0("Unreachable tagged object is passed to heapReferenceCallback\n");
                    nsk_jvmti_setFailStatus();
                }
                break;
            }
        }

        if (reference_kind != JVMTI_HEAP_REFERENCE_CLASS && found <= 0) {
            NSK_COMPLAIN0("Unknown tagged object is passed to heapReferenceCallback\n");
            nsk_jvmti_setFailStatus();
        }
    }

    if (user_data != &fakeUserData && !userDataError) {
       NSK_COMPLAIN2("Unexpected user_data is passed to heapReferenceCallback:\n"
                      "   expected:       0x%p\n"
                      "   actual:         0x%p\n",
                      user_data,
                      &fakeUserData);
        nsk_jvmti_setFailStatus();
        userDataError++;
    }

    switch (reference_kind) {
        int i;
        case JVMTI_HEAP_REFERENCE_CLASS: {
            if (tag == 0) {
                break;
            }
            if (tag != rootClassTag && tag != chainClassTag) {
                NSK_COMPLAIN0("Unknown tagged class is passed to heapReferenceCallback\n");
                nsk_jvmti_setFailStatus();
            }
            for (i = 0; i < objectsCount; i++) {
               if (ref_tag == objectDescList[i].tag) {
                   if (objectDescList[i].exp_class_tag != tag) {
                       NSK_COMPLAIN2("Wrong tag in heapReferenceCallback/JVMTI_HEAP_REFERENCE_CLASS:\n"
                                     "Expected: %-3ld\n"
                                     "Passed:   %-3ld\n",
                                      objectDescList[i].exp_class_tag,
                                      tag);
                       nsk_jvmti_setFailStatus();
                   }
                   break;
               }
            }
            break;
        }
        case JVMTI_HEAP_REFERENCE_JNI_GLOBAL:
        case JVMTI_HEAP_REFERENCE_SYSTEM_CLASS:
        case JVMTI_HEAP_REFERENCE_MONITOR:
        case JVMTI_HEAP_REFERENCE_STACK_LOCAL:
        case JVMTI_HEAP_REFERENCE_JNI_LOCAL:
        case JVMTI_HEAP_REFERENCE_THREAD:
        case JVMTI_HEAP_REFERENCE_OTHER: {
            NSK_COMPLAIN1("This reference kind was not expected: %s\n",
                           ref_kind_str[reference_kind]);
            fflush(0);
            nsk_jvmti_setFailStatus();
            return 0;
        }
        default:
            // TODO: check that realy should be done w/ other jvmtiHeapReferenceKind
            break;
    }
    return JVMTI_VISIT_OBJECTS;
}

jint JNICALL primitiveFieldCallback(
     jvmtiHeapReferenceKind        reference_kind,
     const jvmtiHeapReferenceInfo* reference_info,
     jlong                         class_tag,
     jlong*                        tag_ptr,
     jvalue                        value,
     jvmtiPrimitiveType            value_type,
     void*                         user_data)
{
    printf(" primitiveFieldCallback: ref=%s, class_tag=%-3ld, tag=%-3ld, type=%c\n",
           ref_kind_str[reference_kind],
           (long) class_tag,
           (long) DEREF(tag_ptr),
           (int) value_type);
    fflush(0);
    return 0;
}

jint JNICALL arrayPrimitiveValueCallback(
     jlong              class_tag,
     jlong              size,
     jlong*             tag_ptr,
     jint               element_count,
     jvmtiPrimitiveType element_type,
     const void*        elements,
     void*              user_data)
{
    printf(" arrayPrimitiveValueCallback: class_tag=%-3ld, tag=%-3ld, len=%d, type=%c\n",
           (long) class_tag,
           (long) DEREF(tag_ptr),
           (int) element_count,
           (int) element_type);
    fflush(0);
    return 0;
}

jint JNICALL stringPrimitiveValueCallback(
     jlong        class_tag,
     jlong        size,
     jlong*       tag_ptr,
     const jchar* value,
     jint         value_length,
     void*        user_data)
{
    printf("stringPrimitiveValueCallback: class_tag=%-3ld, tag=%-3ld, len=%d\n",
           (long) class_tag,
           (long) DEREF(tag_ptr),
           (int) value_length);
    fflush(0);
    return 0;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    jobject rootObject = NULL;

    printf("Wait for tested objects created\n");
    fflush(0);
    if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout))) {
        return;
    }

    printf(">>> Obtain and tag tested objects from debugee class\n");
    fflush(0);
    {
        if (!NSK_VERIFY(getAndTagTestedObjects(jvmti, jni,
                                               chainLength, &objectsCount,
                                               &objectDescList, &rootObject))) {
            return;
        }
    }

    printf(">>> Let debugee to clean links to unreachable objects\n");
    fflush(0);
    {
        if (!NSK_VERIFY(nsk_jvmti_resumeSync())) {
            return;
        }
        if (!NSK_VERIFY(nsk_jvmti_waitForSync(timeout))) {
            return;
        }
    }

    printf("\n\n>>> Start 1-st iteration for root tested object: 0x%p\n", rootObject);
    fflush(0);
    {
        jint heap_filter = JVMTI_HEAP_FILTER_UNTAGGED | JVMTI_HEAP_FILTER_CLASS_UNTAGGED;
        if (!NSK_JVMTI_VERIFY(jvmti->FollowReferences(heap_filter,
                                                      (jclass) NULL, /* class          */
                                                      rootObject,    /* initial_object */
                                                      &heapCallbacks,
                                                      (const void *) &fakeUserData))) {
             nsk_jvmti_setFailStatus();
             return;
        }
    }

    printf(">>> Check if reachable objects were iterated\n");
    fflush(0);
    {
        if (!checkTestedObjects(jvmti, jni, chainLength, objectDescList)) {
            nsk_jvmti_setFailStatus();
        }
    }


    {            /* Reinstall the expectations */
        int k;
        for (k = 0; k < objectsCount; k++) {
            (objectDescList)[k].exp_found = 0;
            (objectDescList)[k].found = 0;
        }
    }

    printf("\n\n>>> Start 2-nd iteration for root tested object: 0x%p\n", rootObject);
    fflush(0);
    {
        /* This time everythig is filtered out */
        jint heap_filter = JVMTI_HEAP_FILTER_UNTAGGED | JVMTI_HEAP_FILTER_CLASS_UNTAGGED |
                           JVMTI_HEAP_FILTER_TAGGED   | JVMTI_HEAP_FILTER_CLASS_TAGGED;
        if (!NSK_JVMTI_VERIFY(jvmti->FollowReferences(heap_filter,
                                                      (jclass) NULL, /* class          */
                                                      rootObject,    /* initial_object */
                                                      &heapCallbacks,
                                                      (const void *) &fakeUserData))) {
             nsk_jvmti_setFailStatus();
             return;
        }
    }

    printf(">>> Check if reachable objects were not reported this time\n");
    fflush(0);
    {
        if (!checkTestedObjects(jvmti, jni, chainLength, objectDescList)) {
            nsk_jvmti_setFailStatus();
        }
    }

    printf(">>> Clean used data\n");
    fflush(0);
    {
        if (!NSK_VERIFY(releaseTestedObjects(jvmti, jni, chainLength, objectDescList, rootObject))) {
            return;
        }
    }

    printf("Let debugee to finish\n");
    fflush(0);
    if (!NSK_VERIFY(nsk_jvmti_resumeSync()))
        return;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_followref002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_followref002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_followref002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

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
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
            return JNI_ERR;
        }
    }

    /* Setting Heap Callbacks */
    heapCallbacks.heap_iteration_callback         = NULL;
    heapCallbacks.heap_reference_callback         = heapReferenceCallback;
    heapCallbacks.primitive_field_callback        = primitiveFieldCallback;
    heapCallbacks.array_primitive_value_callback  = arrayPrimitiveValueCallback;
    heapCallbacks.string_primitive_value_callback = stringPrimitiveValueCallback;

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
