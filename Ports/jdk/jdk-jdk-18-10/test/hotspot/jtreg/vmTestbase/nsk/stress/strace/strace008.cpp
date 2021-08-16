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

#include <stdio.h>
#include "nsk_strace.h"

extern "C" {

static const char *Stest_cn="nsk/stress/strace/strace008";

JNIEXPORT void JNICALL
Java_nsk_stress_strace_strace008Thread_recursiveMethod(JNIEnv *env, jobject obj)
{
    jfieldID field;
    jmethodID method;
    jint currDepth;
    jclass testClass, threadClass;
    jint maxDepth;
    jboolean isDone;
    jint achivedCount;
    jobject doSnapshot;
    jint sleepTime;

    FIND_CLASS(testClass, Stest_cn);
    GET_OBJECT_CLASS(threadClass, obj);

    /* currDepth++ */
    GET_INT_FIELD(currDepth, obj, threadClass, "currentDepth");
    currDepth++;
    SET_INT_FIELD(obj, threadClass, "currentDepth", currDepth);

    GET_STATIC_INT_FIELD(maxDepth, testClass, "DEPTH");
    GET_STATIC_INT_FIELD(sleepTime, testClass, "SLEEP_TIME");

    if (maxDepth - currDepth > 0)
    {
        CALL_VOID_NOPARAM(obj, threadClass, "recursiveMethod");
    }


    if (maxDepth == currDepth)
    {
        GET_STATIC_OBJ_FIELD(doSnapshot, testClass, "doSnapshot",
                                "Ljava/lang/Object;");

        MONITOR_ENTER(doSnapshot);

        GET_STATIC_INT_FIELD(achivedCount, testClass, "achivedCount");
        achivedCount++;
        SET_STATIC_INT_FIELD(testClass, "achivedCount", achivedCount);

        CALL_VOID_NOPARAM(doSnapshot, testClass, "notify");
        MONITOR_EXIT(doSnapshot);
    }

    GET_STATIC_BOOL_FIELD(isDone, testClass, "isSnapshotDone");

    while (!isDone)
    {
        CALL_STATIC_VOID(threadClass, "sleep", "(J)V", (jlong)sleepTime);
        GET_STATIC_BOOL_FIELD(isDone, testClass, "isSnapshotDone");
    }

    currDepth--;
    SET_INT_FIELD(obj, threadClass, "currentDepth", currDepth);
}

}
