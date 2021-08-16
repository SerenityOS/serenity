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
#include <sys/types.h>
#include <netinet/in.h>
#include <netinet/in_systm.h>
#include <netinet/ip.h>
#include <netinet/ip_icmp.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>

#include "net_util.h"

#include "java_net_Inet4AddressImpl.h"

#if defined(MACOSX)
extern jobjectArray lookupIfLocalhost(JNIEnv *env, const char *hostname, jboolean includeV6);
#endif

#define SET_NONBLOCKING(fd) {       \
    int flags = fcntl(fd, F_GETFL); \
    flags |= O_NONBLOCK;            \
    fcntl(fd, F_SETFL, flags);      \
}

/*
 * Inet4AddressImpl
 */

/*
 * Class:     java_net_Inet4AddressImpl
 * Method:    getLocalHostName
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_java_net_Inet4AddressImpl_getLocalHostName(JNIEnv *env, jobject this) {
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

/*
 * Find an internet address for a given hostname. Note that this
 * code only works for addresses of type INET. The translation
 * of %d.%d.%d.%d to an address (int) occurs in java now, so the
 * String "host" shouldn't be a %d.%d.%d.%d string. The only
 * exception should be when any of the %d are out of range and
 * we fallback to a lookup.
 *
 * Class:     java_net_Inet4AddressImpl
 * Method:    lookupAllHostAddr
 * Signature: (Ljava/lang/String;)[[B
 */
JNIEXPORT jobjectArray JNICALL
Java_java_net_Inet4AddressImpl_lookupAllHostAddr(JNIEnv *env, jobject this,
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
    hints.ai_family = AF_INET;

    error = getaddrinfo(hostname, NULL, &hints, &res);

    if (error) {
#if defined(MACOSX)
        // If getaddrinfo fails try getifaddrs, see bug 8170910.
        ret = lookupIfLocalhost(env, hostname, JNI_FALSE);
        if (ret != NULL || (*env)->ExceptionCheck(env)) {
            goto cleanupAndReturn;
        }
#endif
        // report error
        NET_ThrowUnknownHostExceptionWithGaiError(env, hostname, error);
        goto cleanupAndReturn;
    } else {
        int i = 0;
        iterator = res;
        while (iterator != NULL) {
            // skip duplicates
            int skip = 0;
            struct addrinfo *iteratorNew = resNew;
            while (iteratorNew != NULL) {
                struct sockaddr_in *addr1, *addr2;
                addr1 = (struct sockaddr_in *)iterator->ai_addr;
                addr2 = (struct sockaddr_in *)iteratorNew->ai_addr;
                if (addr1->sin_addr.s_addr == addr2->sin_addr.s_addr) {
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
            }
            iterator = iterator->ai_next;
        }

        // allocate array - at this point i contains the number of addresses
        ret = (*env)->NewObjectArray(env, i, ia_class, NULL);
        if (IS_NULL(ret)) {
            goto cleanupAndReturn;
        }

        i = 0;
        iterator = resNew;
        while (iterator != NULL) {
            jobject iaObj = (*env)->NewObject(env, ia4_class, ia4_ctrID);
            if (IS_NULL(iaObj)) {
                ret = NULL;
                goto cleanupAndReturn;
            }
            setInetAddress_addr(env, iaObj, ntohl(((struct sockaddr_in *)
                                (iterator->ai_addr))->sin_addr.s_addr));
            if ((*env)->ExceptionCheck(env))
                goto cleanupAndReturn;
            setInetAddress_hostName(env, iaObj, host);
            if ((*env)->ExceptionCheck(env))
                goto cleanupAndReturn;
            (*env)->SetObjectArrayElement(env, ret, i++, iaObj);
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
 * Class:     java_net_Inet4AddressImpl
 * Method:    getHostByAddr
 * Signature: ([B)Ljava/lang/String;
 *
 * Theoretically the UnknownHostException could be enriched with gai error
 * information. But as it is silently ignored anyway, there's no need for this.
 * It's only important that either a valid hostname is returned or an
 * UnknownHostException is thrown.
 */
JNIEXPORT jstring JNICALL
Java_java_net_Inet4AddressImpl_getHostByAddr(JNIEnv *env, jobject this,
                                             jbyteArray addrArray) {
    jstring ret = NULL;
    char host[NI_MAXHOST + 1];
    jbyte caddr[4];
    jint addr;
    struct sockaddr_in sa;

    // construct a sockaddr_in structure
    memset((char *)&sa, 0, sizeof(struct sockaddr_in));
    (*env)->GetByteArrayRegion(env, addrArray, 0, 4, caddr);
    addr = ((caddr[0] << 24) & 0xff000000);
    addr |= ((caddr[1] << 16) & 0xff0000);
    addr |= ((caddr[2] << 8) & 0xff00);
    addr |= (caddr[3] & 0xff);
    sa.sin_addr.s_addr = htonl(addr);
    sa.sin_family = AF_INET;

    if (getnameinfo((struct sockaddr *)&sa, sizeof(struct sockaddr_in),
                    host, sizeof(host), NULL, 0, NI_NAMEREQD)) {
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
tcp_ping4(JNIEnv *env, SOCKETADDRESS *sa, SOCKETADDRESS *netif, jint timeout,
          jint ttl)
{
    jint fd;
    int connect_rv = -1;

    // open a TCP socket
    fd = socket(AF_INET, SOCK_STREAM, 0);
    if (fd == -1) {
        // note: if you run out of fds, you may not be able to load
        // the exception class, and get a NoClassDefFoundError instead.
        NET_ThrowNew(env, errno, "Can't create socket");
        return JNI_FALSE;
    }

    // set TTL
    if (ttl > 0) {
        setsockopt(fd, IPPROTO_IP, IP_TTL, &ttl, sizeof(ttl));
    }

    // A network interface was specified, so let's bind to it.
    if (netif != NULL) {
        if (bind(fd, &netif->sa, sizeof(struct sockaddr_in)) < 0) {
            NET_ThrowNew(env, errno, "Can't bind socket");
            close(fd);
            return JNI_FALSE;
        }
    }

    // Make the socket non blocking so we can use select/poll.
    SET_NONBLOCKING(fd);

    sa->sa4.sin_port = htons(7); // echo port
    connect_rv = NET_Connect(fd, &sa->sa, sizeof(struct sockaddr_in));

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
ping4(JNIEnv *env, jint fd, SOCKETADDRESS *sa, SOCKETADDRESS *netif,
      jint timeout, jint ttl)
{
    jint n, size = 60 * 1024, hlen, tmout2, seq = 1;
    socklen_t len;
    unsigned char sendbuf[1500], recvbuf[1500];
    struct icmp *icmp;
    struct ip *ip;
    struct sockaddr_in sa_recv;
    jchar pid;
    struct timeval tv;
    size_t plen = ICMP_ADVLENMIN + sizeof(tv);

    setsockopt(fd, SOL_SOCKET, SO_RCVBUF, &size, sizeof(size));

    // sets the ttl (max number of hops)
    if (ttl > 0) {
        setsockopt(fd, IPPROTO_IP, IP_TTL, &ttl, sizeof(ttl));
    }

    // a specific interface was specified, so let's bind the socket
    // to that interface to ensure the requests are sent only through it.
    if (netif != NULL) {
        if (bind(fd, &netif->sa, sizeof(struct sockaddr_in)) < 0) {
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
        icmp = (struct icmp *)sendbuf;
        icmp->icmp_type = ICMP_ECHO;
        icmp->icmp_code = 0;
        // let's tag the ECHO packet with our pid so we can identify it
        icmp->icmp_id = htons(pid);
        icmp->icmp_seq = htons(seq);
        seq++;
        gettimeofday(&tv, NULL);
        memcpy(icmp->icmp_data, &tv, sizeof(tv));
        icmp->icmp_cksum = 0;
        // manually calculate checksum
        icmp->icmp_cksum = in_cksum((u_short *)icmp, plen);
        // send it
        n = sendto(fd, sendbuf, plen, 0, &sa->sa, sizeof(struct sockaddr_in));
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
                if (n < (jint)sizeof(struct ip)) {
                    continue;
                }
                ip = (struct ip *)recvbuf;
                hlen = ((jint)(unsigned int)(ip->ip_hl)) << 2;
                // check if we received enough data
                if (n < (jint)(hlen + sizeof(struct icmp))) {
                    continue;
                }
                icmp = (struct icmp *)(recvbuf + hlen);
                // We did receive something, but is it what we were expecting?
                // I.E.: An ICMP_ECHO_REPLY packet with the proper PID and
                //       from the host that we are trying to determine is reachable.
                if (icmp->icmp_type == ICMP_ECHOREPLY &&
                    (ntohs(icmp->icmp_id) == pid))
                {
                    if (sa->sa4.sin_addr.s_addr == sa_recv.sin_addr.s_addr) {
                        close(fd);
                        return JNI_TRUE;
                    } else if (sa->sa4.sin_addr.s_addr == 0) {
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
 * Class:     java_net_Inet4AddressImpl
 * Method:    isReachable0
 * Signature: ([bI[bI)Z
 */
JNIEXPORT jboolean JNICALL
Java_java_net_Inet4AddressImpl_isReachable0(JNIEnv *env, jobject this,
                                            jbyteArray addrArray, jint timeout,
                                            jbyteArray ifArray, jint ttl)
{
    jbyte caddr[4];
    jint addr = 0, sz, fd;
    SOCKETADDRESS sa, inf, *netif = NULL;

    // check if address array size is 4 (IPv4 address)
    sz = (*env)->GetArrayLength(env, addrArray);
    if (sz != 4) {
      return JNI_FALSE;
    }

    // convert IP address from byte array to integer
    memset((char *)caddr, 0, sizeof(caddr));
    (*env)->GetByteArrayRegion(env, addrArray, 0, 4, caddr);
    addr = ((caddr[0] << 24) & 0xff000000);
    addr |= ((caddr[1] << 16) & 0xff0000);
    addr |= ((caddr[2] << 8) & 0xff00);
    addr |= (caddr[3] & 0xff);
    memset((char *)&sa, 0, sizeof(SOCKETADDRESS));
    sa.sa4.sin_addr.s_addr = htonl(addr);
    sa.sa4.sin_family = AF_INET;

    // If a network interface was specified, let's convert its address as well.
    if (!(IS_NULL(ifArray))) {
        memset((char *)caddr, 0, sizeof(caddr));
        (*env)->GetByteArrayRegion(env, ifArray, 0, 4, caddr);
        addr = ((caddr[0] << 24) & 0xff000000);
        addr |= ((caddr[1] << 16) & 0xff0000);
        addr |= ((caddr[2] << 8) & 0xff00);
        addr |= (caddr[3] & 0xff);
        memset((char *)&inf, 0, sizeof(SOCKETADDRESS));
        inf.sa4.sin_addr.s_addr = htonl(addr);
        inf.sa4.sin_family = AF_INET;
        netif = &inf;
    }

    // Let's try to create a RAW socket to send ICMP packets.
    // This usually requires "root" privileges, so it's likely to fail.
    fd = socket(AF_INET, SOCK_RAW, IPPROTO_ICMP);
    if (fd == -1) {
        return tcp_ping4(env, &sa, netif, timeout, ttl);
    } else {
        // It didn't fail, so we can use ICMP_ECHO requests.
        return ping4(env, fd, &sa, netif, timeout, ttl);
    }
}
