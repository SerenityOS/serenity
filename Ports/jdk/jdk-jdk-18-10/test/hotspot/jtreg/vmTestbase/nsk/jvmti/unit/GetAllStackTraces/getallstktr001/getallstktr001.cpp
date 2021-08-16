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

#include <stdio.h>
#include <string.h>
#include "jvmti.h"
#include "jni_tools.h"
#include "agent_common.h"

extern "C" {

#define STATUS_FAILED 2
#define PASSED 0

#define JVMTI_ERROR_CHECK(str,res)   \
    if (res != JVMTI_ERROR_NONE) {  \
        printf("%s %d\n" ,str, res); \
        return res;                  \
    }

#define JVMTI_ERROR_CHECK_EXPECTED_ERROR(str,res,err) \
    if (res != err) {                                \
        printf("%s unexpected error %d\n", str, res); \
        return res;                                   \
    }

#define JVMTI_ERROR_CHECK_VOID(str,res) \
    if (res != JVMTI_ERROR_NONE) {      \
        printf("%s %d\n" ,str, res);    \
        iGlobalStatus = STATUS_FAILED;         \
    }

#define JVMTI_ERROR_CHECK_EXPECTED_ERROR_VOID(str,res,err) \
    if (res != err) {                                      \
        printf("%s unexpected error %d\n",str, res);       \
        iGlobalStatus = STATUS_FAILED;                            \
    }


static jvmtiEnv *jvmti;
static jint iGlobalStatus = PASSED;
static jvmtiCapabilities jvmti_caps;
static jrawMonitorID jraw_monitor;


#define MAX_FRAMES_CNT  30
static jvmtiStackInfo  *stack_buf1    = NULL;
static jvmtiStackInfo  *stack_buf2    = NULL;
static jthread         *thread_list   = NULL;
static jvmtiThreadInfo *thread_info   = NULL;
static jint             threads_count = 0;


#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getallstktr001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getallstktr001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getallstktr001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM * jvm, char *options, void *reserved) {
    jint res;

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res < 0) {
        printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    /* Add capabilities */
    res = jvmti->GetPotentialCapabilities(&jvmti_caps);
    JVMTI_ERROR_CHECK("GetPotentialCapabilities returned error", res);

    res = jvmti->AddCapabilities(&jvmti_caps);
    JVMTI_ERROR_CHECK("GetPotentialCapabilities returned error", res);

    return JNI_OK;
}


JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_GetAllStackTraces_getallstktr001_GetResult(
    JNIEnv * env, jclass cls)
{
    return iGlobalStatus;
}


JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_GetAllStackTraces_getallstktr001_CreateRawMonitor(
    JNIEnv * env, jclass cls)
{
    jvmtiError ret;
    char sz[128];

    sprintf(sz, "Raw-monitor");
    ret = jvmti->CreateRawMonitor(sz, &jraw_monitor);

    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: Raw monitor create %d \n", ret);
        iGlobalStatus = STATUS_FAILED;
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_GetAllStackTraces_getallstktr001_RawMonitorEnter(
    JNIEnv * env, jclass cls)
{
    jvmtiError ret;

    ret = jvmti->RawMonitorEnter(jraw_monitor);

    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: Raw monitor enter %d \n", ret);
        iGlobalStatus = STATUS_FAILED;
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_GetAllStackTraces_getallstktr001_RawMonitorExit(
    JNIEnv * env, jclass cls)
{
    jvmtiError ret;

    ret = jvmti->RawMonitorExit(jraw_monitor);

    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: RawMonitorExit %d \n", ret);
        iGlobalStatus = STATUS_FAILED;
    }
}

void compare_all_frames(int ti, int frames_count,
                        jvmtiFrameInfo *fr_buf1,
                        jvmtiFrameInfo *fr_buf2)
{
    int fi;
    jvmtiFrameInfo *fr1, *fr2;

    for (fi = 0; fi < frames_count; fi++) {
        fr1 = &fr_buf1[fi];
        fr2 = &fr_buf2[fi];
        if (fr1->method != fr2->method) {
            printf("FAILED: compare frame: thread %d: frame %d: "
                   "different methods", ti, fi);
            iGlobalStatus = STATUS_FAILED;
            return;
        }
        if (fr1->location != fr2->location) {
            printf("FAILED: compare frame: thread %d: frame %d: "
                   "different locations", ti, fi);
            iGlobalStatus = STATUS_FAILED;
            return;
        }
        printf("thr #%d: compare frame #%d: fields are the same: "
               " method: 0x%p, location: %#" LL "x\n",
               ti, fi, fr1->method, fr1->location);
        fflush(0);
    }
}

void compare_one_stack_trace(int ti,
                             jvmtiStackInfo *stk1,
                             jvmtiStackInfo *stk2,
                             jvmtiThreadInfo *thr_info)
{
    static const char* TEST_THREAD_NAME_PREFIX = "getallstktr001-";
    size_t PFX_LEN = strlen(TEST_THREAD_NAME_PREFIX);

    if (thr_info->name != NULL) {
        printf("compare stack #%d: thread: %s\n", ti, thr_info->name);
    } else {
        printf("compare stack #%d: thread is NULL\n", ti);
        return;
    }

    if (strlen(thr_info->name) < PFX_LEN ||
        strncmp(thr_info->name, TEST_THREAD_NAME_PREFIX, PFX_LEN) != 0)
    {
        printf("compare stack #%d: %s isn't tested thread - skip it\n",
                ti, thr_info->name);
        return;
    }

    if (stk1->state != stk2->state)  {
        printf("FAILED: compare stack #%d: different states: "
               "st1: %d, st2: %d\n",
                ti, stk1->state, stk2->state);
        iGlobalStatus = STATUS_FAILED;
        return;
    }
    if (stk1->frame_count != stk2->frame_count)  {
        printf("FAILED: compare stack #%d: different frame_count: "
               "cnt1: %d, cnt2: %d\n",
                ti, stk1->frame_count, stk2->frame_count);
        iGlobalStatus = STATUS_FAILED;
        return;
    }

    printf("compare stack #%d: fields are the same: "
           " jthread: 0x%p, state: %d, frame_count: %d\n",
           ti, stk1->thread, stk1->state, stk1->frame_count);

    fflush(0);
    compare_all_frames(ti,
                       stk1->frame_count,
                       stk1->frame_buffer,
                       stk2->frame_buffer);
}

void compare_all_stack_traces(int thr_count,
                              jvmtiStackInfo  *stk_buf1,
                              jvmtiStackInfo  *stk_buf2,
                              jvmtiThreadInfo *thr_info)
{
    int ti;
    for (ti = 0; ti < thr_count; ti++) {
        compare_one_stack_trace(ti, &stk_buf1[ti], &stk_buf2[ti], &thr_info[ti]);
    }
}


JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_GetAllStackTraces_getallstktr001_GetAllStackTraces(
     JNIEnv * env, jclass cls)
{
    jvmtiError ret;
    int ti;

    ret = jvmti->GetAllStackTraces(MAX_FRAMES_CNT, &stack_buf1, &threads_count);
    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: GetAllStackTraces %d \n", ret);
        iGlobalStatus = STATUS_FAILED;
    }

    ret = jvmti->Allocate(sizeof(jthread) * threads_count,
                          (unsigned char**) &thread_list);
    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: Allocate failed with  %d \n", ret);
        iGlobalStatus = STATUS_FAILED;
    }

    for (ti = 0; ti < threads_count; ti++) {
        thread_list[ti] =
          (jthread)env->NewGlobalRef(stack_buf1[ti].thread);
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_GetAllStackTraces_getallstktr001_GetThreadsInfo(
     JNIEnv * env, jclass cls)
{
    jvmtiError ret;
    int ti;

    ret = jvmti->Allocate(sizeof(jvmtiThreadInfo) * threads_count,
                          (unsigned char**)&thread_info);
    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: Allocate failed with  %d \n", ret);
        iGlobalStatus = STATUS_FAILED;
    }

    for (ti = 0; ti < threads_count; ti++) {
        ret = jvmti->GetThreadInfo(thread_list[ti], &thread_info[ti]);
        if (ret != JVMTI_ERROR_NONE) {
            printf("Error: GetThreadInfo %d \n", ret);
            iGlobalStatus = STATUS_FAILED;
        }
        printf("GetThreadInfo %d: thread: %s\n", ti, thread_info[ti].name);
        fflush(0);
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_GetAllStackTraces_getallstktr001_GetThreadListStackTraces(
     JNIEnv * env, jclass cls)
{
    jvmtiError ret;

    ret = jvmti->GetThreadListStackTraces(
        threads_count, thread_list, MAX_FRAMES_CNT, &stack_buf2);
    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: GetThreadListStackTraces %d \n", ret);
        iGlobalStatus = STATUS_FAILED;
    }

}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_GetAllStackTraces_getallstktr001_ForceGC(
     JNIEnv * env, jclass cls)
{
    jvmtiError ret;
    ret = jvmti->ForceGarbageCollection();

    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: ForceGarbageCollection %d \n", ret);
        iGlobalStatus = STATUS_FAILED;
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_GetAllStackTraces_getallstktr001_CompareStackTraces(
     JNIEnv * env, jclass cls)
{
    compare_all_stack_traces(threads_count, stack_buf1, stack_buf2, thread_info);
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_GetAllStackTraces_getallstktr001_DeallocateBuffers(
     JNIEnv * env, jclass cls)
{
    jvmtiError ret;

    ret = jvmti->Deallocate((unsigned char *) stack_buf1);
    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: Deallocate stack_buf1 failed with  %d \n", ret);
        iGlobalStatus = STATUS_FAILED;
    }

    ret = jvmti->Deallocate((unsigned char *) stack_buf2);
    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: Deallocate stack_buf2 failed with  %d \n", ret);
        iGlobalStatus = STATUS_FAILED;
    }

    ret = jvmti->Deallocate((unsigned char *) thread_info);
    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: Deallocate thread_info failed with  %d \n", ret);
        iGlobalStatus = STATUS_FAILED;
    }
    ret = jvmti->Deallocate((unsigned char *) thread_list);
    if (ret != JVMTI_ERROR_NONE) {
        printf("Error: Deallocate thread_list failed with  %d \n", ret);
        iGlobalStatus = STATUS_FAILED;
    }
}

}
