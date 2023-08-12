/*
 * Copyright (c) 2023, Marcus Nilsson <marcus.nilsson@genarp.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <arpa/inet.h>
#include <errno.h>
#include <netdb.h>

TEST_CASE(gethostbyname_should_return_host_not_found)
{
    auto* res = gethostbyname("unknownhostthatdoesntexistandhopefullyneverwill.com");
    EXPECT_EQ(res, nullptr);
    EXPECT_EQ(h_errno, HOST_NOT_FOUND);
}

TEST_CASE(gethostbyname)
{
    auto* result = gethostbyname("google.com");
    EXPECT_NE(result, nullptr);
    EXPECT_EQ(h_errno, 0);
    EXPECT_EQ(result->h_aliases[0], nullptr);
    EXPECT_EQ(result->h_addr_list[1], nullptr);
    EXPECT_EQ(result->h_addrtype, AF_INET);
}

TEST_CASE(gethostbyname_r_should_return_erange_when_buffer_is_to_small)
{
    constexpr size_t buffer_size = 2;
    char buffer[buffer_size] = { 0 };
    int h_errnop;
    struct hostent ret;
    struct hostent* result;

    int rc = gethostbyname_r("127.0.0.1", &ret, buffer, buffer_size, &result, &h_errnop);
    EXPECT_EQ(rc, ERANGE);
}

TEST_CASE(gethostbyname_r_should_return_host_not_found)
{
    constexpr size_t buffer_size = 1024;
    char buffer[buffer_size] = { 0 };
    int h_errnop;
    struct hostent ret;
    struct hostent* result;

    int rc = gethostbyname_r("unknownhostthatdoesntexistandhopefullyneverwill.com", &ret, buffer, buffer_size, &result, &h_errnop);
    EXPECT(rc < 0);
    EXPECT_EQ(h_errnop, HOST_NOT_FOUND);
}

TEST_CASE(gethostbyname_r)
{
    constexpr size_t buffer_size = 1024;
    char buffer[buffer_size] = { 0 };
    int h_errnop;
    struct hostent ret;
    struct hostent* result;

    int rc = gethostbyname_r("google.com", &ret, buffer, buffer_size, &result, &h_errnop);
    EXPECT_EQ(rc, 0);
    EXPECT_EQ(h_errnop, 0);
    EXPECT_NE(result, nullptr);
    EXPECT_EQ(result->h_aliases[0], nullptr);
    EXPECT_EQ(result->h_addr_list[1], nullptr);
    EXPECT_EQ(result->h_addrtype, AF_INET);
}

TEST_CASE(getaddrinfo_should_find_https)
{
    struct addrinfo hints;
    struct addrinfo* result;

    memset(&hints, 0, sizeof(struct addrinfo));

    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_flags = AI_PASSIVE;

    int status = getaddrinfo(nullptr, "https", &hints, &result);
    EXPECT_EQ(status, 0);

    freeaddrinfo(result);
}

TEST_CASE(getaddrinfo_should_not_find_service_that_doesnt_exist)
{
    struct addrinfo hints;
    struct addrinfo* result;

    memset(&hints, 0, sizeof(struct addrinfo));

    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_flags = AI_PASSIVE;

    int status = getaddrinfo(nullptr, "unknownservicethatdoesntexistandhopefullyneverwill", &hints, &result);
    EXPECT_EQ(status, EAI_FAIL);

    freeaddrinfo(result);
}

TEST_CASE(getaddrinfo_should_find_googles_ip)
{
    struct addrinfo hints;
    struct addrinfo* result;

    memset(&hints, 0, sizeof(struct addrinfo));

    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;

    int status = getaddrinfo("google.com", nullptr, &hints, &result);
    EXPECT_EQ(status, 0);

    freeaddrinfo(result);
}
