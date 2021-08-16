/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include <jvmti.h>
#include "jni_tools.h"
#include "jvmti_tools.h"
#include "jvmti_FollowRefObjects.h"

extern "C" {

/* ============================================================================= */

int g_fakeUserData = 0;
int g_userDataError = 0;
jvmtiHeapCallbacks g_wrongHeapCallbacks;

/* This array has to be up-to-date with the jvmtiHeapReferenceKind enum */
const char * const g_refKindStr[28] = {
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
   "JVMTI_HEAP_REFERENCE_SUPERCLASS",
   "unknown_11", "unknown_12", "unknown_13", "unknown_14", "unknown_15",
   "unknown_16", "unknown_17", "unknown_18", "unknown_19", "unknown_20",
   "JVMTI_HEAP_REFERENCE_JNI_GLOBAL",
   "JVMTI_HEAP_REFERENCE_SYSTEM_CLASS",
   "JVMTI_HEAP_REFERENCE_MONITOR",
   "JVMTI_HEAP_REFERENCE_STACK_LOCAL",
   "JVMTI_HEAP_REFERENCE_JNI_LOCAL",
   "JVMTI_HEAP_REFERENCE_THREAD",
   "JVMTI_HEAP_REFERENCE_OTHER"
};

/* ============================================================================= */

char * g_szTagInfo[MAX_TAG];
char g_tagFlags[MAX_TAG];
int g_tagVisitCount[MAX_TAG];

/* ============================================================================= */

void markTagSet(jlong tag_val)
{
    if (tag_val > 0 && tag_val < MAX_TAG)
        g_tagFlags[tag_val] |= FLAG_TAG_SET;
}

void markTagVisited(jlong tag_val)
{
    if (tag_val > 0 && tag_val < MAX_TAG) {
        g_tagVisitCount[tag_val]++;
    }
}

jboolean checkThatAllTagsVisited()
{
    jboolean ok = JNI_TRUE;
    jlong i;

    NSK_DISPLAY0("Checking that all set tags have been visited\n");

    for (i = 1; i < MAX_TAG; i++) {
        char flags = g_tagFlags[i];

        if ((g_tagFlags[i] & FLAG_TAG_SET)) {
            if (g_tagVisitCount[i] == 0) {
                NSK_COMPLAIN1("Tag %" LL "d has not been visited: %x\n", i);
                ok = JNI_FALSE;
            }

            DBG(printf(">>> Tag %" LL "d has been visited %i times: %s\n", i, g_tagVisitCount[i], g_szTagInfo[i]));
        }
    }

    return ok;
}

JNIEXPORT void JNICALL Java_nsk_jvmti_unit_FollowReferences_FollowRefObjects_resetTags(JNIEnv* jni, jclass klass)
{
    memset(g_szTagInfo, 0, sizeof(g_szTagInfo));
    memset(g_tagFlags, 0, sizeof(g_tagFlags));
    memset(g_tagVisitCount, 0, sizeof(g_tagVisitCount));
}

JNIEXPORT jboolean JNICALL Java_nsk_jvmti_unit_FollowReferences_FollowRefObjects_setTag(JNIEnv* jni, jclass klass, jobject o, jlong tag, jstring sInfo)
{
    jvmtiEnv * jvmti = nsk_jvmti_getAgentJVMTIEnv();
    jint hashCode;

    if (!NSK_VERIFY(jvmti->SetTag(o, tag) == JVMTI_ERROR_NONE)) {
        NSK_COMPLAIN2("Can't set tag %li for object %lx\n", tag, o);
        return JNI_FALSE;
    }

    if (!NSK_VERIFY(jvmti->GetObjectHashCode(o, &hashCode) == JVMTI_ERROR_NONE)) {
        NSK_COMPLAIN1("Can't get hash object %lx\n", o);
        return JNI_FALSE;
    }

    NSK_DISPLAY2("setTag: %08x <- % 3li", hashCode, tag);

    if (tag > 0 && tag < MAX_TAG) {
        jboolean fCopy;
        const char * s;

        if (!NSK_VERIFY((s = jni->GetStringUTFChars(sInfo, &fCopy)) != NULL)) {
            NSK_COMPLAIN1("Can't get string at %#p\n", sInfo);
            return JNI_FALSE;
        }

        if (!s) {
            NSK_COMPLAIN1("Can't get string at %#p: NULL\n", sInfo);
            return JNI_FALSE;
        }

        g_szTagInfo[tag] = strdup(s);

        jni->ReleaseStringUTFChars(sInfo, s);

        NSK_DISPLAY1(" // %s", g_szTagInfo[tag]);

    }

    markTagSet(tag);

    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_nsk_jvmti_unit_FollowReferences_FollowRefObjects_getTag(JNIEnv* jni, jclass klass, jobject o)
{
    jvmtiEnv * jvmti = nsk_jvmti_getAgentJVMTIEnv();

    jlong tag;
    jvmtiError r;
    if (!NSK_VERIFY((r = jvmti->GetTag(o, &tag)) == JVMTI_ERROR_NONE)) {
        NSK_COMPLAIN2("Can't GetTag for object %lx. Return code: %i\n", o, r);
        return -1;
    }

    return tag;
}

/* ============================================================================= */

int g_refsToVerifyCnt;

RefToVerify g_refsToVerify[MAX_REFS];

/* ============================================================================= */

JNIEXPORT void JNICALL Java_nsk_jvmti_unit_FollowReferences_FollowRefObjects_resetRefsToVerify(JNIEnv* jni, jclass klass)
{
    g_refsToVerifyCnt = 0;
}

static RefToVerify * findRefToVerify(jlong tagFrom, jlong tagTo, jint refKind)
{
    int i;
    RefToVerify * pRefRec = g_refsToVerify;

    for (i = g_refsToVerifyCnt; i > 0; i--, pRefRec++) {
        pRefRec = &g_refsToVerify[i];
        if (pRefRec->_tagFrom == tagFrom && pRefRec->_tagTo == tagTo && pRefRec->_refKind == refKind) {
            return pRefRec;
        }
    }

    return NULL;
}

static jboolean addRefToVerify(jlong tagFrom, jlong tagTo, jint refKind, int expectedCount, int actualCount)
{
    RefToVerify * pRefRec;

    if (g_refsToVerifyCnt >= MAX_REFS) {
        NSK_COMPLAIN0("TEST_BUG: Max. number of refs reached!");
        nsk_jvmti_setFailStatus();
        return JNI_FALSE;
    }

    pRefRec = &g_refsToVerify[g_refsToVerifyCnt++];

    pRefRec->_tagFrom = tagFrom;
    pRefRec->_tagTo = tagTo;
    pRefRec->_refKind = refKind;
    pRefRec->_expectedCount = expectedCount;
    pRefRec->_actualCount = actualCount;

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_nsk_jvmti_unit_FollowReferences_FollowRefObjects_addRefToVerify(JNIEnv* jni, jclass klass, jobject from, jobject to, jint refKind, jint count)
{
    jvmtiEnv * jvmti = nsk_jvmti_getAgentJVMTIEnv();
    jvmtiError r;
    jlong tagFrom, tagTo;
    RefToVerify * pRefRec;

    if (!NSK_VERIFY((r = jvmti->GetTag(from, &tagFrom)) == JVMTI_ERROR_NONE)) {
        NSK_COMPLAIN2("TEST_BUG: Can't GetTag for object %lx. Return code: %i\n", from, r);
        nsk_jvmti_setFailStatus();
        return JNI_FALSE;
    }


    if (!NSK_VERIFY((r = jvmti->GetTag(to, &tagTo)) == JVMTI_ERROR_NONE)) {
        NSK_COMPLAIN2("TEST_BUG: Can't GetTag for object %lx. Return code: %i\n", to, r);
        nsk_jvmti_setFailStatus();
        return JNI_FALSE;
    }

    pRefRec = findRefToVerify(tagFrom, tagTo, refKind);
    if (pRefRec != NULL) {
        pRefRec->_expectedCount += count;
        return JNI_TRUE;
    }

    return addRefToVerify(tagFrom, tagTo, refKind, count, 0);
}

jboolean markRefToVerify(jlong tagFrom, jlong tagTo, int refKind)
{
    RefToVerify * pRefRec;

    pRefRec = findRefToVerify(tagFrom, tagTo, refKind);
    if (pRefRec != NULL) {
        pRefRec->_actualCount++;
        return JNI_TRUE;
    }

    return addRefToVerify(tagFrom, tagTo, refKind, 0, 1);
}

/* ============================================================================= */

void checkUserData(const char * szFile, const int line, void * user_data)
{
    if (user_data != &g_fakeUserData && !g_userDataError) {
       NSK_COMPLAIN4("%s, %i: Unexpected user_data is passed"
                     " to heapReferenceCallback:\n"
                      "   expected:       0x%p\n"
                      "   actual:         0x%p\n",
                      szFile, line,
                      &g_fakeUserData,
                      user_data);
        g_userDataError++;
    }
}

#define CHECK_USER_DATA(p) checkUserData(__FILE__, __LINE__, (p))

void printHeapRefCallbackInfo(
     jvmtiHeapReferenceKind        reference_kind,
     const jvmtiHeapReferenceInfo* reference_info,
     jlong                         class_tag,
     jlong                         referrer_class_tag,
     jlong                         size,
     jlong*                        tag_ptr,
     jlong*                        referrer_tag_ptr,
     jint                          length)
{
    const char * szInfo, * szRefInfo;
    jlong tag_val = tag_ptr ? *tag_ptr : 0;

    NSK_DISPLAY1("heapReferenceCallback: %s", g_refKindStr[reference_kind]);

    NSK_DISPLAY3("   reference_info: %#lx, class_tag: %#" LL "d, referrer_class_tag: %#" LL "d\n",
                     reference_info,       class_tag,            referrer_class_tag);

    NSK_DISPLAY4("   size: %" LL "d, tag_ptr: %p,  referrer_tag_ptr: %p,  length: %-ld\n",
                     size,           tag_ptr,      referrer_tag_ptr,      length);

    NSK_DISPLAY2("   tag: %" LL "d, referrer_tag: %" LL "d\n",
                     tag_val, DEREF(referrer_tag_ptr));

    szInfo = (tag_val > 0 && tag_val < MAX_TAG) ? g_szTagInfo[tag_val] : "<none>";
    szRefInfo = (referrer_tag_ptr && *referrer_tag_ptr > 0 && *referrer_tag_ptr < MAX_TAG) ? g_szTagInfo[*referrer_tag_ptr] : "<none>";

    NSK_DISPLAY3("   summary: %s: %s <- %s\n",
                     g_refKindStr[reference_kind], szInfo, szRefInfo);
}

/* ============================================================================= */

jint JNICALL wrongHeapReferenceCallback(
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
    CHECK_USER_DATA(user_data);
    NSK_COMPLAIN0("heap reference callback was called, where it should not be\n");
    nsk_jvmti_setFailStatus();
    printHeapRefCallbackInfo(reference_kind, reference_info, class_tag, referrer_class_tag, size, tag_ptr, referrer_tag_ptr, length);

    return JVMTI_VISIT_OBJECTS;
}

jint JNICALL wrongPrimitiveFieldCallback(
     jvmtiHeapReferenceKind        reference_kind,
     const jvmtiHeapReferenceInfo* reference_info,
     jlong                         class_tag,
     jlong*                        tag_ptr,
     jvalue                        value,
     jvmtiPrimitiveType            value_type,
     void*                         user_data)
{
    CHECK_USER_DATA(user_data);
    NSK_COMPLAIN0("primitive field callback was called, where it should not be\n");
    nsk_jvmti_setFailStatus();

    return JVMTI_VISIT_OBJECTS;
}

jint JNICALL wrongArrayPrimitiveValueCallback(
     jlong              class_tag,
     jlong              size,
     jlong*             tag_ptr,
     jint               element_count,
     jvmtiPrimitiveType element_type,
     const void*        elements,
     void*              user_data)
{
    CHECK_USER_DATA(user_data);
    NSK_COMPLAIN0("array primitive value callback was called, where it should not be\n");
    nsk_jvmti_setFailStatus();

    return JVMTI_VISIT_OBJECTS;
}

jint JNICALL wrongStringPrimitiveValueCallback(
     jlong        class_tag,
     jlong        size,
     jlong*       tag_ptr,
     const jchar* value,
     jint         value_length,
     void*        user_data)
{
    CHECK_USER_DATA(user_data);
    NSK_COMPLAIN0("string primitive value callback was called, where it should not be\n");
    nsk_jvmti_setFailStatus();

    return JVMTI_VISIT_OBJECTS;
}

/* ============================================================================= */

void jvmti_FollowRefObject_init()
{
    g_wrongHeapCallbacks.heap_iteration_callback         = NULL;
    g_wrongHeapCallbacks.heap_reference_callback         = wrongHeapReferenceCallback;
    g_wrongHeapCallbacks.primitive_field_callback        = wrongPrimitiveFieldCallback;
    g_wrongHeapCallbacks.array_primitive_value_callback  = wrongArrayPrimitiveValueCallback;
    g_wrongHeapCallbacks.string_primitive_value_callback = wrongStringPrimitiveValueCallback;

    Java_nsk_jvmti_unit_FollowReferences_FollowRefObjects_resetTags(NULL, NULL);
    Java_nsk_jvmti_unit_FollowReferences_FollowRefObjects_resetRefsToVerify(NULL, NULL);
}

/* ============================================================================= */

}
