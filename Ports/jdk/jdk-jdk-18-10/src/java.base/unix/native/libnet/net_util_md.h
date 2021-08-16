/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef NET_UTILS_MD_H
#define NET_UTILS_MD_H

#include <netdb.h>
#include <poll.h>
#include <sys/socket.h>

/************************************************************************
 * Macros and constants
 */

#define NET_NSEC_PER_MSEC 1000000
#define NET_NSEC_PER_SEC  1000000000
#define NET_NSEC_PER_USEC 1000

/* in case NI_MAXHOST is not defined in netdb.h */
#ifndef NI_MAXHOST
#define NI_MAXHOST 1025
#endif

/* Defines SO_REUSEPORT */
#ifndef SO_REUSEPORT
#ifdef __linux__
#define SO_REUSEPORT 15
#elif defined(AIX) || defined(MACOSX)
#define SO_REUSEPORT 0x0200
#else
#define SO_REUSEPORT 0
#endif
#endif

/*
 * On 64-bit JDKs we use a much larger stack and heap buffer.
 */
#ifdef _LP64
#define MAX_BUFFER_LEN 65536
#define MAX_HEAP_BUFFER_LEN 131072
#else
#define MAX_BUFFER_LEN 8192
#define MAX_HEAP_BUFFER_LEN 65536
#endif

typedef union {
    struct sockaddr     sa;
    struct sockaddr_in  sa4;
    struct sockaddr_in6 sa6;
} SOCKETADDRESS;

/************************************************************************
 * Functions
 */

int NET_Timeout(JNIEnv *env, int s, long timeout, jlong  nanoTimeStamp);
int NET_Read(int s, void* buf, size_t len);
int NET_NonBlockingRead(int s, void* buf, size_t len);
int NET_RecvFrom(int s, void *buf, int len, unsigned int flags,
                 struct sockaddr *from, socklen_t *fromlen);
int NET_Send(int s, void *msg, int len, unsigned int flags);
int NET_SendTo(int s, const void *msg, int len,  unsigned  int
               flags, const struct sockaddr *to, int tolen);
int NET_Connect(int s, struct sockaddr *addr, int addrlen);
int NET_Accept(int s, struct sockaddr *addr, socklen_t *addrlen);
int NET_SocketClose(int s);
int NET_Dup2(int oldfd, int newfd);
int NET_Poll(struct pollfd *ufds, unsigned int nfds, int timeout);

void NET_ThrowUnknownHostExceptionWithGaiError(JNIEnv *env,
                                               const char* hostname,
                                               int gai_error);
void NET_ThrowByNameWithLastError(JNIEnv *env, const char *name,
                                  const char *defaultDetail);
void NET_SetTrafficClass(SOCKETADDRESS *sa, int trafficClass);

#endif /* NET_UTILS_MD_H */
