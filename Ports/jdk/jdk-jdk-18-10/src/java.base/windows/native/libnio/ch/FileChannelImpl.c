/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "jlong.h"
#include <io.h>
#include "nio.h"
#include "nio_util.h"
#include "sun_nio_ch_FileChannelImpl.h"
#include "java_lang_Integer.h"

#include <Mswsock.h>
#pragma comment(lib, "Mswsock.lib")

static jfieldID chan_fd; /* id for jobject 'fd' in java.io.FileChannel */

/**************************************************************
 * static method to store field ID's in initializers
 * and retrieve the allocation granularity
 */
JNIEXPORT jlong JNICALL
Java_sun_nio_ch_FileChannelImpl_initIDs(JNIEnv *env, jclass clazz)
{
    SYSTEM_INFO si;
    jint align;
    GetSystemInfo(&si);
    align = si.dwAllocationGranularity;
    chan_fd = (*env)->GetFieldID(env, clazz, "fd", "Ljava/io/FileDescriptor;");
    return align;
}


/**************************************************************
 * Channel
 */

JNIEXPORT jlong JNICALL
Java_sun_nio_ch_FileChannelImpl_map0(JNIEnv *env, jobject this,
                                     jint prot, jlong off, jlong len, jboolean map_sync)
{
    void *mapAddress = 0;
    jint lowOffset = (jint)off;
    jint highOffset = (jint)(off >> 32);
    jlong maxSize = off + len;
    jint lowLen = (jint)(maxSize);
    jint highLen = (jint)(maxSize >> 32);
    jobject fdo = (*env)->GetObjectField(env, this, chan_fd);
    HANDLE fileHandle = (HANDLE)(handleval(env, fdo));
    HANDLE mapping;
    DWORD mapAccess = FILE_MAP_READ;
    DWORD fileProtect = PAGE_READONLY;
    DWORD mapError;
    BOOL result;

    if (prot == sun_nio_ch_FileChannelImpl_MAP_RO) {
        fileProtect = PAGE_READONLY;
        mapAccess = FILE_MAP_READ;
    } else if (prot == sun_nio_ch_FileChannelImpl_MAP_RW) {
        fileProtect = PAGE_READWRITE;
        mapAccess = FILE_MAP_WRITE;
    } else if (prot == sun_nio_ch_FileChannelImpl_MAP_PV) {
        fileProtect = PAGE_WRITECOPY;
        mapAccess = FILE_MAP_COPY;
    }

    if (map_sync) {
        JNU_ThrowInternalError(env, "should never call map on platform where MAP_SYNC is unimplemented");
        return IOS_THROWN;
    }

    mapping = CreateFileMapping(
        fileHandle,      /* Handle of file */
        NULL,            /* Not inheritable */
        fileProtect,     /* Read and write */
        highLen,         /* High word of max size */
        lowLen,          /* Low word of max size */
        NULL);           /* No name for object */

    if (mapping == NULL) {
        JNU_ThrowIOExceptionWithLastError(env, "Map failed");
        return IOS_THROWN;
    }

    mapAddress = MapViewOfFile(
        mapping,             /* Handle of file mapping object */
        mapAccess,           /* Read and write access */
        highOffset,          /* High word of offset */
        lowOffset,           /* Low word of offset */
        (DWORD)len);         /* Number of bytes to map */
    mapError = GetLastError();

    result = CloseHandle(mapping);
    if (result == 0) {
        JNU_ThrowIOExceptionWithLastError(env, "Map failed");
        return IOS_THROWN;
    }

    if (mapAddress == NULL) {
        if (mapError == ERROR_NOT_ENOUGH_MEMORY)
            JNU_ThrowOutOfMemoryError(env, "Map failed");
        else
            JNU_ThrowIOExceptionWithLastError(env, "Map failed");
        return IOS_THROWN;
    }

    return ptr_to_jlong(mapAddress);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileChannelImpl_unmap0(JNIEnv *env, jobject this,
                                 jlong address, jlong len)
{
    BOOL result;
    void *a = (void *) jlong_to_ptr(address);

    result = UnmapViewOfFile(a);
    if (result == 0) {
        JNU_ThrowIOExceptionWithLastError(env, "Unmap failed");
        return IOS_THROWN;
    }
    return 0;
}

// Integer.MAX_VALUE - 1 is the maximum transfer size for TransmitFile()
#define MAX_TRANSMIT_SIZE (java_lang_Integer_MAX_VALUE - 1)

JNIEXPORT jlong JNICALL
Java_sun_nio_ch_FileChannelImpl_transferTo0(JNIEnv *env, jobject this,
                                            jobject srcFD,
                                            jlong position, jlong count,
                                            jobject dstFD)
{
    const int PACKET_SIZE = 524288;

    LARGE_INTEGER where;
    HANDLE src = (HANDLE)(handleval(env, srcFD));
    SOCKET dst = (SOCKET)(fdval(env, dstFD));
    DWORD chunkSize = (count > MAX_TRANSMIT_SIZE) ?
        MAX_TRANSMIT_SIZE : (DWORD)count;
    BOOL result;

    where.QuadPart = position;
    result = SetFilePointerEx(src, where, &where, FILE_BEGIN);
    if (result == 0) {
        JNU_ThrowIOExceptionWithLastError(env, "SetFilePointerEx failed");
        return IOS_THROWN;
    }

    result = TransmitFile(
        dst,
        src,
        chunkSize,
        PACKET_SIZE,
        NULL,
        NULL,
        TF_USE_KERNEL_APC
    );
    if (!result) {
        int error = WSAGetLastError();
        if (WSAEINVAL == error && count >= 0) {
            return IOS_UNSUPPORTED_CASE;
        }
        if (WSAENOTSOCK == error) {
            return IOS_UNSUPPORTED_CASE;
        }
        JNU_ThrowIOExceptionWithLastError(env, "transfer failed");
        return IOS_THROWN;
    }
    return chunkSize;
}


JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileChannelImpl_maxDirectTransferSize0(JNIEnv* env, jobject this)
{
    return MAX_TRANSMIT_SIZE;
}
