/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

#include "util.h"
#include "ThreadReferenceImpl.h"
#include "eventHandler.h"
#include "threadControl.h"
#include "inStream.h"
#include "outStream.h"
#include "FrameID.h"

static jboolean
name(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jthread thread;

    env = getEnv();

    thread = inStream_readThreadRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    if (threadControl_isDebugThread(thread)) {
        outStream_setError(out, JDWP_ERROR(INVALID_THREAD));
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 1) {

        jvmtiThreadInfo info;
        jvmtiError error;

        (void)memset(&info, 0, sizeof(info));

        error = JVMTI_FUNC_PTR(gdata->jvmti,GetThreadInfo)
                                (gdata->jvmti, thread, &info);

        if (error != JVMTI_ERROR_NONE) {
            outStream_setError(out, map2jdwpError(error));
        } else {
            (void)outStream_writeString(out, info.name);
        }

        if ( info.name != NULL )
            jvmtiDeallocate(info.name);

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jboolean
suspend(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiError error;
    jthread thread;

    thread = inStream_readThreadRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    if (threadControl_isDebugThread(thread)) {
        outStream_setError(out, JDWP_ERROR(INVALID_THREAD));
        return JNI_TRUE;
    }
    error = threadControl_suspendThread(thread, JNI_FALSE);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
    }
    return JNI_TRUE;
}

static jboolean
resume(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiError error;
    jthread thread;

    thread = inStream_readThreadRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    if (threadControl_isDebugThread(thread)) {
        outStream_setError(out, JDWP_ERROR(INVALID_THREAD));
        return JNI_TRUE;
    }

    /* true means it is okay to unblock the commandLoop thread */
    error = threadControl_resumeThread(thread, JNI_TRUE);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
    }
    return JNI_TRUE;
}

static jboolean
status(PacketInputStream *in, PacketOutputStream *out)
{
    jdwpThreadStatus threadStatus;
    jint statusFlags;
    jvmtiError error;
    jthread thread;

    thread = inStream_readThreadRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    if (threadControl_isDebugThread(thread)) {
        outStream_setError(out, JDWP_ERROR(INVALID_THREAD));
        return JNI_TRUE;
    }

    error = threadControl_applicationThreadStatus(thread, &threadStatus,
                                                          &statusFlags);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_TRUE;
    }
    (void)outStream_writeInt(out, threadStatus);
    (void)outStream_writeInt(out, statusFlags);
    return JNI_TRUE;
}

static jboolean
threadGroup(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jthread thread;

    env = getEnv();

    thread = inStream_readThreadRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    if (threadControl_isDebugThread(thread)) {
        outStream_setError(out, JDWP_ERROR(INVALID_THREAD));
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 1) {

        jvmtiThreadInfo info;
        jvmtiError error;

        (void)memset(&info, 0, sizeof(info));

        error = JVMTI_FUNC_PTR(gdata->jvmti,GetThreadInfo)
                                (gdata->jvmti, thread, &info);

        if (error != JVMTI_ERROR_NONE) {
            outStream_setError(out, map2jdwpError(error));
        } else {
            (void)outStream_writeObjectRef(env, out, info.thread_group);
        }

        if ( info.name!=NULL )
            jvmtiDeallocate(info.name);

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jboolean
validateSuspendedThread(PacketOutputStream *out, jthread thread)
{
    jvmtiError error;
    jint count;

    error = threadControl_suspendCount(thread, &count);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_FALSE;
    }

    if (count == 0) {
        outStream_setError(out, JDWP_ERROR(THREAD_NOT_SUSPENDED));
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

static jboolean
frames(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiError error;
    FrameNumber index;
    jint count;
    jint filledIn;
    JNIEnv *env;
    jthread thread;
    jint startIndex;
    jint length;
    jvmtiFrameInfo* frames;

    env = getEnv();

    thread = inStream_readThreadRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    startIndex = inStream_readInt(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    length = inStream_readInt(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    if (threadControl_isDebugThread(thread)) {
        outStream_setError(out, JDWP_ERROR(INVALID_THREAD));
        return JNI_TRUE;
    }

    if (!validateSuspendedThread(out, thread)) {
        return JNI_TRUE;
    }

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetFrameCount)
                        (gdata->jvmti, thread, &count);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_TRUE;
    }

    if (length == -1) {
        length = count - startIndex;
    }

    if (length == 0) {
        (void)outStream_writeInt(out, 0);
        return JNI_TRUE;
    }

    if ((startIndex < 0) || (startIndex > count - 1)) {
        outStream_setError(out, JDWP_ERROR(INVALID_INDEX));
        return JNI_TRUE;
    }

    if ((length < 0) || (length + startIndex > count)) {
        outStream_setError(out, JDWP_ERROR(INVALID_LENGTH));
        return JNI_TRUE;
    }

    (void)outStream_writeInt(out, length);

    frames = jvmtiAllocate(sizeof(jvmtiFrameInfo) * length);

    if (frames == NULL) {
        outStream_setError(out, JDWP_ERROR(OUT_OF_MEMORY));
        return JNI_TRUE;
    }

    error = JVMTI_FUNC_PTR(gdata->jvmti, GetStackTrace)
                          (gdata->jvmti, thread, startIndex, length, frames,
                           &filledIn);

    /* Should not happen. */
    if (error == JVMTI_ERROR_NONE && length != filledIn) {
        error = JVMTI_ERROR_INTERNAL;
    }

    for (index = 0; index < filledIn && error == JVMTI_ERROR_NONE; ++index) {
        WITH_LOCAL_REFS(env, 1) {
            jclass clazz;
            error = methodClass(frames[index].method, &clazz);

            if (error == JVMTI_ERROR_NONE) {
                FrameID frame = createFrameID(thread, index + startIndex);
                outStream_writeFrameID(out, frame);
                writeCodeLocation(out, clazz, frames[index].method,
                                  frames[index].location);
            }
        } END_WITH_LOCAL_REFS(env);
    }

    jvmtiDeallocate(frames);

    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
    }
    return JNI_TRUE;
}

static jboolean
getFrameCount(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiError error;
    jint count;
    jthread thread;

    thread = inStream_readThreadRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    if (threadControl_isDebugThread(thread)) {
        outStream_setError(out, JDWP_ERROR(INVALID_THREAD));
        return JNI_TRUE;
    }

    if (!validateSuspendedThread(out, thread)) {
        return JNI_TRUE;
    }

    error = JVMTI_FUNC_PTR(gdata->jvmti,GetFrameCount)
                        (gdata->jvmti, thread, &count);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_TRUE;
    }
    (void)outStream_writeInt(out, count);

    return JNI_TRUE;
}

static jboolean
ownedMonitors(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jthread thread;

    env = getEnv();

    thread = inStream_readThreadRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    if (threadControl_isDebugThread(thread)) {
        outStream_setError(out, JDWP_ERROR(INVALID_THREAD));
        return JNI_TRUE;
    }

    if (!validateSuspendedThread(out, thread)) {
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 1) {

        jvmtiError error;
        jint count = 0;
        jobject *monitors = NULL;

        error = JVMTI_FUNC_PTR(gdata->jvmti,GetOwnedMonitorInfo)
                                (gdata->jvmti, thread, &count, &monitors);
        if (error != JVMTI_ERROR_NONE) {
            outStream_setError(out, map2jdwpError(error));
        } else {
            int i;
            (void)outStream_writeInt(out, count);
            for (i = 0; i < count; i++) {
                jobject monitor = monitors[i];
                (void)outStream_writeByte(out, specificTypeKey(env, monitor));
                (void)outStream_writeObjectRef(env, out, monitor);
            }
        }
        if (monitors != NULL)
            jvmtiDeallocate(monitors);

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jboolean
currentContendedMonitor(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jthread thread;

    env = getEnv();

    thread = inStream_readThreadRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    if (thread == NULL || threadControl_isDebugThread(thread)) {
        outStream_setError(out, JDWP_ERROR(INVALID_THREAD));
        return JNI_TRUE;
    }

    if (!validateSuspendedThread(out, thread)) {
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 1) {

        jobject monitor;
        jvmtiError error;

        error = JVMTI_FUNC_PTR(gdata->jvmti,GetCurrentContendedMonitor)
                                (gdata->jvmti, thread, &monitor);

        if (error != JVMTI_ERROR_NONE) {
            outStream_setError(out, map2jdwpError(error));
        } else {
            (void)outStream_writeByte(out, specificTypeKey(env, monitor));
            (void)outStream_writeObjectRef(env, out, monitor);
        }

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jboolean
stop(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiError error;
    jthread thread;
    jobject throwable;
    JNIEnv *env;

    env = getEnv();
    thread = inStream_readThreadRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    throwable = inStream_readObjectRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    if (threadControl_isDebugThread(thread)) {
        outStream_setError(out, JDWP_ERROR(INVALID_THREAD));
        return JNI_TRUE;
    }

    error = threadControl_stop(thread, throwable);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
    }
    return JNI_TRUE;
}

static jboolean
interrupt(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiError error;
    jthread thread;

    thread = inStream_readThreadRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    if (threadControl_isDebugThread(thread)) {
        outStream_setError(out, JDWP_ERROR(INVALID_THREAD));
        return JNI_TRUE;
    }

    error = threadControl_interrupt(thread);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
    }
    return JNI_TRUE;
}

static jboolean
suspendCount(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiError error;
    jint count;
    jthread thread;

    thread = inStream_readThreadRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    if (threadControl_isDebugThread(thread)) {
        outStream_setError(out, JDWP_ERROR(INVALID_THREAD));
        return JNI_TRUE;
    }

    error = threadControl_suspendCount(thread, &count);
    if (error != JVMTI_ERROR_NONE) {
        outStream_setError(out, map2jdwpError(error));
        return JNI_TRUE;
    }

    (void)outStream_writeInt(out, count);
    return JNI_TRUE;
}

static jboolean
ownedMonitorsWithStackDepth(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jthread thread;

    thread = inStream_readThreadRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    if (thread == NULL || threadControl_isDebugThread(thread)) {
        outStream_setError(out, JDWP_ERROR(INVALID_THREAD));
        return JNI_TRUE;
    }

    if (!validateSuspendedThread(out, thread)) {
        return JNI_TRUE;
    }

    env = getEnv();

    WITH_LOCAL_REFS(env, 1) {

        jvmtiError error = JVMTI_ERROR_NONE;
        jint count = 0;
        jvmtiMonitorStackDepthInfo *monitors=NULL;

        error = JVMTI_FUNC_PTR(gdata->jvmti,GetOwnedMonitorStackDepthInfo)
                                (gdata->jvmti, thread, &count, &monitors);

        if (error != JVMTI_ERROR_NONE) {
            outStream_setError(out, map2jdwpError(error));
        } else {
            int i;
            (void)outStream_writeInt(out, count);
            for (i = 0; i < count; i++) {
                jobject monitor = monitors[i].monitor;
                (void)outStream_writeByte(out, specificTypeKey(env, monitor));
                (void)outStream_writeObjectRef(getEnv(), out, monitor);
                (void)outStream_writeInt(out,monitors[i].stack_depth);
            }
        }
        if (monitors != NULL) {
            jvmtiDeallocate(monitors);
        }

    } END_WITH_LOCAL_REFS(env);

    return JNI_TRUE;
}

static jboolean
forceEarlyReturn(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jthread thread;
    jvalue value;
    jbyte typeKey;
    jvmtiError error;

    env = getEnv();
    thread = inStream_readThreadRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    if (threadControl_isDebugThread(thread)) {
        outStream_setError(out, JDWP_ERROR(INVALID_THREAD));
        return JNI_TRUE;
    }

    typeKey = inStream_readByte(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    if (isObjectTag(typeKey)) {
        value.l = inStream_readObjectRef(env, in);
        error = JVMTI_FUNC_PTR(gdata->jvmti,ForceEarlyReturnObject)
                        (gdata->jvmti, thread, value.l);
    } else {
        switch (typeKey) {
            case JDWP_TAG(VOID):
                error = JVMTI_FUNC_PTR(gdata->jvmti,ForceEarlyReturnVoid)
                                (gdata->jvmti, thread);
                break;
            case JDWP_TAG(BYTE):
                value.b = inStream_readByte(in);
                error = JVMTI_FUNC_PTR(gdata->jvmti,ForceEarlyReturnInt)
                                (gdata->jvmti, thread, value.b);
                break;

            case JDWP_TAG(CHAR):
                value.c = inStream_readChar(in);
                error = JVMTI_FUNC_PTR(gdata->jvmti,ForceEarlyReturnInt)
                                (gdata->jvmti, thread, value.c);
                break;

            case JDWP_TAG(FLOAT):
                value.f = inStream_readFloat(in);
                error = JVMTI_FUNC_PTR(gdata->jvmti,ForceEarlyReturnFloat)
                                (gdata->jvmti, thread, value.f);
                break;

            case JDWP_TAG(DOUBLE):
                value.d = inStream_readDouble(in);
                error = JVMTI_FUNC_PTR(gdata->jvmti,ForceEarlyReturnDouble)
                                (gdata->jvmti, thread, value.d);
                break;

            case JDWP_TAG(INT):
                value.i = inStream_readInt(in);
                error = JVMTI_FUNC_PTR(gdata->jvmti,ForceEarlyReturnInt)
                                (gdata->jvmti, thread, value.i);
                break;

            case JDWP_TAG(LONG):
                value.j = inStream_readLong(in);
                error = JVMTI_FUNC_PTR(gdata->jvmti,ForceEarlyReturnLong)
                                (gdata->jvmti, thread, value.j);
                break;

            case JDWP_TAG(SHORT):
                value.s = inStream_readShort(in);
                error = JVMTI_FUNC_PTR(gdata->jvmti,ForceEarlyReturnInt)
                                (gdata->jvmti, thread, value.s);
                break;

            case JDWP_TAG(BOOLEAN):
                value.z = inStream_readBoolean(in);
                error = JVMTI_FUNC_PTR(gdata->jvmti,ForceEarlyReturnInt)
                                (gdata->jvmti, thread, value.z);
                break;

            default:
                error =  AGENT_ERROR_INVALID_TAG;
                break;
        }
    }
    {
      jdwpError serror = map2jdwpError(error);
      if (serror != JDWP_ERROR(NONE)) {
        outStream_setError(out, serror);
      }
    }
    return JNI_TRUE;
}

Command ThreadReference_Commands[] = {
    {name, "Name"},
    {suspend, "Suspend"},
    {resume, "Resume"},
    {status, "Status"},
    {threadGroup, "ThreadGroup"},
    {frames, "Frames"},
    {getFrameCount, "GetFrameCount"},
    {ownedMonitors, "OwnedMonitors"},
    {currentContendedMonitor, "CurrentContendedMonitor"},
    {stop, "Stop"},
    {interrupt, "Interrupt"},
    {suspendCount, "SuspendCount"},
    {ownedMonitorsWithStackDepth, "OwnedMonitorsWithStackDepth"},
    {forceEarlyReturn, "ForceEarlyReturn"}
};

DEBUG_DISPATCH_DEFINE_CMDSET(ThreadReference)
