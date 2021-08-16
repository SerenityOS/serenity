/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "io_util.h"
#include "io_util_md.h"
#include <string.h>
#include <unistd.h>

#if defined(__linux__) || defined(_ALLBSD_SOURCE) || defined(_AIX)
#include <sys/ioctl.h>
#endif

#if defined(__linux__)
#include <linux/fs.h>
#include <sys/stat.h>
#endif

#ifdef MACOSX

#include <CoreFoundation/CoreFoundation.h>

__private_extern__
jstring newStringPlatform(JNIEnv *env, const char* str)
{
    jstring rv = NULL;
    CFMutableStringRef csref = CFStringCreateMutable(NULL, 0);
    if (csref == NULL) {
        JNU_ThrowOutOfMemoryError(env, "native heap");
    } else {
        CFStringAppendCString(csref, str, kCFStringEncodingUTF8);
        CFStringNormalize(csref, kCFStringNormalizationFormC);
        int clen = CFStringGetLength(csref);
        int ulen = (clen + 1) * 2;        // utf16 + zero padding
        char* chars = malloc(ulen);
        if (chars == NULL) {
            CFRelease(csref);
            JNU_ThrowOutOfMemoryError(env, "native heap");
        } else {
            if (CFStringGetCString(csref, chars, ulen, kCFStringEncodingUTF16)) {
                rv = (*env)->NewString(env, (jchar*)chars, clen);
            }
            free(chars);
            CFRelease(csref);
        }
    }
    return rv;
}
#endif

FD
handleOpen(const char *path, int oflag, int mode) {
    FD fd;
    RESTARTABLE(open64(path, oflag, mode), fd);
    if (fd != -1) {
        struct stat64 buf64;
        int result;
        RESTARTABLE(fstat64(fd, &buf64), result);
        if (result != -1) {
            if (S_ISDIR(buf64.st_mode)) {
                close(fd);
                errno = EISDIR;
                fd = -1;
            }
        } else {
            close(fd);
            fd = -1;
        }
    }
    return fd;
}

FD getFD(JNIEnv *env, jobject obj, jfieldID fid) {
  jobject fdo = (*env)->GetObjectField(env, obj, fid);
  if (fdo == NULL) {
    return -1;
  }
  return (*env)->GetIntField(env, fdo, IO_fd_fdID);
}

void
fileOpen(JNIEnv *env, jobject this, jstring path, jfieldID fid, int flags)
{
    WITH_PLATFORM_STRING(env, path, ps) {
        FD fd;

#if defined(__linux__) || defined(_ALLBSD_SOURCE)
        /* Remove trailing slashes, since the kernel won't */
        char *p = (char *)ps + strlen(ps) - 1;
        while ((p > ps) && (*p == '/'))
            *p-- = '\0';
#endif
        fd = handleOpen(ps, flags, 0666);
        if (fd != -1) {
            jobject fdobj;
            jboolean append;
            fdobj = (*env)->GetObjectField(env, this, fid);
            if (fdobj != NULL) {
                // Set FD
                (*env)->SetIntField(env, fdobj, IO_fd_fdID, fd);
                append = (flags & O_APPEND) == 0 ? JNI_FALSE : JNI_TRUE;
                (*env)->SetBooleanField(env, fdobj, IO_append_fdID, append);
            }
        } else {
            throwFileNotFoundException(env, path);
        }
    } END_PLATFORM_STRING(env, ps);
}

// Function to close the fd held by this FileDescriptor and set fd to -1.
void
fileDescriptorClose(JNIEnv *env, jobject this)
{
    FD fd = (*env)->GetIntField(env, this, IO_fd_fdID);
    if ((*env)->ExceptionOccurred(env)) {
        return;
    }

    if (fd == -1) {
        return;     // already closed and set to -1
    }

    /* Set the fd to -1 before closing it so that the timing window
     * of other threads using the wrong fd (closed but recycled fd,
     * that gets re-opened with some other filename) is reduced.
     * Practically the chance of its occurance is low, however, we are
     * taking extra precaution over here.
     */
    (*env)->SetIntField(env, this, IO_fd_fdID, -1);
    if ((*env)->ExceptionOccurred(env)) {
        return;
    }
    /*
     * Don't close file descriptors 0, 1, or 2. If we close these stream
     * then a subsequent file open or socket will use them. Instead we
     * just redirect these file descriptors to /dev/null.
     */
    if (fd >= STDIN_FILENO && fd <= STDERR_FILENO) {
        int devnull = open("/dev/null", O_WRONLY);
        if (devnull < 0) {
            (*env)->SetIntField(env, this, IO_fd_fdID, fd);
            JNU_ThrowIOExceptionWithLastError(env, "open /dev/null failed");
        } else {
            dup2(devnull, fd);
            close(devnull);
        }
    } else {
        int result;
#if defined(_AIX)
        /* AIX allows close to be restarted after EINTR */
        RESTARTABLE(close(fd), result);
#else
        result = close(fd);
#endif
        if (result == -1 && errno != EINTR) {
            JNU_ThrowIOExceptionWithLastError(env, "close failed");
        }
    }
}

ssize_t
handleRead(FD fd, void *buf, jint len)
{
    ssize_t result;
    RESTARTABLE(read(fd, buf, len), result);
    return result;
}

ssize_t
handleWrite(FD fd, const void *buf, jint len)
{
    ssize_t result;
    RESTARTABLE(write(fd, buf, len), result);
    return result;
}

jint
handleAvailable(FD fd, jlong *pbytes)
{
    int mode;
    struct stat64 buf64;
    jlong size = -1, current = -1;

    int result;
    RESTARTABLE(fstat64(fd, &buf64), result);
    if (result != -1) {
        mode = buf64.st_mode;
        if (S_ISCHR(mode) || S_ISFIFO(mode) || S_ISSOCK(mode)) {
            int n;
            int result;
            RESTARTABLE(ioctl(fd, FIONREAD, &n), result);
            if (result >= 0) {
                *pbytes = n;
                return 1;
            }
        } else if (S_ISREG(mode)) {
            size = buf64.st_size;
        }
    }

    if ((current = lseek64(fd, 0, SEEK_CUR)) == -1) {
        return 0;
    }

    if (size < current) {
        if ((size = lseek64(fd, 0, SEEK_END)) == -1)
            return 0;
        else if (lseek64(fd, current, SEEK_SET) == -1)
            return 0;
    }

    *pbytes = size - current;
    return 1;
}

jint
handleSetLength(FD fd, jlong length)
{
    int result;
    RESTARTABLE(ftruncate64(fd, length), result);
    return result;
}

jlong
handleGetLength(FD fd)
{
    struct stat64 sb;
    int result;
    RESTARTABLE(fstat64(fd, &sb), result);
    if (result < 0) {
        return -1;
    }
#if defined(__linux__) && defined(BLKGETSIZE64)
    if (S_ISBLK(sb.st_mode)) {
        uint64_t size;
        if(ioctl(fd, BLKGETSIZE64, &size) < 0) {
            return -1;
        }
        return (jlong)size;
    }
#endif
    return sb.st_size;
}
