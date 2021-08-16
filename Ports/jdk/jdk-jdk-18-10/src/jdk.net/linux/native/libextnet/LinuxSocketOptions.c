/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/types.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>

#include <jni.h>
#include <netinet/tcp.h>
#include <netinet/in.h>
#include "jni_util.h"
#include "jdk_net_LinuxSocketOptions.h"

#ifndef SO_INCOMING_NAPI_ID
#define SO_INCOMING_NAPI_ID    56
#endif

static void handleError(JNIEnv *env, jint rv, const char *errmsg) {
    if (rv < 0) {
        if (errno == ENOPROTOOPT) {
            JNU_ThrowByName(env, "java/lang/UnsupportedOperationException",
                    "unsupported socket option");
        } else {
            JNU_ThrowByNameWithLastError(env, "java/net/SocketException", errmsg);
        }
    }
}

static jint socketOptionSupported(jint level, jint optname) {
    jint one = 1;
    jint rv, s;
    socklen_t sz = sizeof (one);
    s = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (s < 0) {
        return 0;
    }
    rv = getsockopt(s, level, optname, (void *) &one, &sz);
    if (rv != 0 && errno == ENOPROTOOPT) {
        rv = 0;
    } else {
        rv = 1;
    }
    close(s);
    return rv;
}

/*
 * Declare library specific JNI_Onload entry if static build
 */
DEF_STATIC_JNI_OnLoad

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    setQuickAck
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_jdk_net_LinuxSocketOptions_setQuickAck0
(JNIEnv *env, jobject unused, jint fd, jboolean on) {
    int optval;
    int rv;
    optval = (on ? 1 : 0);
    rv = setsockopt(fd, SOL_SOCKET, TCP_QUICKACK, &optval, sizeof (optval));
    handleError(env, rv, "set option TCP_QUICKACK failed");
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    getQuickAck
 * Signature: (I)Z;
 */
JNIEXPORT jboolean JNICALL Java_jdk_net_LinuxSocketOptions_getQuickAck0
(JNIEnv *env, jobject unused, jint fd) {
    int on;
    socklen_t sz = sizeof (on);
    int rv = getsockopt(fd, SOL_SOCKET, TCP_QUICKACK, &on, &sz);
    handleError(env, rv, "get option TCP_QUICKACK failed");
    return on != 0;
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    quickAckSupported
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jdk_net_LinuxSocketOptions_quickAckSupported0
(JNIEnv *env, jobject unused) {
    return socketOptionSupported(SOL_SOCKET, TCP_QUICKACK);
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    getSoPeerCred0
 * Signature: (I)L
 */
JNIEXPORT jlong JNICALL Java_jdk_net_LinuxSocketOptions_getSoPeerCred0
  (JNIEnv *env, jclass clazz, jint fd) {

    int rv;
    struct ucred cred;
    socklen_t len = sizeof(cred);

    if ((rv=getsockopt(fd, SOL_SOCKET, SO_PEERCRED, &cred, &len)) < 0) {
        handleError(env, rv, "get SO_PEERCRED failed");
    } else {
        if ((int)cred.uid == -1) {
            handleError(env, -1, "get SO_PEERCRED failed");
            cred.uid = cred.gid = -1;
        }
    }
    return (((jlong)cred.uid) << 32) | (cred.gid & 0xffffffffL);
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    keepAliveOptionsSupported0
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jdk_net_LinuxSocketOptions_keepAliveOptionsSupported0
(JNIEnv *env, jobject unused) {
    return socketOptionSupported(SOL_TCP, TCP_KEEPIDLE) && socketOptionSupported(SOL_TCP, TCP_KEEPCNT)
            && socketOptionSupported(SOL_TCP, TCP_KEEPINTVL);
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    setTcpkeepAliveProbes0
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_jdk_net_LinuxSocketOptions_setTcpkeepAliveProbes0
(JNIEnv *env, jobject unused, jint fd, jint optval) {
    jint rv = setsockopt(fd, SOL_TCP, TCP_KEEPCNT, &optval, sizeof (optval));
    handleError(env, rv, "set option TCP_KEEPCNT failed");
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    setTcpKeepAliveTime0
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_jdk_net_LinuxSocketOptions_setTcpKeepAliveTime0
(JNIEnv *env, jobject unused, jint fd, jint optval) {
    jint rv = setsockopt(fd, SOL_TCP, TCP_KEEPIDLE, &optval, sizeof (optval));
    handleError(env, rv, "set option TCP_KEEPIDLE failed");
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    setTcpKeepAliveIntvl0
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_jdk_net_LinuxSocketOptions_setTcpKeepAliveIntvl0
(JNIEnv *env, jobject unused, jint fd, jint optval) {
    jint rv = setsockopt(fd, SOL_TCP, TCP_KEEPINTVL, &optval, sizeof (optval));
    handleError(env, rv, "set option TCP_KEEPINTVL failed");
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    getTcpkeepAliveProbes0
 * Signature: (I)I;
 */
JNIEXPORT jint JNICALL Java_jdk_net_LinuxSocketOptions_getTcpkeepAliveProbes0
(JNIEnv *env, jobject unused, jint fd) {
    jint optval, rv;
    socklen_t sz = sizeof (optval);
    rv = getsockopt(fd, SOL_TCP, TCP_KEEPCNT, &optval, &sz);
    handleError(env, rv, "get option TCP_KEEPCNT failed");
    return optval;
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    getTcpKeepAliveTime0
 * Signature: (I)I;
 */
JNIEXPORT jint JNICALL Java_jdk_net_LinuxSocketOptions_getTcpKeepAliveTime0
(JNIEnv *env, jobject unused, jint fd) {
    jint optval, rv;
    socklen_t sz = sizeof (optval);
    rv = getsockopt(fd, SOL_TCP, TCP_KEEPIDLE, &optval, &sz);
    handleError(env, rv, "get option TCP_KEEPIDLE failed");
    return optval;
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    getTcpKeepAliveIntvl0
 * Signature: (I)I;
 */
JNIEXPORT jint JNICALL Java_jdk_net_LinuxSocketOptions_getTcpKeepAliveIntvl0
(JNIEnv *env, jobject unused, jint fd) {
    jint optval, rv;
    socklen_t sz = sizeof (optval);
    rv = getsockopt(fd, SOL_TCP, TCP_KEEPINTVL, &optval, &sz);
    handleError(env, rv, "get option TCP_KEEPINTVL failed");
    return optval;
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    incomingNapiIdSupported0
 * Signature: ()Z;
 */
JNIEXPORT jboolean JNICALL Java_jdk_net_LinuxSocketOptions_incomingNapiIdSupported0
(JNIEnv *env, jobject unused) {
    return socketOptionSupported(SOL_SOCKET, SO_INCOMING_NAPI_ID);
}

/*
 * Class:     jdk_net_LinuxSocketOptions
 * Method:    getIncomingNapiId0
 * Signature: (I)I;
 */
JNIEXPORT jint JNICALL Java_jdk_net_LinuxSocketOptions_getIncomingNapiId0
(JNIEnv *env, jobject unused, jint fd) {
    jint optval, rv;
    socklen_t sz = sizeof (optval);
    rv = getsockopt(fd, SOL_SOCKET, SO_INCOMING_NAPI_ID, &optval, &sz);
    handleError(env, rv, "get option SO_INCOMING_NAPI_ID failed");
    return optval;
}
