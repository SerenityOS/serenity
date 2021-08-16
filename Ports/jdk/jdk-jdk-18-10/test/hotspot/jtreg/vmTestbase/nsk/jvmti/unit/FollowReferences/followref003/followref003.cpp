/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

static unsigned int info = INFO_ALL;

#define DEBUGEE_CLASS_NAME      "nsk/jvmti/unit/FollowReferences/followref003"
#define ROOT_OBJECT_CLASS_NAME  "nsk/jvmti/unit/FollowReferences/followref003RootTestedClass"
#define ROOT_OBJECT_CLASS_SIG   "L" ROOT_OBJECT_CLASS_NAME ";"
#define CHAIN_OBJECT_CLASS_NAME "nsk/jvmti/unit/FollowReferences/followref003TestedClass"
#define CHAIN_OBJECT_CLASS_SIG  "L" CHAIN_OBJECT_CLASS_NAME ";"

#define OBJECT_FIELD_NAME               "object"
#define REACHABLE_CHAIN_FIELD_NAME      "reachableChain"
#define UNREACHABLE_CHAIN_FIELD_NAME    "unreachableChain"
#define TAIL_FIELD_NAME                 "tail"


#define DEFAULT_CHAIN_LENGTH 3
#define MAXDEPTH 50
#define MAXSLOT  16

typedef struct ObjectDescStruct {
    jlong tag;
    jlong class_tag;
    jlong exp_class_tag;
    jint exp_found;
    jint found;
} ObjectDesc;

static int chainLength   = 0;
static int objectsCount  = 0;
static int fakeUserData  = 0;
static int userDataError = 0;

static ObjectDesc* objectDescList = NULL;

#define TARG_THREAD_TAG  11
#define FIRST_THREAD_TAG (TARG_THREAD_TAG + 1)

#define TARG_FRAME_DEPTH  1

static jlong rootClassTag   = 9;
static jlong chainClassTag  = 99;
static jlong thrObjectTag   = FIRST_THREAD_TAG;
static jlong rootObjectTag  = 55;
static jlong chainObjectTag = 100;


/* Java method frame slots interesting to check */
#define ARGV_STRING_ARR_SLOT   1
#define FIRST_PRIM_ARR_SLOT    3
#define LAST_PRIM_ARR_SLOT     10
#define DUMMY_STRING_ARR_SLOT  11


static jvmtiHeapCallbacks heapCallbacks;

static const char* ref_kind_str[28] = {
   "unknown_0",
   "REFERENCE_CLASS",
   "REFERENCE_FIELD",
   "REFERENCE_ARRAY_ELEMENT",
   "REFERENCE_CLASS_LOADER",
   "REFERENCE_SIGNERS",
   "REFERENCE_PROTECTION_DOMAIN",
   "REFERENCE_INTERFACE",
   "REFERENCE_STATIC_FIELD",
   "REFERENCE_CONSTANT_POOL",
   "unknown_10", "unknown_11", "unknown_12",
   "unknown_13", "unknown_14", "unknown_15", "unknown_16",
   "unknown_17", "unknown_18", "unknown_19", "unknown_20",
   "REFERENCE_JNI_GLOBAL",
   "REFERENCE_SYSTEM_CLASS",
   "REFERENCE_MONITOR",
   "REFERENCE_STACK_LOCAL",
   "REFERENCE_JNI_LOCAL",
   "REFERENCE_THREAD",
   "REFERENCE_OTHER"
};


#define DEREF(ptr) (((ptr) == NULL ? 0 : *(ptr)))


/* ============================================================================= */

/** Obtain chain of tested objects and tag them recursively. */
static int getChainObjects(jvmtiEnv* jvmti, JNIEnv* jni, jobject firstObject,
                           jfieldID firstField, const char firstFieldName[],
                           jfieldID nextField, const char nextFieldName[],
                           int count, ObjectDesc objectDescList[],
                           jlong tag, int reachable) {
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

    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(obj, objTag))) {
        nsk_jvmti_setFailStatus();
    }
    printf("        tag=%-5ld object=0x%p\n", (long)objTag, (void*)obj);
    fflush(0);
    if (!getChainObjects(jvmti, jni, obj, nextField, nextFieldName,
                                nextField, nextFieldName,
                                count, objectDescList, tag, reachable)) {
        return NSK_FALSE;
    }

    NSK_TRACE(jni->DeleteLocalRef(obj));
    return NSK_TRUE;
}

/** Obtain all tested objects from debugee class and tag them recursively. */
static int getTestedObjects(jvmtiEnv* jvmti, JNIEnv* jni, int chainLength,
                            int *objectsCount, ObjectDesc* *objectDescList,
                            jobject* rootObject) {
    jclass debugeeClass = NULL;
    jclass rootObjectClass = NULL;
    jclass chainObjectClass = NULL;

    jfieldID objectField = NULL;
    jfieldID reachableChainField = NULL;
    jfieldID unreachableChainField = NULL;
    jfieldID tailField = NULL;

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
    printf("        tag=%-5ld rootClass=0x%p\n",
           (long)rootClassTag, (void*)rootObjectClass);

    printf("Find chain object class: %s\n", CHAIN_OBJECT_CLASS_NAME);
    fflush(0);
    if (!NSK_JNI_VERIFY(jni, (chainObjectClass =
            jni->FindClass(CHAIN_OBJECT_CLASS_NAME)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }
    printf("  ... found class: 0x%p\n",
           (void*)chainObjectClass);

    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(chainObjectClass, chainClassTag))) {
        nsk_jvmti_setFailStatus();
    }
    printf("        tag=%-5ld chainClass=0x%p\n",
           (long)chainClassTag, (void*)chainObjectClass);

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

    printf("    root tested object\n");
    fflush(0);
    if (!NSK_JVMTI_VERIFY(jvmti->SetTag(*rootObject, rootObjectTag))) {
        nsk_jvmti_setFailStatus();
    }
    printf("        tag=%-5ld object=0x%p\n",
           (long)rootObjectTag, (void*)*rootObject);

    /* Root object must be reported 1 time */
    (*objectDescList)[0].exp_found = 1;
    (*objectDescList)[0].tag = rootObjectTag;

    printf("    reachable objects chain: %d objects\n", chainLength);
    fflush(0);
    if (!getChainObjects(jvmti, jni, *rootObject,
                                reachableChainField, REACHABLE_CHAIN_FIELD_NAME,
                                tailField, TAIL_FIELD_NAME,
                                chainLength, (*objectDescList) + 1,
                                chainObjectTag, NSK_TRUE)) {
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }

    /* First unreachable object must be reported once
     * as JVMTI_HEAP_REFERENCE_STACK_LOCAL */
    (*objectDescList)[2 * chainLength].exp_found = 1;

    printf("    unreachable objects chain: %d objects\n", chainLength);
    if (!getChainObjects(jvmti, jni, *rootObject,
                                unreachableChainField, UNREACHABLE_CHAIN_FIELD_NAME,
                                tailField, TAIL_FIELD_NAME,
                                chainLength, (*objectDescList) + 1 + chainLength,
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
        if (objectDescList[idx].found > 0 && objectDescList[idx].exp_found == 0) {
            NSK_COMPLAIN0("Unreachable object was iterated\n");
            nsk_jvmti_setFailStatus();
        }
        fflush(0);
    }

    return NSK_TRUE;
}

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
}

/* ============================================================================= */

/* Some diagnostics happen in the first FollowReferences call only */
static int first_followref = 1;

typedef struct ThreadDescStruct {
    jlong tag;
    jlong id;
} ThreadDesc;

#define MAX_THREADS 1024
static ThreadDesc thrDesc [MAX_THREADS] = {};

static jlong registerThread(jlong thr_id, jlong thr_tag) {
    if (thr_id <= 0 || thr_id >= MAX_THREADS) {
        NSK_COMPLAIN1("Unexpected thread ID: %ld\n", thr_id);
        nsk_jvmti_setFailStatus();
        return 0;
    }
    if (thrDesc[thr_id].id == 0) {
        /* need to set the first occurence info */
        thrDesc[thr_id].id  = thr_id;
        thrDesc[thr_id].tag = thr_tag;
    } else if (thr_tag != thrDesc[thr_id].tag) {
        NSK_COMPLAIN3("Thread tag doesn't match the first occurence: thr_id= %ld\n"
               "\t first thr_tag=%#lx, curr thr_tag=%#lx\n",
               thr_id, thrDesc[thr_id].tag, thr_tag);
        nsk_jvmti_setFailStatus();
        return 0;
    }
    return thr_id;
} /* registerThread */

typedef struct FrameDescStruct {
    jlong     thr_id;
    jint      depth;
    jmethodID method;
} FrameDesc;

#define MAX_FRAMES  256
static FrameDesc frameDesc[MAX_FRAMES] = {};
static int curr_frame_id = 0;  /* Index 0 should not be used */

/* returns frame slot number in the table of frames */
static int registerFrame(jlong thr_id, jint depth, jmethodID method,
                         jvmtiHeapReferenceKind ref_kind)
{
    int idx;
    int failed = 0;

    FrameDesc *fr;
    if (depth < 0 || depth > MAXDEPTH) {
        NSK_COMPLAIN1("Incorrect frame depth: %ld\n", depth);
        failed = 1;
    }
    /* JNI_LOCAL references from native methods may not have a jmethodID.
     * (Do we have to clarify this in the JVMTI spec?)
     * Do not consider the test as failing in such a case.
     */
    if (method == NULL && ref_kind != JVMTI_HEAP_REFERENCE_JNI_LOCAL) {
        NSK_COMPLAIN0("methodID must not be NULL\n");
        failed = 1;
    }
    if (failed) {
        nsk_jvmti_setFailStatus();
        return 0;
    }

    /* Check if this frame was registered */
    for (idx = 1; idx <= curr_frame_id; idx++) {
        fr = &frameDesc[idx];
        if (fr->thr_id == thr_id && fr->depth == depth && fr->method == method) {
            return idx;
        }
    }
    if (++curr_frame_id >= MAX_FRAMES) {
        NSK_COMPLAIN1("Internal: Insufficient frames table size: %ld\n", MAX_FRAMES);
        return 0;
    }
    fr = &frameDesc[curr_frame_id];
    fr->thr_id = thr_id;
    fr->depth  = depth;
    fr->method = method;

    return curr_frame_id;
} /* registerFrame */


typedef struct LocalDescStruct {
    jint      frame_id;
    jlocation location;
    jint      slot;
    jlong     tag;
} LocalDesc;

#define MAX_LOCALS   100
static LocalDesc locDesc [MAX_LOCALS] = {};
static int curr_local_idx = 0;  /* Index 0 should not be used */

/* returns frame slot number in the table of frames */
static int registerLocal(jint frame_id, jlocation location, jint slot, jlong tag) {
    int idx;
    LocalDesc *loc;
    int failed = 0;

    if (slot < 0 || slot > MAXSLOT) {
        NSK_COMPLAIN1("Incorrect stack local slot#: %ld\n", slot);
        failed = 1;
    }
    if ((jlong) location == -1L) {
        NSK_COMPLAIN0("Location must not be -1\n");
        failed = 1;
    }

    if (failed) {
        nsk_jvmti_setFailStatus();
        return 0;
    }

    /* Check if this local was registered */
    for (idx = 1; idx <= curr_local_idx; idx++) {
        loc = &locDesc[idx];
        if (loc->frame_id == frame_id &&
            loc->slot == slot) {
            if (first_followref) {
                /* Do this check on the first FollowReferences call only */
                FrameDesc *fr = &frameDesc[frame_id];
                printf("Second report of the local: "
                       "loc_idx=%d, frame_id=%d, slot=%d\n",
                       idx, frame_id, slot);
                printf("\t thr_id=%" LL "d, depth=%d, meth=0x%p\n",
                       fr->thr_id, fr->depth, fr->method);
                failed = 1;
            }
            if (loc->tag != tag) {
                NSK_COMPLAIN2("Tag mismatch:      expected %#lx, passed: %#lx\n",
                               loc->tag, tag);
                failed = 1;
            }
            if (loc->location != location) {
                NSK_COMPLAIN2("Location mismatch: expected %ld, passed: %ld\n",
                               (long) loc->location, (long) location);
                failed = 1;
            }
            if (failed) {
                nsk_jvmti_setFailStatus();
                return 0;
            }
            return idx;
        }
    }
    if (++curr_local_idx >= MAX_LOCALS) {
        printf("Internal: Insufficient locals table size: %d\n", MAX_FRAMES);
        return 0;
    }
    loc = &locDesc[curr_local_idx];
    loc->frame_id = frame_id;
    loc->location = location;
    loc->slot     = slot;
    loc->tag      = tag;

    return curr_local_idx;
} /* registerLocal */


/** heapReferenceCallback for heap iterator. */
jint JNICALL heapReferenceCallback(
     jvmtiHeapReferenceKind        ref_kind,
     const jvmtiHeapReferenceInfo* reference_info,
     jlong                         class_tag,
     jlong                         referrer_class_tag,
     jlong                         size,
     jlong*                        tag_ptr,
     jlong*                        referrer_tag_ptr,
     jint                          length,
     void*                         user_data)
{
    jint depth         = -1;
    jint slot          = -1;
    jint index         = -1;
    jmethodID method   = (jmethodID) NULL;
    jlocation location = (jlocation)(-1);
    jlong tag          = DEREF(tag_ptr);
    jlong ref_tag      = DEREF(referrer_tag_ptr);
    jlong thr_tag      = -1;
    jlong thr_id       = -1;
    jlong thr_idx      = -1;
    int res            = -1;
    int meth_idx       = -1;

    switch (ref_kind) {
        case JVMTI_HEAP_REFERENCE_CONSTANT_POOL:
            index = reference_info->constant_pool.index;
            break;
        case JVMTI_HEAP_REFERENCE_FIELD:
        case JVMTI_HEAP_REFERENCE_STATIC_FIELD:
            index = reference_info->field.index;
            break;
        case JVMTI_HEAP_REFERENCE_ARRAY_ELEMENT:
            index = reference_info->array.index;
            break;
        case JVMTI_HEAP_REFERENCE_STACK_LOCAL:
            thr_tag  = reference_info->stack_local.thread_tag;
            thr_id   = reference_info->stack_local.thread_id;
            depth    = reference_info->stack_local.depth;
            method   = reference_info->stack_local.method;
            location = reference_info->stack_local.location;
            slot     = reference_info->stack_local.slot;
            index    = slot | depth << 16;
            break;
        case JVMTI_HEAP_REFERENCE_JNI_LOCAL:
            thr_tag  = reference_info->jni_local.thread_tag;
            thr_id   = reference_info->jni_local.thread_id;
            depth    = reference_info->jni_local.depth;
            method   = reference_info->jni_local.method;
            index    = depth;
            break;
        default:
            // TODO: check that realy should be done w/ other jvmtiHeapReferenceKind
            break;
    }

    if (ref_kind == JVMTI_HEAP_REFERENCE_OTHER      ||
        ref_kind == JVMTI_HEAP_REFERENCE_JNI_GLOBAL ||
        ref_kind == JVMTI_HEAP_REFERENCE_SYSTEM_CLASS) {
        return 0; /* Skip it as there is a plan to test it differently */
    }

    if (ref_kind == JVMTI_HEAP_REFERENCE_THREAD) {
        /* Target thread has been tagged already */
        if (tag == 0) {
            tag = *tag_ptr = thrObjectTag++;
            /* Just want to report new tag for thread object */
            printf("     heapReferenceCallback: ref=%s, tag=%-3ld, size=%-3ld\n",
                   ref_kind_str[ref_kind],
                   (long) *tag_ptr,
                   (long) size);
        }

        fflush(0);
        return 0;
    }

    printf("     heapReferenceCallback: ref=%s, class_tag=%-3ld, tag=%-3ld,"
           " size=%-3ld, len=%-2d\n"
           "\t\t ref_tag=%-" LL "d, thr_tag=%-3ld, thr_id=%" LL "d, "
           "meth=0x%p, loc=%ld, idx=%#x\n",
           ref_kind_str[ref_kind],
           (long) class_tag,
           (long) tag,
           (long) size,
           (int) length,
           ref_tag,
           (long) thr_tag,
           thr_id,
           method,
           (long) location,
           (int) index);
    fflush(0);

    if (tag_ptr == NULL) {
        NSK_COMPLAIN1("NULL tag_ptr is passed to heapReferenceCallback:"
                      " tag_ptr=0x%p\n", (void*)tag_ptr);
        nsk_jvmti_setFailStatus();
    }

    if (tag_ptr != NULL && tag != 0) {
        int found = 0;
        int i;

        for (i = 0; i < objectsCount; i++) {
            if (*tag_ptr == objectDescList[i].tag) {
                found++;
                objectDescList[i].found++;

                if (*tag_ptr < 0 && *tag_ptr != -chainObjectTag &&
                    ref_kind != JVMTI_HEAP_REFERENCE_STACK_LOCAL)
                {
                    NSK_COMPLAIN0("Unreachable tagged object is "
                                  "passed to heapReferenceCallback\n");
                    nsk_jvmti_setFailStatus();
                    break;
                }
                break;
            }
        }

        if (ref_kind != JVMTI_HEAP_REFERENCE_CLASS &&
            ref_kind != JVMTI_HEAP_REFERENCE_JNI_LOCAL && found <= 0 &&
            tag < FIRST_THREAD_TAG && tag > (thrObjectTag - 1))
        {
            NSK_COMPLAIN0("Unknown tagged object is passed "
                          "to heapReferenceCallback\n");
            nsk_jvmti_setFailStatus();
        }
    }

    if (user_data != &fakeUserData && !userDataError) {
       NSK_COMPLAIN2("Unexpected user_data is passed "
                     "to heapReferenceCallback:\n"
                      "   expected:       0x%p\n"
                      "   actual:         0x%p\n",
                      user_data,
                      &fakeUserData);
        nsk_jvmti_setFailStatus();
        userDataError++;
    }

    switch (ref_kind) {
        case JVMTI_HEAP_REFERENCE_CLASS: {
            int i;
            if (tag == 0) {
                return 0;
            }
            if (tag != rootClassTag && tag != chainClassTag) {
                NSK_COMPLAIN0("Unknown tagged class is passed "
                              "to heapReferenceCallback\n");
                nsk_jvmti_setFailStatus();
                break;
            }
            for (i = 0; i < objectsCount; i++) {
               if (ref_tag == objectDescList[i].tag) {
                   if (objectDescList[i].exp_class_tag != tag) {
                       NSK_COMPLAIN2("Wrong tag in heapReferenceCallback"
                                     "/JVMTI_HEAP_REFERENCE_CLASS:\n"
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

        case JVMTI_HEAP_REFERENCE_STATIC_FIELD:
            if (tag != rootObjectTag || class_tag != rootClassTag) {
                 NSK_COMPLAIN1("This reference kind was not expected: %s\n",
                               ref_kind_str[ref_kind]);
                 fflush(0);
                 nsk_jvmti_setFailStatus();
            }
            break;

        case JVMTI_HEAP_REFERENCE_STACK_LOCAL:
            // Skip local references from non-main (e.g. compiler) threads.
            if (thr_tag == TARG_THREAD_TAG) {
                thr_idx  = registerThread(thr_id, thr_tag);
                meth_idx = registerFrame(thr_id, depth, method, ref_kind);
                if (meth_idx > 0) {
                    jint loc_idx  = registerLocal(meth_idx, location, slot, tag);
                }
            }
            /* This part is kind of hack. It has some expectations about stack layout */
            if (thr_tag == TARG_THREAD_TAG &&
                reference_info->stack_local.depth == TARG_FRAME_DEPTH) {
               if (length != -1) {
                   jint exp_len = length;

                   if (reference_info->stack_local.slot == ARGV_STRING_ARR_SLOT) {
                       exp_len = 0;
                   }
                   else if (reference_info->stack_local.slot >= FIRST_PRIM_ARR_SLOT &&
                            reference_info->stack_local.slot <= LAST_PRIM_ARR_SLOT) {
                       exp_len = 2;
                   }
                   else if (reference_info->stack_local.slot == DUMMY_STRING_ARR_SLOT) {
                       exp_len = 3;
                   }
                   if (length != exp_len) {
                       NSK_COMPLAIN2("Wrong length of the local array:"
                                     " expected: %-d, found: %-d\n\n", exp_len, length);
                   }
                } else { /* length == -1 */
                    if ((reference_info->stack_local.slot >= FIRST_PRIM_ARR_SLOT &&
                         reference_info->stack_local.slot <= DUMMY_STRING_ARR_SLOT) ||
                         reference_info->stack_local.slot == ARGV_STRING_ARR_SLOT) {
                       NSK_COMPLAIN0("Length of array must not be -1\n");
                    }
                }
               if (length == 0
                    && reference_info->stack_local.slot != ARGV_STRING_ARR_SLOT
                    && reference_info->stack_local.slot < FIRST_PRIM_ARR_SLOT
                    && reference_info->stack_local.slot > DUMMY_STRING_ARR_SLOT) {
                   NSK_COMPLAIN1("Wrong length of the local variable:"
                                 " expected: -1, found: %-d\n\n", length);
                   nsk_jvmti_setFailStatus();
               }
            }
            break;
        case JVMTI_HEAP_REFERENCE_JNI_LOCAL:
            // Skip JNI local references from non-main (e.g. compiler) threads.
            if (thr_tag == TARG_THREAD_TAG) {
                thr_idx  = registerThread(thr_id, thr_tag);
                meth_idx = registerFrame(thr_id, depth, method, ref_kind);
            }
            break;

        case JVMTI_HEAP_REFERENCE_ARRAY_ELEMENT:
        case JVMTI_HEAP_REFERENCE_JNI_GLOBAL:
        case JVMTI_HEAP_REFERENCE_SYSTEM_CLASS:
        case JVMTI_HEAP_REFERENCE_MONITOR:
        case JVMTI_HEAP_REFERENCE_OTHER:
            /* These reference kinds are expected */
            break;

        default: {
            NSK_COMPLAIN1("This reference kind was not expected: %s\n\n",
                           ref_kind_str[ref_kind]);
            fflush(0);
            nsk_jvmti_setFailStatus();
            break;
        }
    }
    return 0;
}

jint JNICALL primitiveFieldCallback
    (jvmtiHeapReferenceKind        ref_kind,
     const jvmtiHeapReferenceInfo* reference_info,
     jlong                         class_tag,
     jlong*                        tag_ptr,
     jvalue                        value,
     jvmtiPrimitiveType            value_type,
     void*                         user_data)
{
    printf(" primitiveFieldCallback: ref=%s, class_tag=%-3ld, tag=%-3ld, type=%c\n",
           ref_kind_str[ref_kind],
           (long) class_tag,
           (long) DEREF(tag_ptr),
           (int) value_type);
    fflush(0);
    return 0;
}

jint JNICALL arrayPrimitiveValueCallback
    (jlong class_tag, jlong size, jlong* tag_ptr, jint element_count,
     jvmtiPrimitiveType element_type, const void* elements, void* user_data)
{
    printf(" arrayPrimitiveValueCallback: class_tag=%-3ld, tag=%-3ld, len=%d, type=%c\n",
           (long) class_tag,
           (long) DEREF(tag_ptr),
           (int) element_count,
           (int) element_type);
    fflush(0);
    return 0;
}

jint JNICALL stringPrimitiveValueCallback
    (jlong class_tag, jlong size, jlong* tag_ptr, const jchar* value,
     jint value_length, void* user_data)
{
    printf("stringPrimitiveValueCallback: class_tag=%-3ld, tag=%-3ld, len=%d\n",
           (long) class_tag,
           (long) DEREF(tag_ptr),
           (int) value_length);
    fflush(0);
    return 0;
}


/* ============================================================================= */
static jthread getTargetThread(jvmtiEnv *jvmti) {
    static const char *target_thread_name = "main";
    jint i;
    jint thread_count = -1;
    jthread *threads = NULL;

    jvmti->GetAllThreads(&thread_count, &threads);

    for (i = 0; i < thread_count; i++) {
        jvmtiThreadInfo thread_info;
        jvmti->GetThreadInfo(threads[i], &thread_info);

        if (strcmp(thread_info.name, target_thread_name) == 0) {
            return threads[i];
        }
    }

    return NULL;
}

static jvmtiError setTagForTargetThread(jvmtiEnv *jvmti, jlong tag) {
    jthread target_thread = getTargetThread(jvmti);
    return jvmti->SetTag(target_thread, tag);
}

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
        if (!NSK_VERIFY(getTestedObjects(jvmti, jni, chainLength, &objectsCount,
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

    if (!NSK_JVMTI_VERIFY(setTagForTargetThread(jvmti, TARG_THREAD_TAG))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    printf("\n\n>>> Start 1-st iteration starting from the heap root\n");
    fflush(0);
    {
        if (!NSK_JVMTI_VERIFY(jvmti->FollowReferences((jint)   0,     /* heap_filter    */
                                                      (jclass)  NULL, /* class          */
                                                      (jobject) NULL, /* initial_object */
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
        /* Heap root object must be reported 2 times */
        objectDescList[0].exp_found = 2;

        /* First unreachable object must be reported once
         * as JVMTI_HEAP_REFERENCE_STACK_LOCAL */
        objectDescList[2 * chainLength].exp_found = 1;
    }

    printf("\n\n>>> Start 2-nd iteration starting from the heap root\n");
    fflush(0);
    first_followref = 0;
    {
        jint heap_filter = JVMTI_HEAP_FILTER_UNTAGGED
                         | JVMTI_HEAP_FILTER_CLASS_UNTAGGED;
        if (!NSK_JVMTI_VERIFY(jvmti->FollowReferences(heap_filter,
                                                      (jclass)  NULL, /* class          */
                                                      (jobject) NULL, /* initial_object */
                                                      &heapCallbacks,
                                                      (const void *) &fakeUserData))) {
             nsk_jvmti_setFailStatus();
             return;
        }
    }

    printf(">>> Check that both reachable and unreachable "
           "objects were not iterated\n");
    fflush(0);
    {
        if (!checkTestedObjects(jvmti, jni, chainLength, objectDescList)) {
            nsk_jvmti_setFailStatus();
        }
    }


    printf(">>> Clean used data\n");
    fflush(0);
    {
        if (!NSK_VERIFY(releaseTestedObjects(jvmti, jni, chainLength,
                        objectDescList, rootObject))) {
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
JNIEXPORT jint JNICALL Agent_OnLoad_followref003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_followref003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_followref003(JavaVM *jvm, char *options, void *reserved) {
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
            else {
                printf("Unknown option value: info=%s\n", infoOpt);
                fflush(0);
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
