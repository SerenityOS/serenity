/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <AK/IPv4Address.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <net/if.h>
#include <netinet/in.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>

static void test_invalid(int);
static void test_no_route(int);
static void test_valid(int);
static void test_send(int);

static ErrorOr<void> test(Function<void(int)> test_fn)
{
    auto fd = TRY(Core::System::socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP));
    test_fn(fd);

    // be a responsible boi
    TRY(Core::System::close(fd));

    return {};
}

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(test(test_invalid));
    TRY(test(test_valid));
    TRY(test(test_no_route));
    TRY(test(test_send));

    return 0;
}

void test_invalid(int fd)
{
    // bind to an interface that does not exist
    char buf[IFNAMSIZ];
    socklen_t buflen = IFNAMSIZ;
    memcpy(buf, "foodev", 7);

    auto setsockopt_maybe_error = Core::System::setsockopt(fd, SOL_SOCKET, SO_BINDTODEVICE, buf, buflen);
    if (setsockopt_maybe_error.is_error()) {
        warnln("setsockopt(SO_BINDTODEVICE) :: invalid (Should fail with ENODEV).");
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

    auto setsockopt_maybe_error = Core::System::setsockopt(fd, SOL_SOCKET, SO_BINDTODEVICE, buf, buflen);
    if (setsockopt_maybe_error.is_error()) {
        warnln("setsockopt(SO_BINDTODEVICE) :: valid");
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

    auto setsockopt_maybe_error = Core::System::setsockopt(fd, SOL_SOCKET, SO_BINDTODEVICE, buf, buflen);
    if (setsockopt_maybe_error.is_error()) {
        warnln("setsockopt(SO_BINDTODEVICE) :: no_route");
        puts("FAIL no_route");
        return;
    }
    sockaddr_in sin;
    memset(&sin, 0, sizeof(sin));

    sin.sin_addr.s_addr = IPv4Address { 10, 0, 2, 15 }.to_u32();
    sin.sin_port = 8080;
    sin.sin_family = AF_INET;

    auto bind_maybe_error = Core::System::bind(fd, (sockaddr*)&sin, sizeof(sin));
    if (bind_maybe_error.is_error()) {
        warnln("bind() :: no_route");
        puts("FAIL no_route");
        return;
    }

    auto sendto_maybe_error = Core::System::sendto(fd, "TEST", 4, 0, (sockaddr*)&sin, sizeof(sin));
    if (sendto_maybe_error.is_error()) {
        warnln("sendto() :: no_route (Should fail with EHOSTUNREACH)");
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

    auto setsockopt_maybe_error = Core::System::setsockopt(fd, SOL_SOCKET, SO_BINDTODEVICE, buf, buflen);
    if (setsockopt_maybe_error.is_error()) {
        warnln("setsockopt(SO_BINDTODEVICE) :: send");
        puts("FAIL send");
        return;
    }
    sockaddr_in sin;
    memset(&sin, 0, sizeof(sin));

    sin.sin_addr.s_addr = IPv4Address { 10, 0, 2, 15 }.to_u32();
    sin.sin_port = 8080;
    sin.sin_family = AF_INET;

    auto bind_maybe_error = Core::System::bind(fd, (sockaddr*)&sin, sizeof(sin));
    if (bind_maybe_error.is_error()) {
        warnln("bind() :: send");
        puts("FAIL send");
        return;
    }

    auto sendto_maybe_error = Core::System::sendto(fd, "TEST", 4, 0, (sockaddr*)&sin, sizeof(sin));
    if (sendto_maybe_error.is_error()) {
        warnln("sendto() :: send");
        puts("FAIL send");
        return;
    }
    puts("PASS send");
}
