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

#include <winsock2.h>
#include <ws2tcpip.h>
#include <afunix.h>

#include "jni.h"

/**
 * The maximum buffer size for WSASend/WSARecv. Microsoft recommendation for
 * blocking operations is to use buffers no larger than 64k. We need the
 * maximum to be less than 128k to support asynchronous close on Windows
 * Server 2003 and newer editions of Windows.
 */
#define MAX_BUFFER_SIZE             ((128*1024)-1)

#define MAX_UNIX_DOMAIN_PATH_LEN \
        (int)(sizeof(((struct sockaddr_un *)0)->sun_path)-2)

jint fdval(JNIEnv *env, jobject fdo);
void setfdval(JNIEnv *env, jobject fdo, jint val);
jlong handleval(JNIEnv *env, jobject fdo);
jint convertReturnVal(JNIEnv *env, jint n, jboolean r);
jlong convertLongReturnVal(JNIEnv *env, jlong n, jboolean r);
jboolean purgeOutstandingICMP(JNIEnv *env, jclass clazz, jint fd);
jint handleSocketError(JNIEnv *env, int errorValue);

#ifdef _WIN64

struct iovec {
    jlong  iov_base;
    jint  iov_len;
};

#else

struct iovec {
    jint  iov_base;
    jint  iov_len;
};

#endif

/* Defined in UnixDomainSockets.c */

jbyteArray sockaddrToUnixAddressBytes(JNIEnv *env, struct sockaddr_un *sa, socklen_t len);

jint unixSocketAddressToSockaddr(JNIEnv *env, jbyteArray uaddr,
                                struct sockaddr_un *sa, int *len);

