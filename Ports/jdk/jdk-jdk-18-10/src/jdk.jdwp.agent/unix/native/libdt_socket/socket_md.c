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

#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include <sys/time.h>
#include <pthread.h>
#include <poll.h>

#include "socket_md.h"
#include "sysSocket.h"

int
dbgsysListen(int fd, int backlog) {
    return listen(fd, backlog);
}

int
dbgsysConnect(int fd, struct sockaddr *name, socklen_t namelen) {
    int rv = connect(fd, name, namelen);
    if (rv < 0 && (errno == EINPROGRESS || errno == EINTR)) {
        return DBG_EINPROGRESS;
    } else {
        return rv;
    }
}

int
dbgsysFinishConnect(int fd, int timeout) {
    int rv = dbgsysPoll(fd, 0, 1, timeout);
    if (rv == 0) {
        return DBG_ETIMEOUT;
    }
    if (rv > 0) {
        return 0;
    }
    return rv;
}

int
dbgsysAccept(int fd, struct sockaddr *name, socklen_t *namelen) {
    int rv;
    for (;;) {
        rv = accept(fd, name, namelen);
        if (rv >= 0) {
            return rv;
        }
        if (errno != ECONNABORTED && errno != EINTR) {
            return rv;
        }
    }
}

int
dbgsysRecvFrom(int fd, char *buf, size_t nBytes,
                  int flags, struct sockaddr *from, socklen_t *fromlen) {
    int rv;
    do {
        rv = recvfrom(fd, buf, nBytes, flags, from, fromlen);
    } while (rv == -1 && errno == EINTR);

    return rv;
}

int
dbgsysSendTo(int fd, char *buf, size_t len,
                int flags, struct sockaddr *to, socklen_t tolen) {
    int rv;
    do {
        rv = sendto(fd, buf, len, flags, to, tolen);
    } while (rv == -1 && errno == EINTR);

    return rv;
}

int
dbgsysRecv(int fd, char *buf, size_t nBytes, int flags) {
    int rv;
    do {
        rv = recv(fd, buf, nBytes, flags);
    } while (rv == -1 && errno == EINTR);

    return rv;
}

int
dbgsysSend(int fd, char *buf, size_t nBytes, int flags) {
    int rv;
    do {
        rv = send(fd, buf, nBytes, flags);
    } while (rv == -1 && errno == EINTR);

    return rv;
}

int
dbgsysGetAddrInfo(const char *hostname, const char *service,
                  const struct addrinfo *hints,
                  struct addrinfo **results) {
    return getaddrinfo(hostname, service, hints, results);
}

void
dbgsysFreeAddrInfo(struct addrinfo *info) {
    freeaddrinfo(info);
}

unsigned short
dbgsysHostToNetworkShort(unsigned short hostshort) {
    return htons(hostshort);
}

int
dbgsysSocket(int domain, int type, int protocol) {
    return socket(domain, type, protocol);
}

int dbgsysSocketClose(int fd) {
    int rv;

    /* AIX recommends to repeat the close call on EINTR */
#if defined(_AIX)
    do {
        rv = close(fd);
    } while (rv == -1 && errno == EINTR);
#else
    rv = close(fd);
#endif

    return rv;
}

int
dbgsysBind(int fd, struct sockaddr *name, socklen_t namelen) {
    return bind(fd, name, namelen);
}

uint32_t
dbgsysHostToNetworkLong(uint32_t hostlong) {
    return htonl(hostlong);
}

unsigned short
dbgsysNetworkToHostShort(unsigned short netshort) {
    return ntohs(netshort);
}

int
dbgsysGetSocketName(int fd, struct sockaddr *name, socklen_t *namelen) {
    return getsockname(fd, name, namelen);
}

uint32_t
dbgsysNetworkToHostLong(uint32_t netlong) {
    return ntohl(netlong);
}


int
dbgsysSetSocketOption(int fd, jint cmd, jboolean on, jvalue value)
{
    if (cmd == TCP_NODELAY) {
        struct protoent *proto = getprotobyname("TCP");
        int tcp_level = (proto == 0 ? IPPROTO_TCP: proto->p_proto);
        uint32_t onl = (uint32_t)on;

        if (setsockopt(fd, tcp_level, TCP_NODELAY,
                       (char *)&onl, sizeof(uint32_t)) < 0) {
                return SYS_ERR;
        }
    } else if (cmd == SO_LINGER) {
        struct linger arg;
        arg.l_onoff = on;
        arg.l_linger = (on) ? (unsigned short)value.i : 0;
        if (setsockopt(fd, SOL_SOCKET, SO_LINGER,
                       (char*)&arg, sizeof(arg)) < 0) {
          return SYS_ERR;
        }
    } else if (cmd == SO_SNDBUF) {
        jint buflen = value.i;
        if (setsockopt(fd, SOL_SOCKET, SO_SNDBUF,
                       (char *)&buflen, sizeof(buflen)) < 0) {
            return SYS_ERR;
        }
    } else if (cmd == SO_REUSEADDR) {
        int oni = (int)on;
        if (setsockopt(fd, SOL_SOCKET, SO_REUSEADDR,
                       (char *)&oni, sizeof(oni)) < 0) {
            return SYS_ERR;

        }
    } else {
        return SYS_ERR;
    }
    return SYS_OK;
}

int
dbgsysConfigureBlocking(int fd, jboolean blocking) {
    int flags = fcntl(fd, F_GETFL);

    if ((blocking == JNI_FALSE) && !(flags & O_NONBLOCK)) {
        return fcntl(fd, F_SETFL, flags | O_NONBLOCK);
    }
    if ((blocking == JNI_TRUE) && (flags & O_NONBLOCK)) {
        return fcntl(fd, F_SETFL, flags & ~O_NONBLOCK);
    }
    return 0;
}

int
dbgsysPoll(int fd, jboolean rd, jboolean wr, long timeout) {
    struct pollfd fds[1];
    int rv;

    fds[0].fd = fd;
    fds[0].events = 0;
    if (rd) {
        fds[0].events |= POLLIN;
    }
    if (wr) {
        fds[0].events |= POLLOUT;
    }
    fds[0].revents = 0;

    rv = poll(&fds[0], 1, timeout);
    if (rv >= 0) {
        rv = 0;
        if (fds[0].revents & POLLIN) {
            rv |= DBG_POLLIN;
        }
        if (fds[0].revents & POLLOUT) {
            rv |= DBG_POLLOUT;
        }
    }
    return rv;
}

int
dbgsysGetLastIOError(char *buf, jint size) {
    char *msg = strerror(errno);
    strncpy(buf, msg, size-1);
    buf[size-1] = '\0';
    return 0;
}

int
dbgsysTlsAlloc() {
    pthread_key_t key;
    if (pthread_key_create(&key, NULL)) {
        perror("pthread_key_create");
        exit(-1);
    }
    return (int)key;
}

void
dbgsysTlsFree(int index) {
    pthread_key_delete((pthread_key_t)index);
}

void
dbgsysTlsPut(int index, void *value) {
    pthread_setspecific((pthread_key_t)index, value) ;
}

void *
dbgsysTlsGet(int index) {
    return pthread_getspecific((pthread_key_t)index);
}

long
dbgsysCurrentTimeMillis() {
    struct timeval t;
    gettimeofday(&t, 0);
    return ((jlong)t.tv_sec) * 1000 + (jlong)(t.tv_usec/1000);
}
