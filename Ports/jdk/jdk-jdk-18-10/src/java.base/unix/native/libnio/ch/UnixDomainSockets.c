/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <poll.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <string.h>
#include <stddef.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <limits.h>

#include "jni.h"
#include "java_props.h"
#include "jni_util.h"
#include "jvm.h"
#include "jlong.h"
#include "sun_nio_ch_Net.h"
#include "nio_util.h"
#include "nio.h"

/* Subtle platform differences in how unnamed sockets (empty path)
 * are returned from getsockname()
 */
#ifdef MACOSX
  #define ZERO_PATHLEN(len) (JNI_FALSE)
#else
  #define ZERO_PATHLEN(len) (len == offsetof(struct sockaddr_un, sun_path))
#endif

jbyteArray sockaddrToUnixAddressBytes(JNIEnv *env, struct sockaddr_un *sa, socklen_t len)
{
    if (sa->sun_family == AF_UNIX) {
        int namelen;
        if (ZERO_PATHLEN(len)) {
            namelen = 0;
        } else {
            namelen = strlen(sa->sun_path);
        }
        jbyteArray name = (*env)->NewByteArray(env, namelen);
        if (namelen != 0) {
            (*env)->SetByteArrayRegion(env, name, 0, namelen, (jbyte*)sa->sun_path);
            if ((*env)->ExceptionOccurred(env)) {
                return NULL;
            }
        }
        return name;
    }
    return NULL;
}

jint unixSocketAddressToSockaddr(JNIEnv *env, jbyteArray path, struct sockaddr_un *sa, int *len)
{
    memset(sa, 0, sizeof(struct sockaddr_un));
    sa->sun_family = AF_UNIX;
    int ret;
    const char* pname = (const char *)(*env)->GetByteArrayElements(env, path, NULL);
    if (pname == NULL) {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Unix domain path not present");
        return -1;
    }
    size_t name_len = (*env)->GetArrayLength(env, path);
    if (name_len > MAX_UNIX_DOMAIN_PATH_LEN) {
        JNU_ThrowByName(env, JNU_JAVANETPKG "SocketException", "Unix domain path too long");
        ret = -1;
    } else {
        memcpy(sa->sun_path, pname, name_len);
        *len = (int)(offsetof(struct sockaddr_un, sun_path) + name_len + 1);
        ret = 0;
    }
    (*env)->ReleaseByteArrayElements(env, path, (jbyte *)pname, 0);
    return ret;
}

JNIEXPORT jboolean JNICALL
Java_sun_nio_ch_UnixDomainSockets_init(JNIEnv *env, jclass cl)
{
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_UnixDomainSockets_socket0(JNIEnv *env, jclass cl)
{
    int fd = socket(PF_UNIX, SOCK_STREAM, 0);
    if (fd < 0) {
        return handleSocketError(env, errno);
    }
    return fd;
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_UnixDomainSockets_bind0(JNIEnv *env, jclass clazz, jobject fdo, jbyteArray path)
{
    struct sockaddr_un sa;
    int sa_len = 0;
    int rv = 0;

    if (unixSocketAddressToSockaddr(env, path, &sa, &sa_len) != 0)
        return;

    rv = bind(fdval(env, fdo), (struct sockaddr *)&sa, sa_len);
    if (rv != 0) {
        handleSocketError(env, errno);
    }
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_UnixDomainSockets_connect0(JNIEnv *env, jclass clazz, jobject fdo, jbyteArray path)
{
    struct sockaddr_un sa;
    int sa_len = 0;
    int rv;

    if (unixSocketAddressToSockaddr(env, path, &sa, &sa_len) != 0) {
        return IOS_THROWN;
    }

    rv = connect(fdval(env, fdo), (struct sockaddr *)&sa, sa_len);
    if (rv != 0) {
        if (errno == EINPROGRESS) {
            return IOS_UNAVAILABLE;
        } else if (errno == EINTR) {
            return IOS_INTERRUPTED;
        }
        return handleSocketError(env, errno);
    }
    return 1;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_UnixDomainSockets_accept0(JNIEnv *env, jclass clazz, jobject fdo, jobject newfdo,
                                          jobjectArray array)
{
    jint fd = fdval(env, fdo);
    jint newfd;
    struct sockaddr_un sa;
    socklen_t sa_len = sizeof(struct sockaddr_un);
    jbyteArray address;

    newfd = accept(fd, (struct sockaddr *)&sa, &sa_len);
    if (newfd < 0) {
        if (errno == EAGAIN || errno == EWOULDBLOCK)
            return IOS_UNAVAILABLE;
        if (errno == EINTR)
            return IOS_INTERRUPTED;
        JNU_ThrowIOExceptionWithLastError(env, "Accept failed");
        return IOS_THROWN;
    }

    setfdval(env, newfdo, newfd);

    address = sockaddrToUnixAddressBytes(env, &sa, sa_len);
    CHECK_NULL_RETURN(address, IOS_THROWN);

    (*env)->SetObjectArrayElement(env, array, 0, address);

    return 1;
}

JNIEXPORT jbyteArray JNICALL
Java_sun_nio_ch_UnixDomainSockets_localAddress0(JNIEnv *env, jclass clazz, jobject fdo)
{
    struct sockaddr_un sa;
    socklen_t sa_len = sizeof(struct sockaddr_un);
    int port;
    if (getsockname(fdval(env, fdo), (struct sockaddr *)&sa, &sa_len) < 0) {
        handleSocketError(env, errno);
        return NULL;
    }
    return sockaddrToUnixAddressBytes(env, &sa, sa_len);
}

