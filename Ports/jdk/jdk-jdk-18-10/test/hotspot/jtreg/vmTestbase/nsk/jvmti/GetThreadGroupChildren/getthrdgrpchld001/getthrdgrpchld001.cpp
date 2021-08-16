/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
static JNIEnv* jni = NULL;
static jvmtiEnv *jvmti = NULL;
static jlong timeout = 0;

/* constant names */
#define RUNNING_THREAD_NAME     "runningThread"
#define NOT_STARTED_THREAD_NAME "notStartedThread"
#define FINISHED_THREAD_NAME    "finishedThread"

#define ROOT_GROUP_NAME         "rootThreadGroup"
#define RUNNING_GROUP_NAME      "runningThreadGroup"
#define NOT_STARTED_GROUP_NAME  "notStartedThreadGroup"
#define FINISHED_GROUP_NAME     "finishedThreadGroup"

/* constants */
#define DEFAULT_THREADS_COUNT   4

static int expectedThreadsCount = 0;

/* ============================================================================= */

static jthreadGroup findThreadGroupByName(jvmtiEnv* jvmti, JNIEnv* jni, const char name[],
                                            jint groupsCount, jthreadGroup groupsList[]);
static int checkThreadGroup(jvmtiEnv* jvmti, JNIEnv* jni,
                                jthreadGroup group, const char groupName[],
                                jint expectedThreadsCount, const char expectedThreadName[]);

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    NSK_DISPLAY0("Wait for threads to prepare\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    /* perform testing */
    {
        jthreadGroup rootGroup = NULL;
        jthreadGroup runningGroup = NULL;
        jthreadGroup notStartedGroup = NULL;
        jthreadGroup finishedGroup = NULL;

        /* find root thread group */
        {
            jint topGroupsCount = 0;
            jthreadGroup* topGroups = NULL;

            NSK_DISPLAY0("Get top level thread groups\n");
            if (!NSK_JVMTI_VERIFY(
                    jvmti->GetTopThreadGroups(&topGroupsCount, &topGroups))) {
                nsk_jvmti_setFailStatus();
                return;
            }
            NSK_DISPLAY1("  ... got groups: %d\n", (int)topGroupsCount);

            if (!NSK_VERIFY(topGroupsCount > 0 && topGroups != NULL))
                return;

            NSK_DISPLAY1("Find thread group by name: %s\n", ROOT_GROUP_NAME);
            if (!NSK_VERIFY((rootGroup =
                    findThreadGroupByName(jvmti, jni, ROOT_GROUP_NAME,
                                            topGroupsCount, topGroups)) != NULL)) {
                NSK_COMPLAIN1("No tested root thread group found: %s\n", ROOT_GROUP_NAME);
                nsk_jvmti_setFailStatus();
                return;
            }
            NSK_DISPLAY1("  ... found rootThreadGroup: %p\n", (void*)rootGroup);
        }

        /* check root thread group */
        {
            jint groupsCount = 0;
            jint threadsCount = 0;
            jthread* threads = NULL;
            jthreadGroup* groups = NULL;

            NSK_DISPLAY1("Get children of root thread group: %p\n", (void*)rootGroup);
            if (!NSK_JVMTI_VERIFY(
                    jvmti->GetThreadGroupChildren(rootGroup, &threadsCount, &threads, &groupsCount, &groups))) {
                nsk_jvmti_setFailStatus();
                return;
            }
            NSK_DISPLAY1("  ... got groups:  %d\n", (int)groupsCount);
            NSK_DISPLAY1("  ... got threads: %d\n", (int)threadsCount);

            if (threadsCount != 0) {
                NSK_COMPLAIN4("Unexpected threads count found in thread group: %p (%s)\n"
                              "#   got threads: %d\n"
                              "#   expected:    %d\n",
                              (void*)rootGroup, ROOT_GROUP_NAME,
                              (int)threadsCount, 0);
                nsk_jvmti_setFailStatus();
            }

            if (groupsCount != 3) {
                NSK_COMPLAIN4("Unexpected groups count found in thread group: %p (%s)\n"
                              "#   got threads: %d\n"
                              "#   expected:    %d\n",
                              (void*)rootGroup, ROOT_GROUP_NAME,
                              (int)groupsCount, 3);
                nsk_jvmti_setFailStatus();
            } else {
                int i;

                NSK_DISPLAY1("Check thread groups: %d groups\n", groupsCount);
                for (i = 0; i < groupsCount; i++) {
                    jvmtiThreadGroupInfo info;

                    if (!NSK_JVMTI_VERIFY(
                            jvmti->GetThreadGroupInfo(groups[i], &info))) {
                        nsk_jvmti_setFailStatus();
                        continue;
                    }

                    if (info.name != 0) {
                        if (strcmp(info.name, RUNNING_GROUP_NAME) == 0) {
                            NSK_DISPLAY2("  ... found runningThreadGroup: %p (%s)\n",
                                                                groups[i], info.name);
                            if (runningGroup != NULL) {
                                NSK_COMPLAIN6("Duplicated runningThreadGroup in rootThreadGroup:\n"
                                              "#   parent group:   %p (%s)\n"
                                              "#   existing group: %p (%s)\n"
                                              "#   duplicated group: %p (%s)\n",
                                                (void*)rootGroup, ROOT_GROUP_NAME,
                                                (void*)runningGroup, RUNNING_GROUP_NAME,
                                                (void*)groups[i], info.name);
                                nsk_jvmti_setFailStatus();
                            } else {
                                runningGroup = groups[i];
                            }
                            continue;
                        }

                        if (strcmp(info.name, NOT_STARTED_GROUP_NAME) == 0) {
                            NSK_DISPLAY2("  ... found notStartedThreadGroup: %p (%s)\n",
                                                                groups[i], info.name);
                            if (notStartedGroup != NULL) {
                                NSK_COMPLAIN6("Duplicated notStartedThreadGroup in rootThreadGroup:\n"
                                              "#   parent group:   %p (%s)\n"
                                              "#   existing group: %p (%s)\n"
                                              "#   duplicated group: %p (%s)\n",
                                                (void*)rootGroup, ROOT_GROUP_NAME,
                                                (void*)notStartedGroup, NOT_STARTED_GROUP_NAME,
                                                (void*)groups[i], info.name);
                                nsk_jvmti_setFailStatus();
                            } else {
                                notStartedGroup = groups[i];
                            }
                            continue;
                        }

                        if (strcmp(info.name, FINISHED_GROUP_NAME) == 0) {
                            NSK_DISPLAY2("  ... found finishedThreadGroup: %p (%s)\n",
                                                                groups[i], info.name);
                            if (finishedGroup != NULL) {
                                NSK_COMPLAIN6("Duplicated finishedThreadGroup in rootThreadGroup:\n"
                                              "#   parent group:   %p (%s)\n"
                                              "#   existing group: %p (%s)\n"
                                              "#   duplicated group: %p (%s)\n",
                                                (void*)rootGroup, ROOT_GROUP_NAME,
                                                (void*)finishedGroup, FINISHED_GROUP_NAME,
                                                (void*)groups[i], info.name);
                                nsk_jvmti_setFailStatus();
                            } else {
                                finishedGroup = groups[i];
                            }
                            continue;
                        }
                    }

                    NSK_COMPLAIN4("Unexpected thread group found inrootThreadGroup:\n"
                                  "#   parent group: %p (%s)\n"
                                  "#   found group:  %p (%s)\n",
                                    (void*)rootGroup, ROOT_GROUP_NAME,
                                    (void*)groups[i], info.name);
                    nsk_jvmti_setFailStatus();
                }

                checkThreadGroup(jvmti, jni, runningGroup, RUNNING_GROUP_NAME,
                                        expectedThreadsCount, RUNNING_THREAD_NAME);
                checkThreadGroup(jvmti, jni, notStartedGroup, NOT_STARTED_GROUP_NAME,
                                        0, NOT_STARTED_THREAD_NAME);
                checkThreadGroup(jvmti, jni, finishedGroup, FINISHED_GROUP_NAME,
                                        0, FINISHED_THREAD_NAME);
            }

            /* deallocate arrays */
            if (!NSK_JVMTI_VERIFY(
                    jvmti->Deallocate((unsigned char*)groups))) {
                nsk_jvmti_setFailStatus();
            }
            if (!NSK_JVMTI_VERIFY(
                    jvmti->Deallocate((unsigned char*)threads))) {
                nsk_jvmti_setFailStatus();
            }
        }
    }

    NSK_DISPLAY0("Let debugee to finish\n");
    if (!nsk_jvmti_resumeSync())
        return;
}

/* ============================================================================= */

/** Check thread group and its chidren. */
static int checkThreadGroup(jvmtiEnv* jvmti, JNIEnv* jni,
                                jthreadGroup group, const char groupName[],
                                jint expectedThreadsCount, const char expectedThreadName[]) {
    size_t threadNameLen = strlen(expectedThreadName);

    if (group == NULL) {
        NSK_COMPLAIN1("No expected group found in rootThreadGroup: %s\n", groupName);
        nsk_jvmti_setFailStatus();
        return NSK_FALSE;
    }

    NSK_DISPLAY2("Get children of thread group: %p (%s):\n", (void*)group, groupName);
    {
        jint threadsCount = 0;
        jint groupsCount = 0;
        jthread* threads = NULL;
        jthreadGroup* groups = NULL;

        if (!NSK_JVMTI_VERIFY(
                jvmti->GetThreadGroupChildren(group, &threadsCount, &threads, &groupsCount, &groups))) {
            nsk_jvmti_setFailStatus();
            return NSK_FALSE;
        }
        NSK_DISPLAY1("  ... got groups:  %d\n", (int)groupsCount);
        NSK_DISPLAY1("  ... got threads: %d\n", (int)threadsCount);

        if (groupsCount != 0) {
            NSK_COMPLAIN4("Unexpected groups count in thread group: %p (%s)\n"
                          "#   got threads: %d\n"
                          "#   expected:    %d\n",
                          (void*)group, groupName,
                          (int)groupsCount, 0);
            nsk_jvmti_setFailStatus();
        }

        if (threadsCount != expectedThreadsCount) {
            NSK_COMPLAIN4("Unexpected threads count in thread group: %p (%s)\n"
                          "#   got threads: %d\n"
                          "#   expected:    %d\n",
                          (void*)group, groupName,
                          (int)threadsCount, expectedThreadsCount);
            nsk_jvmti_setFailStatus();
        } else {
            int i;

            NSK_DISPLAY1("Check threads: %d threads\n", threadsCount);
            for (i = 0; i < threadsCount; i++) {
                jvmtiThreadInfo info;

                if (!NSK_JVMTI_VERIFY(
                        jvmti->GetThreadInfo(threads[i], &info))) {
                    nsk_jvmti_setFailStatus();
                    continue;
                }

                NSK_DISPLAY2("  ... found thread: %p (%s)\n",
                                    (void*)threads[i], nsk_null_string(info.name));

                if (info.name == NULL ||
                            strncmp(info.name, expectedThreadName, threadNameLen) != 0) {
                    NSK_COMPLAIN5("Found unexpected thread in thread group:\n"
                                  "#   thread group:  %p (%s)\n"
                                  "#   found thread:  %p (%s)\n"
                                  "#   expected name: (%s)\n",
                                    (void*)group, groupName,
                                    (void*)threads[i], nsk_null_string(info.name),
                                    expectedThreadName);
                    nsk_jvmti_setFailStatus();
                }
            }
        }

        /* deallocate arrays */
        if (!NSK_JVMTI_VERIFY(
                jvmti->Deallocate((unsigned char*)groups))) {
            nsk_jvmti_setFailStatus();
        }
        if (!NSK_JVMTI_VERIFY(
                jvmti->Deallocate((unsigned char*)threads))) {
            nsk_jvmti_setFailStatus();
        }
    }

    return NSK_TRUE;
}

/** Recursively find thread group by its name among given groups and their childrens. */
static jthreadGroup findThreadGroupByName(jvmtiEnv* jvmti, JNIEnv* jni, const char name[],
                                                jint count, jthreadGroup groupsList[]) {
    jthreadGroup foundGroup = NULL;
    int i;

    for (i = 0; i < count && foundGroup == NULL; i++) {
        jint threadsCount = 0;
        jint groupsCount = 0;
        jthread* threads = NULL;
        jthreadGroup * groups = NULL;

        if (!NSK_JVMTI_VERIFY(
                jvmti->GetThreadGroupChildren(groupsList[i], &threadsCount, &threads, &groupsCount, &groups))) {
            nsk_jvmti_setFailStatus();
            return NULL;
        }

        if (groupsCount > 0) {
            int j;

            if (!NSK_VERIFY(groups != NULL))
                return NULL;

            for (j = 0; j < groupsCount; j++) {
                jvmtiThreadGroupInfo info;

                if (groups[j] != NULL) {

                    if (!NSK_JVMTI_VERIFY(
                            jvmti->GetThreadGroupInfo(groups[j], &info))) {
                        nsk_jvmti_setFailStatus();
                        continue;
                    }

                    if (info.name != 0 && strcmp(info.name, name) == 0) {
                        foundGroup = groups[j];
                        break;
                    }
                }
            }

            if (foundGroup == NULL) {
                foundGroup = findThreadGroupByName(jvmti, jni, name, groupsCount, groups);
            }
        }

        /* deallocate arrays */
        if (!NSK_JVMTI_VERIFY(
                jvmti->Deallocate((unsigned char*)groups))) {
            nsk_jvmti_setFailStatus();
        }
        if (!NSK_JVMTI_VERIFY(
                jvmti->Deallocate((unsigned char*)threads))) {
            nsk_jvmti_setFailStatus();
        }
    }

    return foundGroup;
}

/* ============================================================================= */

/* Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getthrdgrpchld001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getthrdgrpchld001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getthrdgrpchld001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    /* get options */
    expectedThreadsCount = nsk_jvmti_findOptionIntValue("threads", DEFAULT_THREADS_COUNT);
    if (!NSK_VERIFY(expectedThreadsCount > 0))
        return JNI_ERR;

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* register agent proc and arg */
    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */

}
