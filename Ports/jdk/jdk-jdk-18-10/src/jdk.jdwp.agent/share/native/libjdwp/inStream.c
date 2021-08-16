/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "utf_util.h"
#include "stream.h"
#include "inStream.h"
#include "transport.h"
#include "bag.h"
#include "commonRef.h"
#include "FrameID.h"

#define INITIAL_REF_ALLOC 50
#define SMALLEST(a, b) ((a) < (b)) ? (a) : (b)

/*
 * TO DO: Support processing of replies through command input streams.
 */
void
inStream_init(PacketInputStream *stream, jdwpPacket packet)
{
    stream->packet = packet;
    stream->error = JDWP_ERROR(NONE);
    stream->left = packet.type.cmd.len - JDWP_HEADER_SIZE;
    stream->current = packet.type.cmd.data;
    stream->refs = bagCreateBag(sizeof(jobject), INITIAL_REF_ALLOC);
    if (stream->refs == NULL) {
        stream->error = JDWP_ERROR(OUT_OF_MEMORY);
    }
}

jint
inStream_id(PacketInputStream *stream)
{
    return stream->packet.type.cmd.id;
}

jbyte
inStream_command(PacketInputStream *stream)
{
    return stream->packet.type.cmd.cmd;
}

static jdwpError
readBytes(PacketInputStream *stream, void *dest, int size)
{
    if (stream->error) {
        return stream->error;
    }

    if (size > stream->left) {
        stream->error = JDWP_ERROR(INTERNAL);
        return stream->error;
    }

    if (dest) {
        (void)memcpy(dest, stream->current, size);
    }
    stream->current += size;
    stream->left -= size;

    return stream->error;
}

jdwpError
inStream_skipBytes(PacketInputStream *stream, jint size) {
    return readBytes(stream, NULL, size);
}

jboolean
inStream_readBoolean(PacketInputStream *stream)
{
    jbyte flag = 0;
    (void)readBytes(stream, &flag, sizeof(flag));
    if (stream->error) {
        return 0;
    } else {
        return flag ? JNI_TRUE : JNI_FALSE;
    }
}

jbyte
inStream_readByte(PacketInputStream *stream)
{
    jbyte val = 0;
    (void)readBytes(stream, &val, sizeof(val));
    return val;
}

jbyte *
inStream_readBytes(PacketInputStream *stream, int length, jbyte *buf)
{
    (void)readBytes(stream, buf, length);
    return buf;
}

jchar
inStream_readChar(PacketInputStream *stream)
{
    jchar val = 0;
    (void)readBytes(stream, &val, sizeof(val));
    return JAVA_TO_HOST_CHAR(val);
}

jshort
inStream_readShort(PacketInputStream *stream)
{
    jshort val = 0;
    (void)readBytes(stream, &val, sizeof(val));
    return JAVA_TO_HOST_SHORT(val);
}

jint
inStream_readInt(PacketInputStream *stream)
{
    jint val = 0;
    (void)readBytes(stream, &val, sizeof(val));
    return JAVA_TO_HOST_INT(val);
}

jlong
inStream_readLong(PacketInputStream *stream)
{
    jlong val = 0;
    (void)readBytes(stream, &val, sizeof(val));
    return JAVA_TO_HOST_LONG(val);
}

jfloat
inStream_readFloat(PacketInputStream *stream)
{
    jfloat val = 0;
    (void)readBytes(stream, &val, sizeof(val));
    return JAVA_TO_HOST_FLOAT(val);
}

jdouble
inStream_readDouble(PacketInputStream *stream)
{
    jdouble val = 0;
    (void)readBytes(stream, &val, sizeof(val));
    return JAVA_TO_HOST_DOUBLE(val);
}

/*
 * Read a module from the stream. The ID used in the wire protocol
 * is converted to a reference which is returned. The reference is
 * global and strong, but it should *not* be deleted by the caller
 * since it is freed when this stream is destroyed.
 */
jobject
inStream_readModuleRef(JNIEnv *env, PacketInputStream *stream)
{
    jobject ref = inStream_readObjectRef(env, stream);
    if (ref == NULL && stream->error == JDWP_ERROR(INVALID_OBJECT)) {
        stream->error = JDWP_ERROR(INVALID_MODULE);
        return NULL;
    }

    return ref;
}

/*
 * Read an object from the stream. The ID used in the wire protocol
 * is converted to a reference which is returned. The reference is
 * global and strong, but it should *not* be deleted by the caller
 * since it is freed when this stream is destroyed.
 */
jobject
inStream_readObjectRef(JNIEnv *env, PacketInputStream *stream)
{
    jobject ref;
    jobject *refPtr;
    jlong id = inStream_readLong(stream);
    if (stream->error) {
        return NULL;
    }
    if (id == NULL_OBJECT_ID) {
        return NULL;
    }

    ref = commonRef_idToRef(env, id);
    if (ref == NULL) {
        stream->error = JDWP_ERROR(INVALID_OBJECT);
        return NULL;
    }

    refPtr = bagAdd(stream->refs);
    if (refPtr == NULL) {
        commonRef_idToRef_delete(env, ref);
        return NULL;
    }

    *refPtr = ref;
    return ref;
}

/*
 * Read a raw object id from the stream. This should be used rarely.
 * Normally, inStream_readObjectRef is preferred since it takes care
 * of reference conversion and tracking. Only code that needs to
 * perform maintence of the commonRef hash table uses this function.
 */
jlong
inStream_readObjectID(PacketInputStream *stream)
{
    return inStream_readLong(stream);
}

jclass
inStream_readClassRef(JNIEnv *env, PacketInputStream *stream)
{
    jobject object = inStream_readObjectRef(env, stream);
    if (object == NULL) {
        /*
         * Could be error or just the null reference. In either case,
         * stop now.
         */
        return NULL;
    }
    if (!isClass(object)) {
        stream->error = JDWP_ERROR(INVALID_CLASS);
        return NULL;
    }
    return object;
}

jthread
inStream_readThreadRef(JNIEnv *env, PacketInputStream *stream)
{
    jobject object = inStream_readObjectRef(env, stream);
    if (object == NULL) {
        /*
         * Could be error or just the null reference. In either case,
         * stop now.
         */
        return NULL;
    }
    if (!isThread(object)) {
        stream->error = JDWP_ERROR(INVALID_THREAD);
        return NULL;
    }
    return object;
}

jthreadGroup
inStream_readThreadGroupRef(JNIEnv *env, PacketInputStream *stream)
{
    jobject object = inStream_readObjectRef(env, stream);
    if (object == NULL) {
        /*
         * Could be error or just the null reference. In either case,
         * stop now.
         */
        return NULL;
    }
    if (!isThreadGroup(object)) {
        stream->error = JDWP_ERROR(INVALID_THREAD_GROUP);
        return NULL;
    }
    return object;
}

jstring
inStream_readStringRef(JNIEnv *env, PacketInputStream *stream)
{
    jobject object = inStream_readObjectRef(env, stream);
    if (object == NULL) {
        /*
         * Could be error or just the null reference. In either case,
         * stop now.
         */
        return NULL;
    }
    if (!isString(object)) {
        stream->error = JDWP_ERROR(INVALID_STRING);
        return NULL;
    }
    return object;
}

jclass
inStream_readClassLoaderRef(JNIEnv *env, PacketInputStream *stream)
{
    jobject object = inStream_readObjectRef(env, stream);
    if (object == NULL) {
        /*
         * Could be error or just the null reference. In either case,
         * stop now.
         */
        return NULL;
    }
    if (!isClassLoader(object)) {
        stream->error = JDWP_ERROR(INVALID_CLASS_LOADER);
        return NULL;
    }
    return object;
}

jarray
inStream_readArrayRef(JNIEnv *env, PacketInputStream *stream)
{
    jobject object = inStream_readObjectRef(env, stream);
    if (object == NULL) {
        /*
         * Could be error or just the null reference. In either case,
         * stop now.
         */
        return NULL;
    }
    if (!isArray(object)) {
        stream->error = JDWP_ERROR(INVALID_ARRAY);
        return NULL;
    }
    return object;
}

/*
 * Next 3 functions read an Int and convert to a Pointer!?
 * If sizeof(jxxxID) == 8 we must read these values as Longs.
 */
FrameID
inStream_readFrameID(PacketInputStream *stream)
{
    if (sizeof(FrameID) == 8) {
        /*LINTED*/
        return (FrameID)inStream_readLong(stream);
    } else {
        /*LINTED*/
        return (FrameID)inStream_readInt(stream);
    }
}

jmethodID
inStream_readMethodID(PacketInputStream *stream)
{
    if (sizeof(jmethodID) == 8) {
        /*LINTED*/
        return (jmethodID)(intptr_t)inStream_readLong(stream);
    } else {
        /*LINTED*/
        return (jmethodID)(intptr_t)inStream_readInt(stream);
    }
}

jfieldID
inStream_readFieldID(PacketInputStream *stream)
{
    if (sizeof(jfieldID) == 8) {
        /*LINTED*/
        return (jfieldID)(intptr_t)inStream_readLong(stream);
    } else {
        /*LINTED*/
        return (jfieldID)(intptr_t)inStream_readInt(stream);
    }
}

jlocation
inStream_readLocation(PacketInputStream *stream)
{
    return (jlocation)inStream_readLong(stream);
}

char *
inStream_readString(PacketInputStream *stream)
{
    int length;
    char *string;

    length = inStream_readInt(stream);
    string = jvmtiAllocate(length + 1);
    if (string != NULL) {
        int new_length;

        (void)readBytes(stream, string, length);
        string[length] = '\0';

        /* This is Standard UTF-8, convert to Modified UTF-8 if necessary */
        new_length = utf8sToUtf8mLength((jbyte*)string, length);
        if ( new_length != length ) {
            char *new_string;

            new_string = jvmtiAllocate(new_length+1);
            utf8sToUtf8m((jbyte*)string, length, (jbyte*)new_string, new_length);
            jvmtiDeallocate(string);
            return new_string;
        }
    }
    return string;
}

jdwpError
inStream_error(PacketInputStream *stream)
{
    return stream->error;
}

void
inStream_clearError(PacketInputStream *stream)
{
    stream->error = JDWP_ERROR(NONE);
}

jvalue
inStream_readValue(PacketInputStream *stream)
{
    jvalue value;
    jbyte typeKey = inStream_readByte(stream);
    if (stream->error) {
        value.j = 0L;
        return value;
    }

    if (isObjectTag(typeKey)) {
        value.l = inStream_readObjectRef(getEnv(), stream);
    } else {
        switch (typeKey) {
            case JDWP_TAG(BYTE):
                value.b = inStream_readByte(stream);
                break;

            case JDWP_TAG(CHAR):
                value.c = inStream_readChar(stream);
                break;

            case JDWP_TAG(FLOAT):
                value.f = inStream_readFloat(stream);
                break;

            case JDWP_TAG(DOUBLE):
                value.d = inStream_readDouble(stream);
                break;

            case JDWP_TAG(INT):
                value.i = inStream_readInt(stream);
                break;

            case JDWP_TAG(LONG):
                value.j = inStream_readLong(stream);
                break;

            case JDWP_TAG(SHORT):
                value.s = inStream_readShort(stream);
                break;

            case JDWP_TAG(BOOLEAN):
                value.z = inStream_readBoolean(stream);
                break;
            default:
                stream->error = JDWP_ERROR(INVALID_TAG);
                break;
        }
    }
    return value;
}

static jboolean
deleteRef(void *elementPtr, void *arg)
{
    JNIEnv *env = arg;
    jobject *refPtr = elementPtr;
    commonRef_idToRef_delete(env, *refPtr);
    return JNI_TRUE;
}

void
inStream_destroy(PacketInputStream *stream)
{
    if (stream->packet.type.cmd.data != NULL) {
    jvmtiDeallocate(stream->packet.type.cmd.data);
    }

    (void)bagEnumerateOver(stream->refs, deleteRef, (void *)getEnv());
    bagDestroyBag(stream->refs);
}
