/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "jlong.h"
#include "nio.h"
#include "nio_util.h"

#include "sun_nio_ch_Iocp.h"


static jfieldID completionStatus_error;
static jfieldID completionStatus_bytesTransferred;
static jfieldID completionStatus_completionKey;
static jfieldID completionStatus_overlapped;


JNIEXPORT void JNICALL
Java_sun_nio_ch_Iocp_initIDs(JNIEnv* env, jclass this)
{
    jclass clazz;

    clazz = (*env)->FindClass(env, "sun/nio/ch/Iocp$CompletionStatus");
    CHECK_NULL(clazz);
    completionStatus_error = (*env)->GetFieldID(env, clazz, "error", "I");
    CHECK_NULL(completionStatus_error);
    completionStatus_bytesTransferred = (*env)->GetFieldID(env, clazz, "bytesTransferred", "I");
    CHECK_NULL(completionStatus_bytesTransferred);
    completionStatus_completionKey = (*env)->GetFieldID(env, clazz, "completionKey", "I");
    CHECK_NULL(completionStatus_completionKey);
    completionStatus_overlapped = (*env)->GetFieldID(env, clazz, "overlapped", "J");
    CHECK_NULL(completionStatus_overlapped);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_ch_Iocp_createIoCompletionPort(JNIEnv* env, jclass this,
    jlong handle, jlong existingPort, jint completionKey, jint concurrency)
{
    ULONG_PTR ck = completionKey;
    HANDLE port = CreateIoCompletionPort((HANDLE)jlong_to_ptr(handle),
                                         (HANDLE)jlong_to_ptr(existingPort),
                                         ck,
                                         (DWORD)concurrency);
    if (port == NULL) {
        JNU_ThrowIOExceptionWithLastError(env, "CreateIoCompletionPort failed");
    }
    return ptr_to_jlong(port);
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_Iocp_close0(JNIEnv* env, jclass this,
    jlong handle)
{
    HANDLE h = (HANDLE)jlong_to_ptr(handle);
    CloseHandle(h);
}


JNIEXPORT void JNICALL
Java_sun_nio_ch_Iocp_getQueuedCompletionStatus(JNIEnv* env, jclass this,
    jlong completionPort, jobject obj)
{
    DWORD bytesTransferred;
    ULONG_PTR completionKey;
    OVERLAPPED *lpOverlapped;
    BOOL res;

    res = GetQueuedCompletionStatus((HANDLE)jlong_to_ptr(completionPort),
                                  &bytesTransferred,
                                  &completionKey,
                                  &lpOverlapped,
                                  INFINITE);
    if (res == 0 && lpOverlapped == NULL) {
        JNU_ThrowIOExceptionWithLastError(env, "GetQueuedCompletionStatus failed");
    } else {
        DWORD ioResult = (res == 0) ? GetLastError() : 0;
        (*env)->SetIntField(env, obj, completionStatus_error, ioResult);
        (*env)->SetIntField(env, obj, completionStatus_bytesTransferred,
            (jint)bytesTransferred);
        (*env)->SetIntField(env, obj, completionStatus_completionKey,
            (jint)completionKey);
        (*env)->SetLongField(env, obj, completionStatus_overlapped,
            ptr_to_jlong(lpOverlapped));

    }
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_Iocp_postQueuedCompletionStatus(JNIEnv* env, jclass this,
    jlong completionPort, jint completionKey)
{
    BOOL res;

    res = PostQueuedCompletionStatus((HANDLE)jlong_to_ptr(completionPort),
                                     (DWORD)0,
                                     (DWORD)completionKey,
                                     NULL);
    if (res == 0) {
        JNU_ThrowIOExceptionWithLastError(env, "PostQueuedCompletionStatus");
    }
}

JNIEXPORT jstring JNICALL
Java_sun_nio_ch_Iocp_getErrorMessage(JNIEnv* env, jclass this, jint errorCode)
{
    WCHAR message[255];

    DWORD len = FormatMessageW(FORMAT_MESSAGE_FROM_SYSTEM,
                               NULL,
                               (DWORD)errorCode,
                               0,
                               &message[0],
                               255,
                               NULL);


    if (len == 0) {
        return NULL;
    } else {
        if (len > 3) {
            // Drop final '.', CR, LF
            if (message[len - 1] == L'\n') len--;
            if (message[len - 1] == L'\r') len--;
            if (message[len - 1] == L'.') len--;
            message[len] = L'\0';
        }

        return (*env)->NewString(env, (const jchar *)message, (jsize)wcslen(message));
    }
}
