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
#include <jvmti.h>
#include "agent_common.h"
#include "jni_tools.h"
#include "jvmti_tools.h"
#include "jvmti_FollowRefObjects.h"

extern "C" {

/* ============================================================================= */

static jlong g_timeout = 0;

#define JAVA_LANG_STRING_CLASS_NAME "java/lang/String"
#define JAVA_IO_SERIALIZABLE_CLASS_NAME "java/io/Serializable"
#define JAVA_UTIL_CALENDAR_CLASS_NAME "java/util/Calendar"

static jobject g_jniGlobalRef = 0;
static jweak g_jniWeakGlobalRef = 0;

static jvmtiHeapCallbacks g_heapCallbacks;

/* ============================================================================= */

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
    CHECK_USER_DATA(user_data);

    printHeapRefCallbackInfo(reference_kind, reference_info, class_tag, referrer_class_tag, size, tag_ptr, referrer_tag_ptr, length);

    markTagVisited(DEREF(tag_ptr));
    markRefToVerify(DEREF(referrer_tag_ptr), DEREF(tag_ptr), reference_kind);

    return JVMTI_VISIT_OBJECTS;

} /* heapReferenceCallback */


jint JNICALL primitiveFieldCallback(
     jvmtiHeapReferenceKind        reference_kind,
     const jvmtiHeapReferenceInfo* reference_info,
     jlong                         class_tag,
     jlong*                        tag_ptr,
     jvalue                        value,
     jvmtiPrimitiveType            value_type,
     void*                         user_data)
{
    CHECK_USER_DATA(user_data);

    printf(" primitiveFieldCallback: ref=%s,"
               " class_tag=%-3ld, tag=%-3ld, type=%c\n",
               g_refKindStr[reference_kind],
               (long) class_tag,
               (long) DEREF(tag_ptr),
               (int) value_type);

    fflush(0);

    markTagVisited(DEREF(tag_ptr));

    return JVMTI_VISIT_OBJECTS;

} /* primitiveFieldCallback */


jint JNICALL arrayPrimitiveValueCallback(
     jlong              class_tag,
     jlong              size,
     jlong*             tag_ptr,
     jint               element_count,
     jvmtiPrimitiveType element_type,
     const void*        elements,
     void*              user_data)
{
    CHECK_USER_DATA(user_data);

    printf("    arrayPrimitiveValueCallback: class_tag=%-3ld, tag=%-3ld, len=%d, type=%c\n",
           (long) class_tag,
           (long) DEREF(tag_ptr),
           (int) element_count,
           (int) element_type);
    fflush(0);

    markTagVisited(DEREF(tag_ptr));

    return JVMTI_VISIT_OBJECTS;

} /* arrayPrimitiveValueCallback */


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

    markTagVisited(DEREF(tag_ptr));

    return JVMTI_VISIT_OBJECTS;

} /* stringPrimitiveValueCallback */

/* ============================================================================= */

static void createGlobalRefs(JNIEnv * jni)
{
    jclass klass;

    if  (!NSK_JNI_VERIFY(jni, (klass = jni->FindClass(JAVA_LANG_STRING_CLASS_NAME)) != NULL)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    if (!NSK_JNI_VERIFY(jni, (g_jniGlobalRef = jni->NewGlobalRef(klass)) != NULL)) {
        nsk_jvmti_setFailStatus();
    }

    if (!NSK_JNI_VERIFY(jni, (g_jniWeakGlobalRef = jni->NewWeakGlobalRef(klass)) != NULL)) {
        nsk_jvmti_setFailStatus();
    }

} /* createGlobalRefs */

/** Agent algorithm. */

static void JNICALL agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg)
{
    jvmtiError retCode;

    printf(">>> Sync with Java code\n");
    fflush(0);

    if (!NSK_VERIFY(nsk_jvmti_waitForSync(g_timeout))) {
        return;
    }

    printf(">>> Create JNI global references\n");
    fflush(0);

    createGlobalRefs(jni);

    retCode = jvmti->FollowReferences((jint) 0,                 /* heap filter */
                                      NULL,                     /* class */
                                      NULL,                     /* inital object */
                                      &g_heapCallbacks,
                                      (const void *) &g_fakeUserData);

    if (!NSK_VERIFY(retCode == JVMTI_ERROR_NONE)) {
        nsk_jvmti_setFailStatus();
    }

    checkThatAllTagsVisited();

    printf(">>> Let debugee to finish\n");
    fflush(0);

    if (!NSK_VERIFY(nsk_jvmti_resumeSync())) {
        return;
    }

} /* agentProc */


/* ============================================================================= */

/** Agent library initialization. */

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_followref004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_followref004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_followref004(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved)
{
    jvmtiEnv* jvmti = NULL;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options))) {
        return JNI_ERR;
    }

    g_timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    if (!NSK_VERIFY((jvmti = nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL)) {
        return JNI_ERR;
    }

    /* Setting Heap Callbacks */
    memset(&g_heapCallbacks, 0, sizeof(g_heapCallbacks));
    g_heapCallbacks.heap_iteration_callback         = NULL;
    g_heapCallbacks.heap_reference_callback         = heapReferenceCallback;
    g_heapCallbacks.primitive_field_callback        = primitiveFieldCallback;
    g_heapCallbacks.array_primitive_value_callback  = arrayPrimitiveValueCallback;
    g_heapCallbacks.string_primitive_value_callback = stringPrimitiveValueCallback;

    jvmti_FollowRefObject_init();

    {
        jvmtiCapabilities caps;

        memset(&caps, 0, sizeof(caps));
        caps.can_tag_objects = 1;
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps))) {
            return JNI_ERR;
        }
    }

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL))) {
        return JNI_ERR;
    }

    return JNI_OK;

} /* Agent_OnLoad */


/* ============================================================================= */

}
