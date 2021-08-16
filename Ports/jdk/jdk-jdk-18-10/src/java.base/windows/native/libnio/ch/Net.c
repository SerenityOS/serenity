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

#include <windows.h>
#include <winsock2.h>

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "jlong.h"
#include "nio.h"
#include "nio_util.h"
#include "net_util.h"

#include "sun_nio_ch_Net.h"
#include "sun_nio_ch_PollArrayWrapper.h"

/**
 * Definitions to allow for building with older SDK include files.
 */

#ifndef MCAST_BLOCK_SOURCE

#define MCAST_BLOCK_SOURCE          43
#define MCAST_UNBLOCK_SOURCE        44
#define MCAST_JOIN_SOURCE_GROUP     45
#define MCAST_LEAVE_SOURCE_GROUP    46

#endif  /* MCAST_BLOCK_SOURCE */

struct my_ip_mreq_source {
    IN_ADDR imr_multiaddr;
    IN_ADDR imr_sourceaddr;
    IN_ADDR imr_interface;
};

struct my_group_source_req {
    ULONG gsr_interface;
    SOCKADDR_STORAGE gsr_group;
    SOCKADDR_STORAGE gsr_source;
};

/**
 * Copy IPv6 address as jbytearray to target
 */
#define COPY_INET6_ADDRESS(env, source, target) \
    (*env)->GetByteArrayRegion(env, source, 0, 16, target)

/**
 * Enable or disable receipt of WSAECONNRESET errors.
 */
static void setConnectionReset(SOCKET s, BOOL enable) {
    DWORD bytesReturned = 0;
    WSAIoctl(s, SIO_UDP_CONNRESET, &enable, sizeof(enable),
             NULL, 0, &bytesReturned, NULL, NULL);
}

jint handleSocketError(JNIEnv *env, int errorValue)
{
    NET_ThrowNew(env, errorValue, NULL);
    return IOS_THROWN;
}

static jclass isa_class;        /* java.net.InetSocketAddress */
static jmethodID isa_ctorID;    /* InetSocketAddress(InetAddress, int) */

JNIEXPORT void JNICALL
Java_sun_nio_ch_Net_initIDs(JNIEnv *env, jclass clazz)
{
     jclass cls = (*env)->FindClass(env, "java/net/InetSocketAddress");
     CHECK_NULL(cls);
     isa_class = (*env)->NewGlobalRef(env, cls);
     if (isa_class == NULL) {
         JNU_ThrowOutOfMemoryError(env, NULL);
         return;
     }
     isa_ctorID = (*env)->GetMethodID(env, cls, "<init>", "(Ljava/net/InetAddress;I)V");
     CHECK_NULL(isa_ctorID);

     initInetAddressIDs(env);
}

JNIEXPORT jboolean JNICALL
Java_sun_nio_ch_Net_isIPv6Available0(JNIEnv* env, jclass cl)
{
    /*
     * Return true if IPv6 is configured
     */
    return ipv6_available() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_sun_nio_ch_Net_isReusePortAvailable0(JNIEnv* env, jclass c1)
{
    // SO_REUSEPORT is not supported on Windows
    return JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_isExclusiveBindAvailable(JNIEnv *env, jclass clazz) {
    return 1;
}

JNIEXPORT jboolean JNICALL
Java_sun_nio_ch_Net_shouldSetBothIPv4AndIPv6Options0(JNIEnv* env, jclass cl)
{
    /* Set both IPv4 and IPv6 socket options when setting multicast options */
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_sun_nio_ch_Net_canIPv6SocketJoinIPv4Group0(JNIEnv* env, jclass cl)
{
    /* IPv6 sockets can join IPv4 multicast groups */
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_sun_nio_ch_Net_canJoin6WithIPv4Group0(JNIEnv* env, jclass cl)
{
    /* IPV6_ADD_MEMBERSHIP cannot be used to join IPv4 multicast groups */
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_sun_nio_ch_Net_canUseIPv6OptionsWithIPv4LocalAddress0(JNIEnv* env, jclass cl)
{
    /* IPV6_XXX socket options cannot be used on IPv6 sockets bound to IPv4 address */
    return JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_socket0(JNIEnv *env, jclass cl, jboolean preferIPv6,
                            jboolean stream, jboolean reuse, jboolean fastLoopback)
{
    SOCKET s;
    int domain = (preferIPv6) ? AF_INET6 : AF_INET;

    s = socket(domain, (stream ? SOCK_STREAM : SOCK_DGRAM), 0);
    if (s != INVALID_SOCKET) {
        SetHandleInformation((HANDLE)s, HANDLE_FLAG_INHERIT, 0);

        /* IPV6_V6ONLY is true by default */
        if (domain == AF_INET6) {
            int opt = 0;
            setsockopt(s, IPPROTO_IPV6, IPV6_V6ONLY,
                       (const char *)&opt, sizeof(opt));
        }

        /* Disable WSAECONNRESET errors for initially unconnected UDP sockets */
        if (!stream) {
            setConnectionReset(s, FALSE);
        }

    } else {
        NET_ThrowNew(env, WSAGetLastError(), "socket");
    }

    if (stream && fastLoopback) {
        static int loopback_available = 1;
        if (loopback_available) {
            int rv = NET_EnableFastTcpLoopback((jint)s);
            if (rv) {
                if (rv == WSAEOPNOTSUPP) {
                    loopback_available = 0;
                } else {
                    NET_ThrowNew(env, rv, "fastLoopback");
                }
            }
        }
    }

    return (jint)s;
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_Net_bind0(JNIEnv *env, jclass clazz, jobject fdo, jboolean preferIPv6,
                          jboolean isExclBind, jobject iao, jint port)
{
    SOCKETADDRESS sa;
    int rv;
    int sa_len = 0;

    if (NET_InetAddressToSockaddr(env, iao, port, &sa, &sa_len, preferIPv6) != 0) {
        return;
    }

    rv = NET_WinBind(fdval(env, fdo), &sa, sa_len, isExclBind);
    if (rv == SOCKET_ERROR)
        NET_ThrowNew(env, WSAGetLastError(), "bind");
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_Net_listen(JNIEnv *env, jclass cl, jobject fdo, jint backlog)
{
    if (listen(fdval(env,fdo), backlog) == SOCKET_ERROR) {
        NET_ThrowNew(env, WSAGetLastError(), "listen");
    }
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_connect0(JNIEnv *env, jclass clazz, jboolean preferIPv6, jobject fdo,
                             jobject iao, jint port)
{
    SOCKETADDRESS sa;
    int rv;
    int so_rv;
    int sa_len = 0;
    SOCKET s = (SOCKET)fdval(env, fdo);
    int type = 0, optlen = sizeof(type);

    if (NET_InetAddressToSockaddr(env, iao, port, &sa, &sa_len, preferIPv6) != 0) {
        return IOS_THROWN;
    }

    so_rv = getsockopt(s, SOL_SOCKET, SO_TYPE, (char*)&type, &optlen);

    /**
     * Windows has a very long socket connect timeout of 2 seconds.
     * If it's the loopback adapter we can shorten the wait interval.
     */
    if (so_rv == 0 && type == SOCK_STREAM && IS_LOOPBACK_ADDRESS(&sa)) {
        NET_EnableFastTcpLoopbackConnect((jint)s);
    }

    rv = connect(s, &sa.sa, sa_len);
    if (rv != 0) {
        int err = WSAGetLastError();
        if (err == WSAEINPROGRESS || err == WSAEWOULDBLOCK) {
            return IOS_UNAVAILABLE;
        }
        NET_ThrowNew(env, err, "connect");
        return IOS_THROWN;
    } else {
        /* Enable WSAECONNRESET errors when a UDP socket is connected */
        if (so_rv == 0 && type == SOCK_DGRAM) {
            setConnectionReset(s, TRUE);
        }
    }
    return 1;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_accept(JNIEnv *env, jclass clazz, jobject fdo, jobject newfdo,
                           jobjectArray isaa)
{
    jint fd = fdval(env,fdo);
    jint newfd;
    SOCKETADDRESS sa;
    int addrlen = sizeof(sa);
    jobject remote_ia;
    jint remote_port = 0;
    jobject isa;

    memset((char *)&sa, 0, sizeof(sa));
    newfd = (jint) accept(fd, &sa.sa, &addrlen);
    if (newfd == INVALID_SOCKET) {
        int theErr = (jint)WSAGetLastError();
        if (theErr == WSAEWOULDBLOCK) {
            return IOS_UNAVAILABLE;
        }
        JNU_ThrowIOExceptionWithLastError(env, "Accept failed");
        return IOS_THROWN;
    }

    SetHandleInformation((HANDLE)(UINT_PTR)newfd, HANDLE_FLAG_INHERIT, 0);
    setfdval(env, newfdo, newfd);

    remote_ia = NET_SockaddrToInetAddress(env, &sa, (int *)&remote_port);
    CHECK_NULL_RETURN(remote_ia, IOS_THROWN);

    isa = (*env)->NewObject(env, isa_class, isa_ctorID, remote_ia, remote_port);
    CHECK_NULL_RETURN(isa, IOS_THROWN);
    (*env)->SetObjectArrayElement(env, isaa, 0, isa);

    return 1;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_localPort(JNIEnv *env, jclass clazz, jobject fdo)
{
    SOCKETADDRESS sa;
    int sa_len = sizeof(sa);

    if (getsockname(fdval(env, fdo), &sa.sa, &sa_len) == SOCKET_ERROR) {
        int error = WSAGetLastError();
        if (error == WSAEINVAL) {
            return 0;
        }
        NET_ThrowNew(env, error, "getsockname");
        return IOS_THROWN;
    }
    return NET_GetPortFromSockaddr(&sa);
}

JNIEXPORT jobject JNICALL
Java_sun_nio_ch_Net_localInetAddress(JNIEnv *env, jclass clazz, jobject fdo)
{
    SOCKETADDRESS sa;
    int sa_len = sizeof(sa);
    int port;

    if (getsockname(fdval(env, fdo), &sa.sa, &sa_len) == SOCKET_ERROR) {
        NET_ThrowNew(env, WSAGetLastError(), "getsockname");
        return NULL;
    }
    return NET_SockaddrToInetAddress(env, &sa, &port);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_remotePort(JNIEnv *env, jclass clazz, jobject fdo)
{
    SOCKETADDRESS sa;
    int sa_len = sizeof(sa);

    if (getpeername(fdval(env, fdo), &sa.sa, &sa_len) == SOCKET_ERROR) {
        int error = WSAGetLastError();
        if (error == WSAEINVAL) {
            return 0;
        }
        NET_ThrowNew(env, error, "getsockname");
        return IOS_THROWN;
    }
    return NET_GetPortFromSockaddr(&sa);
}

JNIEXPORT jobject JNICALL
Java_sun_nio_ch_Net_remoteInetAddress(JNIEnv *env, jclass clazz, jobject fdo)
{
    SOCKETADDRESS sa;
    int sa_len = sizeof(sa);
    int port;

    if (getpeername(fdval(env, fdo), &sa.sa, &sa_len) == SOCKET_ERROR) {
        NET_ThrowNew(env, WSAGetLastError(), "getsockname");
        return NULL;
    }
    return NET_SockaddrToInetAddress(env, &sa, &port);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_getIntOption0(JNIEnv *env, jclass clazz, jobject fdo,
                                  jboolean mayNeedConversion, jint level, jint opt)
{
    int result = 0;
    struct linger linger;
    char *arg;
    int arglen, n;

    if (level == SOL_SOCKET && opt == SO_LINGER) {
        arg = (char *)&linger;
        arglen = sizeof(linger);
    } else {
        arg = (char *)&result;
        arglen = sizeof(result);
    }

    /**
     * HACK: IP_TOS is deprecated on Windows and querying the option
     * returns a protocol error. NET_GetSockOpt handles this and uses
     * a fallback mechanism. Same applies to IPV6_TCLASS
     */
    if ((level == IPPROTO_IP && opt == IP_TOS) || (level == IPPROTO_IPV6 && opt == IPV6_TCLASS)) {
        mayNeedConversion = JNI_TRUE;
    }

    if (mayNeedConversion) {
        n = NET_GetSockOpt(fdval(env, fdo), level, opt, arg, &arglen);
    } else {
        n = getsockopt(fdval(env, fdo), level, opt, arg, &arglen);
    }
    if (n == SOCKET_ERROR) {
        handleSocketError(env, WSAGetLastError());
        return IOS_THROWN;
    }

    if (level == SOL_SOCKET && opt == SO_LINGER)
        return linger.l_onoff ? linger.l_linger : -1;
    else
        return result;
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_Net_setIntOption0(JNIEnv *env, jclass clazz, jobject fdo,
                                  jboolean mayNeedConversion, jint level, jint opt, jint arg, jboolean ipv6)
{
    struct linger linger;
    char *parg;
    int arglen, n;

    if (level == SOL_SOCKET && opt == SO_LINGER) {
        parg = (char *)&linger;
        arglen = sizeof(linger);
        if (arg >= 0) {
            linger.l_onoff = 1;
            linger.l_linger = (unsigned short)arg;
        } else {
            linger.l_onoff = 0;
            linger.l_linger = 0;
        }
    } else {
        parg = (char *)&arg;
        arglen = sizeof(arg);
    }

    if (level == IPPROTO_IPV6 && opt == IPV6_TCLASS) {
        /* No op */
        return;
    }

    if (mayNeedConversion) {
        n = NET_SetSockOpt(fdval(env, fdo), level, opt, parg, arglen);
    } else {
        n = setsockopt(fdval(env, fdo), level, opt, parg, arglen);
    }
    if (n == SOCKET_ERROR)
        handleSocketError(env, WSAGetLastError());
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_joinOrDrop4(JNIEnv *env, jobject this, jboolean join, jobject fdo,
                                jint group, jint interf, jint source)
{
    struct ip_mreq mreq;
    struct my_ip_mreq_source mreq_source;
    int opt, n, optlen;
    void* optval;

    if (source == 0) {
        mreq.imr_multiaddr.s_addr = htonl(group);
        mreq.imr_interface.s_addr = htonl(interf);
        opt = (join) ? IP_ADD_MEMBERSHIP : IP_DROP_MEMBERSHIP;
        optval = (void*)&mreq;
        optlen = sizeof(mreq);
    } else {
        mreq_source.imr_multiaddr.s_addr = htonl(group);
        mreq_source.imr_sourceaddr.s_addr = htonl(source);
        mreq_source.imr_interface.s_addr = htonl(interf);
        opt = (join) ? IP_ADD_SOURCE_MEMBERSHIP : IP_DROP_SOURCE_MEMBERSHIP;
        optval = (void*)&mreq_source;
        optlen = sizeof(mreq_source);
    }

    n = setsockopt(fdval(env,fdo), IPPROTO_IP, opt, optval, optlen);
    if (n == SOCKET_ERROR) {
        if (join && (WSAGetLastError() == WSAENOPROTOOPT))
            return IOS_UNAVAILABLE;
        handleSocketError(env, WSAGetLastError());
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_blockOrUnblock4(JNIEnv *env, jobject this, jboolean block, jobject fdo,
                                   jint group, jint interf, jint source)
{
    struct my_ip_mreq_source mreq_source;
    int n;
    int opt = (block) ? IP_BLOCK_SOURCE : IP_UNBLOCK_SOURCE;

    mreq_source.imr_multiaddr.s_addr = htonl(group);
    mreq_source.imr_sourceaddr.s_addr = htonl(source);
    mreq_source.imr_interface.s_addr = htonl(interf);

    n = setsockopt(fdval(env,fdo), IPPROTO_IP, opt,
                   (void*)&mreq_source, sizeof(mreq_source));
    if (n == SOCKET_ERROR) {
        if (block && (WSAGetLastError() == WSAENOPROTOOPT))
            return IOS_UNAVAILABLE;
        handleSocketError(env, WSAGetLastError());
    }
    return 0;
}

/**
 * Call setsockopt with a IPPROTO_IPV6 level socket option
 * and a group_source_req structure as the option value. The
 * given IPv6 group, interface index, and IPv6 source address
 * are copied into the structure.
 */
static int setGroupSourceReqOption(JNIEnv* env,
                                   jobject fdo,
                                   int opt,
                                   jbyteArray group,
                                   jint index,
                                   jbyteArray source)
{
    struct my_group_source_req req;
    struct sockaddr_in6* sin6;

    req.gsr_interface = (ULONG)index;

    sin6 = (struct sockaddr_in6*)&(req.gsr_group);
    sin6->sin6_family = AF_INET6;
    COPY_INET6_ADDRESS(env, group, (jbyte*)&(sin6->sin6_addr));

    sin6 = (struct sockaddr_in6*)&(req.gsr_source);
    sin6->sin6_family = AF_INET6;
    COPY_INET6_ADDRESS(env, source, (jbyte*)&(sin6->sin6_addr));

    return setsockopt(fdval(env,fdo), IPPROTO_IPV6, opt, (void*)&req, sizeof(req));
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_joinOrDrop6(JNIEnv *env, jobject this, jboolean join, jobject fdo,
                                jbyteArray group, jint index, jbyteArray source)
{
    struct ipv6_mreq mreq6;
    int n;

    if (source == NULL) {
        int opt = (join) ? IPV6_ADD_MEMBERSHIP : IPV6_DROP_MEMBERSHIP;
        COPY_INET6_ADDRESS(env, group, (jbyte*)&(mreq6.ipv6mr_multiaddr));
        mreq6.ipv6mr_interface = (int)index;
        n = setsockopt(fdval(env,fdo), IPPROTO_IPV6, opt,
                       (void*)&mreq6, sizeof(mreq6));
    } else {
        int opt = (join) ? MCAST_JOIN_SOURCE_GROUP : MCAST_LEAVE_SOURCE_GROUP;
        n = setGroupSourceReqOption(env, fdo, opt, group, index, source);
    }

    if (n == SOCKET_ERROR) {
        handleSocketError(env, WSAGetLastError());
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_blockOrUnblock6(JNIEnv *env, jobject this, jboolean block, jobject fdo,
                                    jbyteArray group, jint index, jbyteArray source)
{
    int opt = (block) ? MCAST_BLOCK_SOURCE : MCAST_UNBLOCK_SOURCE;
    int n = setGroupSourceReqOption(env, fdo, opt, group, index, source);
    if (n == SOCKET_ERROR) {
        handleSocketError(env, WSAGetLastError());
    }
    return 0;
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_Net_setInterface4(JNIEnv* env, jobject this, jobject fdo, jint interf)
{
    struct in_addr in;
    int arglen = sizeof(struct in_addr);
    int n;

    in.s_addr = htonl(interf);

    n = setsockopt(fdval(env, fdo), IPPROTO_IP, IP_MULTICAST_IF,
                   (void*)&(in.s_addr), arglen);
    if (n == SOCKET_ERROR) {
        handleSocketError(env, WSAGetLastError());
    }
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_getInterface4(JNIEnv* env, jobject this, jobject fdo)
{
    struct in_addr in;
    int arglen = sizeof(struct in_addr);
    int n;

    n = getsockopt(fdval(env, fdo), IPPROTO_IP, IP_MULTICAST_IF, (void*)&in, &arglen);
    if (n == SOCKET_ERROR) {
        handleSocketError(env, WSAGetLastError());
        return IOS_THROWN;
    }
    return ntohl(in.s_addr);
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_Net_setInterface6(JNIEnv* env, jobject this, jobject fdo, jint index)
{
    DWORD value = (jint)index;
    int arglen = sizeof(value);
    int n;

    n = setsockopt(fdval(env, fdo), IPPROTO_IPV6, IPV6_MULTICAST_IF,
                   (void*)&(index), arglen);
    if (n == SOCKET_ERROR) {
        handleSocketError(env, WSAGetLastError());
    }
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_getInterface6(JNIEnv* env, jobject this, jobject fdo)
{
    DWORD index;
    int arglen = sizeof(index);
    int n;

    n = getsockopt(fdval(env, fdo), IPPROTO_IPV6, IPV6_MULTICAST_IF, (void*)&index, &arglen);
    if (n == SOCKET_ERROR) {
        handleSocketError(env, WSAGetLastError());
        return -1;
    }
    return (jint)index;
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_Net_shutdown(JNIEnv *env, jclass cl, jobject fdo, jint jhow) {
    int how = (jhow == sun_nio_ch_Net_SHUT_RD) ? SD_RECEIVE :
        (jhow == sun_nio_ch_Net_SHUT_WR) ? SD_SEND : SD_BOTH;
    if (shutdown(fdval(env, fdo), how) == SOCKET_ERROR) {
        NET_ThrowNew(env, WSAGetLastError(), "shutdown");
    }
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_available(JNIEnv *env, jclass cl, jobject fdo)
{
    int count = 0;
    if (NET_SocketAvailable(fdval(env, fdo), &count) != 0) {
        handleSocketError(env, WSAGetLastError());
        return IOS_THROWN;
    }
    return (jint) count;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_poll(JNIEnv* env, jclass this, jobject fdo, jint events, jlong timeout)
{
    int rv;
    int revents = 0;
    struct timeval t;
    fd_set rd, wr, ex;
    jint fd = fdval(env, fdo);

    FD_ZERO(&rd);
    FD_ZERO(&wr);
    FD_ZERO(&ex);
    if (events & POLLIN) {
        FD_SET(fd, &rd);
    }
    if (events & POLLOUT) {
        FD_SET(fd, &wr);
    }
    FD_SET(fd, &ex);

    if (timeout >= 0) {
        t.tv_sec = (long)(timeout / 1000);
        t.tv_usec = (timeout % 1000) * 1000;
    }

    rv = select(fd+1, &rd, &wr, &ex, (timeout >= 0) ? &t : NULL);

    /* save last winsock error */
    if (rv == SOCKET_ERROR) {
        handleSocketError(env, WSAGetLastError());
        return IOS_THROWN;
    } else if (rv >= 0) {
        rv = 0;
        if (FD_ISSET(fd, &rd)) {
            rv |= POLLIN;
        }
        if (FD_ISSET(fd, &wr)) {
            rv |= POLLOUT;
        }
        if (FD_ISSET(fd, &ex)) {
            rv |= POLLERR;
        }
    }
    return rv;
}

JNIEXPORT jboolean JNICALL
Java_sun_nio_ch_Net_pollConnect(JNIEnv* env, jclass this, jobject fdo, jlong timeout)
{
    int optError = 0;
    int result;
    int n = sizeof(int);
    jint fd = fdval(env, fdo);
    fd_set wr, ex;
    struct timeval t;

    FD_ZERO(&wr);
    FD_ZERO(&ex);
    FD_SET((u_int)fd, &wr);
    FD_SET((u_int)fd, &ex);

    if (timeout >= 0) {
        t.tv_sec = (long)(timeout / 1000);
        t.tv_usec = (timeout % 1000) * 1000;
    }

    result = select(fd+1, 0, &wr, &ex, (timeout >= 0) ? &t : NULL);

    if (result == SOCKET_ERROR) {
        handleSocketError(env, WSAGetLastError());
        return JNI_FALSE;
    } else if (result == 0) {
        return JNI_FALSE;
    } else {
        // connection established if writable and no error to check
        if (FD_ISSET(fd, &wr) && !FD_ISSET(fd, &ex)) {
            return JNI_TRUE;
        }
        result = getsockopt((SOCKET)fd,
                            SOL_SOCKET,
                            SO_ERROR,
                            (char *)&optError,
                            &n);
        if (result == SOCKET_ERROR) {
            int lastError = WSAGetLastError();
            if (lastError != WSAEINPROGRESS) {
                NET_ThrowNew(env, lastError, "getsockopt");
            }
        } else if (optError != NO_ERROR) {
            handleSocketError(env, optError);
        }
        return JNI_FALSE;
    }
}

JNIEXPORT jshort JNICALL
Java_sun_nio_ch_Net_pollinValue(JNIEnv *env, jclass this)
{
    return (jshort)POLLIN;
}

JNIEXPORT jshort JNICALL
Java_sun_nio_ch_Net_polloutValue(JNIEnv *env, jclass this)
{
    return (jshort)POLLOUT;
}

JNIEXPORT jshort JNICALL
Java_sun_nio_ch_Net_pollerrValue(JNIEnv *env, jclass this)
{
    return (jshort)POLLERR;
}

JNIEXPORT jshort JNICALL
Java_sun_nio_ch_Net_pollhupValue(JNIEnv *env, jclass this)
{
    return (jshort)POLLHUP;
}

JNIEXPORT jshort JNICALL
Java_sun_nio_ch_Net_pollnvalValue(JNIEnv *env, jclass this)
{
    return (jshort)POLLNVAL;
}

JNIEXPORT jshort JNICALL
Java_sun_nio_ch_Net_pollconnValue(JNIEnv *env, jclass this)
{
    return (jshort)POLLOUT;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_sendOOB(JNIEnv* env, jclass this, jobject fdo, jbyte b)
{
    int n = send(fdval(env, fdo), (const char*)&b, 1, MSG_OOB);
    if (n == SOCKET_ERROR) {
        if (WSAGetLastError() == WSAEWOULDBLOCK) {
            return IOS_UNAVAILABLE;
        } else {
            JNU_ThrowIOExceptionWithLastError(env, "send failed");
            return IOS_THROWN;
        }
    } else {
        return n;
    }
}

JNIEXPORT jboolean JNICALL
Java_sun_nio_ch_Net_discardOOB(JNIEnv* env, jclass clazz, jobject fdo)
{
    char buf[8];
    jboolean discarded = JNI_FALSE;
    for (;;) {
        int n = recv(fdval(env, fdo), (char*)&buf, sizeof(buf), MSG_OOB);
        if (n == SOCKET_ERROR) {
            if (WSAGetLastError() != WSAEWOULDBLOCK) {
                JNU_ThrowIOExceptionWithLastError(env, "recv failed");
            }
            return discarded;
        }
        if (n <= 0)
            return discarded;
        if (n < (int)sizeof(buf))
            return JNI_TRUE;
        discarded = JNI_TRUE;
    }
}
