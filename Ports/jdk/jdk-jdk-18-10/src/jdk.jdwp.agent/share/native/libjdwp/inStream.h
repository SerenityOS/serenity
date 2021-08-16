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

#ifndef JDWP_INSTREAM_H
#define JDWP_INSTREAM_H

#include "transport.h"
#include "FrameID.h"

struct bag;

typedef struct PacketInputStream {
    jbyte *current;
    jint left;
    jdwpError error;
    jdwpPacket packet;
    struct bag *refs;
} PacketInputStream;

void inStream_init(PacketInputStream *stream, jdwpPacket packet);

jint inStream_id(PacketInputStream *stream);
jbyte inStream_command(PacketInputStream *stream);

jboolean inStream_readBoolean(PacketInputStream *stream);
jbyte inStream_readByte(PacketInputStream *stream);
jbyte* inStream_readBytes(PacketInputStream *stream,
                          int length, jbyte *buf);
jchar inStream_readChar(PacketInputStream *stream);
jshort inStream_readShort(PacketInputStream *stream);
jint inStream_readInt(PacketInputStream *stream);
jlong inStream_readLong(PacketInputStream *stream);
jfloat inStream_readFloat(PacketInputStream *stream);
jdouble inStream_readDouble(PacketInputStream *stream);
jlong inStream_readObjectID(PacketInputStream *stream);
FrameID inStream_readFrameID(PacketInputStream *stream);
jmethodID inStream_readMethodID(PacketInputStream *stream);
jfieldID inStream_readFieldID(PacketInputStream *stream);
jlocation inStream_readLocation(PacketInputStream *stream);

jobject inStream_readModuleRef(JNIEnv *env, PacketInputStream *stream);
jobject inStream_readObjectRef(JNIEnv *env, PacketInputStream *stream);
jclass inStream_readClassRef(JNIEnv *env, PacketInputStream *stream);
jthread inStream_readThreadRef(JNIEnv *env, PacketInputStream *stream);
jthreadGroup inStream_readThreadGroupRef(JNIEnv *env, PacketInputStream *stream);
jobject inStream_readClassLoaderRef(JNIEnv *env, PacketInputStream *stream);
jstring inStream_readStringRef(JNIEnv *env, PacketInputStream *stream);
jarray inStream_readArrayRef(JNIEnv *env, PacketInputStream *stream);

char *inStream_readString(PacketInputStream *stream);
jvalue inStream_readValue(struct PacketInputStream *in);

jdwpError inStream_skipBytes(PacketInputStream *stream, jint count);

jdwpError inStream_error(PacketInputStream *stream);
void inStream_clearError(PacketInputStream *stream);
void inStream_destroy(PacketInputStream *stream);

#endif /* _INSTREAM_H */
