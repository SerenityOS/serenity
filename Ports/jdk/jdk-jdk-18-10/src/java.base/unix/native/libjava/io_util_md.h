/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "jni_util.h"

/*
 * Macros to use the right data type for file descriptors
 */
#define FD jint

/*
 * Prototypes for functions in io_util_md.c called from io_util.c,
 * FileDescriptor.c, FileInputStream.c, FileOutputStream.c,
 * UnixFileSystem_md.c
 */
ssize_t handleWrite(FD fd, const void *buf, jint len);
ssize_t handleRead(FD fd, void *buf, jint len);
jint handleAvailable(FD fd, jlong *pbytes);
jint handleSetLength(FD fd, jlong length);
jlong handleGetLength(FD fd);
FD handleOpen(const char *path, int oflag, int mode);

/*
 * Functions to get fd from the java.io.FileDescriptor field
 * of an object.  These functions rely on having an appropriately
 * defined object with a FileDescriptor object at the fid offset.
 * If the FD object is null, return -1 to avoid crashing VM.
 */
FD getFD(JNIEnv *env, jobject cur, jfieldID fid);

/*
 * Macros to set/get fd when inside java.io.FileDescriptor
 */
#define THIS_FD(obj) (*env)->GetIntField(env, obj, IO_fd_fdID)

/*
 * Route the routines
 */
#define IO_Sync fsync
#define IO_Read handleRead
#define IO_Write handleWrite
#define IO_Append handleWrite
#define IO_Available handleAvailable
#define IO_SetLength handleSetLength
#define IO_GetLength handleGetLength

#ifdef _ALLBSD_SOURCE
#define open64 open
#define fstat64 fstat
#define stat64 stat
#define lseek64 lseek
#define ftruncate64 ftruncate
#define IO_Lseek lseek
#else
#define IO_Lseek lseek64
#endif

/*
 * On Solaris, the handle field is unused
 */
#define SET_HANDLE(fd) return (jlong)-1

/*
 * Retry the operation if it is interrupted
 */
#define RESTARTABLE(_cmd, _result)                \
    do {                                          \
        _result = _cmd;                           \
    } while ((_result == -1) && (errno == EINTR))

void fileDescriptorClose(JNIEnv *env, jobject this);

#ifdef MACOSX
jstring newStringPlatform(JNIEnv *env, const char* str);
#endif
