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

#include <AK/Assertions.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <Kernel/Net/IPv4.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

extern "C" {

static hostent __gethostbyname_buffer;
static char __gethostbyname_name_buffer[512];
static in_addr_t __gethostbyname_address;
static in_addr_t* __gethostbyname_address_list_buffer[2];

static hostent __gethostbyaddr_buffer;
static char __gethostbyaddr_name_buffer[512];
static in_addr_t* __gethostbyaddr_address_list_buffer[2];

static int connect_to_lookup_server()
{
    int fd = socket(AF_LOCAL, SOCK_STREAM | SOCK_CLOEXEC, 0);
    if (fd < 0) {
        perror("socket");
        return -1;
    }

    sockaddr_un address;
    address.sun_family = AF_LOCAL;
    strcpy(address.sun_path, "/tmp/portal/lookup");

    if (connect(fd, (const sockaddr*)&address, sizeof(address)) < 0) {
        perror("connect_to_lookup_server");
        close(fd);
        return -1;
    }
    return fd;
}

hostent* gethostbyname(const char* name)
{
    auto ipv4_address = IPv4Address::from_string(name);
    if (ipv4_address.has_value()) {
        sprintf(__gethostbyname_name_buffer, "%s", ipv4_address.value().to_string().characters());
        __gethostbyname_buffer.h_name = __gethostbyname_name_buffer;
        __gethostbyname_buffer.h_aliases = nullptr;
        __gethostbyname_buffer.h_addrtype = AF_INET;
        new (&__gethostbyname_address) IPv4Address(ipv4_address.value());
        __gethostbyname_address_list_buffer[0] = &__gethostbyname_address;
        __gethostbyname_address_list_buffer[1] = nullptr;
        __gethostbyname_buffer.h_addr_list = (char**)__gethostbyname_address_list_buffer;
        __gethostbyname_buffer.h_length = 4;

        return &__gethostbyname_buffer;
    }

    int fd = connect_to_lookup_server();
    if (fd < 0)
        return nullptr;

    auto close_fd_on_exit = ScopeGuard([fd] {
        close(fd);
    });

    auto line = String::format("L%s\n", name);
    int nsent = write(fd, line.characters(), line.length());
    if (nsent < 0) {
        perror("write");
        return nullptr;
    }

    ASSERT((size_t)nsent == line.length());

    char buffer[1024];
    int nrecv = read(fd, buffer, sizeof(buffer) - 1);
    if (nrecv < 0) {
        perror("recv");
        return nullptr;
    }

    if (!memcmp(buffer, "Not found.", sizeof("Not found.") - 1))
        return nullptr;

    auto responses = String(buffer, nrecv).split('\n');
    if (responses.is_empty())
        return nullptr;

    auto& response = responses[0];

    int rc = inet_pton(AF_INET, response.characters(), &__gethostbyname_address);
    if (rc <= 0)
        return nullptr;

    strncpy(__gethostbyname_name_buffer, name, strlen(name));

    __gethostbyname_buffer.h_name = __gethostbyname_name_buffer;
    __gethostbyname_buffer.h_aliases = nullptr;
    __gethostbyname_buffer.h_addrtype = AF_INET;
    __gethostbyname_address_list_buffer[0] = &__gethostbyname_address;
    __gethostbyname_address_list_buffer[1] = nullptr;
    __gethostbyname_buffer.h_addr_list = (char**)__gethostbyname_address_list_buffer;
    __gethostbyname_buffer.h_length = 4;

    return &__gethostbyname_buffer;
}

hostent* gethostbyaddr(const void* addr, socklen_t addr_size, int type)
{
    if (type != AF_INET) {
        errno = EAFNOSUPPORT;
        return nullptr;
    }

    if (addr_size < sizeof(in_addr)) {
        errno = EINVAL;
        return nullptr;
    }

    int fd = connect_to_lookup_server();
    if (fd < 0)
        return nullptr;

    auto close_fd_on_exit = ScopeGuard([fd] {
        close(fd);
    });

    IPv4Address ipv4_address((const u8*)&((const in_addr*)addr)->s_addr);

    auto line = String::format("R%d.%d.%d.%d.in-addr.arpa\n",
        ipv4_address[3],
        ipv4_address[2],
        ipv4_address[1],
        ipv4_address[0]);
    int nsent = write(fd, line.characters(), line.length());
    if (nsent < 0) {
        perror("write");
        return nullptr;
    }

    ASSERT((size_t)nsent == line.length());

    char buffer[1024];
    int nrecv = read(fd, buffer, sizeof(buffer) - 1);
    if (nrecv < 0) {
        perror("recv");
        return nullptr;
    }

    if (!memcmp(buffer, "Not found.", sizeof("Not found.") - 1))
        return nullptr;

    auto responses = String(buffer, nrecv).split('\n');
    if (responses.is_empty())
        return nullptr;

    auto& response = responses[0];

    strncpy(__gethostbyaddr_name_buffer, response.characters(), max(sizeof(__gethostbyaddr_name_buffer), response.length()));

    __gethostbyaddr_buffer.h_name = __gethostbyaddr_name_buffer;
    __gethostbyaddr_buffer.h_aliases = nullptr;
    __gethostbyaddr_buffer.h_addrtype = AF_INET;
    // FIXME: Should we populate the hostent's address list here with a sockaddr_in for the provided host?
    __gethostbyaddr_address_list_buffer[0] = nullptr;
    __gethostbyaddr_buffer.h_addr_list = (char**)__gethostbyaddr_address_list_buffer;
    __gethostbyaddr_buffer.h_length = 4;

    return &__gethostbyaddr_buffer;
}
}
