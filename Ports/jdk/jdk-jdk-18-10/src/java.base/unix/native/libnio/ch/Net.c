/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <limits.h>

#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "jlong.h"
#include "sun_nio_ch_Net.h"
#include "net_util.h"
#include "net_util_md.h"
#include "nio_util.h"
#include "nio.h"

#ifdef _AIX
#include <stdlib.h>
#include <sys/utsname.h>
#endif

/**
 * IP_MULTICAST_ALL supported since 2.6.31 but may not be available at
 * build time.
 */
#ifdef __linux__
  #ifndef IP_MULTICAST_ALL
    #define IP_MULTICAST_ALL    49
  #endif
#endif

/**
 * IPV6_ADD_MEMBERSHIP/IPV6_DROP_MEMBERSHIP may not be defined on OSX and AIX
 */
#if defined(__APPLE__) || defined(_AIX)
  #ifndef IPV6_ADD_MEMBERSHIP
    #define IPV6_ADD_MEMBERSHIP     IPV6_JOIN_GROUP
    #define IPV6_DROP_MEMBERSHIP    IPV6_LEAVE_GROUP
  #endif
#endif

#define COPY_INET6_ADDRESS(env, source, target) \
    (*env)->GetByteArrayRegion(env, source, 0, 16, target)

/*
 * Copy IPv6 group, interface index, and IPv6 source address
 * into group_source_req structure.
 */
static void initGroupSourceReq(JNIEnv* env, jbyteArray group, jint index,
                               jbyteArray source, struct group_source_req *req)
{
    struct sockaddr_in6* sin6;

    req->gsr_interface = (uint32_t)index;

    sin6 = (struct sockaddr_in6 *)&(req->gsr_group);
    sin6->sin6_family = AF_INET6;
    COPY_INET6_ADDRESS(env, group, (jbyte *)&(sin6->sin6_addr));

    sin6 = (struct sockaddr_in6 *)&(req->gsr_source);
    sin6->sin6_family = AF_INET6;
    COPY_INET6_ADDRESS(env, source, (jbyte *)&(sin6->sin6_addr));
}

#ifdef _AIX

/*
 * Checks whether or not "socket extensions for multicast source filters" is supported.
 * Returns JNI_TRUE if it is supported, JNI_FALSE otherwise
 */
static jboolean isSourceFilterSupported(){
    static jboolean alreadyChecked = JNI_FALSE;
    static jboolean result = JNI_TRUE;
    if (alreadyChecked != JNI_TRUE){
        struct utsname uts;
        memset(&uts, 0, sizeof(uts));
        strcpy(uts.sysname, "?");
        const int utsRes = uname(&uts);
        int major = -1;
        int minor = -1;
        major = atoi(uts.version);
        minor = atoi(uts.release);
        if (strcmp(uts.sysname, "AIX") == 0) {
            if (major < 6 || (major == 6 && minor < 1)) {// unsupported on aix < 6.1
                result = JNI_FALSE;
            }
        }
        alreadyChecked = JNI_TRUE;
    }
    return result;
}

#endif  /* _AIX */

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
    return (ipv6_available()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_sun_nio_ch_Net_isReusePortAvailable0(JNIEnv* env, jclass c1)
{
    return (reuseport_available()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_isExclusiveBindAvailable(JNIEnv *env, jclass clazz) {
    return -1;
}

JNIEXPORT jboolean JNICALL
Java_sun_nio_ch_Net_shouldSetBothIPv4AndIPv6Options0(JNIEnv* env, jclass cl)
{
#if defined(__linux__)
    /* Set both IPv4 and IPv6 socket options when setting multicast options */
    return JNI_TRUE;
#else
    /* Do not set both IPv4 and IPv6 socket options when setting multicast options */
    return JNI_FALSE;
#endif
}

JNIEXPORT jboolean JNICALL
Java_sun_nio_ch_Net_canIPv6SocketJoinIPv4Group0(JNIEnv* env, jclass cl)
{
#if defined(__linux__) || defined(__APPLE__)
    /* IPv6 sockets can join IPv4 multicast groups */
    return JNI_TRUE;
#else
    /* IPv6 sockets cannot join IPv4 multicast groups */
    return JNI_FALSE;
#endif
}

JNIEXPORT jboolean JNICALL
Java_sun_nio_ch_Net_canJoin6WithIPv4Group0(JNIEnv* env, jclass cl)
{
#if defined(__APPLE__)
    /* IPV6_ADD_MEMBERSHIP can be used to join IPv4 multicast groups */
    return JNI_TRUE;
#else
    /* IPV6_ADD_MEMBERSHIP cannot be used to join IPv4 multicast groups */
    return JNI_FALSE;
#endif
}

JNIEXPORT jboolean JNICALL
Java_sun_nio_ch_Net_canUseIPv6OptionsWithIPv4LocalAddress0(JNIEnv* env, jclass cl)
{
    /* IPV6_XXX socket options can be used on IPv6 sockets bound to IPv4 address */
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_socket0(JNIEnv *env, jclass cl, jboolean preferIPv6,
                            jboolean stream, jboolean reuse, jboolean ignored)
{
    int fd;
    int type = (stream ? SOCK_STREAM : SOCK_DGRAM);
    int domain = (ipv6_available() && preferIPv6) ? AF_INET6 : AF_INET;

    fd = socket(domain, type, 0);
    if (fd < 0) {
        return handleSocketError(env, errno);
    }

    /*
     * If IPv4 is available, disable IPV6_V6ONLY to ensure dual-socket support.
     */
    if (domain == AF_INET6 && ipv4_available()) {
        int arg = 0;
        if (setsockopt(fd, IPPROTO_IPV6, IPV6_V6ONLY, (char*)&arg,
                       sizeof(int)) < 0) {
            JNU_ThrowByNameWithLastError(env,
                                         JNU_JAVANETPKG "SocketException",
                                         "Unable to set IPV6_V6ONLY");
            close(fd);
            return -1;
        }
    }

    if (reuse) {
        int arg = 1;
        if (setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, (char*)&arg,
                       sizeof(arg)) < 0) {
            JNU_ThrowByNameWithLastError(env,
                                         JNU_JAVANETPKG "SocketException",
                                         "Unable to set SO_REUSEADDR");
            close(fd);
            return -1;
        }
    }

#if defined(__linux__)
    if (type == SOCK_DGRAM) {
        int arg = 0;
        int level = (domain == AF_INET6) ? IPPROTO_IPV6 : IPPROTO_IP;
        if ((setsockopt(fd, level, IP_MULTICAST_ALL, (char*)&arg, sizeof(arg)) < 0) &&
            (errno != ENOPROTOOPT)) {
            JNU_ThrowByNameWithLastError(env,
                                         JNU_JAVANETPKG "SocketException",
                                         "Unable to set IP_MULTICAST_ALL");
            close(fd);
            return -1;
        }
    }

    /* By default, Linux uses the route default */
    if (domain == AF_INET6 && type == SOCK_DGRAM) {
        int arg = 1;
        if (setsockopt(fd, IPPROTO_IPV6, IPV6_MULTICAST_HOPS, &arg,
                       sizeof(arg)) < 0) {
            JNU_ThrowByNameWithLastError(env,
                                         JNU_JAVANETPKG "SocketException",
                                         "Unable to set IPV6_MULTICAST_HOPS");
            close(fd);
            return -1;
        }
    }
#endif

#ifdef __APPLE__
    /**
     * Attempt to set SO_SNDBUF to a minimum size to allow sending large datagrams
     * (net.inet.udp.maxdgram defaults to 9216).
     */
    if (type == SOCK_DGRAM) {
        int size;
        socklen_t arglen = sizeof(size);
        if (getsockopt(fd, SOL_SOCKET, SO_SNDBUF, &size, &arglen) == 0) {
            int minSize = (domain == AF_INET6) ? 65527  : 65507;
            if (size < minSize) {
                setsockopt(fd, SOL_SOCKET, SO_SNDBUF, &minSize, sizeof(minSize));
            }
        }
    }
#endif

    return fd;
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_Net_bind0(JNIEnv *env, jclass clazz, jobject fdo, jboolean preferIPv6,
                          jboolean useExclBind, jobject iao, int port)
{
    SOCKETADDRESS sa;
    int sa_len = 0;
    int rv = 0;

    if (NET_InetAddressToSockaddr(env, iao, port, &sa, &sa_len,
                                  preferIPv6) != 0) {
        return;
    }

    rv = NET_Bind(fdval(env, fdo), &sa, sa_len);
    if (rv != 0) {
        handleSocketError(env, errno);
    }
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_Net_listen(JNIEnv *env, jclass cl, jobject fdo, jint backlog)
{
    if (listen(fdval(env, fdo), backlog) < 0)
        handleSocketError(env, errno);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_connect0(JNIEnv *env, jclass clazz, jboolean preferIPv6,
                             jobject fdo, jobject iao, jint port)
{
    SOCKETADDRESS sa;
    int sa_len = 0;
    int rv;

    if (NET_InetAddressToSockaddr(env, iao, port, &sa, &sa_len, preferIPv6) != 0) {
        return IOS_THROWN;
    }

    rv = connect(fdval(env, fdo), &sa.sa, sa_len);
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
Java_sun_nio_ch_Net_accept(JNIEnv *env, jclass clazz, jobject fdo, jobject newfdo,
                           jobjectArray isaa)
{
    jint fd = fdval(env, fdo);
    jint newfd;
    SOCKETADDRESS sa;
    socklen_t sa_len = sizeof(SOCKETADDRESS);
    jobject remote_ia;
    jint remote_port = 0;
    jobject isa;

    /* accept connection but ignore ECONNABORTED */
    for (;;) {
        newfd = accept(fd, &sa.sa, &sa_len);
        if (newfd >= 0) {
            break;
        }
        if (errno != ECONNABORTED) {
            break;
        }
        /* ECONNABORTED => restart accept */
    }

    if (newfd < 0) {
        if (errno == EAGAIN || errno == EWOULDBLOCK)
            return IOS_UNAVAILABLE;
        if (errno == EINTR)
            return IOS_INTERRUPTED;
        JNU_ThrowIOExceptionWithLastError(env, "Accept failed");
        return IOS_THROWN;
    }

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
    socklen_t sa_len = sizeof(SOCKETADDRESS);
    if (getsockname(fdval(env, fdo), &sa.sa, &sa_len) < 0) {
#ifdef _ALLBSD_SOURCE
        /*
         * XXXBSD:
         * ECONNRESET is specific to the BSDs. We can not return an error,
         * as the calling Java code with raise a java.lang.Error given the expectation
         * that getsockname() will never fail. According to the Single UNIX Specification,
         * it shouldn't fail. As such, we just fill in generic Linux-compatible values.
         */
        if (errno == ECONNRESET) {
            bzero(&sa.sa4, sizeof(sa));
            sa.sa4.sin_len = sizeof(struct sockaddr_in);
            sa.sa4.sin_family = AF_INET;
            sa.sa4.sin_port = htonl(0);
            sa.sa4.sin_addr.s_addr = INADDR_ANY;
        } else {
            handleSocketError(env, errno);
            return -1;
        }
#else /* _ALLBSD_SOURCE */
        handleSocketError(env, errno);
        return -1;
#endif /* _ALLBSD_SOURCE */
    }
    return NET_GetPortFromSockaddr(&sa);
}

JNIEXPORT jobject JNICALL
Java_sun_nio_ch_Net_localInetAddress(JNIEnv *env, jclass clazz, jobject fdo)
{
    SOCKETADDRESS sa;
    socklen_t sa_len = sizeof(SOCKETADDRESS);
    int port;
    if (getsockname(fdval(env, fdo), &sa.sa, &sa_len) < 0) {
#ifdef _ALLBSD_SOURCE
        /*
         * XXXBSD:
         * ECONNRESET is specific to the BSDs. We can not return an error,
         * as the calling Java code with raise a java.lang.Error with the expectation
         * that getsockname() will never fail. According to the Single UNIX Specification,
         * it shouldn't fail. As such, we just fill in generic Linux-compatible values.
         */
        if (errno == ECONNRESET) {
            bzero(&sa.sa4, sizeof(sa));
            sa.sa4.sin_len  = sizeof(struct sockaddr_in);
            sa.sa4.sin_family = AF_INET;
            sa.sa4.sin_port = htonl(0);
            sa.sa4.sin_addr.s_addr = INADDR_ANY;
        } else {
            handleSocketError(env, errno);
            return NULL;
        }
#else /* _ALLBSD_SOURCE */
        handleSocketError(env, errno);
        return NULL;
#endif /* _ALLBSD_SOURCE */
    }
    return NET_SockaddrToInetAddress(env, &sa, &port);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_remotePort(JNIEnv *env, jclass clazz, jobject fdo)
{
    SOCKETADDRESS sa;
    socklen_t sa_len = sizeof(sa);

    if (getpeername(fdval(env, fdo), &sa.sa, &sa_len) < 0) {
        handleSocketError(env, errno);
        return IOS_THROWN;
    }
    return NET_GetPortFromSockaddr(&sa);
}

JNIEXPORT jobject JNICALL
Java_sun_nio_ch_Net_remoteInetAddress(JNIEnv *env, jclass clazz, jobject fdo)
{
    SOCKETADDRESS sa;
    socklen_t sa_len = sizeof(sa);
    int port;

    if (getpeername(fdval(env, fdo), &sa.sa, &sa_len) < 0) {
        handleSocketError(env, errno);
        return NULL;
    }
    return NET_SockaddrToInetAddress(env, &sa, &port);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_getIntOption0(JNIEnv *env, jclass clazz, jobject fdo,
                                  jboolean mayNeedConversion, jint level, jint opt)
{
    int result;
    struct linger linger;
    u_char carg;
    void *arg;
    socklen_t arglen;
    int n;

    /* Option value is an int except for a few specific cases */

    arg = (void *)&result;
    arglen = sizeof(result);

    if (level == IPPROTO_IP &&
        (opt == IP_MULTICAST_TTL || opt == IP_MULTICAST_LOOP)) {
        arg = (void*)&carg;
        arglen = sizeof(carg);
    }

    if (level == SOL_SOCKET && opt == SO_LINGER) {
        arg = (void *)&linger;
        arglen = sizeof(linger);
    }

    if (mayNeedConversion) {
        n = NET_GetSockOpt(fdval(env, fdo), level, opt, arg, (int*)&arglen);
    } else {
        n = getsockopt(fdval(env, fdo), level, opt, arg, &arglen);
    }
    if (n < 0) {
        JNU_ThrowByNameWithLastError(env,
                                     JNU_JAVANETPKG "SocketException",
                                     "sun.nio.ch.Net.getIntOption");
        return -1;
    }

    if (level == IPPROTO_IP &&
        (opt == IP_MULTICAST_TTL || opt == IP_MULTICAST_LOOP))
    {
        return (jint)carg;
    }

    if (level == SOL_SOCKET && opt == SO_LINGER)
        return linger.l_onoff ? (jint)linger.l_linger : (jint)-1;

    return (jint)result;
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_Net_setIntOption0(JNIEnv *env, jclass clazz, jobject fdo,
                                  jboolean mayNeedConversion, jint level,
                                  jint opt, jint arg, jboolean isIPv6)
{
    int result;
    struct linger linger;
    u_char carg;
    void *parg;
    socklen_t arglen;
    int n;

    /* Option value is an int except for a few specific cases */

    parg = (void*)&arg;
    arglen = sizeof(arg);

    if (level == IPPROTO_IP &&
        (opt == IP_MULTICAST_TTL || opt == IP_MULTICAST_LOOP)) {
        parg = (void*)&carg;
        arglen = sizeof(carg);
        carg = (u_char)arg;
    }

    if (level == SOL_SOCKET && opt == SO_LINGER) {
        parg = (void *)&linger;
        arglen = sizeof(linger);
        if (arg >= 0) {
            linger.l_onoff = 1;
            linger.l_linger = arg;
        } else {
            linger.l_onoff = 0;
            linger.l_linger = 0;
        }
    }

    if (mayNeedConversion) {
        n = NET_SetSockOpt(fdval(env, fdo), level, opt, parg, arglen);
    } else {
        n = setsockopt(fdval(env, fdo), level, opt, parg, arglen);
    }
    if (n < 0) {
        JNU_ThrowByNameWithLastError(env,
                                     JNU_JAVANETPKG "SocketException",
                                     "sun.nio.ch.Net.setIntOption");
    }
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_joinOrDrop4(JNIEnv *env, jobject this, jboolean join, jobject fdo,
                                jint group, jint interf, jint source)
{
    struct ip_mreq mreq;
    struct ip_mreq_source mreq_source;
    int opt, n, optlen;
    void* optval;

    if (source == 0) {
        mreq.imr_multiaddr.s_addr = htonl(group);
        mreq.imr_interface.s_addr = htonl(interf);
        opt = (join) ? IP_ADD_MEMBERSHIP : IP_DROP_MEMBERSHIP;
        optval = (void*)&mreq;
        optlen = sizeof(mreq);
    } else {

#ifdef _AIX
        /* check AIX for support of source filtering */
        if (isSourceFilterSupported() != JNI_TRUE){
            return IOS_UNAVAILABLE;
        }
#endif

        mreq_source.imr_multiaddr.s_addr = htonl(group);
        mreq_source.imr_sourceaddr.s_addr = htonl(source);
        mreq_source.imr_interface.s_addr = htonl(interf);
        opt = (join) ? IP_ADD_SOURCE_MEMBERSHIP : IP_DROP_SOURCE_MEMBERSHIP;
        optval = (void*)&mreq_source;
        optlen = sizeof(mreq_source);
    }

    n = setsockopt(fdval(env,fdo), IPPROTO_IP, opt, optval, optlen);
#ifdef __APPLE__
    // workaround macOS bug where IP_ADD_MEMBERSHIP fails intermittently
    if (n < 0 && errno == ENOMEM) {
        n = setsockopt(fdval(env,fdo), IPPROTO_IP, opt, optval, optlen);
    }
#endif

    if (n < 0) {
        if (join && (errno == ENOPROTOOPT || errno == EOPNOTSUPP))
            return IOS_UNAVAILABLE;
        handleSocketError(env, errno);
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_blockOrUnblock4(JNIEnv *env, jobject this, jboolean block, jobject fdo,
                                    jint group, jint interf, jint source)
{
#ifdef __APPLE__
    /* no IPv4 exclude-mode filtering for now */
    return IOS_UNAVAILABLE;
#else
    struct ip_mreq_source mreq_source;
    int n;
    int opt = (block) ? IP_BLOCK_SOURCE : IP_UNBLOCK_SOURCE;

#ifdef _AIX
    /* check AIX for support of source filtering */
    if (isSourceFilterSupported() != JNI_TRUE){
        return IOS_UNAVAILABLE;
    }
#endif

    mreq_source.imr_multiaddr.s_addr = htonl(group);
    mreq_source.imr_sourceaddr.s_addr = htonl(source);
    mreq_source.imr_interface.s_addr = htonl(interf);

    n = setsockopt(fdval(env,fdo), IPPROTO_IP, opt,
                   (void*)&mreq_source, sizeof(mreq_source));
    if (n < 0) {
        if (block && (errno == ENOPROTOOPT || errno == EOPNOTSUPP))
            return IOS_UNAVAILABLE;
        handleSocketError(env, errno);
    }
    return 0;
#endif
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_joinOrDrop6(JNIEnv *env, jobject this, jboolean join, jobject fdo,
                                jbyteArray group, jint index, jbyteArray source)
{
    struct ipv6_mreq mreq6;
    struct group_source_req req;
    int opt, n, optlen;
    void* optval;

    if (source == NULL) {
        COPY_INET6_ADDRESS(env, group, (jbyte*)&(mreq6.ipv6mr_multiaddr));
        mreq6.ipv6mr_interface = (int)index;
        opt = (join) ? IPV6_ADD_MEMBERSHIP : IPV6_DROP_MEMBERSHIP;
        optval = (void*)&mreq6;
        optlen = sizeof(mreq6);
    } else {
#ifdef __APPLE__
        /* no IPv6 include-mode filtering for now */
        return IOS_UNAVAILABLE;
#else
        initGroupSourceReq(env, group, index, source, &req);
        opt = (join) ? MCAST_JOIN_SOURCE_GROUP : MCAST_LEAVE_SOURCE_GROUP;
        optval = (void*)&req;
        optlen = sizeof(req);
#endif
    }

    n = setsockopt(fdval(env,fdo), IPPROTO_IPV6, opt, optval, optlen);
#ifdef __APPLE__
    // workaround macOS bug where IPV6_ADD_MEMBERSHIP fails intermittently
    if (n < 0 && errno == ENOMEM) {
        n = setsockopt(fdval(env,fdo), IPPROTO_IPV6, opt, optval, optlen);
    }
#endif

    if (n < 0) {
        if (join && (errno == ENOPROTOOPT || errno == EOPNOTSUPP))
            return IOS_UNAVAILABLE;
        handleSocketError(env, errno);
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_blockOrUnblock6(JNIEnv *env, jobject this, jboolean block, jobject fdo,
                                    jbyteArray group, jint index, jbyteArray source)
{
#ifdef __APPLE__
    /* no IPv6 exclude-mode filtering for now */
    return IOS_UNAVAILABLE;
#else
    struct group_source_req req;
    int n;
    int opt = (block) ? MCAST_BLOCK_SOURCE : MCAST_UNBLOCK_SOURCE;

    initGroupSourceReq(env, group, index, source, &req);

    n = setsockopt(fdval(env,fdo), IPPROTO_IPV6, opt,
        (void*)&req, sizeof(req));
    if (n < 0) {
        if (block && (errno == ENOPROTOOPT || errno == EOPNOTSUPP))
            return IOS_UNAVAILABLE;
        handleSocketError(env, errno);
    }
    return 0;
#endif
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_Net_setInterface4(JNIEnv* env, jobject this, jobject fdo, jint interf)
{
    struct in_addr in;
    socklen_t arglen = sizeof(struct in_addr);
    int n;

    in.s_addr = htonl(interf);

    n = setsockopt(fdval(env, fdo), IPPROTO_IP, IP_MULTICAST_IF,
                   (void*)&(in.s_addr), arglen);
    if (n < 0) {
        handleSocketError(env, errno);
    }
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_getInterface4(JNIEnv* env, jobject this, jobject fdo)
{
    struct in_addr in;
    socklen_t arglen = sizeof(struct in_addr);
    int n;

    n = getsockopt(fdval(env, fdo), IPPROTO_IP, IP_MULTICAST_IF, (void*)&in, &arglen);
    if (n < 0) {
        handleSocketError(env, errno);
        return -1;
    }
    return ntohl(in.s_addr);
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_Net_setInterface6(JNIEnv* env, jobject this, jobject fdo, jint index)
{
    int value = (jint)index;
    socklen_t arglen = sizeof(value);
    int n;

    n = setsockopt(fdval(env, fdo), IPPROTO_IPV6, IPV6_MULTICAST_IF,
                   (void*)&(index), arglen);
    if (n < 0) {
        handleSocketError(env, errno);
    }
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_getInterface6(JNIEnv* env, jobject this, jobject fdo)
{
    int index;
    socklen_t arglen = sizeof(index);
    int n;

    n = getsockopt(fdval(env, fdo), IPPROTO_IPV6, IPV6_MULTICAST_IF, (void*)&index, &arglen);
    if (n < 0) {
        handleSocketError(env, errno);
        return -1;
    }
    return (jint)index;
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_Net_shutdown(JNIEnv *env, jclass cl, jobject fdo, jint jhow)
{
    int how = (jhow == sun_nio_ch_Net_SHUT_RD) ? SHUT_RD :
        (jhow == sun_nio_ch_Net_SHUT_WR) ? SHUT_WR : SHUT_RDWR;
    if ((shutdown(fdval(env, fdo), how) < 0) && (errno != ENOTCONN))
        handleSocketError(env, errno);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_available(JNIEnv *env, jclass cl, jobject fdo)
{
    int count = 0;
    if (NET_SocketAvailable(fdval(env, fdo), &count) != 0) {
        handleSocketError(env, errno);
        return IOS_THROWN;
    }
    return (jint) count;
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_Net_poll(JNIEnv* env, jclass this, jobject fdo, jint events, jlong timeout)
{
    struct pollfd pfd;
    int rv;
    pfd.fd = fdval(env, fdo);
    pfd.events = events;
    if (timeout < -1) {
        timeout = -1;
    } else if (timeout > INT_MAX) {
        timeout = INT_MAX;
    }
    rv = poll(&pfd, 1, (int)timeout);

    if (rv >= 0) {
        return pfd.revents;
    } else if (errno == EINTR) {
        // interrupted, no events to return
        return 0;
    } else {
        handleSocketError(env, errno);
        return IOS_THROWN;
    }
}

JNIEXPORT jboolean JNICALL
Java_sun_nio_ch_Net_pollConnect(JNIEnv *env, jobject this, jobject fdo, jlong timeout)
{
    jint fd = fdval(env, fdo);
    struct pollfd poller;
    int result;

    poller.fd = fd;
    poller.events = POLLOUT;
    poller.revents = 0;
    if (timeout < -1) {
        timeout = -1;
    } else if (timeout > INT_MAX) {
        timeout = INT_MAX;
    }

    result = poll(&poller, 1, (int)timeout);

    if (result > 0) {
        int error = 0;
        socklen_t n = sizeof(int);
        errno = 0;
        result = getsockopt(fd, SOL_SOCKET, SO_ERROR, &error, &n);
        if (result < 0) {
            handleSocketError(env, errno);
            return JNI_FALSE;
        } else if (error) {
            handleSocketError(env, error);
            return JNI_FALSE;
        } else if ((poller.revents & POLLHUP) != 0) {
            handleSocketError(env, ENOTCONN);
            return JNI_FALSE;
        }
        // connected
        return JNI_TRUE;
    } else if (result == 0 || errno == EINTR) {
        return JNI_FALSE;
    } else {
        JNU_ThrowIOExceptionWithLastError(env, "poll failed");
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
    int n = send(fdval(env, fdo), (const void*)&b, 1, MSG_OOB);
    return convertReturnVal(env, n, JNI_FALSE);
}

/* Declared in nio_util.h */

jint handleSocketError(JNIEnv *env, jint errorValue)
{
    char *xn;
    switch (errorValue) {
        case EINPROGRESS:       /* Non-blocking connect */
            return 0;
#ifdef EPROTO
        case EPROTO:
            xn = JNU_JAVANETPKG "ProtocolException";
            break;
#endif
        case ECONNREFUSED:
        case ETIMEDOUT:
        case ENOTCONN:
            xn = JNU_JAVANETPKG "ConnectException";
            break;

        case EHOSTUNREACH:
            xn = JNU_JAVANETPKG "NoRouteToHostException";
            break;
        case EADDRINUSE:  /* Fall through */
        case EADDRNOTAVAIL:
        case EACCES:
            xn = JNU_JAVANETPKG "BindException";
            break;
        default:
            xn = JNU_JAVANETPKG "SocketException";
            break;
    }
    errno = errorValue;
    JNU_ThrowByNameWithLastError(env, xn, "NioSocketError");
    return IOS_THROWN;
}
