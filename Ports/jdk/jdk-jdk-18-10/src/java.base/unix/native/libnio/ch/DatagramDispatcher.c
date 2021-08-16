/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <sys/types.h>
#include <sys/uio.h>
#include <sys/socket.h>
#include <string.h>
#include <limits.h>

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "jlong.h"
#include "nio.h"
#include "nio_util.h"
#include "sun_nio_ch_DatagramDispatcher.h"

JNIEXPORT jint JNICALL
Java_sun_nio_ch_DatagramDispatcher_read0(JNIEnv *env, jclass clazz,
                                         jobject fdo, jlong address, jint len)
{
    jint fd = fdval(env, fdo);
    void *buf = (void *)jlong_to_ptr(address);
    int result = recv(fd, buf, len, 0);
    if (result < 0 && errno == ECONNREFUSED) {
        JNU_ThrowByName(env, JNU_JAVANETPKG "PortUnreachableException", 0);
        return IOS_THROWN;
    }
    return convertReturnVal(env, result, JNI_TRUE);
}


JNIEXPORT jlong JNICALL
Java_sun_nio_ch_DatagramDispatcher_readv0(JNIEnv *env, jclass clazz,
                                          jobject fdo, jlong address, jint len)
{
    jint fd = fdval(env, fdo);
    ssize_t result = 0;
    struct iovec *iov = (struct iovec *)jlong_to_ptr(address);
    struct msghdr m;
    if (len > IOV_MAX) {
        len = IOV_MAX;
    }

    // initialize the message
    memset(&m, 0, sizeof(m));
    m.msg_iov = iov;
    m.msg_iovlen = len;

    result = recvmsg(fd, &m, 0);
    if (result < 0 && errno == ECONNREFUSED) {
        JNU_ThrowByName(env, JNU_JAVANETPKG "PortUnreachableException", 0);
        return IOS_THROWN;
    }
    return convertLongReturnVal(env, (jlong)result, JNI_TRUE);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_DatagramDispatcher_write0(JNIEnv *env, jclass clazz,
                                          jobject fdo, jlong address, jint len)
{
    jint fd = fdval(env, fdo);
    void *buf = (void *)jlong_to_ptr(address);
    int result = send(fd, buf, len, 0);
    if (result < 0 && errno == ECONNREFUSED) {
        JNU_ThrowByName(env, JNU_JAVANETPKG "PortUnreachableException", 0);
        return IOS_THROWN;
    }
    return convertReturnVal(env, result, JNI_FALSE);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_ch_DatagramDispatcher_writev0(JNIEnv *env, jclass clazz,
                                           jobject fdo, jlong address, jint len)
{
    jint fd = fdval(env, fdo);
    struct iovec *iov = (struct iovec *)jlong_to_ptr(address);
    struct msghdr m;
    ssize_t result = 0;
    if (len > IOV_MAX) {
        len = IOV_MAX;
    }

    // initialize the message
    memset(&m, 0, sizeof(m));
    m.msg_iov = iov;
    m.msg_iovlen = len;

    result = sendmsg(fd, &m, 0);
    if (result < 0 && errno == ECONNREFUSED) {
        JNU_ThrowByName(env, JNU_JAVANETPKG "PortUnreachableException", 0);
        return IOS_THROWN;
    }
    return convertLongReturnVal(env, (jlong)result, JNI_FALSE);
}
