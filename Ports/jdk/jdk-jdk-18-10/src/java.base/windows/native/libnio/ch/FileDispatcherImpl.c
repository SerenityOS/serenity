/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <windows.h>
#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "jlong.h"
#include "sun_nio_ch_FileDispatcherImpl.h"
#include <io.h>
#include "nio.h"
#include "nio_util.h"
#include "jlong.h"


/**************************************************************
 * FileDispatcherImpl.c
 */

JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileDispatcherImpl_read0(JNIEnv *env, jclass clazz, jobject fdo,
                                      jlong address, jint len)
{
    DWORD read = 0;
    BOOL result = 0;
    HANDLE h = (HANDLE)(handleval(env, fdo));

    if (h == INVALID_HANDLE_VALUE) {
        JNU_ThrowIOExceptionWithLastError(env, "Invalid handle");
        return IOS_THROWN;
    }
    result = ReadFile(h,          /* File handle to read */
                      (LPVOID)address,    /* address to put data */
                      len,        /* number of bytes to read */
                      &read,      /* number of bytes read */
                      NULL);      /* no overlapped struct */
    if (result == 0) {
        int error = GetLastError();
        if (error == ERROR_BROKEN_PIPE) {
            return IOS_EOF;
        }
        if (error == ERROR_NO_DATA) {
            return IOS_UNAVAILABLE;
        }
        JNU_ThrowIOExceptionWithLastError(env, "Read failed");
        return IOS_THROWN;
    }
    return convertReturnVal(env, (jint)read, JNI_TRUE);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_ch_FileDispatcherImpl_readv0(JNIEnv *env, jclass clazz, jobject fdo,
                                       jlong address, jint len)
{
    DWORD read = 0;
    BOOL result = 0;
    jlong totalRead = 0;
    LPVOID loc;
    int i = 0;
    DWORD num = 0;
    struct iovec *iovecp = (struct iovec *)jlong_to_ptr(address);
    HANDLE h = (HANDLE)(handleval(env, fdo));

    if (h == INVALID_HANDLE_VALUE) {
        JNU_ThrowIOExceptionWithLastError(env, "Invalid handle");
        return IOS_THROWN;
    }

    for(i=0; i<len; i++) {
        loc = (LPVOID)jlong_to_ptr(iovecp[i].iov_base);
        num = iovecp[i].iov_len;
        result = ReadFile(h,                /* File handle to read */
                          loc,              /* address to put data */
                          num,              /* number of bytes to read */
                          &read,            /* number of bytes read */
                          NULL);            /* no overlapped struct */
        if (read > 0) {
            totalRead += read;
        }
        if (read < num) {
            break;
        }
    }

    if (result == 0) {
        int error = GetLastError();
        if (error == ERROR_BROKEN_PIPE) {
            return IOS_EOF;
        }
        if (error == ERROR_NO_DATA) {
            return IOS_UNAVAILABLE;
        }
        JNU_ThrowIOExceptionWithLastError(env, "Read failed");
        return IOS_THROWN;
    }

    return convertLongReturnVal(env, totalRead, JNI_TRUE);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileDispatcherImpl_pread0(JNIEnv *env, jclass clazz, jobject fdo,
                            jlong address, jint len, jlong offset)
{
    DWORD read = 0;
    BOOL result = 0;
    HANDLE h = (HANDLE)(handleval(env, fdo));
    LARGE_INTEGER currPos;
    OVERLAPPED ov;

    if (h == INVALID_HANDLE_VALUE) {
        JNU_ThrowIOExceptionWithLastError(env, "Invalid handle");
        return IOS_THROWN;
    }

    currPos.QuadPart = 0;
    result = SetFilePointerEx(h, currPos, &currPos, FILE_CURRENT);
    if (result == 0) {
        JNU_ThrowIOExceptionWithLastError(env, "Seek failed");
        return IOS_THROWN;
    }

    ZeroMemory(&ov, sizeof(ov));
    ov.Offset = (DWORD)offset;
    ov.OffsetHigh = (DWORD)(offset >> 32);

    result = ReadFile(h,                /* File handle to read */
                      (LPVOID)address,  /* address to put data */
                      len,              /* number of bytes to read */
                      &read,            /* number of bytes read */
                      &ov);             /* position to read from */

    if (result == 0) {
        int error = GetLastError();
        if (error == ERROR_BROKEN_PIPE) {
            return IOS_EOF;
        }
        if (error == ERROR_NO_DATA) {
            return IOS_UNAVAILABLE;
        }
        if (error != ERROR_HANDLE_EOF) {
            JNU_ThrowIOExceptionWithLastError(env, "Read failed");
            return IOS_THROWN;
        }
    }

    result = SetFilePointerEx(h, currPos, NULL, FILE_BEGIN);
    if (result == 0) {
        JNU_ThrowIOExceptionWithLastError(env, "Seek failed");
        return IOS_THROWN;
    }

    return convertReturnVal(env, (jint)read, JNI_TRUE);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileDispatcherImpl_write0(JNIEnv *env, jclass clazz, jobject fdo,
                                          jlong address, jint len, jboolean append)
{
    BOOL result = 0;
    DWORD written = 0;
    HANDLE h = (HANDLE)(handleval(env, fdo));

    if (h != INVALID_HANDLE_VALUE) {
        OVERLAPPED ov;
        LPOVERLAPPED lpOv;
        if (append == JNI_TRUE) {
            ZeroMemory(&ov, sizeof(ov));
            ov.Offset = (DWORD)0xFFFFFFFF;
            ov.OffsetHigh = (DWORD)0xFFFFFFFF;
            lpOv = &ov;
        } else {
            lpOv = NULL;
        }
        result = WriteFile(h,                /* File handle to write */
                           (LPCVOID)address, /* pointer to the buffer */
                           len,              /* number of bytes to write */
                           &written,         /* receives number of bytes written */
                           lpOv);            /* overlapped struct */
    }

    if ((h == INVALID_HANDLE_VALUE) || (result == 0)) {
        JNU_ThrowIOExceptionWithLastError(env, "Write failed");
        return IOS_THROWN;
    }

    return convertReturnVal(env, (jint)written, JNI_FALSE);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_ch_FileDispatcherImpl_writev0(JNIEnv *env, jclass clazz, jobject fdo,
                                           jlong address, jint len, jboolean append)
{
    BOOL result = 0;
    DWORD written = 0;
    HANDLE h = (HANDLE)(handleval(env, fdo));
    jlong totalWritten = 0;

    if (h != INVALID_HANDLE_VALUE) {
        LPVOID loc;
        int i = 0;
        DWORD num = 0;
        struct iovec *iovecp = (struct iovec *)jlong_to_ptr(address);
        OVERLAPPED ov;
        LPOVERLAPPED lpOv;
        if (append == JNI_TRUE) {
            ZeroMemory(&ov, sizeof(ov));
            ov.Offset = (DWORD)0xFFFFFFFF;
            ov.OffsetHigh = (DWORD)0xFFFFFFFF;
            lpOv = &ov;
        } else {
            lpOv = NULL;
        }
        for(i=0; i<len; i++) {
            loc = (LPVOID)jlong_to_ptr(iovecp[i].iov_base);
            num = iovecp[i].iov_len;
            result = WriteFile(h,       /* File handle to write */
                               loc,     /* pointers to the buffers */
                               num,     /* number of bytes to write */
                               &written,/* receives number of bytes written */
                               lpOv);   /* overlapped struct */
            if (written > 0) {
                totalWritten += written;
            }
            if (written < num) {
                break;
            }
        }
    }

    if ((h == INVALID_HANDLE_VALUE) || (result == 0)) {
        JNU_ThrowIOExceptionWithLastError(env, "Write failed");
        return IOS_THROWN;
    }

    return convertLongReturnVal(env, totalWritten, JNI_FALSE);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileDispatcherImpl_pwrite0(JNIEnv *env, jclass clazz, jobject fdo,
                            jlong address, jint len, jlong offset)
{
    BOOL result = 0;
    DWORD written = 0;
    HANDLE h = (HANDLE)(handleval(env, fdo));
    LARGE_INTEGER currPos;
    OVERLAPPED ov;

    currPos.QuadPart = 0;
    result = SetFilePointerEx(h, currPos, &currPos, FILE_CURRENT);
    if (result == 0) {
        JNU_ThrowIOExceptionWithLastError(env, "Seek failed");
        return IOS_THROWN;
    }

    ZeroMemory(&ov, sizeof(ov));
    ov.Offset = (DWORD)offset;
    ov.OffsetHigh = (DWORD)(offset >> 32);

    result = WriteFile(h,                /* File handle to write */
                       (LPCVOID)address, /* pointer to the buffer */
                       len,              /* number of bytes to write */
                       &written,         /* receives number of bytes written */
                       &ov);             /* position to write at */

    if ((h == INVALID_HANDLE_VALUE) || (result == 0)) {
        JNU_ThrowIOExceptionWithLastError(env, "Write failed");
        return IOS_THROWN;
    }

    result = SetFilePointerEx(h, currPos, NULL, FILE_BEGIN);
    if (result == 0) {
        JNU_ThrowIOExceptionWithLastError(env, "Seek failed");
        return IOS_THROWN;
    }

    return convertReturnVal(env, (jint)written, JNI_FALSE);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_ch_FileDispatcherImpl_seek0(JNIEnv *env, jclass clazz,
                                         jobject fdo, jlong offset)
{
    BOOL result = 0;
    HANDLE h = (HANDLE)(handleval(env, fdo));
    LARGE_INTEGER where;
    DWORD whence;

    if (offset < 0) {
        where.QuadPart = 0;
        whence = FILE_CURRENT;
    } else {
        where.QuadPart = offset;
        whence = FILE_BEGIN;
    }

    result = SetFilePointerEx(h, where, &where, whence);
    if (result == 0) {
        JNU_ThrowIOExceptionWithLastError(env, "SetFilePointerEx failed");
        return IOS_THROWN;
    }
    return (jlong)where.QuadPart;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileDispatcherImpl_force0(JNIEnv *env, jobject this,
                                          jobject fdo, jboolean md)
{
    int result = 0;
    HANDLE h = (HANDLE)(handleval(env, fdo));

    if (h != INVALID_HANDLE_VALUE) {
        result = FlushFileBuffers(h);
        if (result == 0) {
            int error = GetLastError();
            if (error != ERROR_ACCESS_DENIED) {
                JNU_ThrowIOExceptionWithLastError(env, "Force failed");
                return IOS_THROWN;
            }
        }
    } else {
        JNU_ThrowIOExceptionWithLastError(env, "Force failed");
        return IOS_THROWN;
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileDispatcherImpl_truncate0(JNIEnv *env, jobject this,
                                             jobject fdo, jlong size)
{
    BOOL result = 0;
    HANDLE h = (HANDLE)(handleval(env, fdo));
    FILE_END_OF_FILE_INFO eofInfo;

    eofInfo.EndOfFile.QuadPart = size;
    result = SetFileInformationByHandle(h,
                                        FileEndOfFileInfo,
                                        &eofInfo,
                                        sizeof(eofInfo));
    if (result == 0) {
        JNU_ThrowIOExceptionWithLastError(env, "Truncation failed");
        return IOS_THROWN;
    }
    return 0;
}

JNIEXPORT jlong JNICALL
Java_sun_nio_ch_FileDispatcherImpl_size0(JNIEnv *env, jobject this, jobject fdo)
{
    BOOL result = 0;
    HANDLE h = (HANDLE)(handleval(env, fdo));
    LARGE_INTEGER size;

    result = GetFileSizeEx(h, &size);
    if (result == 0) {
        JNU_ThrowIOExceptionWithLastError(env, "Size failed");
        return IOS_THROWN;
    }
    return (jlong)size.QuadPart;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileDispatcherImpl_lock0(JNIEnv *env, jobject this, jobject fdo,
                                      jboolean block, jlong pos, jlong size,
                                      jboolean shared)
{
    HANDLE h = (HANDLE)(handleval(env, fdo));
    DWORD lowPos = (DWORD)pos;
    long highPos = (long)(pos >> 32);
    DWORD lowNumBytes = (DWORD)size;
    DWORD highNumBytes = (DWORD)(size >> 32);
    BOOL result;
    DWORD flags = 0;
    OVERLAPPED o;
    o.hEvent = 0;
    o.Offset = lowPos;
    o.OffsetHigh = highPos;
    if (block == JNI_FALSE) {
        flags |= LOCKFILE_FAIL_IMMEDIATELY;
    }
    if (shared == JNI_FALSE) {
        flags |= LOCKFILE_EXCLUSIVE_LOCK;
    }
    result = LockFileEx(h, flags, 0, lowNumBytes, highNumBytes, &o);
    if (result == 0) {
        int error = GetLastError();
        if (error == ERROR_IO_PENDING) {
            DWORD dwBytes;
            result = GetOverlappedResult(h, &o, &dwBytes, TRUE);
            if (result != 0) {
                return sun_nio_ch_FileDispatcherImpl_LOCKED;
            }
            error = GetLastError();
        }
        if (error != ERROR_LOCK_VIOLATION) {
            JNU_ThrowIOExceptionWithLastError(env, "Lock failed");
            return sun_nio_ch_FileDispatcherImpl_NO_LOCK;
        }
        if (flags & LOCKFILE_FAIL_IMMEDIATELY) {
            return sun_nio_ch_FileDispatcherImpl_NO_LOCK;
        }
        JNU_ThrowIOExceptionWithLastError(env, "Lock failed");
        return sun_nio_ch_FileDispatcherImpl_NO_LOCK;
    }
    return sun_nio_ch_FileDispatcherImpl_LOCKED;
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_FileDispatcherImpl_release0(JNIEnv *env, jobject this,
                                        jobject fdo, jlong pos, jlong size)
{
    HANDLE h = (HANDLE)(handleval(env, fdo));
    DWORD lowPos = (DWORD)pos;
    long highPos = (long)(pos >> 32);
    DWORD lowNumBytes = (DWORD)size;
    DWORD highNumBytes = (DWORD)(size >> 32);
    BOOL result = 0;
    OVERLAPPED o;
    o.hEvent = 0;
    o.Offset = lowPos;
    o.OffsetHigh = highPos;
    result = UnlockFileEx(h, 0, lowNumBytes, highNumBytes, &o);
    if (result == 0) {
        int error = GetLastError();
        if (error == ERROR_IO_PENDING) {
            DWORD dwBytes;
            result = GetOverlappedResult(h, &o, &dwBytes, TRUE);
            if (result != 0) {
                return;
            }
            error = GetLastError();
        }
        if (error != ERROR_NOT_LOCKED) {
            JNU_ThrowIOExceptionWithLastError(env, "Release failed");
        }
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_FileDispatcherImpl_close0(JNIEnv *env, jclass clazz, jobject fdo)
{
    HANDLE h = (HANDLE)handleval(env, fdo);
    if (h != INVALID_HANDLE_VALUE) {
        int result = CloseHandle(h);
        if (result == 0)
            JNU_ThrowIOExceptionWithLastError(env, "Close failed");
    }
}

JNIEXPORT jlong JNICALL
Java_sun_nio_ch_FileDispatcherImpl_duplicateHandle(JNIEnv *env, jclass this, jlong handle)
{
    HANDLE hProcess = GetCurrentProcess();
    HANDLE hFile = jlong_to_ptr(handle);
    HANDLE hResult;
    BOOL res = DuplicateHandle(hProcess, hFile, hProcess, &hResult, 0, FALSE,
                               DUPLICATE_SAME_ACCESS);
    if (res == 0)
       JNU_ThrowIOExceptionWithLastError(env, "DuplicateHandle failed");
    return ptr_to_jlong(hResult);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_FileDispatcherImpl_setDirect0(JNIEnv *env, jclass this,
                                              jobject fdObj, jobject buffer)
{
    jint result = -1;

    HANDLE orig = (HANDLE)(handleval(env, fdObj));

    HANDLE modify = ReOpenFile(orig, 0, 0,
            FILE_FLAG_NO_BUFFERING | FILE_FLAG_WRITE_THROUGH);

    if (modify != INVALID_HANDLE_VALUE) {
        DWORD sectorsPerCluster;
        DWORD bytesPerSector;
        DWORD numberOfFreeClusters;
        DWORD totalNumberOfClusters;
        LPCWSTR lpRootPathName = (*env)->GetDirectBufferAddress(env, buffer);
        BOOL res = GetDiskFreeSpaceW(lpRootPathName,
                                     &sectorsPerCluster,
                                     &bytesPerSector,
                                     &numberOfFreeClusters,
                                     &totalNumberOfClusters);
        if (res == 0) {
            JNU_ThrowIOExceptionWithLastError(env, "DirectIO setup failed");
        }
        result = bytesPerSector;
    }
    return result;
}
