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
#include "StackFrameImpl.h"
#include "inStream.h"
#include "outStream.h"
#include "threadControl.h"
#include "FrameID.h"

static jdwpError
validateThreadFrame(jthread thread, FrameID frame)
{
    jvmtiError error;
    jdwpError  serror;
    jint count;
    error = threadControl_suspendCount(thread, &count);
    if ( error == JVMTI_ERROR_NONE ) {
        if ( count > 0 ) {
            serror = validateFrameID(thread, frame);
        } else {
            serror = JDWP_ERROR(THREAD_NOT_SUSPENDED);
        }
    } else {
        serror =  map2jdwpError(error);
    }
    return serror;
}

static jdwpError
writeVariableValue(JNIEnv *env, PacketOutputStream *out, jthread thread,
                   FrameNumber fnum, jint slot, jbyte typeKey)
{
    jvmtiError error;
    jvalue value;

    if (isObjectTag(typeKey)) {

        WITH_LOCAL_REFS(env, 1) {

            error = JVMTI_FUNC_PTR(gdata->jvmti,GetLocalObject)
                        (gdata->jvmti, thread, fnum, slot, &value.l);

            if (error != JVMTI_ERROR_NONE) {
                outStream_setError(out, map2jdwpError(error));
            } else {
                (void)outStream_writeByte(out, specificTypeKey(env, value.l));
                (void)outStream_writeObjectRef(env, out, value.l);
            }

        } END_WITH_LOCAL_REFS(env);

    } else {
        /*
         * For primitive types, the type key is bounced back as is.
         */
        (void)outStream_writeByte(out, typeKey);
        switch (typeKey) {
            case JDWP_TAG(BYTE): {
                    jint intValue;
                    error = JVMTI_FUNC_PTR(gdata->jvmti,GetLocalInt)
                                (gdata->jvmti, thread, fnum, slot, &intValue);
                    (void)outStream_writeByte(out, (jbyte)intValue);
                    break;
                }

            case JDWP_TAG(CHAR): {
                    jint intValue;
                    error = JVMTI_FUNC_PTR(gdata->jvmti,GetLocalInt)
                                (gdata->jvmti, thread, fnum, slot, &intValue);
                    (void)outStream_writeChar(out, (jchar)intValue);
                    break;
                }

            case JDWP_TAG(FLOAT):
                error = JVMTI_FUNC_PTR(gdata->jvmti,GetLocalFloat)
                                (gdata->jvmti, thread, fnum, slot, &value.f);
                (void)outStream_writeFloat(out, value.f);
                break;

            case JDWP_TAG(DOUBLE):
                error = JVMTI_FUNC_PTR(gdata->jvmti,GetLocalDouble)
                                (gdata->jvmti, thread, fnum, slot, &value.d);
                (void)outStream_writeDouble(out, value.d);
                break;

            case JDWP_TAG(INT):
                error = JVMTI_FUNC_PTR(gdata->jvmti,GetLocalInt)
                                (gdata->jvmti, thread, fnum, slot, &value.i);
                (void)outStream_writeInt(out, value.i);
                break;

            case JDWP_TAG(LONG):
                error = JVMTI_FUNC_PTR(gdata->jvmti,GetLocalLong)
                                (gdata->jvmti, thread, fnum, slot, &value.j);
                (void)outStream_writeLong(out, value.j);
                break;

            case JDWP_TAG(SHORT): {
                jint intValue;
                error = JVMTI_FUNC_PTR(gdata->jvmti,GetLocalInt)
                                (gdata->jvmti, thread, fnum, slot, &intValue);
                (void)outStream_writeShort(out, (jshort)intValue);
                break;
            }

            case JDWP_TAG(BOOLEAN):{
                jint intValue;
                error = JVMTI_FUNC_PTR(gdata->jvmti,GetLocalInt)
                                (gdata->jvmti, thread, fnum, slot, &intValue);
                (void)outStream_writeBoolean(out, (jboolean)intValue);
                break;
            }

            default:
                return JDWP_ERROR(INVALID_TAG);
        }
    }

    return map2jdwpError(error);
}

static jdwpError
readVariableValue(JNIEnv *env, PacketInputStream *in, jthread thread,
                  FrameNumber fnum, jint slot, jbyte typeKey)
{
    jvmtiError error;
    jvalue value;

    if (isObjectTag(typeKey)) {

        value.l = inStream_readObjectRef(env, in);

        error = JVMTI_FUNC_PTR(gdata->jvmti,SetLocalObject)
                        (gdata->jvmti, thread, fnum, slot, value.l);

    } else {
        switch (typeKey) {
            case JDWP_TAG(BYTE):
                value.b = inStream_readByte(in);
                error = JVMTI_FUNC_PTR(gdata->jvmti,SetLocalInt)
                                (gdata->jvmti, thread, fnum, slot, value.b);
                break;

            case JDWP_TAG(CHAR):
                value.c = inStream_readChar(in);
                error = JVMTI_FUNC_PTR(gdata->jvmti,SetLocalInt)
                                (gdata->jvmti, thread, fnum, slot, value.c);
                break;

            case JDWP_TAG(FLOAT):
                value.f = inStream_readFloat(in);
                error = JVMTI_FUNC_PTR(gdata->jvmti,SetLocalFloat)
                                (gdata->jvmti, thread, fnum, slot, value.f);
                break;

            case JDWP_TAG(DOUBLE):
                value.d = inStream_readDouble(in);
                error = JVMTI_FUNC_PTR(gdata->jvmti,SetLocalDouble)
                                (gdata->jvmti, thread, fnum, slot, value.d);
                break;

            case JDWP_TAG(INT):
                value.i = inStream_readInt(in);
                error = JVMTI_FUNC_PTR(gdata->jvmti,SetLocalInt)
                                (gdata->jvmti, thread, fnum, slot, value.i);
                break;

            case JDWP_TAG(LONG):
                value.j = inStream_readLong(in);
                error = JVMTI_FUNC_PTR(gdata->jvmti,SetLocalLong)
                                (gdata->jvmti, thread, fnum, slot, value.j);
                break;

            case JDWP_TAG(SHORT):
                value.s = inStream_readShort(in);
                error = JVMTI_FUNC_PTR(gdata->jvmti,SetLocalInt)
                                (gdata->jvmti, thread, fnum, slot, value.s);
                break;

            case JDWP_TAG(BOOLEAN):
                value.z = inStream_readBoolean(in);
                error = JVMTI_FUNC_PTR(gdata->jvmti,SetLocalInt)
                                (gdata->jvmti, thread, fnum, slot, value.z);
                break;

            default:
                return JDWP_ERROR(INVALID_TAG);
        }
    }

    return map2jdwpError(error);
}

static jboolean
getValues(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    int i;
    jdwpError serror;
    jthread thread;
    FrameID frame;
    jint variableCount;

    env = getEnv();

    thread = inStream_readThreadRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    frame = inStream_readFrameID(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    variableCount = inStream_readInt(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    /*
     * Validate the frame id
     */
    serror = validateThreadFrame(thread, frame);
    if (serror != JDWP_ERROR(NONE)) {
        outStream_setError(out, serror);
        return JNI_TRUE;
    }

    (void)outStream_writeInt(out, variableCount);
    for (i = 0; (i < variableCount) && !outStream_error(out); i++) {
        jint slot;
        jbyte typeKey;
        FrameNumber fnum;

        slot = inStream_readInt(in);
        if (inStream_error(in))
            break;
        typeKey = inStream_readByte(in);
        if (inStream_error(in))
            break;

        fnum = getFrameNumber(frame);
        serror = writeVariableValue(env, out, thread, fnum, slot, typeKey);
        if (serror != JDWP_ERROR(NONE)) {
            outStream_setError(out, serror);
            break;
        }
    }

    return JNI_TRUE;
}

static jboolean
setValues(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jint i;
    jdwpError serror;
    jthread thread;
    FrameID frame;
    jint variableCount;

    env = getEnv();

    thread = inStream_readThreadRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    frame = inStream_readFrameID(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }
    variableCount = inStream_readInt(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    /*
     * Validate the frame id
     */
    serror = validateThreadFrame(thread, frame);
    if (serror != JDWP_ERROR(NONE)) {
        outStream_setError(out, serror);
        return JNI_TRUE;
    }

    for (i = 0; (i < variableCount) && !inStream_error(in); i++) {

        jint slot;
        jbyte typeKey;
        FrameNumber fnum;

        slot = inStream_readInt(in);
        if (inStream_error(in)) {
            return JNI_TRUE;
        }
        typeKey = inStream_readByte(in);
        if (inStream_error(in)) {
            return JNI_TRUE;
        }

        fnum = getFrameNumber(frame);
        serror = readVariableValue(env, in, thread, fnum, slot, typeKey);
        if (serror != JDWP_ERROR(NONE))
            break;
    }

    if (serror != JDWP_ERROR(NONE)) {
        outStream_setError(out, serror);
    }

    return JNI_TRUE;
}

static jboolean
thisObject(PacketInputStream *in, PacketOutputStream *out)
{
    JNIEnv *env;
    jdwpError serror;
    jthread thread;
    FrameID frame;

    env = getEnv();

    thread = inStream_readThreadRef(env, in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    frame = inStream_readFrameID(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    /*
     * Validate the frame id
     */
    serror = validateThreadFrame(thread, frame);
    if (serror != JDWP_ERROR(NONE)) {
        outStream_setError(out, serror);
        return JNI_TRUE;
    }

    WITH_LOCAL_REFS(env, 2) {

        jvmtiError error;
        jmethodID method;
        jlocation location;
        FrameNumber fnum;

        /*
         * Find out if the given frame is for a static or native method.
         */
        fnum = getFrameNumber(frame);
        error = JVMTI_FUNC_PTR(gdata->jvmti,GetFrameLocation)
                (gdata->jvmti, thread, fnum, &method, &location);
        if (error == JVMTI_ERROR_NONE) {

            jint modifiers;

            error = methodModifiers(method, &modifiers);
            if (error == JVMTI_ERROR_NONE) {

                jobject this_object;

                /*
                 * Return null for static or native methods; otherwise, the JVM
                 * spec guarantees that "this" is in slot 0
                 */
                if (modifiers & (MOD_STATIC | MOD_NATIVE)) {
                    this_object = NULL;
                    (void)outStream_writeByte(out, specificTypeKey(env, this_object));
                    (void)outStream_writeObjectRef(env, out, this_object);
                } else {
                    error = JVMTI_FUNC_PTR(gdata->jvmti,GetLocalObject)
                                (gdata->jvmti, thread, fnum, 0, &this_object);
                    if (error == JVMTI_ERROR_NONE) {
                        (void)outStream_writeByte(out, specificTypeKey(env, this_object));
                        (void)outStream_writeObjectRef(env, out, this_object);
                    }
                }

            }
        }
        serror = map2jdwpError(error);

    } END_WITH_LOCAL_REFS(env);

    if (serror != JDWP_ERROR(NONE))
        outStream_setError(out, serror);

    return JNI_TRUE;
}

static jboolean
popFrames(PacketInputStream *in, PacketOutputStream *out)
{
    jvmtiError error;
    jdwpError serror;
    jthread thread;
    FrameID frame;
    FrameNumber fnum;

    thread = inStream_readThreadRef(getEnv(), in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    frame = inStream_readFrameID(in);
    if (inStream_error(in)) {
        return JNI_TRUE;
    }

    /*
     * Validate the frame id
     */
    serror = validateThreadFrame(thread, frame);
    if (serror != JDWP_ERROR(NONE)) {
        outStream_setError(out, serror);
        return JNI_TRUE;
    }

    if (threadControl_isDebugThread(thread)) {
        outStream_setError(out, JDWP_ERROR(INVALID_THREAD));
        return JNI_TRUE;
    }

    fnum = getFrameNumber(frame);
    error = threadControl_popFrames(thread, fnum);
    if (error != JVMTI_ERROR_NONE) {
        serror = map2jdwpError(error);
        outStream_setError(out, serror);
    }
    return JNI_TRUE;
}

Command StackFrame_Commands[] = {
    {getValues, "GetValues"},
    {setValues, "SetValues"},
    {thisObject, "ThisObject"},
    {popFrames, "PopFrames"}
};

DEBUG_DISPATCH_DEFINE_CMDSET(StackFrame)
