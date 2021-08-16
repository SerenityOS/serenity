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
#include "jvmti_FollowRefObjects.h"

extern "C" {

static jlong g_timeout = 0;

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    jvmtiError retCode;
    jlong tag;
    jint cnt;
    jobject * pObjs;
    jlong * pObjTags;

    printf(">>> Check that FollowReferences(), IterateThroughHeap(), GetTag(), SetTag() and GetObjectsWithTags() \n"
           "    return an error if env. doesn't possess can_tag_objects capability\n");

    retCode = jvmti->FollowReferences((jint) 0,                 /* heap filter */
                                      NULL,                     /* class */
                                      NULL,                     /* inital object */
                                      &g_wrongHeapCallbacks,
                                      (const void *) &g_fakeUserData);

    if (!NSK_VERIFY(retCode == JVMTI_ERROR_MUST_POSSESS_CAPABILITY)) {
        NSK_COMPLAIN1("FollowReferences() returned %i", retCode);
        nsk_jvmti_setFailStatus();
    }

    retCode = jvmti->IterateThroughHeap((jint) 0,                 /* heap filter */
                                        NULL,                     /* class */
                                        &g_wrongHeapCallbacks,
                                        (const void *) &g_fakeUserData);

    if (!NSK_VERIFY(retCode == JVMTI_ERROR_MUST_POSSESS_CAPABILITY)) {
        NSK_COMPLAIN1("IterateThroughHeap() returned %i", retCode);
        nsk_jvmti_setFailStatus();
    }

    retCode = jvmti->GetTag((jobject) &g_wrongHeapCallbacks, &tag);

    if (!NSK_VERIFY(retCode == JVMTI_ERROR_MUST_POSSESS_CAPABILITY)) {
        NSK_COMPLAIN1("GetTag() returned %i", retCode);
        nsk_jvmti_setFailStatus();
    }

    retCode = jvmti->SetTag((jobject) &g_wrongHeapCallbacks, tag);

    if (!NSK_VERIFY(retCode == JVMTI_ERROR_MUST_POSSESS_CAPABILITY)) {
        NSK_COMPLAIN1("SetTag() returned %i", retCode);
        nsk_jvmti_setFailStatus();
    }

    retCode = jvmti->GetObjectsWithTags(1, &tag, &cnt, &pObjs, &pObjTags);

    if (!NSK_VERIFY(retCode == JVMTI_ERROR_MUST_POSSESS_CAPABILITY)) {
        NSK_COMPLAIN1("GetObjectsWithTags() returned %i", retCode);
        nsk_jvmti_setFailStatus();
    }

    printf(">>> Let debugee to finish\n");
    fflush(0);

    if (!NSK_VERIFY(nsk_jvmti_waitForSync(g_timeout))) {
        return;
    }

    if (!NSK_VERIFY(nsk_jvmti_resumeSync())) {
        return;
    }
} /* agentProc */


/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_followref005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_followref005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_followref005(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options))) {
        return JNI_ERR;
    }

    g_timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    if (!NSK_VERIFY((jvmti = nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL)) {
        return JNI_ERR;
    }

    {
        jvmtiCapabilities caps;

        memset(&caps, 0, sizeof(caps));
        /* Don't add can_tag_objects capability */
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
