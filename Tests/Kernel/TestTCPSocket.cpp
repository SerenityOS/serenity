/*
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <netinet/in.h>
#include <pthread.h>
#include <sys/socket.h>

static constexpr u16 port = 1337;

static void* server_handler(void*)
{
    int server_fd = socket(AF_INET, SOCK_STREAM, 0);
    EXPECT(server_fd >= 0);

    sockaddr_in sin {};
    sin.sin_family = AF_INET;
    sin.sin_port = htons(port);
    sin.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    int rc = bind(server_fd, (sockaddr*)(&sin), sizeof(sin));
    EXPECT_EQ(rc, 0);

    rc = listen(server_fd, 1);
    EXPECT_EQ(rc, 0);

    int client_fd = accept(server_fd, nullptr, nullptr);
    EXPECT(client_fd >= 0);

    u8 data;
    int nread = recv(client_fd, &data, sizeof(data), 0);
    EXPECT_EQ(nread, 1);
    EXPECT_EQ(data, 'A');

    rc = close(client_fd);
    EXPECT_EQ(rc, 0);

    rc = close(server_fd);
    EXPECT_EQ(rc, 0);

    pthread_exit(nullptr);
    VERIFY_NOT_REACHED();
}

static pthread_t start_tcp_server()
{
    pthread_t thread;
    int rc = pthread_create(&thread, nullptr, server_handler, nullptr);
    EXPECT_EQ(rc, 0);
    return thread;
}

TEST_CASE(tcp_sendto)
{
    pthread_t server = start_tcp_server();

    int client_fd = socket(AF_INET, SOCK_STREAM, 0);
    EXPECT(client_fd >= 0);

    sockaddr_in sin {};
    sin.sin_family = AF_INET;
    sin.sin_port = htons(port);
    sin.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    int rc = connect(client_fd, (sockaddr*)(&sin), sizeof(sin));
    EXPECT_EQ(rc, 0);

    u8 data = 'A';
    sockaddr_in dst {};
    dst.sin_family = AF_INET;
    dst.sin_port = htons(port + 1); // Different port, should be ignored
    dst.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    int nwritten = sendto(client_fd, &data, sizeof(data), 0, (sockaddr*)(&dst), sizeof(dst));
    EXPECT_EQ(nwritten, 1);

    rc = close(client_fd);
    EXPECT_EQ(rc, 0);

    rc = pthread_join(server, nullptr);
    EXPECT_EQ(rc, 0);
}
