/* $Id: netdb.h,v 1.1 2008/10/06 12:36:20 joerg Exp $ */

/*
 * Copyright (C) 2000-2003 Damien Miller.  All rights reserved.
 * Copyright (C) 1999 WIDE Project.  All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the project nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE PROJECT AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE PROJECT OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

/*
 * Pseudo-implementation of RFC2553 name / address resolution functions
 *
 * But these functions are not implemented correctly. The minimum subset
 * is implemented for ssh use only. For example, this routine assumes
 * that ai_family is AF_INET. Don't use it for another purpose.
 */

#ifndef _NBCOMPAT_NETDB_H
#define _NBCOMPAT_NETDB_H

#include <sys/types.h>
#if defined(HAVE_NETDB_H)
# include <netdb.h>
#endif
#if defined(HAVE_SYS_SOCKET_H)
# include <sys/socket.h>
#endif

/*
 * First, socket and INET6 related definitions 
 */
#ifndef HAVE_STRUCT_SOCKADDR_STORAGE
# define	_SS_MAXSIZE	128	/* Implementation specific max size */
# define       _SS_PADSIZE     (_SS_MAXSIZE - sizeof (struct sockaddr))
struct sockaddr_storage {
	struct sockaddr	ss_sa;
	char		__ss_pad2[_SS_PADSIZE];
};
# define ss_family ss_sa.sa_family
#endif /* !HAVE_STRUCT_SOCKADDR_STORAGE */

#ifndef IN6_IS_ADDR_LOOPBACK
# define IN6_IS_ADDR_LOOPBACK(a) \
	(((uint32_t *)(a))[0] == 0 && ((uint32_t *)(a))[1] == 0 && \
	 ((uint32_t *)(a))[2] == 0 && ((uint32_t *)(a))[3] == htonl(1))
#endif /* !IN6_IS_ADDR_LOOPBACK */

#ifndef IN6_IS_ADDR_V4MAPPED
#define IN6_IS_ADDR_V4MAPPED(a)		      \
	(((uint32_t *)(a))[0] == 0 && ((uint32_t *)(a))[1] == 0 && \
	 ((uint32_t *)(a))[2] == ntohl(0x0000ffff))
#endif

#ifndef INET6_ADDRSTRLEN
#define INET6_ADDRSTRLEN	46
#endif

#ifndef HAVE_SOCKLEN_T
typedef unsigned int socklen_t;
#endif

#ifndef HAVE_STRUCT_IN6_ADDR
struct in6_addr {
	uint8_t		s6_addr[16];
};
#endif /* !HAVE_STRUCT_IN6_ADDR */

#ifndef HAVE_STRUCT_SOCKADDR_IN6
struct sockaddr_in6 {
	unsigned short	sin6_family;
	uint16_t	sin6_port;
	uint32_t	sin6_flowinfo;
	struct in6_addr	sin6_addr;
	uint32_t	sin6_scope_id;
};
#endif /* !HAVE_STRUCT_SOCKADDR_IN6 */

#ifndef AF_INET6
/* Define it to something that should never appear */
#define AF_INET6 AF_MAX
#endif

/*
 * Next, RFC2553 name / address resolution API
 */

#ifndef NI_NUMERICHOST
# define NI_NUMERICHOST    (1)
#endif
#ifndef NI_NAMEREQD
# define NI_NAMEREQD       (1<<1)
#endif
#ifndef NI_NUMERICSERV
# define NI_NUMERICSERV    (1<<2)
#endif

#ifndef AI_PASSIVE
# define AI_PASSIVE		(1)
#endif
#ifndef AI_CANONNAME
# define AI_CANONNAME		(1<<1)
#endif
#ifndef AI_NUMERICHOST
# define AI_NUMERICHOST		(1<<2)
#endif

#ifndef NI_MAXSERV
# define NI_MAXSERV 32
#endif /* !NI_MAXSERV */
#ifndef NI_MAXHOST
# define NI_MAXHOST 1025
#endif /* !NI_MAXHOST */

#ifndef EAI_NODATA
# define EAI_NODATA	(INT_MAX - 1)
#endif
#ifndef EAI_MEMORY
# define EAI_MEMORY	(INT_MAX - 2)
#endif
#ifndef EAI_NONAME
# define EAI_NONAME	(INT_MAX - 3)
#endif
#ifndef EAI_SYSTEM
# define EAI_SYSTEM	(INT_MAX - 4)
#endif
#ifndef EAI_FAMILY
# define EAI_FAMILY	(INT_MAX - 5)
#endif
#ifndef EAI_AGAIN
# define EAI_AGAIN	(INT_MAX - 6)
#endif
#ifndef EAI_FAIL
# define EAI_FAIL	(INT_MAX - 7)
#endif

#ifndef HAVE_STRUCT_ADDRINFO
struct addrinfo {
	int	ai_flags;	/* AI_PASSIVE, AI_CANONNAME */
	int	ai_family;	/* PF_xxx */
	int	ai_socktype;	/* SOCK_xxx */
	int	ai_protocol;	/* 0 or IPPROTO_xxx for IPv4 and IPv6 */
	size_t	ai_addrlen;	/* length of ai_addr */
	char	*ai_canonname;	/* canonical name for hostname */
	struct sockaddr *ai_addr;	/* binary address */
	struct addrinfo *ai_next;	/* next structure in linked list */
};
#endif /* !HAVE_STRUCT_ADDRINFO */

#ifndef HAVE_GETADDRINFO
void freeaddrinfo(struct addrinfo *);
int getaddrinfo(const char *, const char *, 
    const struct addrinfo *, struct addrinfo **);
#endif /* !HAVE_GETADDRINFO */

#if !defined(HAVE_GAI_STRERROR)
char *gai_strerror(int);
#endif /* !HAVE_GAI_STRERROR */

#ifndef HAVE_GETNAMEINFO
int getnameinfo(const struct sockaddr *, size_t, char *, size_t, 
    char *, size_t, int);
#endif /* !HAVE_GETNAMEINFO */

#endif /* !_FAKE_RFC2553_H */

