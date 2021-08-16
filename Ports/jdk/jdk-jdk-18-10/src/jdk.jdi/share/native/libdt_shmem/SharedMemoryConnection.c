/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include <string.h>
#include <stdlib.h>
#include <stddef.h>
#include <jni.h>
#include "SharedMemory.h"
#include "com_sun_tools_jdi_SharedMemoryConnection.h"
#include "jdwpTransport.h"
#include "shmemBase.h"
#include "sys.h"

/*
 * JNI interface to the shared memory transport. These JNI methods
 * call the base shared memory support to do the real work.
 *
 * That is, this is the front-ends interface to our shared memory
 * communication code.
 */

/*
 * Cached architecture
 */
static int byte_ordering_known;
static int is_big_endian;


/*
 * Returns 1 if big endian architecture
 */
static int isBigEndian() {
    if (!byte_ordering_known) {
        unsigned int i = 0xff000000;
        if (((char *)(&i))[0] != 0) {
            is_big_endian = 1;
        } else {
            is_big_endian = 0;
        }
        byte_ordering_known = 1;
    }
    return is_big_endian;
}

/*
 * Convert to big endian
 */
static jint intToBigInt(jint i) {
    unsigned int b[4];
    if (isBigEndian()) {
        return i;
    }
    b[0] = (i >> 24) & 0xff;
    b[1] = (i >> 16) & 0xff;
    b[2] = (i >> 8) & 0xff;
    b[3] = i & 0xff;

    /*
     * It doesn't matter that jint is signed as we are or'ing
     * and hence end up with the correct bits.
     */
    return (b[3] << 24) | (b[2] << 16) | (b[1] << 8) | b[0];
}

/*
 * Convert unsigned short to big endian
 */
static unsigned short shortToBigShort(unsigned short s) {
    unsigned int b[2];
    if (isBigEndian()) {
        return s;
    }
    b[0] = (s >> 8) & 0xff;
    b[1] = s & 0xff;
    return (b[1] << 8) + b[0];
}

/*
 * Create a byte[] from a packet struct. All data in the byte array
 * is JDWP packet suitable for wire transmission. That is, all fields,
 * and data are in big-endian format as required by the JDWP
 * specification.
 */
static jbyteArray
packetToByteArray(JNIEnv *env, jdwpPacket *str)
{
    jbyteArray array;
    jsize data_length;
    jint total_length;
    jint tmpInt;

    total_length = str->type.cmd.len;
    data_length = total_length - JDWP_HEADER_SIZE;

    /* total packet length is header + data */
    array = (*env)->NewByteArray(env, total_length);
    if ((*env)->ExceptionOccurred(env)) {
        return NULL;
    }

    /* First 4 bytes of packet are the length (in big endian format) */
    tmpInt = intToBigInt((unsigned int)total_length);
    (*env)->SetByteArrayRegion(env, array, 0, 4, (const jbyte *)&tmpInt);

    /* Next 4 bytes are the id field */
    tmpInt = intToBigInt(str->type.cmd.id);
    (*env)->SetByteArrayRegion(env, array, 4, 4, (const jbyte *)&tmpInt);

    /* next byte is the flags */
    (*env)->SetByteArrayRegion(env, array, 8, 1, (const jbyte *)&(str->type.cmd.flags));

    /* next two bytes are either the error code or the command set/command */
    if (str->type.cmd.flags & JDWPTRANSPORT_FLAGS_REPLY) {
        short s = shortToBigShort(str->type.reply.errorCode);
        (*env)->SetByteArrayRegion(env, array, 9, 2, (const jbyte *)&(s));
    } else {
        (*env)->SetByteArrayRegion(env, array, 9, 1, (const jbyte *)&(str->type.cmd.cmdSet));
        (*env)->SetByteArrayRegion(env, array, 10, 1, (const jbyte *)&(str->type.cmd.cmd));
    }

    /* finally the data */

    if (data_length > 0) {
        (*env)->SetByteArrayRegion(env, array, JDWP_HEADER_SIZE,
                                   data_length, str->type.cmd.data);
        if ((*env)->ExceptionOccurred(env)) {
            return NULL;
        }
    }

    return array;
}

/*
 * Fill a packet struct from a byte array. The byte array is a
 * JDWP packet suitable for wire transmission. That is, all fields,
 * and data are in big-endian format as required by the JDWP
 * specification. We thus need to convert the fields from big
 * endian to the platform endian.
 *
 * The jbyteArray provided to this function is assumed to
 * of a length than is equal or greater than the length of
 * the JDWP packet that is contains.
 */
static void
byteArrayToPacket(JNIEnv *env, jbyteArray b, jdwpPacket *str)
{
    jsize total_length, data_length;
    jbyte *data;
    unsigned char pktHeader[JDWP_HEADER_SIZE];

    /*
     * Get the packet header
     */
    (*env)->GetByteArrayRegion(env, b, 0, sizeof(pktHeader), pktHeader);
    if ((*env)->ExceptionOccurred(env)) {
        /* b shorter than sizeof(pktHeader) */
        return;
    }

    total_length = (int)pktHeader[3] | ((int)pktHeader[2] << 8) |
                   ((int)pktHeader[1] << 16) | ((int)pktHeader[0] << 24);

    if (total_length < sizeof(pktHeader)) {
        throwException(env, "java/lang/IllegalArgumentException",
                            "JDWP header is incorrect");
        return;
    }

    /*
     * The id field is in big endian (also errorCode field in the case
     * of reply packets).
     */
    str->type.cmd.id = (int)pktHeader[7] | ((int)pktHeader[6] << 8) |
                       ((int)pktHeader[5] << 16) | ((int)pktHeader[4] << 24);

    str->type.cmd.flags = (jbyte)pktHeader[8];

    if (str->type.cmd.flags & JDWPTRANSPORT_FLAGS_REPLY) {
        str->type.reply.errorCode = (int)pktHeader[9] + ((int)pktHeader[10] << 8);
    } else {
        /* command packet */
        str->type.cmd.cmdSet = (jbyte)pktHeader[9];
        str->type.cmd.cmd = (jbyte)pktHeader[10];
    }

    /*
     * The length of the JDWP packet is sizeof(pktHeader) + data
     */
    data_length = total_length - sizeof(pktHeader);

    if (data_length == 0) {
        data = NULL;
    } else {
        data = malloc(data_length);
        if (data == NULL) {
            throwException(env, "java/lang/OutOfMemoryError",
                           "Unable to allocate command data buffer");
            return;
        }

        (*env)->GetByteArrayRegion(env, b, sizeof(pktHeader), /*sizeof(CmdPacket)+4*/ data_length, data);
        if ((*env)->ExceptionOccurred(env)) {
            free(data);
            return;
        }
    }

    str->type.cmd.len = total_length;
    str->type.cmd.data = data;
}

static void
freePacketData(jdwpPacket *packet)
{
    if (packet->type.cmd.len > 0) {
        free(packet->type.cmd.data);
    }
}

/*
 * Class:     com_sun_tools_jdi_SharedMemoryConnection
 * Method:    close0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_tools_jdi_SharedMemoryConnection_close0
  (JNIEnv *env, jobject thisObject, jlong id)
{
    SharedMemoryConnection *connection = ID_TO_CONNECTION(id);
    shmemBase_closeConnection(connection);
}

/*
 * Class:     com_sun_tools_jdi_SharedMemoryConnection
 * Method:    receiveByte0
 * Signature: (J)B
 */
JNIEXPORT jbyte JNICALL Java_com_sun_tools_jdi_SharedMemoryConnection_receiveByte0
  (JNIEnv *env, jobject thisObject, jlong id)
{
    SharedMemoryConnection *connection = ID_TO_CONNECTION(id);
    jbyte b = 0;
    jint rc;

    rc = shmemBase_receiveByte(connection, &b);
    if (rc != SYS_OK) {
        throwShmemException(env, "shmemBase_receiveByte failed", rc);
    }

    return b;
}

/*
 * Class:     com_sun_tools_jdi_SharedMemoryConnection
 * Method:    receivePacket0
 * Signature: (JLcom/sun/tools/jdi/Packet;)V
 */
JNIEXPORT jbyteArray JNICALL Java_com_sun_tools_jdi_SharedMemoryConnection_receivePacket0
  (JNIEnv *env, jobject thisObject, jlong id)
{
    SharedMemoryConnection *connection = ID_TO_CONNECTION(id);
    jdwpPacket packet;
    jint rc;

    rc = shmemBase_receivePacket(connection, &packet);
    if (rc != SYS_OK) {
        throwShmemException(env, "shmemBase_receivePacket failed", rc);
        return NULL;
    } else {
        jbyteArray array = packetToByteArray(env, &packet);

        /* Free the packet even if there was an exception above */
        freePacketData(&packet);
        return array;
    }
}

/*
 * Class:     com_sun_tools_jdi_SharedMemoryConnection
 * Method:    sendByte0
 * Signature: (JB)V
 */
JNIEXPORT void JNICALL Java_com_sun_tools_jdi_SharedMemoryConnection_sendByte0
  (JNIEnv *env, jobject thisObject, jlong id, jbyte b)
{
    SharedMemoryConnection *connection = ID_TO_CONNECTION(id);
    jint rc;

    rc = shmemBase_sendByte(connection, b);
    if (rc != SYS_OK) {
        throwShmemException(env, "shmemBase_sendByte failed", rc);
    }
}

/*
 * Class:     com_sun_tools_jdi_SharedMemoryConnection
 * Method:    sendPacket0
 * Signature: (JLcom/sun/tools/jdi/Packet;)V
 */
JNIEXPORT void JNICALL Java_com_sun_tools_jdi_SharedMemoryConnection_sendPacket0
  (JNIEnv *env, jobject thisObject, jlong id, jbyteArray b)
{
    SharedMemoryConnection *connection = ID_TO_CONNECTION(id);
    jdwpPacket packet;
    jint rc;

    byteArrayToPacket(env, b, &packet);
    if ((*env)->ExceptionOccurred(env)) {
        return;
    }

    rc = shmemBase_sendPacket(connection, &packet);
    if (rc != SYS_OK) {
        throwShmemException(env, "shmemBase_sendPacket failed", rc);
    }
    freePacketData(&packet);
}
