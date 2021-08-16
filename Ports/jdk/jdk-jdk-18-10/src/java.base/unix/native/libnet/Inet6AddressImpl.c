/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <ctype.h>
#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <netinet/icmp6.h>

#if defined(_ALLBSD_SOURCE)
#include <ifaddrs.h>
#include <net/if.h>
#endif

#include "net_util.h"

#include "java_net_InetAddress.h"
#include "java_net_Inet4AddressImpl.h"
#include "java_net_Inet6AddressImpl.h"

#define SET_NONBLOCKING(fd) {       \
    int flags = fcntl(fd, F_GETFL); \
    flags |= O_NONBLOCK;            \
    fcntl(fd, F_SETFL, flags);      \
}

/*
 * Inet6AddressImpl
 */

/*
 * Class:     java_net_Inet6AddressImpl
 * Method:    getLocalHostName
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_java_net_Inet6AddressImpl_getLocalHostName(JNIEnv *env, jobject this) {
    char hostname[NI_MAXHOST + 1];

    hostname[0] = '\0';
    if (gethostname(hostname, sizeof(hostname)) != 0) {
        strcpy(hostname, "localhost");
    } else {
        // make sure string is null-terminated
        hostname[NI_MAXHOST] = '\0';
    }
    return (*env)->NewStringUTF(env, hostname);
}

#if defined(MACOSX)
/* also called from Inet4AddressImpl.c */
__private_extern__ jobjectArray
lookupIfLocalhost(JNIEnv *env, const char *hostname, jboolean includeV6)
{
    jobjectArray result = NULL;
    char myhostname[NI_MAXHOST + 1];
    struct ifaddrs *ifa = NULL;
    int familyOrder = 0;
    int count = 0, i, j;
    int addrs4 = 0, addrs6 = 0, numV4Loopbacks = 0, numV6Loopbacks = 0;
    jboolean includeLoopback = JNI_FALSE;
    jobject name;

    initInetAddressIDs(env);
    JNU_CHECK_EXCEPTION_RETURN(env, NULL);

    /* If the requested name matches this host's hostname, return IP addresses
     * from all attached interfaces. (#2844683 et al) This prevents undesired
     * PPP dialup, but may return addresses that don't actually correspond to
     * the name (if the name actually matches something in DNS etc.
     */
    myhostname[0] = '\0';
    if (gethostname(myhostname, sizeof(myhostname)) == -1) {
        /* Something went wrong, maybe networking is not setup? */
        return NULL;
    }
    myhostname[NI_MAXHOST] = '\0';

    if (strcmp(myhostname, hostname) != 0) {
        // Non-self lookup
        return NULL;
    }

    if (getifaddrs(&ifa) != 0) {
        NET_ThrowNew(env, errno, "Can't get local interface addresses");
        return NULL;
    }

    name = (*env)->NewStringUTF(env, hostname);
    if (name == NULL) {
        freeifaddrs(ifa);
        return NULL;
    }

    /* Iterate over the interfaces, and total up the number of IPv4 and IPv6
     * addresses we have. Also keep a count of loopback addresses. We need to
     * exclude them in the normal case, but return them if we don't get an IP
     * address.
     */
    struct ifaddrs *iter = ifa;
    while (iter) {
        if (iter->ifa_addr != NULL) {
            int family = iter->ifa_addr->sa_family;
            if (iter->ifa_name[0] != '\0') {
                jboolean isLoopback = iter->ifa_flags & IFF_LOOPBACK;
                if (family == AF_INET) {
                    addrs4++;
                    if (isLoopback) numV4Loopbacks++;
                } else if (family == AF_INET6 && includeV6) {
                    addrs6++;
                    if (isLoopback) numV6Loopbacks++;
                } // else we don't care, e.g. AF_LINK
            }
        }
        iter = iter->ifa_next;
    }

    if (addrs4 == numV4Loopbacks && addrs6 == numV6Loopbacks) {
        // We don't have a real IP address, just loopback. We need to include
        // loopback in our results.
        includeLoopback = JNI_TRUE;
    }

    /* Create and fill the Java array. */
    int arraySize = addrs4 + addrs6 -
        (includeLoopback ? 0 : (numV4Loopbacks + numV6Loopbacks));
    result = (*env)->NewObjectArray(env, arraySize, ia_class, NULL);
    if (!result) goto done;

    if ((*env)->GetStaticBooleanField(env, ia_class, ia_preferIPv6AddressID)) {
        i = includeLoopback ? addrs6 : (addrs6 - numV6Loopbacks);
        j = 0;
    } else {
        i = 0;
        j = includeLoopback ? addrs4 : (addrs4 - numV4Loopbacks);
    }

    // Now loop around the ifaddrs
    iter = ifa;
    while (iter != NULL) {
        if (iter->ifa_addr != NULL) {
            jboolean isLoopback = iter->ifa_flags & IFF_LOOPBACK;
            int family = iter->ifa_addr->sa_family;

            if (iter->ifa_name[0] != '\0' &&
                (family == AF_INET || (family == AF_INET6 && includeV6)) &&
                (!isLoopback || includeLoopback))
            {
                int port;
                int index = (family == AF_INET) ? i++ : j++;
                jobject o = NET_SockaddrToInetAddress(env,
                                (SOCKETADDRESS *)iter->ifa_addr, &port);
                if (!o) {
                    freeifaddrs(ifa);
                    if (!(*env)->ExceptionCheck(env))
                        JNU_ThrowOutOfMemoryError(env, "Object allocation failed");
                    return NULL;
                }
                setInetAddress_hostName(env, o, name);
                if ((*env)->ExceptionCheck(env))
                    goto done;
                (*env)->SetObjectArrayElement(env, result, index, o);
                (*env)->DeleteLocalRef(env, o);
            }
        }
        iter = iter->ifa_next;
    }

  done:
    freeifaddrs(ifa);

    return result;
}
#endif

/*
 * Class:     java_net_Inet6AddressImpl
 * Method:    lookupAllHostAddr
 * Signature: (Ljava/lang/String;)[[B
 */
JNIEXPORT jobjectArray JNICALL
Java_java_net_Inet6AddressImpl_lookupAllHostAddr(JNIEnv *env, jobject this,
                                                 jstring host) {
    jobjectArray ret = NULL;
    const char *hostname;
    int error = 0;
    struct addrinfo hints, *res = NULL, *resNew = NULL, *last = NULL,
        *iterator;

    initInetAddressIDs(env);
    JNU_CHECK_EXCEPTION_RETURN(env, NULL);

    if (IS_NULL(host)) {
        JNU_ThrowNullPointerException(env, "host argument is null");
        return NULL;
    }
    hostname = JNU_GetStringPlatformChars(env, host, JNI_FALSE);
    CHECK_NULL_RETURN(hostname, NULL);

    // try once, with our static buffer
    memset(&hints, 0, sizeof(hints));
    hints.ai_flags = AI_CANONNAME;
    hints.ai_family = AF_UNSPEC;

    error = getaddrinfo(hostname, NULL, &hints, &res);

    if (error) {
#if defined(MACOSX)
        // if getaddrinfo fails try getifaddrs
        ret = lookupIfLocalhost(env, hostname, JNI_TRUE);
        if (ret != NULL || (*env)->ExceptionCheck(env)) {
            goto cleanupAndReturn;
        }
#endif
        // report error
        NET_ThrowUnknownHostExceptionWithGaiError(env, hostname, error);
        goto cleanupAndReturn;
    } else {
        int i = 0, inetCount = 0, inet6Count = 0, inetIndex = 0,
            inet6Index = 0, originalIndex = 0;
        int addressPreference =
            (*env)->GetStaticIntField(env, ia_class, ia_preferIPv6AddressID);;
        iterator = res;
        while (iterator != NULL) {
            // skip duplicates
            int skip = 0;
            struct addrinfo *iteratorNew = resNew;
            while (iteratorNew != NULL) {
                if (iterator->ai_family == iteratorNew->ai_family &&
                    iterator->ai_addrlen == iteratorNew->ai_addrlen) {
                    if (iteratorNew->ai_family == AF_INET) { /* AF_INET */
                        struct sockaddr_in *addr1, *addr2;
                        addr1 = (struct sockaddr_in *)iterator->ai_addr;
                        addr2 = (struct sockaddr_in *)iteratorNew->ai_addr;
                        if (addr1->sin_addr.s_addr == addr2->sin_addr.s_addr) {
                            skip = 1;
                            break;
                        }
                    } else {
                        int t;
                        struct sockaddr_in6 *addr1, *addr2;
                        addr1 = (struct sockaddr_in6 *)iterator->ai_addr;
                        addr2 = (struct sockaddr_in6 *)iteratorNew->ai_addr;

                        for (t = 0; t < 16; t++) {
                            if (addr1->sin6_addr.s6_addr[t] !=
                                addr2->sin6_addr.s6_addr[t]) {
                                break;
                            }
                        }
                        if (t < 16) {
                            iteratorNew = iteratorNew->ai_next;
                            continue;
                        } else {
                            skip = 1;
                            break;
                        }
                    }
                } else if (iterator->ai_family != AF_INET &&
                           iterator->ai_family != AF_INET6) {
                    // we can't handle other family types
                    skip = 1;
                    break;
                }
                iteratorNew = iteratorNew->ai_next;
            }

            if (!skip) {
                struct addrinfo *next
                    = (struct addrinfo *)malloc(sizeof(struct addrinfo));
                if (!next) {
                    JNU_ThrowOutOfMemoryError(env, "Native heap allocation failed");
                    ret = NULL;
                    goto cleanupAndReturn;
                }
                memcpy(next, iterator, sizeof(struct addrinfo));
                next->ai_next = NULL;
                if (resNew == NULL) {
                    resNew = next;
                } else {
                    last->ai_next = next;
                }
                last = next;
                i++;
                if (iterator->ai_family == AF_INET) {
                    inetCount++;
                } else if (iterator->ai_family == AF_INET6) {
                    inet6Count++;
                }
            }
            iterator = iterator->ai_next;
        }

        // allocate array - at this point i contains the number of addresses
        ret = (*env)->NewObjectArray(env, i, ia_class, NULL);
        if (IS_NULL(ret)) {
            /* we may have memory to free at the end of this */
            goto cleanupAndReturn;
        }

        if (addressPreference == java_net_InetAddress_PREFER_IPV6_VALUE) {
            inetIndex = inet6Count;
            inet6Index = 0;
        } else if (addressPreference == java_net_InetAddress_PREFER_IPV4_VALUE) {
            inetIndex = 0;
            inet6Index = inetCount;
        } else if (addressPreference == java_net_InetAddress_PREFER_SYSTEM_VALUE) {
            inetIndex = inet6Index = originalIndex = 0;
        }

        iterator = resNew;
        while (iterator != NULL) {
            if (iterator->ai_family == AF_INET) {
                jobject iaObj = (*env)->NewObject(env, ia4_class, ia4_ctrID);
                if (IS_NULL(iaObj)) {
                    ret = NULL;
                    goto cleanupAndReturn;
                }
                setInetAddress_addr(env, iaObj, ntohl(((struct sockaddr_in*)iterator->ai_addr)->sin_addr.s_addr));
                if ((*env)->ExceptionCheck(env))
                    goto cleanupAndReturn;
                setInetAddress_hostName(env, iaObj, host);
                if ((*env)->ExceptionCheck(env))
                    goto cleanupAndReturn;
                (*env)->SetObjectArrayElement(env, ret, (inetIndex | originalIndex), iaObj);
                inetIndex++;
            } else if (iterator->ai_family == AF_INET6) {
                jint scope = 0;
                jboolean ret1;
                jobject iaObj = (*env)->NewObject(env, ia6_class, ia6_ctrID);
                if (IS_NULL(iaObj)) {
                    ret = NULL;
                    goto cleanupAndReturn;
                }
                ret1 = setInet6Address_ipaddress(env, iaObj, (char *)&(((struct sockaddr_in6*)iterator->ai_addr)->sin6_addr));
                if (ret1 == JNI_FALSE) {
                    ret = NULL;
                    goto cleanupAndReturn;
                }
                scope = ((struct sockaddr_in6 *)iterator->ai_addr)->sin6_scope_id;
                if (scope != 0) { // zero is default value, no need to set
                    setInet6Address_scopeid(env, iaObj, scope);
                }
                setInetAddress_hostName(env, iaObj, host);
                if ((*env)->ExceptionCheck(env))
                    goto cleanupAndReturn;
                (*env)->SetObjectArrayElement(env, ret, (inet6Index | originalIndex), iaObj);
                inet6Index++;
            }
            if (addressPreference == java_net_InetAddress_PREFER_SYSTEM_VALUE) {
                originalIndex++;
                inetIndex = inet6Index = 0;
            }
            iterator = iterator->ai_next;
        }
    }
cleanupAndReturn:
    JNU_ReleaseStringPlatformChars(env, host, hostname);
    while (resNew != NULL) {
        last = resNew;
        resNew = resNew->ai_next;
        free(last);
    }
    if (res != NULL) {
        freeaddrinfo(res);
    }
    return ret;
}

/*
 * Class:     java_net_Inet6AddressImpl
 * Method:    getHostByAddr
 * Signature: ([B)Ljava/lang/String;
 *
 * Theoretically the UnknownHostException could be enriched with gai error
 * information. But as it is silently ignored anyway, there's no need for this.
 * It's only important that either a valid hostname is returned or an
 * UnknownHostException is thrown.
 */
JNIEXPORT jstring JNICALL
Java_java_net_Inet6AddressImpl_getHostByAddr(JNIEnv *env, jobject this,
                                             jbyteArray addrArray) {
    jstring ret = NULL;
    char host[NI_MAXHOST + 1];
    int len = 0;
    jbyte caddr[16];
    SOCKETADDRESS sa;

    memset((void *)&sa, 0, sizeof(SOCKETADDRESS));

    // construct a sockaddr_in structure (AF_INET or AF_INET6)
    if ((*env)->GetArrayLength(env, addrArray) == 4) {
        jint addr;
        (*env)->GetByteArrayRegion(env, addrArray, 0, 4, caddr);
        addr = ((caddr[0] << 24) & 0xff000000);
        addr |= ((caddr[1] << 16) & 0xff0000);
        addr |= ((caddr[2] << 8) & 0xff00);
        addr |= (caddr[3] & 0xff);
        sa.sa4.sin_addr.s_addr = htonl(addr);
        sa.sa4.sin_family = AF_INET;
        len = sizeof(struct sockaddr_in);
    } else {
        (*env)->GetByteArrayRegion(env, addrArray, 0, 16, caddr);
        memcpy((void *)&sa.sa6.sin6_addr, caddr, sizeof(struct in6_addr));
        sa.sa6.sin6_family = AF_INET6;
        len = sizeof(struct sockaddr_in6);
    }

    if (getnameinfo(&sa.sa, len, host, sizeof(host), NULL, 0, NI_NAMEREQD)) {
        JNU_ThrowByName(env, "java/net/UnknownHostException", NULL);
    } else {
        ret = (*env)->NewStringUTF(env, host);
        if (ret == NULL) {
            JNU_ThrowByName(env, "java/net/UnknownHostException", NULL);
        }
    }

    return ret;
}

/**
 * ping implementation using tcp port 7 (echo)
 */
static jboolean
tcp_ping6(JNIEnv *env, SOCKETADDRESS *sa, SOCKETADDRESS *netif, jint timeout,
          jint ttl)
{
    jint fd;
    int connect_rv = -1;

    // open a TCP socket
    fd = socket(AF_INET6, SOCK_STREAM, 0);
    if (fd == -1) {
        // note: if you run out of fds, you may not be able to load
        // the exception class, and get a NoClassDefFoundError instead.
        NET_ThrowNew(env, errno, "Can't create socket");
        return JNI_FALSE;
    }

    // set TTL
    if (ttl > 0) {
        setsockopt(fd, IPPROTO_IPV6, IPV6_UNICAST_HOPS, &ttl, sizeof(ttl));
    }

    // A network interface was specified, so let's bind to it.
    if (netif != NULL) {
        if (bind(fd, &netif->sa, sizeof(struct sockaddr_in6)) <0) {
            NET_ThrowNew(env, errno, "Can't bind socket");
            close(fd);
            return JNI_FALSE;
        }
    }

    // Make the socket non blocking so we can use select/poll.
    SET_NONBLOCKING(fd);

    sa->sa6.sin6_port = htons(7); // echo port
    connect_rv = NET_Connect(fd, &sa->sa, sizeof(struct sockaddr_in6));

    // connection established or refused immediately, either way it means
    // we were able to reach the host!
    if (connect_rv == 0 || errno == ECONNREFUSED) {
        close(fd);
        return JNI_TRUE;
    }

    switch (errno) {
    case ENETUNREACH:   // Network Unreachable
    case EAFNOSUPPORT:  // Address Family not supported
    case EADDRNOTAVAIL: // address is not available on the remote machine
#if defined(__linux__) || defined(_AIX)
        // On some Linux versions, when a socket is bound to the loopback
        // interface, connect will fail and errno will be set to EINVAL
        // or EHOSTUNREACH.  When that happens, don't throw an exception,
        // just return false.
    case EINVAL:
    case EHOSTUNREACH:  // No route to host
#endif
        close(fd);
        return JNI_FALSE;
    case EINPROGRESS:   // this is expected as we'll probably have to wait
        break;
    default:
        NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "ConnectException",
                                     "connect failed");
        close(fd);
        return JNI_FALSE;
    }

    timeout = NET_Wait(env, fd, NET_WAIT_CONNECT, timeout);
    if (timeout >= 0) {
        // connection has been established, check for error condition
        socklen_t optlen = (socklen_t)sizeof(connect_rv);
        if (getsockopt(fd, SOL_SOCKET, SO_ERROR, (void*)&connect_rv,
                       &optlen) <0)
        {
            connect_rv = errno;
        }
        if (connect_rv == 0 || connect_rv == ECONNREFUSED) {
            close(fd);
            return JNI_TRUE;
        }
    }
    close(fd);
    return JNI_FALSE;
}

/**
 * ping implementation.
 * Send an ICMP_ECHO_REQUEST packet every second until either the timeout
 * expires or an answer is received.
 * Returns true if an ECHO_REPLY is received, false otherwise.
 */
static jboolean
ping6(JNIEnv *env, jint fd, SOCKETADDRESS *sa, SOCKETADDRESS *netif,
      jint timeout, jint ttl)
{
    jint n, size = 60 * 1024, tmout2, seq = 1;
    socklen_t len;
    unsigned char sendbuf[1500], recvbuf[1500];
    struct icmp6_hdr *icmp6;
    struct sockaddr_in6 sa_recv;
    jchar pid;
    struct timeval tv;
    size_t plen = sizeof(struct icmp6_hdr) + sizeof(tv);

#if defined(__linux__)
    /**
     * For some strange reason, the linux kernel won't calculate the
     * checksum of ICMPv6 packets unless you set this socket option
     */
    int csum_offset = 2;
    setsockopt(fd, SOL_RAW, IPV6_CHECKSUM, &csum_offset, sizeof(int));
#endif

    setsockopt(fd, SOL_SOCKET, SO_RCVBUF, &size, sizeof(size));

    // sets the ttl (max number of hops)
    if (ttl > 0) {
        setsockopt(fd, IPPROTO_IPV6, IPV6_UNICAST_HOPS, &ttl, sizeof(ttl));
    }

    // a specific interface was specified, so let's bind the socket
    // to that interface to ensure the requests are sent only through it.
    if (netif != NULL) {
        if (bind(fd, &netif->sa, sizeof(struct sockaddr_in6)) <0) {
            NET_ThrowNew(env, errno, "Can't bind socket");
            close(fd);
            return JNI_FALSE;
        }
    }

    // icmp_id is a 16 bit data type, therefore down cast the pid
    pid = (jchar)getpid();

    // Make the socket non blocking so we can use select
    SET_NONBLOCKING(fd);
    do {
        // create the ICMP request
        icmp6 = (struct icmp6_hdr *)sendbuf;
        icmp6->icmp6_type = ICMP6_ECHO_REQUEST;
        icmp6->icmp6_code = 0;
        // let's tag the ECHO packet with our pid so we can identify it
        icmp6->icmp6_id = htons(pid);
        icmp6->icmp6_seq = htons(seq);
        seq++;
        gettimeofday(&tv, NULL);
        memcpy(sendbuf + sizeof(struct icmp6_hdr), &tv, sizeof(tv));
        icmp6->icmp6_cksum = 0;
        // send it
        n = sendto(fd, sendbuf, plen, 0, &sa->sa, sizeof(struct sockaddr_in6));
        if (n < 0 && errno != EINPROGRESS) {
#if defined(__linux__)
            /*
             * On some Linux versions, when a socket is bound to the loopback
             * interface, sendto will fail and errno will be set to
             * EINVAL or EHOSTUNREACH. When that happens, don't throw an
             * exception, just return false.
             */
            if (errno != EINVAL && errno != EHOSTUNREACH) {
                NET_ThrowNew(env, errno, "Can't send ICMP packet");
            }
#else
            NET_ThrowNew(env, errno, "Can't send ICMP packet");
#endif
            close(fd);
            return JNI_FALSE;
        }

        tmout2 = timeout > 1000 ? 1000 : timeout;
        do {
            tmout2 = NET_Wait(env, fd, NET_WAIT_READ, tmout2);
            if (tmout2 >= 0) {
                len = sizeof(sa_recv);
                n = recvfrom(fd, recvbuf, sizeof(recvbuf), 0,
                             (struct sockaddr *)&sa_recv, &len);
                // check if we received enough data
                if (n < (jint)sizeof(struct icmp6_hdr)) {
                    continue;
                }
                icmp6 = (struct icmp6_hdr *)recvbuf;
                // We did receive something, but is it what we were expecting?
                // I.E.: An ICMP6_ECHO_REPLY packet with the proper PID and
                //       from the host that we are trying to determine is reachable.
                if (icmp6->icmp6_type == ICMP6_ECHO_REPLY &&
                    (ntohs(icmp6->icmp6_id) == pid))
                {
                    if (NET_IsEqual((jbyte *)&sa->sa6.sin6_addr,
                                    (jbyte *)&sa_recv.sin6_addr)) {
                        close(fd);
                        return JNI_TRUE;
                    } else if (NET_IsZeroAddr((jbyte *)&sa->sa6.sin6_addr)) {
                        close(fd);
                        return JNI_TRUE;
                    }
                }
            }
        } while (tmout2 > 0);
        timeout -= 1000;
    } while (timeout > 0);
    close(fd);
    return JNI_FALSE;
}

/*
 * Class:     java_net_Inet6AddressImpl
 * Method:    isReachable0
 * Signature: ([BII[BII)Z
 */
JNIEXPORT jboolean JNICALL
Java_java_net_Inet6AddressImpl_isReachable0(JNIEnv *env, jobject this,
                                            jbyteArray addrArray, jint scope,
                                            jint timeout, jbyteArray ifArray,
                                            jint ttl, jint if_scope)
{
    jbyte caddr[16];
    jint sz, fd;
    SOCKETADDRESS sa, inf, *netif = NULL;

    // If IPv6 is not enabled, then we can't reach an IPv6 address, can we?
    // Actually, we probably shouldn't even get here.
    if (!ipv6_available()) {
        return JNI_FALSE;
    }

    // If it's an IPv4 address, ICMP won't work with IPv4 mapped address,
    // therefore, let's delegate to the Inet4Address method.
    sz = (*env)->GetArrayLength(env, addrArray);
    if (sz == 4) {
        return Java_java_net_Inet4AddressImpl_isReachable0(env, this,
                                                           addrArray, timeout,
                                                           ifArray, ttl);
    }

    // load address to SOCKETADDRESS
    memset((char *)caddr, 0, 16);
    (*env)->GetByteArrayRegion(env, addrArray, 0, 16, caddr);
    memset((char *)&sa, 0, sizeof(SOCKETADDRESS));
    memcpy((void *)&sa.sa6.sin6_addr, caddr, sizeof(struct in6_addr));
    sa.sa6.sin6_family = AF_INET6;
    if (scope > 0) {
        sa.sa6.sin6_scope_id = scope;
    }

    // load network interface address to SOCKETADDRESS, if specified
    if (!(IS_NULL(ifArray))) {
        memset((char *)caddr, 0, 16);
        (*env)->GetByteArrayRegion(env, ifArray, 0, 16, caddr);
        memset((char *)&inf, 0, sizeof(SOCKETADDRESS));
        memcpy((void *)&inf.sa6.sin6_addr, caddr, sizeof(struct in6_addr));
        inf.sa6.sin6_family = AF_INET6;
        inf.sa6.sin6_scope_id = if_scope;
        netif = &inf;
    }

    // Let's try to create a RAW socket to send ICMP packets.
    // This usually requires "root" privileges, so it's likely to fail.
    fd = socket(AF_INET6, SOCK_RAW, IPPROTO_ICMPV6);
    if (fd == -1) {
        return tcp_ping6(env, &sa, netif, timeout, ttl);
    } else {
        // It didn't fail, so we can use ICMP_ECHO requests.
        return ping6(env, fd, &sa, netif, timeout, ttl);
    }
}
