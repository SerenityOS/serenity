/*
 * Copyright (c) 2020, the SerenityOS developers
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

#include <AK/Function.h>
#include <AK/IPv4Address.h>
#include <net/if.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>

static void test_invalid(int);
static void test_no_route(int);
static void test_valid(int);
static void test_send(int);

static void test(AK::Function<void(int)> test_fn)
{

    int fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (fd < 0) {
        perror("socket");
        return;
    }

    test_fn(fd);

    // be a responsible boi
    close(fd);
}

auto main() -> int
{
    test(test_invalid);
    test(test_valid);
    test(test_no_route);
    test(test_send);
}

void test_invalid(int fd)
{
    // bind to an interface that does not exist
    char buf[IFNAMSIZ];
    socklen_t buflen = IFNAMSIZ;
    memcpy(buf, "foodev", 7);

    if (setsockopt(fd, SOL_SOCKET, SO_BINDTODEVICE, buf, buflen) < 0) {
        perror("setsockopt(SO_BINDTODEVICE) :: invalid (Should fail with ENODEV)");
        puts("PASS invalid");
    } else {
        puts("FAIL invalid");
    }
}

void test_valid(int fd)
{
    // bind to an interface that exists
    char buf[IFNAMSIZ];
    socklen_t buflen = IFNAMSIZ;
    memcpy(buf, "loop0", 6);

    if (setsockopt(fd, SOL_SOCKET, SO_BINDTODEVICE, buf, buflen) < 0) {
        perror("setsockopt(SO_BINDTODEVICE) :: valid");
        puts("FAIL valid");
    } else {
        puts("PASS valid");
    }
}

void test_no_route(int fd)
{
    // bind to an interface that cannot deliver
    char buf[IFNAMSIZ];
    socklen_t buflen = IFNAMSIZ;
    memcpy(buf, "loop0", 6);

    if (setsockopt(fd, SOL_SOCKET, SO_BINDTODEVICE, buf, buflen) < 0) {
        perror("setsockopt(SO_BINDTODEVICE) :: no_route");
        puts("FAIL no_route");
        return;
    }
    sockaddr_in sin;
    memset(&sin, 0, sizeof(sin));

    sin.sin_addr.s_addr = IPv4Address { 10, 0, 2, 15 }.to_u32();
    sin.sin_port = 8080;
    sin.sin_family = AF_INET;

    if (bind(fd, (sockaddr*)&sin, sizeof(sin)) < 0) {
        perror("bind() :: no_route");
        puts("FAIL no_route");
        return;
    }

    if (sendto(fd, "TEST", 4, 0, (sockaddr*)&sin, sizeof(sin)) < 0) {
        perror("sendto() :: no_route (Should fail with EHOSTUNREACH)");
        puts("PASS no_route");
    } else
        puts("FAIL no_route");
}

void test_send(int fd)
{
    // bind to an interface that cannot deliver
    char buf[IFNAMSIZ];
    socklen_t buflen = IFNAMSIZ;
    memcpy(buf, "e1k0", 5);

    if (setsockopt(fd, SOL_SOCKET, SO_BINDTODEVICE, buf, buflen) < 0) {
        perror("setsockopt(SO_BINDTODEVICE) :: send");
        puts("FAIL send");
        return;
    }
    sockaddr_in sin;
    memset(&sin, 0, sizeof(sin));

    sin.sin_addr.s_addr = IPv4Address { 10, 0, 2, 15 }.to_u32();
    sin.sin_port = 8080;
    sin.sin_family = AF_INET;

    if (bind(fd, (sockaddr*)&sin, sizeof(sin)) < 0) {
        perror("bind() :: send");
        puts("FAIL send");
        return;
    }

    if (sendto(fd, "TEST", 4, 0, (sockaddr*)&sin, sizeof(sin)) < 0) {
        perror("sendto() :: send");
        puts("FAIL send");
        return;
    }
    puts("PASS send");
}
