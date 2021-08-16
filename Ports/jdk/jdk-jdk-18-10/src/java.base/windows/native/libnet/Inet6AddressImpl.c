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
#include <malloc.h>

#include "net_util.h"

#include "java_net_InetAddress.h"
#include "java_net_Inet4AddressImpl.h"
#include "java_net_Inet6AddressImpl.h"

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
    char hostname[256];

    if (gethostname(hostname, sizeof(hostname)) == -1) {
        strcpy(hostname, "localhost");
    }
    return JNU_NewStringPlatform(env, hostname);
}

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
        // report error
        NET_ThrowByNameWithLastError(env, "java/net/UnknownHostException",
                                     hostname);
        goto cleanupAndReturn;
    } else {
        int i = 0, inetCount = 0, inet6Count = 0, inetIndex = 0,
            inet6Index = 0, originalIndex = 0;
        int addressPreference =
            (*env)->GetStaticIntField(env, ia_class, ia_preferIPv6AddressID);
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

    if (getnameinfo(&sa.sa, len, host, NI_MAXHOST, NULL, 0, NI_NAMEREQD)) {
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
    WSAEVENT hEvent;

    // open a TCP socket
    fd = NET_Socket(AF_INET6, SOCK_STREAM, 0);
    if (fd == SOCKET_ERROR) {
        // note: if you run out of fds, you may not be able to load
        // the exception class, and get a NoClassDefFoundError instead.
        NET_ThrowNew(env, WSAGetLastError(), "Can't create socket");
        return JNI_FALSE;
    }

    // set TTL
    if (ttl > 0) {
        setsockopt(fd, IPPROTO_IPV6, IPV6_UNICAST_HOPS, (const char *)&ttl, sizeof(ttl));
    }

    // A network interface was specified, so let's bind to it.
    if (netif != NULL) {
        if (bind(fd, &netif->sa, sizeof(struct sockaddr_in6)) < 0) {
            NET_ThrowNew(env, WSAGetLastError(), "Can't bind socket to interface");
            closesocket(fd);
            return JNI_FALSE;
        }
    }

    // Make the socket non blocking so we can use select/poll.
    hEvent = WSACreateEvent();
    WSAEventSelect(fd, hEvent, FD_READ|FD_CONNECT|FD_CLOSE);

    sa->sa6.sin6_port = htons(7); // echo port
    connect_rv = connect(fd, &sa->sa, sizeof(struct sockaddr_in6));

    // connection established or refused immediately, either way it means
    // we were able to reach the host!
    if (connect_rv == 0 || WSAGetLastError() == WSAECONNREFUSED) {
        WSACloseEvent(hEvent);
        closesocket(fd);
        return JNI_TRUE;
    }

    switch (WSAGetLastError()) {
    case WSAEHOSTUNREACH:   // Host Unreachable
    case WSAENETUNREACH:    // Network Unreachable
    case WSAENETDOWN:       // Network is down
    case WSAEPFNOSUPPORT:   // Protocol Family unsupported
        WSACloseEvent(hEvent);
        closesocket(fd);
        return JNI_FALSE;
    case WSAEWOULDBLOCK:    // this is expected as we'll probably have to wait
        break;
    default:
        NET_ThrowByNameWithLastError(env, JNU_JAVANETPKG "ConnectException",
                                     "connect failed");
        WSACloseEvent(hEvent);
        closesocket(fd);
        return JNI_FALSE;
    }

    timeout = NET_Wait(env, fd, NET_WAIT_CONNECT, timeout);
    if (timeout >= 0) {
        // connection has been established, check for error condition
        int optlen = sizeof(connect_rv);
        if (getsockopt(fd, SOL_SOCKET, SO_ERROR, (void *)&connect_rv,
                       &optlen) < 0)
        {
            connect_rv = WSAGetLastError();
        }
        if (connect_rv == 0 || connect_rv == WSAECONNREFUSED) {
            WSACloseEvent(hEvent);
            closesocket(fd);
            return JNI_TRUE;
        }
    }
    WSACloseEvent(hEvent);
    closesocket(fd);
    return JNI_FALSE;
}

/**
 * ping implementation.
 * Send a ICMP_ECHO_REQUEST packet every second until either the timeout
 * expires or a answer is received.
 * Returns true is an ECHO_REPLY is received, otherwise, false.
 */
static jboolean
ping6(JNIEnv *env, HANDLE hIcmpFile, SOCKETADDRESS *sa,
      SOCKETADDRESS *netif, jint timeout)
{
    DWORD dwRetVal = 0;
    char SendData[32] = {0};
    LPVOID ReplyBuffer = NULL;
    DWORD ReplySize = 0;
    IP_OPTION_INFORMATION ipInfo = {255, 0, 0, 0, NULL};
    SOCKETADDRESS dftNetif;

    ReplySize = sizeof(ICMPV6_ECHO_REPLY) + sizeof(SendData);
    ReplyBuffer = (VOID *)malloc(ReplySize);
    if (ReplyBuffer == NULL) {
        IcmpCloseHandle(hIcmpFile);
        NET_ThrowNew(env, -1, "Unable to allocate memory");
        return JNI_FALSE;
    }

    //define local source information
    if (netif == NULL) {
        dftNetif.sa6.sin6_addr = in6addr_any;
        dftNetif.sa6.sin6_family = AF_INET6;
        dftNetif.sa6.sin6_flowinfo = 0;
        dftNetif.sa6.sin6_port = 0;
        netif = &dftNetif;
    }

    dwRetVal = Icmp6SendEcho2(hIcmpFile,    // HANDLE IcmpHandle,
                              NULL,         // HANDLE Event,
                              NULL,         // PIO_APC_ROUTINE ApcRoutine,
                              NULL,         // PVOID ApcContext,
                              &netif->sa6,  // struct sockaddr_in6 *SourceAddress,
                              &sa->sa6,     // struct sockaddr_in6 *DestinationAddress,
                              SendData,     // LPVOID RequestData,
                              sizeof(SendData), // WORD RequestSize,
                              &ipInfo,      // PIP_OPTION_INFORMATION RequestOptions,
                              ReplyBuffer,  // LPVOID ReplyBuffer,
                              ReplySize,    // DWORD ReplySize,
                              timeout);     // DWORD Timeout

    free(ReplyBuffer);
    IcmpCloseHandle(hIcmpFile);

    if (dwRetVal == 0) { // if the call failed
        return JNI_FALSE;
    } else {
        return JNI_TRUE;
    }
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
    jint sz;
    SOCKETADDRESS sa, inf, *netif = NULL;
    HANDLE hIcmpFile;

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

    // Let's try to create an ICMP handle.
    hIcmpFile = Icmp6CreateFile();
    if (hIcmpFile == INVALID_HANDLE_VALUE) {
        int err = WSAGetLastError();
        if (err == ERROR_ACCESS_DENIED) {
            // fall back to TCP echo if access is denied to ICMP
            return tcp_ping6(env, &sa, netif, timeout, ttl);
        } else {
            NET_ThrowNew(env, err, "Unable to create ICMP file handle");
            return JNI_FALSE;
        }
    } else {
        // It didn't fail, so we can use ICMP.
        return ping6(env, hIcmpFile, &sa, netif, timeout);
    }
}
