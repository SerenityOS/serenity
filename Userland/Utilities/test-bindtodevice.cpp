/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/IPv4Address.h>
#include <net/if.h>
#include <netinet/in.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

static void test_invalid(int);
static void test_no_route(int);
static void test_valid(int);
static void test_send(int);

static void test(Function<void(int)> test_fn)
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
    memcpy(buf, "loop", 5);

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
    memcpy(buf, "loop", 5);

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
    // FIXME: Look up the proper device name instead of hard-coding it
    memcpy(buf, "ep0s7", 6);

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
