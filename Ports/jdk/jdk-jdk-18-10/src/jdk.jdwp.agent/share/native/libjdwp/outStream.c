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
#include "outStream.h"
#include "inStream.h"
#include "transport.h"
#include "commonRef.h"
#include "bag.h"
#include "FrameID.h"

#define INITIAL_ID_ALLOC  50
#define SMALLEST(a, b) ((a) < (b)) ? (a) : (b)

static void
commonInit(PacketOutputStream *stream)
{
    stream->current = &stream->initialSegment[0];
    stream->left = sizeof(stream->initialSegment);
    stream->segment = &stream->firstSegment;
    stream->segment->length = 0;
    stream->segment->data = &stream->initialSegment[0];
    stream->segment->next = NULL;
    stream->error = JDWP_ERROR(NONE);
    stream->sent = JNI_FALSE;
    stream->ids = bagCreateBag(sizeof(jlong), INITIAL_ID_ALLOC);
    if (stream->ids == NULL) {
        stream->error = JDWP_ERROR(OUT_OF_MEMORY);
    }
}

void
outStream_initCommand(PacketOutputStream *stream, jint id,
                      jbyte flags, jbyte commandSet, jbyte command)
{
    commonInit(stream);

    /*
     * Command-specific initialization
     */
    stream->packet.type.cmd.id = id;
    stream->packet.type.cmd.cmdSet = commandSet;
    stream->packet.type.cmd.cmd = command;

    stream->packet.type.cmd.flags = flags;
}

void
outStream_initReply(PacketOutputStream *stream, jint id)
{
    commonInit(stream);

    /*
     * Reply-specific initialization
     */
    stream->packet.type.reply.id = id;
    stream->packet.type.reply.errorCode = 0x0;
    stream->packet.type.cmd.flags = (jbyte)JDWPTRANSPORT_FLAGS_REPLY;
}

jint
outStream_id(PacketOutputStream *stream)
{
    return stream->packet.type.cmd.id;
}

jbyte
outStream_command(PacketOutputStream *stream)
{
    /* Only makes sense for commands */
    JDI_ASSERT(!(stream->packet.type.cmd.flags & JDWPTRANSPORT_FLAGS_REPLY));
    return stream->packet.type.cmd.cmd;
}

static jdwpError
writeBytes(PacketOutputStream *stream, void *source, int size)
{
    jbyte *bytes = (jbyte *)source;

    if (stream->error) {
        return stream->error;
    }
    while (size > 0) {
        jint count;
        if (stream->left == 0) {
            jint segSize = SMALLEST(2 * stream->segment->length, MAX_SEGMENT_SIZE);
            jbyte *newSeg = jvmtiAllocate(segSize);
            struct PacketData *newHeader = jvmtiAllocate(sizeof(*newHeader));
            if ((newSeg == NULL) || (newHeader == NULL)) {
                jvmtiDeallocate(newSeg);
                jvmtiDeallocate(newHeader);
                stream->error = JDWP_ERROR(OUT_OF_MEMORY);
                return stream->error;
            }
            newHeader->length = 0;
            newHeader->data = newSeg;
            newHeader->next = NULL;
            stream->segment->next = newHeader;
            stream->segment = newHeader;
            stream->current = newHeader->data;
            stream->left = segSize;
        }
        count = SMALLEST(size, stream->left);
        (void)memcpy(stream->current, bytes, count);
        stream->current += count;
        stream->left -= count;
        stream->segment->length += count;
        size -= count;
        bytes += count;
    }
    return JDWP_ERROR(NONE);
}

jdwpError
outStream_writeBoolean(PacketOutputStream *stream, jboolean val)
{
    jbyte byte = (val != 0) ? 1 : 0;
    return writeBytes(stream, &byte, sizeof(byte));
}

jdwpError
outStream_writeByte(PacketOutputStream *stream, jbyte val)
{
    return writeBytes(stream, &val, sizeof(val));
}

jdwpError
outStream_writeChar(PacketOutputStream *stream, jchar val)
{
    val = HOST_TO_JAVA_CHAR(val);
    return writeBytes(stream, &val, sizeof(val));
}

jdwpError
outStream_writeShort(PacketOutputStream *stream, jshort val)
{
    val = HOST_TO_JAVA_SHORT(val);
    return writeBytes(stream, &val, sizeof(val));
}

jdwpError
outStream_writeInt(PacketOutputStream *stream, jint val)
{
    val = HOST_TO_JAVA_INT(val);
    return writeBytes(stream, &val, sizeof(val));
}

jdwpError
outStream_writeLong(PacketOutputStream *stream, jlong val)
{
    val = HOST_TO_JAVA_LONG(val);
    return writeBytes(stream, &val, sizeof(val));
}

jdwpError
outStream_writeFloat(PacketOutputStream *stream, jfloat val)
{
    val = HOST_TO_JAVA_FLOAT(val);
    return writeBytes(stream, &val, sizeof(val));
}

jdwpError
outStream_writeDouble(PacketOutputStream *stream, jdouble val)
{
    val = HOST_TO_JAVA_DOUBLE(val);
    return writeBytes(stream, &val, sizeof(val));
}

jdwpError
outStream_writeObjectTag(JNIEnv *env, PacketOutputStream *stream, jobject val)
{
    return outStream_writeByte(stream, specificTypeKey(env, val));
}

jdwpError
outStream_writeModuleRef(JNIEnv *env, PacketOutputStream *stream, jobject val)
{
    return outStream_writeObjectRef(env, stream, val);
}

jdwpError
outStream_writeObjectRef(JNIEnv *env, PacketOutputStream *stream, jobject val)
{
    jlong id;
    jlong *idPtr;

    if (stream->error) {
        return stream->error;
    }

    if (val == NULL) {
        id = NULL_OBJECT_ID;
    } else {
        /* Convert the object to an object id */
        id = commonRef_refToID(env, val);
        if (id == NULL_OBJECT_ID) {
            stream->error = JDWP_ERROR(OUT_OF_MEMORY);
            return stream->error;
        }

        /* Track the common ref in case we need to release it on a future error */
        idPtr = bagAdd(stream->ids);
        if (idPtr == NULL) {
            commonRef_release(env, id);
            stream->error = JDWP_ERROR(OUT_OF_MEMORY);
            return stream->error;
        } else {
            *idPtr = id;
        }

        /* Add the encoded object id to the stream */
        id = HOST_TO_JAVA_LONG(id);
    }

    return writeBytes(stream, &id, sizeof(id));
}

jdwpError
outStream_writeFrameID(PacketOutputStream *stream, FrameID val)
{
    /*
     * Not good - we're writing a pointer as a jint.  Need
     * to write as a jlong if sizeof(FrameID) == 8.
     */
    if (sizeof(FrameID) == 8) {
        /*LINTED*/
        return outStream_writeLong(stream, (jlong)val);
    } else {
        /*LINTED*/
        return outStream_writeInt(stream, (jint)val);
    }
}

jdwpError
outStream_writeMethodID(PacketOutputStream *stream, jmethodID val)
{
    /*
     * Not good - we're writing a pointer as a jint.  Need
     * to write as a jlong if sizeof(jmethodID) == 8.
     */
    if (sizeof(jmethodID) == 8) {
        /*LINTED*/
        return outStream_writeLong(stream, (jlong)(intptr_t)val);
    } else {
        /*LINTED*/
        return outStream_writeInt(stream, (jint)(intptr_t)val);
    }
}

jdwpError
outStream_writeFieldID(PacketOutputStream *stream, jfieldID val)
{
    /*
     * Not good - we're writing a pointer as a jint.  Need
     * to write as a jlong if sizeof(jfieldID) == 8.
     */
    if (sizeof(jfieldID) == 8) {
        /*LINTED*/
        return outStream_writeLong(stream, (jlong)(intptr_t)val);
    } else {
        /*LINTED*/
        return outStream_writeInt(stream, (jint)(intptr_t)val);
    }
}

jdwpError
outStream_writeLocation(PacketOutputStream *stream, jlocation val)
{
    return outStream_writeLong(stream, (jlong)val);
}

jdwpError
outStream_writeByteArray(PacketOutputStream*stream, jint length,
                         jbyte *bytes)
{
    (void)outStream_writeInt(stream, length);
    return writeBytes(stream, bytes, length);
}

jdwpError
outStream_writeString(PacketOutputStream *stream, char *string)
{
    jdwpError error;
    jint      length = string != NULL ? (int)strlen(string) : 0;

    /* Options utf8=y/n controls if we want Standard UTF-8 or Modified */
    if ( gdata->modifiedUtf8 ) {
        (void)outStream_writeInt(stream, length);
        error = writeBytes(stream, (jbyte *)string, length);
    } else {
        jint      new_length;

        new_length = utf8mToUtf8sLength((jbyte*)string, length);
        if ( new_length == length ) {
            (void)outStream_writeInt(stream, length);
            error = writeBytes(stream, (jbyte *)string, length);
        } else {
            char *new_string;

            new_string = jvmtiAllocate(new_length+1);
            utf8mToUtf8s((jbyte*)string, length, (jbyte*)new_string, new_length);
            (void)outStream_writeInt(stream, new_length);
            error = writeBytes(stream, (jbyte *)new_string, new_length);
            jvmtiDeallocate(new_string);
        }
    }
    return error;
}

jdwpError
outStream_writeValue(JNIEnv *env, PacketOutputStream *out,
                     jbyte typeKey, jvalue value)
{
    if (typeKey == JDWP_TAG(OBJECT)) {
        (void)outStream_writeByte(out, specificTypeKey(env, value.l));
    } else {
        (void)outStream_writeByte(out, typeKey);
    }
    if (isObjectTag(typeKey)) {
        (void)outStream_writeObjectRef(env, out, value.l);
    } else {
        switch (typeKey) {
            case JDWP_TAG(BYTE):
                return outStream_writeByte(out, value.b);

            case JDWP_TAG(CHAR):
                return outStream_writeChar(out, value.c);

            case JDWP_TAG(FLOAT):
                return outStream_writeFloat(out, value.f);

            case JDWP_TAG(DOUBLE):
                return outStream_writeDouble(out, value.d);

            case JDWP_TAG(INT):
                return outStream_writeInt(out, value.i);

            case JDWP_TAG(LONG):
                return outStream_writeLong(out, value.j);

            case JDWP_TAG(SHORT):
                return outStream_writeShort(out, value.s);

            case JDWP_TAG(BOOLEAN):
                return outStream_writeBoolean(out, value.z);

            case JDWP_TAG(VOID):  /* happens with function return values */
                /* write nothing */
                return JDWP_ERROR(NONE);

            default:
                EXIT_ERROR(AGENT_ERROR_INVALID_OBJECT,"Invalid type key");
                break;
        }
    }
    return JDWP_ERROR(NONE);
}

jdwpError
outStream_skipBytes(PacketOutputStream *stream, jint count)
{
    int i;
    for (i = 0; i < count; i++) {
        (void)outStream_writeByte(stream, 0);
    }
    return stream->error;
}

jdwpError
outStream_error(PacketOutputStream *stream)
{
    return stream->error;
}

void
outStream_setError(PacketOutputStream *stream, jdwpError error)
{
    if (stream->error == JDWP_ERROR(NONE)) {
        stream->error = error;
        LOG_MISC(("outStream_setError error=%s(%d)", jdwpErrorText(error), error));
    }
}

static jint
outStream_send(PacketOutputStream *stream) {

    jint rc;
    jint len = 0;
    PacketData *segment;
    jbyte *data, *posP;

    /*
     * If there's only 1 segment then we just send the
     * packet.
     */
    if (stream->firstSegment.next == NULL) {
        stream->packet.type.cmd.len = JDWP_HEADER_SIZE + stream->firstSegment.length;
        stream->packet.type.cmd.data = stream->firstSegment.data;
        rc = transport_sendPacket(&stream->packet);
        return rc;
    }

    /*
     * Multiple segments
     */
    len = 0;
    segment = (PacketData *)&(stream->firstSegment);
    do {
        len += segment->length;
        segment = segment->next;
    } while (segment != NULL);

    data = jvmtiAllocate(len);
    if (data == NULL) {
        return JDWP_ERROR(OUT_OF_MEMORY);
    }

    posP = data;
    segment = (PacketData *)&(stream->firstSegment);
    while (segment != NULL) {
        (void)memcpy(posP, segment->data, segment->length);
        posP += segment->length;
        segment = segment->next;
    }

    stream->packet.type.cmd.len = JDWP_HEADER_SIZE + len;
    stream->packet.type.cmd.data = data;
    rc = transport_sendPacket(&stream->packet);
    stream->packet.type.cmd.data = NULL;
    jvmtiDeallocate(data);

    return rc;
}

void
outStream_sendReply(PacketOutputStream *stream)
{
    jint rc;
    if (stream->error) {
        /*
         * Don't send any collected stream data on an error reply
         */
        stream->packet.type.reply.len = 0;
        stream->packet.type.reply.errorCode = (jshort)stream->error;
    }
    rc = outStream_send(stream);
    if (rc == 0) {
        stream->sent = JNI_TRUE;
    }
}

void
outStream_sendCommand(PacketOutputStream *stream)
{
    jint rc;
    if (!stream->error) {
        rc = outStream_send(stream);
        if (rc == 0) {
            stream->sent = JNI_TRUE;
        }
    }
}


static jboolean
releaseID(void *elementPtr, void *arg)
{
    jlong *idPtr = elementPtr;
    commonRef_release(getEnv(), *idPtr);
    return JNI_TRUE;
}

void
outStream_destroy(PacketOutputStream *stream)
{
    struct PacketData *next;

    if (stream->error || !stream->sent) {
        (void)bagEnumerateOver(stream->ids, releaseID, NULL);
    }

    next = stream->firstSegment.next;
    while (next != NULL) {
        struct PacketData *p = next;
        next = p->next;
        jvmtiDeallocate(p->data);
        jvmtiDeallocate(p);
    }
    bagDestroyBag(stream->ids);
}
