/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

struct hostent {
    char* h_name;
    char** h_aliases;
    int h_addrtype;
    int h_length;
    char** h_addr_list;
#define h_addr h_addr_list[0]
};

struct hostent* gethostbyname(const char*);
struct hostent* gethostbyaddr(const void* addr, socklen_t len, int type);

struct servent {
    char* s_name;
    char** s_aliases;
    int s_port;
    char* s_proto;
};

struct servent* getservent();
struct servent* getservbyname(const char* name, const char* protocol);
struct servent* getservbyport(int port, const char* protocol);
void setservent(int stay_open);
void endservent();

struct protoent {
    char* p_name;
    char** p_aliases;
    int p_proto;
};

void endprotoent();
struct protoent* getprotobyname(const char* name);
struct protoent* getprotobynumber(int proto);
struct protoent* getprotoent();
void setprotoent(int stay_open);

extern int h_errno;

#define HOST_NOT_FOUND 101
#define NO_DATA 102
#define NO_RECOVERY 103
#define TRY_AGAIN 104

struct addrinfo {
    int ai_flags;
    int ai_family;
    int ai_socktype;
    int ai_protocol;
    socklen_t ai_addrlen;
    struct sockaddr* ai_addr;
    char* ai_canonname;
    struct addrinfo* ai_next;
};

#define EAI_ADDRFAMILY 1
#define EAI_AGAIN 2
#define EAI_BADFLAGS 3
#define EAI_FAIL 4
#define EAI_FAMILY 5
#define EAI_MEMORY 6
#define EAI_NODATA 7
#define EAI_NONAME 8
#define EAI_SERVICE 9
#define EAI_SOCKTYPE 10
#define EAI_SYSTEM 11

int getaddrinfo(const char* __restrict node, const char* __restrict service, const struct addrinfo* __restrict hints, struct addrinfo** __restrict res);
void freeaddrinfo(struct addrinfo* res);
const char* gai_strerror(int errcode);

__END_DECLS
