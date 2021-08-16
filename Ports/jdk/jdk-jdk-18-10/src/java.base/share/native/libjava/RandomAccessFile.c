/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "jlong.h"
#include "jvm.h"

#include "io_util.h"
#include "io_util_md.h"
#include "java_io_RandomAccessFile.h"

#include <fcntl.h>

/*
 * static method to store field ID's in initializers
 */

jfieldID raf_fd; /* id for jobject 'fd' in java.io.RandomAccessFile */

JNIEXPORT void JNICALL
Java_java_io_RandomAccessFile_initIDs(JNIEnv *env, jclass fdClass) {
    raf_fd = (*env)->GetFieldID(env, fdClass, "fd", "Ljava/io/FileDescriptor;");
}


JNIEXPORT void JNICALL
Java_java_io_RandomAccessFile_open0(JNIEnv *env,
                                    jobject this, jstring path, jint mode)
{
    int flags = 0;
    if (mode & java_io_RandomAccessFile_O_RDONLY)
        flags = O_RDONLY;
    else if (mode & java_io_RandomAccessFile_O_RDWR) {
        flags = O_RDWR | O_CREAT;
        if (mode & java_io_RandomAccessFile_O_SYNC)
            flags |= O_SYNC;
        else if (mode & java_io_RandomAccessFile_O_DSYNC)
            flags |= O_DSYNC;
    }
#ifdef WIN32
    if (mode & java_io_RandomAccessFile_O_TEMPORARY)
        flags |= O_TEMPORARY;
#endif
    fileOpen(env, this, path, raf_fd, flags);
}

JNIEXPORT jint JNICALL
Java_java_io_RandomAccessFile_read0(JNIEnv *env, jobject this) {
    return readSingle(env, this, raf_fd);
}

JNIEXPORT jint JNICALL
Java_java_io_RandomAccessFile_readBytes(JNIEnv *env,
    jobject this, jbyteArray bytes, jint off, jint len) {
    return readBytes(env, this, bytes, off, len, raf_fd);
}

JNIEXPORT void JNICALL
Java_java_io_RandomAccessFile_write0(JNIEnv *env, jobject this, jint byte) {
    writeSingle(env, this, byte, JNI_FALSE, raf_fd);
}

JNIEXPORT void JNICALL
Java_java_io_RandomAccessFile_writeBytes(JNIEnv *env,
    jobject this, jbyteArray bytes, jint off, jint len) {
    writeBytes(env, this, bytes, off, len, JNI_FALSE, raf_fd);
}

JNIEXPORT jlong JNICALL
Java_java_io_RandomAccessFile_getFilePointer(JNIEnv *env, jobject this) {
    FD fd;
    jlong ret;

    fd = getFD(env, this, raf_fd);
    if (fd == -1) {
        JNU_ThrowIOException(env, "Stream Closed");
        return -1;
    }
    if ((ret = IO_Lseek(fd, 0L, SEEK_CUR)) == -1) {
        JNU_ThrowIOExceptionWithLastError(env, "Seek failed");
    }
    return ret;
}

JNIEXPORT jlong JNICALL
Java_java_io_RandomAccessFile_length(JNIEnv *env, jobject this) {

    FD fd;
    jlong length = jlong_zero;

    fd = getFD(env, this, raf_fd);
    if (fd == -1) {
        JNU_ThrowIOException(env, "Stream Closed");
        return -1;
    }
    if ((length = IO_GetLength(fd)) == -1) {
        JNU_ThrowIOExceptionWithLastError(env, "GetLength failed");
    }
    return length;
}

JNIEXPORT void JNICALL
Java_java_io_RandomAccessFile_seek0(JNIEnv *env,
                    jobject this, jlong pos) {

    FD fd;

    fd = getFD(env, this, raf_fd);
    if (fd == -1) {
        JNU_ThrowIOException(env, "Stream Closed");
        return;
    }
    if (pos < jlong_zero) {
        JNU_ThrowIOException(env, "Negative seek offset");
    } else if (IO_Lseek(fd, pos, SEEK_SET) == -1) {
        JNU_ThrowIOExceptionWithLastError(env, "Seek failed");
    }
}

JNIEXPORT void JNICALL
Java_java_io_RandomAccessFile_setLength(JNIEnv *env, jobject this,
                                        jlong newLength)
{
    FD fd;
    jlong cur;

    fd = getFD(env, this, raf_fd);
    if (fd == -1) {
        JNU_ThrowIOException(env, "Stream Closed");
        return;
    }
    if ((cur = IO_Lseek(fd, 0L, SEEK_CUR)) == -1) goto fail;
    if (IO_SetLength(fd, newLength) == -1) goto fail;
    if (cur > newLength) {
        if (IO_Lseek(fd, 0L, SEEK_END) == -1) goto fail;
    } else {
        if (IO_Lseek(fd, cur, SEEK_SET) == -1) goto fail;
    }
    return;

 fail:
    JNU_ThrowIOExceptionWithLastError(env, "setLength failed");
}
