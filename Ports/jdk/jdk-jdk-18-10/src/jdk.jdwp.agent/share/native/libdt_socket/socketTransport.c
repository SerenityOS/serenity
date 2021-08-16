/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <stdlib.h>
#include <ctype.h>

#include "jni.h"
#include "jdwpTransport.h"
#include "sysSocket.h"

#ifdef _WIN32
 #include <winsock2.h>
 #include <ws2tcpip.h>
 #include <iphlpapi.h>
#else
 #include <arpa/inet.h>
 #include <sys/socket.h>
 #include <net/if.h>
#endif

/*
 * The Socket Transport Library.
 *
 * This module is an implementation of the Java Debug Wire Protocol Transport
 * Service Provider Interface - see src/share/javavm/export/jdwpTransport.h.
 */

static int serverSocketFD = -1;
static int socketFD = -1;
static jdwpTransportCallback *callback;
static JavaVM *jvm;
static int tlsIndex;
static jboolean initialized;
static struct jdwpTransportNativeInterface_ interface;
static jdwpTransportEnv single_env = (jdwpTransportEnv)&interface;

#define RETURN_ERROR(err, msg) \
        if (1==1) { \
            setLastError(err, msg); \
            return err; \
        }

#define RETURN_IO_ERROR(msg)    RETURN_ERROR(JDWPTRANSPORT_ERROR_IO_ERROR, msg);

#define RETURN_RECV_ERROR(n) \
        if (n == 0) { \
            RETURN_ERROR(JDWPTRANSPORT_ERROR_IO_ERROR, "premature EOF"); \
        } else { \
            RETURN_IO_ERROR("recv error"); \
        }

#define MAX_DATA_SIZE 1000

static jint recv_fully(int, char *, int);
static jint send_fully(int, char *, int);

/* version >= JDWPTRANSPORT_VERSION_1_1 */
typedef struct {
    /* subnet and mask are stored as IPv6 addresses, IPv4 is stored as mapped IPv6 */
    struct in6_addr subnet;
    struct in6_addr netmask;
} AllowedPeerInfo;

#define STR(x) #x
#define MAX_PEER_ENTRIES 32
#define MAX_PEERS_STR STR(MAX_PEER_ENTRIES)
static AllowedPeerInfo _peers[MAX_PEER_ENTRIES];
static int _peers_cnt = 0;


static int allowOnlyIPv4 = 0;                  // reflects "java.net.preferIPv4Stack" sys. property
static int preferredAddressFamily = AF_INET;   // "java.net.preferIPv6Addresses"

/*
 * Record the last error for this thread.
 */
static void
setLastError(jdwpTransportError err, char *newmsg) {
    char buf[255];
    char *msg;

    /* get any I/O first in case any system calls override errno */
    if (err == JDWPTRANSPORT_ERROR_IO_ERROR) {
        dbgsysGetLastIOError(buf, sizeof(buf));
    }

    msg = (char *)dbgsysTlsGet(tlsIndex);
    if (msg != NULL) {
        (*callback->free)(msg);
    }

    if (err == JDWPTRANSPORT_ERROR_IO_ERROR) {
        char *join_str = ": ";
        int msg_len = (int)strlen(newmsg) + (int)strlen(join_str) +
                      (int)strlen(buf) + 3;
        msg = (*callback->alloc)(msg_len);
        if (msg != NULL) {
            strcpy(msg, newmsg);
            strcat(msg, join_str);
            strcat(msg, buf);
        }
    } else {
        msg = (*callback->alloc)((int)strlen(newmsg)+1);
        if (msg != NULL) {
            strcpy(msg, newmsg);
        }
    }

    dbgsysTlsPut(tlsIndex, msg);
}

/*
 * Return the last error for this thread (may be NULL)
 */
static char*
getLastError() {
    return (char *)dbgsysTlsGet(tlsIndex);
}

/* Set options common to client and server sides */
static jdwpTransportError
setOptionsCommon(int domain, int fd)
{
    jvalue dontcare;
    int err;

    if (domain == AF_INET6) {
        int off = 0;
        // make the socket a dual mode socket
        // this may fail if IPv4 is not supported - it's ok
        setsockopt(fd, IPPROTO_IPV6, IPV6_V6ONLY, (char *)&off, sizeof(off));
    }

    dontcare.i = 0;  /* keep compiler happy */
    err = dbgsysSetSocketOption(fd, TCP_NODELAY, JNI_TRUE, dontcare);
    if (err < 0) {
        RETURN_IO_ERROR("setsockopt TCPNODELAY failed");
    }

    return JDWPTRANSPORT_ERROR_NONE;
}

/* Set the SO_REUSEADDR option */
static jdwpTransportError
setReuseAddrOption(int fd)
{
    jvalue dontcare;
    int err;

    dontcare.i = 0;  /* keep compiler happy */

    err = dbgsysSetSocketOption(fd, SO_REUSEADDR, JNI_TRUE, dontcare);
    if (err < 0) {
        RETURN_IO_ERROR("setsockopt SO_REUSEADDR failed");
    }

    return JDWPTRANSPORT_ERROR_NONE;
}

static jdwpTransportError
handshake(int fd, jlong timeout) {
    const char *hello = "JDWP-Handshake";
    char b[16];
    int rv, helloLen, received;

    if (timeout > 0) {
        dbgsysConfigureBlocking(fd, JNI_FALSE);
    }
    helloLen = (int)strlen(hello);
    received = 0;
    while (received < helloLen) {
        int n;
        char *buf;
        if (timeout > 0) {
            rv = dbgsysPoll(fd, JNI_TRUE, JNI_FALSE, (long)timeout);
            if (rv <= 0) {
                setLastError(0, "timeout during handshake");
                return JDWPTRANSPORT_ERROR_IO_ERROR;
            }
        }
        buf = b;
        buf += received;
        n = recv_fully(fd, buf, helloLen-received);
        if (n == 0) {
            setLastError(0, "handshake failed - connection prematurally closed");
            return JDWPTRANSPORT_ERROR_IO_ERROR;
        }
        if (n < 0) {
            RETURN_IO_ERROR("recv failed during handshake");
        }
        received += n;
    }
    if (timeout > 0) {
        dbgsysConfigureBlocking(fd, JNI_TRUE);
    }
    if (strncmp(b, hello, received) != 0) {
        char msg[80+2*16];
        b[received] = '\0';
        /*
         * We should really use snprintf here but it's not available on Windows.
         * We can't use jio_snprintf without linking the transport against the VM.
         */
        sprintf(msg, "handshake failed - received >%s< - expected >%s<", b, hello);
        setLastError(0, msg);
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    if (send_fully(fd, (char*)hello, helloLen) != helloLen) {
        RETURN_IO_ERROR("send failed during handshake");
    }
    return JDWPTRANSPORT_ERROR_NONE;
}

static int
getPortNumber(const char *s_port) {
    u_long n;
    char *eptr;

    if (*s_port == 0) {
        // bad address - colon with no port number in parameters
        return -1;
    }

    n = strtoul(s_port, &eptr, 10);
    if (eptr != s_port + strlen(s_port)) {
        // incomplete conversion - port number contains non-digit
        return -1;
    }

    if (n > (u_short) -1) {
        // check that value supplied by user is less than
        // maximum possible u_short value (65535) and
        // will not be truncated later.
        return -1;
    }

    return n;
}

static unsigned short getPort(struct sockaddr *sa)
{
    return dbgsysNetworkToHostShort(sa->sa_family == AF_INET
                                    ? (((struct sockaddr_in*)sa)->sin_port)
                                    : (((struct sockaddr_in6*)sa)->sin6_port));
}

/*
 * Parses scope id.
 * Scope id is ulong on Windows, uint32 on unix, so returns long which can be cast to uint32.
 * On error sets last error and returns -1.
 */
static long parseScopeId(const char *str) {
    // try to handle scope as interface name
    unsigned long scopeId = if_nametoindex(str);
    if (scopeId == 0) {
        // try to parse integer value
        char *end;
        scopeId = strtoul(str, &end, 10);
        if (*end != '\0') {
            setLastError(JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT, "failed to parse scope");
            return -1;
        }
    }
    // ensure parsed value is in uint32 range
    if (scopeId > 0xFFFFFFFF) {
        setLastError(JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT, "scope is out of range");
        return -1;
    }
    return (long)scopeId;
}

/*
 * Wrapper for dbgsysGetAddrInfo (getaddrinfo).
 * Handles enclosing square brackets and scopes.
 */
static jdwpTransportError
getAddrInfo(const char *hostname, size_t hostnameLen,
            const char *service,
            const struct addrinfo *hints,
            struct addrinfo **result)
{
    int err = 0;
    char *buffer = NULL;
    long scopeId = 0;

    if (hostname != NULL) {
        char *scope = NULL;
        // skip surrounding
        if (hostnameLen > 2 && hostname[0] == '[' && hostname[hostnameLen - 1] == ']') {
            hostname++;
            hostnameLen -= 2;
        }
        buffer = (*callback->alloc)((int)hostnameLen + 1);
        if (buffer == NULL) {
            RETURN_ERROR(JDWPTRANSPORT_ERROR_OUT_OF_MEMORY, "out of memory");
        }
        memcpy(buffer, hostname, hostnameLen);
        buffer[hostnameLen] = '\0';

        scope = strchr(buffer, '%');
        if (scope != NULL) {
            // drop scope from the address
            *scope = '\0';
            // and parse the value
            scopeId = parseScopeId(scope + 1);
            if (scopeId < 0) {
                (*callback->free)(buffer);
                return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
            }
        }
    }

    err = dbgsysGetAddrInfo(buffer, service, hints, result);

    if (buffer != NULL) {
        (*callback->free)(buffer);
    }
    if (err != 0) {
        setLastError(err, "getaddrinfo: failed to parse address");
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    }

    if (scopeId > 0) {
        if ((*result)->ai_family != AF_INET6) {
            RETURN_ERROR(JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT, "IPv4 address cannot contain scope");
        }

        ((struct sockaddr_in6 *)((*result)->ai_addr))->sin6_scope_id = (uint32_t)scopeId;
    }

    return JDWPTRANSPORT_ERROR_NONE;
}

/*
 * Result must be released with dbgsysFreeAddrInfo.
 */
static jdwpTransportError
parseAddress(const char *address, struct addrinfo **result) {
    const char *colon;
    size_t hostnameLen;
    const char *port;
    struct addrinfo hints;

    *result = NULL;

    /* check for host:port or port */
    colon = strrchr(address, ':');
    port = (colon == NULL ? address : colon + 1);

    /* ensure the port is valid (getaddrinfo allows port to be empty) */
    if (getPortNumber(port) < 0) {
        RETURN_ERROR(JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT, "invalid port number specified");
    }

    memset (&hints, 0, sizeof(hints));
    hints.ai_family = allowOnlyIPv4 ? AF_INET : AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;
    hints.ai_flags = AI_NUMERICSERV;    // port must be a number

    hostnameLen = (colon == NULL ? 0 : colon - address);
    if (hostnameLen == 0) {
        /* no hostname - use localhost address (pass NULL to getaddrinfo) */
        address = NULL;
    } else  if (*address == '*' && hostnameLen == 1) {
        /* *:port - listen on all interfaces
         * use IPv6 socket (to accept IPv6 and mapped IPv4),
         * pass hostname == NULL to getaddrinfo.
         */
        hints.ai_family = allowOnlyIPv4 ? AF_INET : AF_INET6;
        hints.ai_flags |= AI_PASSIVE | (allowOnlyIPv4 ? 0 : AI_V4MAPPED | AI_ALL);
        address = NULL;
    }

    return getAddrInfo(address, hostnameLen, port, &hints, result);
}

/*
 * Input is sockaddr just because all clients have it.
 */
static void convertIPv4ToIPv6(const struct sockaddr *addr4, struct in6_addr *addr6) {
    // Implement in a platform-independent way.
    // Spec requires in_addr has s_addr member, in6_addr has s6_addr[16] member.
    struct in_addr *a4 = &(((struct sockaddr_in*)addr4)->sin_addr);
    memset(addr6, 0, sizeof(*addr6));   // for safety

    // Mapped address contains 80 zero bits, then 16 "1" bits, then IPv4 address (4 bytes).
    addr6->s6_addr[10] = addr6->s6_addr[11] = 0xFF;
    memcpy(&(addr6->s6_addr[12]), &(a4->s_addr), 4);
}

/*
 * Parses address (IPv4 or IPv6), fills in result by parsed address.
 * For IPv4 mapped IPv6 is returned in result, isIPv4 is set.
 */
static jdwpTransportError
parseAllowedAddr(const char *buffer, struct in6_addr *result, int *isIPv4) {
    struct addrinfo hints;
    struct addrinfo *addrInfo = NULL;
    jdwpTransportError err;

    /*
     * To parse both IPv4 and IPv6 need to specify AF_UNSPEC family
     * (with AF_INET6 IPv4 addresses are not parsed even with AI_V4MAPPED and AI_ALL flags).
     */
    memset (&hints, 0, sizeof(hints));
    hints.ai_family = AF_UNSPEC;            // IPv6 or mapped IPv4
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;
    hints.ai_flags = AI_NUMERICHOST;        // only numeric addresses, no resolution

    err = getAddrInfo(buffer, strlen(buffer), NULL, &hints, &addrInfo);

    if (err != JDWPTRANSPORT_ERROR_NONE) {
        return err;
    }

    if (addrInfo->ai_family == AF_INET6) {
        memcpy(result, &(((struct sockaddr_in6 *)(addrInfo->ai_addr))->sin6_addr), sizeof(*result));
        *isIPv4 = 0;
    } else {    // IPv4 address - convert to mapped IPv6
        struct in6_addr addr6;
        convertIPv4ToIPv6(addrInfo->ai_addr, &addr6);
        memcpy(result, &addr6, sizeof(*result));
        *isIPv4 = 1;
    }

    dbgsysFreeAddrInfo(addrInfo);

    return JDWPTRANSPORT_ERROR_NONE;
}

/*
 * Parses prefix length from buffer (integer value), fills in result with corresponding net mask.
 * For IPv4 (isIPv4 is set), maximum prefix length is 32 bit, for IPv6 - 128 bit.
 */
static jdwpTransportError
parseAllowedMask(const char *buffer, int isIPv4, struct in6_addr *result) {
    int prefixLen = 0;
    int maxValue = isIPv4 ? 32 : 128;
    int i;

    do {
        if (*buffer < '0' || *buffer > '9') {
            return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
        }
        prefixLen = prefixLen * 10 + (*buffer - '0');
        if (prefixLen > maxValue) {  // avoid overflow
            return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
        }
        buffer++;
    } while (*buffer != '\0');

    if (isIPv4) {
        // IPv4 are stored as mapped IPv6, prefixLen needs to be converted too
        prefixLen += 96;
    }

    if (prefixLen == 0) {
        return JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
    }

    // generate mask for prefix length
    memset(result, 0, sizeof(*result));

    // prefixLen <= 128, so we won't go over result's size
    for (i = 0; prefixLen > 0; i++, prefixLen -= 8) {
        if (prefixLen >= 8) {
            // set the whole byte
            result->s6_addr[i] = 0xFF;
        } else {
            // set only "prefixLen" bits
            result->s6_addr[i] = (char)(0xFF << (8 - prefixLen));
        }
    }

    return JDWPTRANSPORT_ERROR_NONE;
}

/*
 * Internal implementation of parseAllowedPeers (requires writable buffer).
 */
static jdwpTransportError
parseAllowedPeersInternal(char *buffer) {
    char *next;
    int isIPv4 = 0;

    do {
        char *mask = NULL;
        char *endOfAddr = strpbrk(buffer, "/+");
        if (endOfAddr == NULL) {
            // this is the last address and there is no prefix length
            next = NULL;
        } else {
            next = endOfAddr + 1;
            if (*endOfAddr == '/') {
                // mask (prefix length) presents
                char *endOfMask = strchr(next, '+');
                mask = next;
                if (endOfMask == NULL) {
                    // no more addresses
                    next = NULL;
                } else {
                    next = endOfMask + 1;
                    *endOfMask = '\0';
                }
            }
            *endOfAddr = '\0';
        }

        // parse subnet address (IPv4 is stored as mapped IPv6)
        if (parseAllowedAddr(buffer, &(_peers[_peers_cnt].subnet), &isIPv4) != JDWPTRANSPORT_ERROR_NONE) {
            _peers_cnt = 0;
            fprintf(stderr, "Error in allow option: '%s'\n", buffer);
            RETURN_ERROR(JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT,
                         "invalid IP address in allow option");
        }
        if (mask != NULL) {
            size_t i;
            if (parseAllowedMask(mask, isIPv4, &(_peers[_peers_cnt].netmask)) != JDWPTRANSPORT_ERROR_NONE) {
                _peers_cnt = 0;
                fprintf(stderr, "Error in allow option: '%s'\n", mask);
                RETURN_ERROR(JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT,
                             "invalid netmask in allow option");
            }
            // for safety update subnet to satisfy the mask
            for (i = 0; i < sizeof(_peers[_peers_cnt].subnet); i++) {
                _peers[_peers_cnt].subnet.s6_addr[i] &= _peers[_peers_cnt].netmask.s6_addr[i];
            }
        } else {
            memset(&(_peers[_peers_cnt].netmask), 0xFF, sizeof(_peers[_peers_cnt].netmask));
        }
        _peers_cnt++;
        buffer = next;
    } while (next != NULL);

    return JDWPTRANSPORT_ERROR_NONE;
}

/*
 * Parses 'allow' argument (fills in list of allowed peers (global _peers variable)).
 * 'Allow' value consists of tokens separated by '+',
 * each token contains IP address (IPv4 or IPv6) and optional prefixLength:
 * '<addr>[/<prefixLength>]'.
 * Example: '192.168.1.10+192.168.0.0/24'
 *   - connections are allowed from 192.168.1.10 and subnet 192.168.0.XX.
 */
static jdwpTransportError
parseAllowedPeers(const char *allowed_peers, size_t len) {
    // Build a list of allowed peers from char string
    // of format 192.168.0.10+192.168.0.0/24

    // writable copy of the value
    char *buffer = (*callback->alloc)((int)len + 1);
    if (buffer == NULL) {
        RETURN_ERROR(JDWPTRANSPORT_ERROR_OUT_OF_MEMORY, "out of memory");
    }
    memcpy(buffer, allowed_peers, len);
    buffer[len] = '\0';

    jdwpTransportError err = parseAllowedPeersInternal(buffer);

    (*callback->free)(buffer);

    return err;
}

static int
isAddressInSubnet(const struct in6_addr *address, const struct in6_addr *subnet, const struct in6_addr *mask) {
    size_t i;
    for (i = 0; i < sizeof(struct in6_addr); i++) {
        if ((address->s6_addr[i] & mask->s6_addr[i]) != subnet->s6_addr[i]) {
            return 0;
        }
    }
    return 1;
}

static int
isPeerAllowed(struct sockaddr_storage *peer) {
    struct in6_addr tmp;
    struct in6_addr *addr6;
    int i;
    // _peers contains IPv6 subnet and mask (IPv4 is converted to mapped IPv6)
    if (peer->ss_family == AF_INET) {
        convertIPv4ToIPv6((struct sockaddr *)peer, &tmp);
        addr6 = &tmp;
    } else {
        addr6 = &(((struct sockaddr_in6 *)peer)->sin6_addr);
    }

    for (i = 0; i < _peers_cnt; ++i) {
        if (isAddressInSubnet(addr6, &(_peers[i].subnet), &(_peers[i].netmask))) {
            return 1;
        }
    }

    return 0;
}

static jdwpTransportError JNICALL
socketTransport_getCapabilities(jdwpTransportEnv* env,
        JDWPTransportCapabilities* capabilitiesPtr)
{
    JDWPTransportCapabilities result;

    memset(&result, 0, sizeof(result));
    result.can_timeout_attach = JNI_TRUE;
    result.can_timeout_accept = JNI_TRUE;
    result.can_timeout_handshake = JNI_TRUE;

    *capabilitiesPtr = result;

    return JDWPTRANSPORT_ERROR_NONE;
}

/*
 * Starts listening on the specified addrinfo,
 * returns listening socket and actual listening port.
 * If the function fails and returned socket != -1, the socket should be closed.
 */
static jdwpTransportError startListening(struct addrinfo *ai, int *socket, char** actualAddress)
{
    int err;

    *socket = dbgsysSocket(ai->ai_family, SOCK_STREAM, IPPROTO_TCP);
    if (*socket < 0) {
        RETURN_IO_ERROR("socket creation failed");
    }

    err = setOptionsCommon(ai->ai_family, *socket);
    if (err) {
        return err;
    }

    if (getPort(ai->ai_addr) != 0) {
        /*
         * Only need SO_REUSEADDR if we're using a fixed port. If we
         * start seeing EADDRINUSE due to collisions in free ports
         * then we should retry the dbgsysBind() a few times.
         */
        err = setReuseAddrOption(*socket);
        if (err) {
            return err;
        }
    }

    err = dbgsysBind(*socket, ai->ai_addr, (socklen_t)ai->ai_addrlen);
    if (err < 0) {
        RETURN_IO_ERROR("bind failed");
    }

    err = dbgsysListen(*socket, 1); // only 1 debugger can attach
    if (err < 0) {
        RETURN_IO_ERROR("listen failed");
    }

    {
        char buf[20];
        struct sockaddr_storage addr;
        socklen_t len = sizeof(addr);
        jint portNum;
        err = dbgsysGetSocketName(*socket, (struct sockaddr *)&addr, &len);
        if (err != 0) {
            RETURN_IO_ERROR("getsockname failed");
        }

        portNum = getPort((struct sockaddr *)&addr);
        sprintf(buf, "%d", portNum);
        *actualAddress = (*callback->alloc)((int)strlen(buf) + 1);
        if (*actualAddress == NULL) {
            RETURN_ERROR(JDWPTRANSPORT_ERROR_OUT_OF_MEMORY, "out of memory");
        } else {
            strcpy(*actualAddress, buf);
        }
    }

    return JDWPTRANSPORT_ERROR_NONE;
}

static int isEqualIPv6Addr(const struct addrinfo *ai, const struct in6_addr in6Addr)
{
    if (ai->ai_addr->sa_family == AF_INET6) {
        const struct sockaddr_in6 sa = *((struct sockaddr_in6*) ai->ai_addr);
        return (memcmp(&sa.sin6_addr, &in6Addr, sizeof(in6Addr)) == 0);
    }
    return 0;
}

static jdwpTransportError JNICALL
socketTransport_startListening(jdwpTransportEnv* env, const char* address,
                               char** actualAddress)
{
    int err;
    struct addrinfo *addrInfo = NULL;
    struct addrinfo *listenAddr = NULL;
    struct addrinfo *ai = NULL;
    struct in6_addr mappedAny = IN6ADDR_ANY_INIT;

    /* no address provided */
    if ((address == NULL) || (address[0] == '\0')) {
        address = "0";
    }

    err = parseAddress(address, &addrInfo);
    if (err != JDWPTRANSPORT_ERROR_NONE) {
        return err;
    }

    // Try to find bind address of preferred address family first.
    for (ai = addrInfo; ai != NULL; ai = ai->ai_next) {
        if (ai->ai_family == preferredAddressFamily) {
            listenAddr = ai;
            break;
        }
    }

    if (listenAddr == NULL) {
        // No address of preferred addres family found, grab the fist one.
        listenAddr = &(addrInfo[0]);
    }

    if (listenAddr == NULL) {
        dbgsysFreeAddrInfo(addrInfo);
        RETURN_ERROR(JDWPTRANSPORT_ERROR_INTERNAL, "listen failed: wrong address");
    }

    // Binding to IN6ADDR_ANY allows to serve both IPv4 and IPv6 connections,
    // but binding to mapped INADDR_ANY (::ffff:0.0.0.0) allows to serve IPv4
    // connections only. Make sure that IN6ADDR_ANY is preferred over
    // mapped INADDR_ANY if preferredAddressFamily is AF_INET6 or not set.

    if (preferredAddressFamily != AF_INET) {
        inet_pton(AF_INET6, "::ffff:0.0.0.0", &mappedAny);

        if (isEqualIPv6Addr(listenAddr, mappedAny)) {
            for (ai = addrInfo; ai != NULL; ai = ai->ai_next) {
                if (isEqualIPv6Addr(listenAddr, in6addr_any)) {
                    listenAddr = ai;
                    break;
                }
            }
        }
    }

    err = startListening(listenAddr, &serverSocketFD, actualAddress);

    dbgsysFreeAddrInfo(addrInfo);

    if (err != JDWPTRANSPORT_ERROR_NONE) {
        if (serverSocketFD >= 0) {
            dbgsysSocketClose(serverSocketFD);
            serverSocketFD = -1;
        }
        return err;
    }

    return JDWPTRANSPORT_ERROR_NONE;
}

static jdwpTransportError JNICALL
socketTransport_accept(jdwpTransportEnv* env, jlong acceptTimeout, jlong handshakeTimeout)
{
    int err = JDWPTRANSPORT_ERROR_NONE;
    struct sockaddr_storage clientAddr;
    socklen_t clientAddrLen;
    jlong startTime = 0;

    /*
     * Use a default handshake timeout if not specified - this avoids an indefinite
     * hang in cases where something other than a debugger connects to our port.
     */
    if (handshakeTimeout == 0) {
        handshakeTimeout = 2000;
    }

    do {
        /*
         * If there is an accept timeout then we put the socket in non-blocking
         * mode and poll for a connection.
         */
        if (acceptTimeout > 0) {
            int rv;
            dbgsysConfigureBlocking(serverSocketFD, JNI_FALSE);
            startTime = dbgsysCurrentTimeMillis();
            rv = dbgsysPoll(serverSocketFD, JNI_TRUE, JNI_FALSE, (long)acceptTimeout);
            if (rv <= 0) {
                /* set the last error here as could be overridden by configureBlocking */
                if (rv == 0) {
                    setLastError(JDWPTRANSPORT_ERROR_IO_ERROR, "poll failed");
                }
                /* restore blocking state */
                dbgsysConfigureBlocking(serverSocketFD, JNI_TRUE);
                if (rv == 0) {
                    RETURN_ERROR(JDWPTRANSPORT_ERROR_TIMEOUT, "timed out waiting for connection");
                } else {
                    return JDWPTRANSPORT_ERROR_IO_ERROR;
                }
            }
        }

        /*
         * Accept the connection
         */
        clientAddrLen = sizeof(clientAddr);
        socketFD = dbgsysAccept(serverSocketFD,
                                (struct sockaddr *)&clientAddr,
                                &clientAddrLen);
        /* set the last error here as could be overridden by configureBlocking */
        if (socketFD < 0) {
            setLastError(JDWPTRANSPORT_ERROR_IO_ERROR, "accept failed");
        }
        /*
         * Restore the blocking state - note that the accepted socket may be in
         * blocking or non-blocking mode (platform dependent). However as there
         * is a handshake timeout set then it will go into non-blocking mode
         * anyway for the handshake.
         */
        if (acceptTimeout > 0) {
            dbgsysConfigureBlocking(serverSocketFD, JNI_TRUE);
        }
        if (socketFD < 0) {
            return JDWPTRANSPORT_ERROR_IO_ERROR;
        }

        /*
         * version >= JDWPTRANSPORT_VERSION_1_1:
         * Verify that peer is allowed to connect.
         */
        if (_peers_cnt > 0) {
            if (!isPeerAllowed(&clientAddr)) {
                char ebuf[64] = { 0 };
                char addrStr[INET_ADDRSTRLEN] = { 0 };
                int err2 = getnameinfo((struct sockaddr *)&clientAddr, clientAddrLen,
                                       addrStr, sizeof(addrStr), NULL, 0,
                                       NI_NUMERICHOST);
                sprintf(ebuf, "ERROR: Peer not allowed to connect: %s\n",
                        (err2 != 0) ? "<bad address>" : addrStr);
                dbgsysSocketClose(socketFD);
                socketFD = -1;
                err = JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT;
                setLastError(err, ebuf);
            }
        }

        if (socketFD > 0) {
          /* handshake with the debugger */
          err = handshake(socketFD, handshakeTimeout);
        }

        /*
         * If the handshake fails then close the connection. If there if an accept
         * timeout then we must adjust the timeout for the next poll.
         */
        if (err != JDWPTRANSPORT_ERROR_NONE) {
            fprintf(stderr, "Debugger failed to attach: %s\n", getLastError());
            dbgsysSocketClose(socketFD);
            socketFD = -1;
            if (acceptTimeout > 0) {
                long endTime = dbgsysCurrentTimeMillis();
                acceptTimeout -= (endTime - startTime);
                if (acceptTimeout <= 0) {
                    setLastError(JDWPTRANSPORT_ERROR_IO_ERROR,
                        "timeout waiting for debugger to connect");
                    return JDWPTRANSPORT_ERROR_IO_ERROR;
                }
            }
        }
    } while (socketFD < 0);

    return JDWPTRANSPORT_ERROR_NONE;
}

static jdwpTransportError JNICALL
socketTransport_stopListening(jdwpTransportEnv *env)
{
    if (serverSocketFD < 0) {
        RETURN_ERROR(JDWPTRANSPORT_ERROR_ILLEGAL_STATE, "connection not open");
    }
    if (dbgsysSocketClose(serverSocketFD) < 0) {
        RETURN_IO_ERROR("close failed");
    }
    serverSocketFD = -1;
    return JDWPTRANSPORT_ERROR_NONE;
}

/*
 * Tries to connect to the specified addrinfo, returns connected socket.
 * If the function fails and returned socket != -1, the socket should be closed.
 */
static jdwpTransportError connectToAddr(struct addrinfo *ai, jlong timeout, int *socket) {
    int err;

    *socket = dbgsysSocket(ai->ai_family, ai->ai_socktype, ai->ai_protocol);
    if (*socket < 0) {
        RETURN_IO_ERROR("unable to create socket");
    }

    err = setOptionsCommon(ai->ai_family, socketFD);
    if (err) {
        return err;
    }

    /*
     * We don't call setReuseAddrOption() for the non-server socket
     * case. If we start seeing EADDRINUSE due to collisions in free
     * ports then we should retry the dbgsysConnect() a few times.
     */

    /*
     * To do a timed connect we make the socket non-blocking
     * and poll with a timeout;
     */
    if (timeout > 0) {
        dbgsysConfigureBlocking(socketFD, JNI_FALSE);
    }

    err = dbgsysConnect(socketFD, ai->ai_addr, (socklen_t)ai->ai_addrlen);

    if (err == DBG_EINPROGRESS && timeout > 0) {
        err = dbgsysFinishConnect(socketFD, (long)timeout);

        if (err == DBG_ETIMEOUT) {
            dbgsysConfigureBlocking(socketFD, JNI_TRUE);
            RETURN_ERROR(JDWPTRANSPORT_ERROR_TIMEOUT, "connect timed out");
        }
    }

    if (err) {
        RETURN_IO_ERROR("connect failed");
    }

    return err;
}


static jdwpTransportError JNICALL
socketTransport_attach(jdwpTransportEnv* env, const char* addressString, jlong attachTimeout,
                       jlong handshakeTimeout)
{
    int err;
    int pass;
    struct addrinfo *addrInfo = NULL;
    struct addrinfo *ai;

    if (addressString == NULL || addressString[0] == '\0') {
        RETURN_ERROR(JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT, "address is missing");
    }

    err = parseAddress(addressString, &addrInfo);
    if (err) {
        return err;
    }

    /* 1st pass - preferredAddressFamily (by default IPv4), 2nd pass - the rest */
    for (pass = 0; pass < 2 && socketFD < 0; pass++) {
        for (ai = addrInfo; ai != NULL; ai = ai->ai_next) {
            if ((pass == 0 && ai->ai_family == preferredAddressFamily) ||
                (pass == 1 && ai->ai_family != preferredAddressFamily))
            {
                err = connectToAddr(ai, attachTimeout, &socketFD);
                if (err == JDWPTRANSPORT_ERROR_NONE) {
                    break;
                }
                if (socketFD >= 0) {
                    dbgsysSocketClose(socketFD);
                    socketFD = -1;
                }
            }
        }
    }

    freeaddrinfo(addrInfo);

    /* err from the last connectToAddr() call */
    if (err != 0) {
        return err;
    }

    if (attachTimeout > 0) {
        dbgsysConfigureBlocking(socketFD, JNI_TRUE);
    }

    err = handshake(socketFD, handshakeTimeout);
    if (err) {
        dbgsysSocketClose(socketFD);
        socketFD = -1;
        return err;
    }

    return JDWPTRANSPORT_ERROR_NONE;
}

static jboolean JNICALL
socketTransport_isOpen(jdwpTransportEnv* env)
{
    if (socketFD >= 0) {
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

static jdwpTransportError JNICALL
socketTransport_close(jdwpTransportEnv* env)
{
    int fd = socketFD;
    socketFD = -1;
    if (fd < 0) {
        return JDWPTRANSPORT_ERROR_NONE;
    }
#ifdef _AIX
    /*
      AIX needs a workaround for I/O cancellation, see:
      http://publib.boulder.ibm.com/infocenter/pseries/v5r3/index.jsp?topic=/com.ibm.aix.basetechref/doc/basetrf1/close.htm
      ...
      The close subroutine is blocked until all subroutines which use the file
      descriptor return to usr space. For example, when a thread is calling close
      and another thread is calling select with the same file descriptor, the
      close subroutine does not return until the select call returns.
      ...
    */
    shutdown(fd, 2);
#endif
    if (dbgsysSocketClose(fd) < 0) {
        /*
         * close failed - it's pointless to restore socketFD here because
         * any subsequent close will likely fail as well.
         */
        RETURN_IO_ERROR("close failed");
    }
    return JDWPTRANSPORT_ERROR_NONE;
}

static jdwpTransportError JNICALL
socketTransport_writePacket(jdwpTransportEnv* env, const jdwpPacket *packet)
{
    jint len, data_len, id;
    /*
     * room for header and up to MAX_DATA_SIZE data bytes
     */
    char header[JDWP_HEADER_SIZE + MAX_DATA_SIZE];
    jbyte *data;

    /* packet can't be null */
    if (packet == NULL) {
        RETURN_ERROR(JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT, "packet is NULL");
    }

    len = packet->type.cmd.len;         /* includes header */
    data_len = len - JDWP_HEADER_SIZE;

    /* bad packet */
    if (data_len < 0) {
        RETURN_ERROR(JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT, "invalid length");
    }

    /* prepare the header for transmission */
    len = (jint)dbgsysHostToNetworkLong(len);
    id = (jint)dbgsysHostToNetworkLong(packet->type.cmd.id);

    memcpy(header + 0, &len, 4);
    memcpy(header + 4, &id, 4);
    header[8] = packet->type.cmd.flags;
    if (packet->type.cmd.flags & JDWPTRANSPORT_FLAGS_REPLY) {
        jshort errorCode =
            dbgsysHostToNetworkShort(packet->type.reply.errorCode);
        memcpy(header + 9, &errorCode, 2);
    } else {
        header[9] = packet->type.cmd.cmdSet;
        header[10] = packet->type.cmd.cmd;
    }

    data = packet->type.cmd.data;
    /* Do one send for short packets, two for longer ones */
    if (data_len <= MAX_DATA_SIZE) {
        memcpy(header + JDWP_HEADER_SIZE, data, data_len);
        if (send_fully(socketFD, (char *)&header, JDWP_HEADER_SIZE + data_len) !=
            JDWP_HEADER_SIZE + data_len) {
            RETURN_IO_ERROR("send failed");
        }
    } else {
        memcpy(header + JDWP_HEADER_SIZE, data, MAX_DATA_SIZE);
        if (send_fully(socketFD, (char *)&header, JDWP_HEADER_SIZE + MAX_DATA_SIZE) !=
            JDWP_HEADER_SIZE + MAX_DATA_SIZE) {
            RETURN_IO_ERROR("send failed");
        }
        /* Send the remaining data bytes right out of the data area. */
        if (send_fully(socketFD, (char *)data + MAX_DATA_SIZE,
                       data_len - MAX_DATA_SIZE) != data_len - MAX_DATA_SIZE) {
            RETURN_IO_ERROR("send failed");
        }
    }

    return JDWPTRANSPORT_ERROR_NONE;
}

static jint
recv_fully(int f, char *buf, int len)
{
    int nbytes = 0;
    while (nbytes < len) {
        int res = dbgsysRecv(f, buf + nbytes, len - nbytes, 0);
        if (res < 0) {
            return res;
        } else if (res == 0) {
            break; /* eof, return nbytes which is less than len */
        }
        nbytes += res;
    }
    return nbytes;
}

jint
send_fully(int f, char *buf, int len)
{
    int nbytes = 0;
    while (nbytes < len) {
        int res = dbgsysSend(f, buf + nbytes, len - nbytes, 0);
        if (res < 0) {
            return res;
        } else if (res == 0) {
            break; /* eof, return nbytes which is less than len */
        }
        nbytes += res;
    }
    return nbytes;
}

static jdwpTransportError JNICALL
socketTransport_readPacket(jdwpTransportEnv* env, jdwpPacket* packet) {
    jint length, data_len;
    jint n;

    /* packet can't be null */
    if (packet == NULL) {
        RETURN_ERROR(JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT, "packet is null");
    }

    /* read the length field */
    n = recv_fully(socketFD, (char *)&length, sizeof(jint));

    /* check for EOF */
    if (n == 0) {
        packet->type.cmd.len = 0;
        return JDWPTRANSPORT_ERROR_NONE;
    }
    if (n != sizeof(jint)) {
        RETURN_RECV_ERROR(n);
    }

    length = (jint)dbgsysNetworkToHostLong(length);
    packet->type.cmd.len = length;


    n = recv_fully(socketFD,(char *)&(packet->type.cmd.id), sizeof(jint));
    if (n < (int)sizeof(jint)) {
        RETURN_RECV_ERROR(n);
    }

    packet->type.cmd.id = (jint)dbgsysNetworkToHostLong(packet->type.cmd.id);

    n = recv_fully(socketFD,(char *)&(packet->type.cmd.flags), sizeof(jbyte));
    if (n < (int)sizeof(jbyte)) {
        RETURN_RECV_ERROR(n);
    }

    if (packet->type.cmd.flags & JDWPTRANSPORT_FLAGS_REPLY) {
        n = recv_fully(socketFD,(char *)&(packet->type.reply.errorCode), sizeof(jbyte));
        if (n < (int)sizeof(jshort)) {
            RETURN_RECV_ERROR(n);
        }

        /* FIXME - should the error be converted to host order?? */


    } else {
        n = recv_fully(socketFD,(char *)&(packet->type.cmd.cmdSet), sizeof(jbyte));
        if (n < (int)sizeof(jbyte)) {
            RETURN_RECV_ERROR(n);
        }

        n = recv_fully(socketFD,(char *)&(packet->type.cmd.cmd), sizeof(jbyte));
        if (n < (int)sizeof(jbyte)) {
            RETURN_RECV_ERROR(n);
        }
    }

    data_len = length - ((sizeof(jint) * 2) + (sizeof(jbyte) * 3));

    if (data_len < 0) {
        setLastError(0, "Badly formed packet received - invalid length");
        return JDWPTRANSPORT_ERROR_IO_ERROR;
    } else if (data_len == 0) {
        packet->type.cmd.data = NULL;
    } else {
        packet->type.cmd.data= (*callback->alloc)(data_len);

        if (packet->type.cmd.data == NULL) {
            RETURN_ERROR(JDWPTRANSPORT_ERROR_OUT_OF_MEMORY, "out of memory");
        }

        n = recv_fully(socketFD,(char *)packet->type.cmd.data, data_len);
        if (n < data_len) {
            (*callback->free)(packet->type.cmd.data);
            RETURN_RECV_ERROR(n);
        }
    }

    return JDWPTRANSPORT_ERROR_NONE;
}

static jdwpTransportError JNICALL
socketTransport_getLastError(jdwpTransportEnv* env, char** msgP) {
    char *msg = (char *)dbgsysTlsGet(tlsIndex);
    if (msg == NULL) {
        return JDWPTRANSPORT_ERROR_MSG_NOT_AVAILABLE;
    }
    *msgP = (*callback->alloc)((int)strlen(msg)+1);
    if (*msgP == NULL) {
        return JDWPTRANSPORT_ERROR_OUT_OF_MEMORY;
    }
    strcpy(*msgP, msg);
    return JDWPTRANSPORT_ERROR_NONE;
}

static jdwpTransportError JNICALL
socketTransport_setConfiguration(jdwpTransportEnv* env, jdwpTransportConfiguration* cfg) {
    const char* allowed_peers = NULL;

    if (cfg == NULL) {
        RETURN_ERROR(JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT,
                     "NULL pointer to transport configuration is invalid");
    }
    allowed_peers = cfg->allowed_peers;
    _peers_cnt = 0;
    if (allowed_peers != NULL) {
        size_t len = strlen(allowed_peers);
        if (len == 0) { /* Impossible: parseOptions() would reject it */
            fprintf(stderr, "Error in allow option: '%s'\n", allowed_peers);
            RETURN_ERROR(JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT,
                         "allow option should not be empty");
        } else if (*allowed_peers == '*') {
            if (len != 1) {
                fprintf(stderr, "Error in allow option: '%s'\n", allowed_peers);
                RETURN_ERROR(JDWPTRANSPORT_ERROR_ILLEGAL_ARGUMENT,
                             "allow option '*' cannot be expanded");
            }
        } else {
            int err = parseAllowedPeers(allowed_peers, len);
            if (err != JDWPTRANSPORT_ERROR_NONE) {
                return err;
            }
        }
    }
    return JDWPTRANSPORT_ERROR_NONE;
}

/*
 * Reads boolean system value, sets *result to
 *  - trueValue if the property is "true";
 *  - falseValue if the property is "false".
 * Doesn't change *result if the property is not set or failed to read.
 */
static int readBooleanSysProp(int *result, int trueValue, int falseValue,
    JNIEnv* jniEnv, jclass sysClass, jmethodID getPropMethod, const char *propName)
{
    jstring value;
    jstring name = (*jniEnv)->NewStringUTF(jniEnv, propName);

    if (name == NULL) {
        return JNI_ERR;
    }
    value = (jstring)(*jniEnv)->CallStaticObjectMethod(jniEnv, sysClass, getPropMethod, name);
    if ((*jniEnv)->ExceptionCheck(jniEnv)) {
        return JNI_ERR;
    }
    if (value != NULL) {
        const char *theValue = (*jniEnv)->GetStringUTFChars(jniEnv, value, NULL);
        if (theValue == NULL) {
            return JNI_ERR;
        }
        if (strcmp(theValue, "true") == 0) {
            *result = trueValue;
        } else if (strcmp(theValue, "false") == 0) {
            *result = falseValue;
        }
        (*jniEnv)->ReleaseStringUTFChars(jniEnv, value, theValue);
    }
    return JNI_OK;
}

JNIEXPORT jint JNICALL
jdwpTransport_OnLoad(JavaVM *vm, jdwpTransportCallback* cbTablePtr,
                     jint version, jdwpTransportEnv** env)
{
    JNIEnv* jniEnv = NULL;

    if (version < JDWPTRANSPORT_VERSION_1_0 ||
        version > JDWPTRANSPORT_VERSION_1_1) {
        return JNI_EVERSION;
    }
    if (initialized) {
        /*
         * This library doesn't support multiple environments (yet)
         */
        return JNI_EEXIST;
    }
    initialized = JNI_TRUE;
    jvm = vm;
    callback = cbTablePtr;

    /* initialize interface table */
    interface.GetCapabilities = &socketTransport_getCapabilities;
    interface.Attach = &socketTransport_attach;
    interface.StartListening = &socketTransport_startListening;
    interface.StopListening = &socketTransport_stopListening;
    interface.Accept = &socketTransport_accept;
    interface.IsOpen = &socketTransport_isOpen;
    interface.Close = &socketTransport_close;
    interface.ReadPacket = &socketTransport_readPacket;
    interface.WritePacket = &socketTransport_writePacket;
    interface.GetLastError = &socketTransport_getLastError;
    if (version >= JDWPTRANSPORT_VERSION_1_1) {
        interface.SetTransportConfiguration = &socketTransport_setConfiguration;
    }
    *env = &single_env;

    /* initialized TLS */
    tlsIndex = dbgsysTlsAlloc();

    // retrieve network-related system properties
    do {
        jclass sysClass;
        jmethodID getPropMethod;
        if ((*vm)->GetEnv(vm, (void **)&jniEnv, JNI_VERSION_9) != JNI_OK) {
            break;
        }
        sysClass = (*jniEnv)->FindClass(jniEnv, "java/lang/System");
        if (sysClass == NULL) {
            break;
        }
        getPropMethod = (*jniEnv)->GetStaticMethodID(jniEnv, sysClass,
            "getProperty", "(Ljava/lang/String;)Ljava/lang/String;");
        if (getPropMethod == NULL) {
            break;
        }
        readBooleanSysProp(&allowOnlyIPv4, 1, 0,
            jniEnv, sysClass, getPropMethod, "java.net.preferIPv4Stack");
        readBooleanSysProp(&preferredAddressFamily, AF_INET6, AF_INET,
            jniEnv, sysClass, getPropMethod, "java.net.preferIPv6Addresses");
    } while (0);

    if (jniEnv != NULL && (*jniEnv)->ExceptionCheck(jniEnv)) {
        (*jniEnv)->ExceptionClear(jniEnv);
    }


    return JNI_OK;
}
