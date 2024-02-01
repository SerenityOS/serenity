/*
 * Copyright (c) 2023, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonArray.h>
#include <LibCore/File.h>
#include <LibTest/TestCase.h>
#include <netinet/in.h>
#include <pthread.h>
#include <semaphore.h>
#include <sys/socket.h>

static constexpr u16 port = 1337;

static void* server_handler(void* accept_semaphore)
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

    rc = sem_post(reinterpret_cast<sem_t*>(accept_semaphore));
    VERIFY(rc == 0);

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
    sem_t accept_semaphore;

    int rc = sem_init(&accept_semaphore, 0, 0);
    VERIFY(rc == 0);
    rc = pthread_create(&thread, nullptr, server_handler, &accept_semaphore);
    VERIFY(rc == 0);
    rc = sem_wait(&accept_semaphore);
    VERIFY(rc == 0);
    rc = sem_destroy(&accept_semaphore);
    VERIFY(rc == 0);
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

TEST_CASE(tcp_bind_connect)
{
    pthread_t server = start_tcp_server();

    int client_fd = socket(AF_INET, SOCK_STREAM, 0);
    EXPECT(client_fd >= 0);

    sockaddr_in sin {};
    sin.sin_family = AF_INET;
    sin.sin_port = htons(port - 1);
    sin.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    int rc = bind(client_fd, (sockaddr*)(&sin), sizeof(sin));
    EXPECT_EQ(rc, 0);

    sockaddr_in dst {};
    dst.sin_family = AF_INET;
    dst.sin_port = htons(port);
    dst.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    rc = connect(client_fd, (sockaddr*)(&dst), sizeof(dst));
    EXPECT_EQ(rc, 0);

    u8 data = 'A';
    int nwritten = send(client_fd, &data, sizeof(data), 0);
    EXPECT_EQ(nwritten, 1);

    rc = close(client_fd);
    EXPECT_EQ(rc, 0);

    rc = pthread_join(server, nullptr);
    EXPECT_EQ(rc, 0);

    // Hacky check to make sure there are no registered TCP sockets, if the sockets were closed properly, there should
    // be none left, but if the early-bind caused a desync in sockets_by_tuple a UAF'd socket will be left in there.
    // NOTE: We have to loop since the TimedWait stage during socket close means the socket might not close immediately
    // after our close(2) call. This also means that on failure we will loop here forever.
    while (true) {
        auto file = MUST(Core::File::open("/sys/kernel/net/tcp"sv, Core::File::OpenMode::Read));
        auto file_contents = MUST(file->read_until_eof());
        auto json = MUST(JsonValue::from_string(file_contents));
        EXPECT(json.is_array());
        if (json.as_array().size() == 0)
            return;
        sched_yield();
    }
}

TEST_CASE(socket_connect_after_bind)
{
    unlink("/tmp/tmp-client.test");
    unlink("/tmp/tmp.test");

    int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    EXPECT(fd >= 0);

    struct sockaddr_un addr {
        .sun_family = AF_UNIX,
        .sun_path = "/tmp/tmp-client.test",
    };

    int bound = bind(fd, (struct sockaddr*)&addr, sizeof(addr));
    EXPECT_NE(bound, -1);

    struct sockaddr_un server_sockaddr {
        .sun_family = AF_UNIX,
        .sun_path = "/tmp/tmp.test",
    };
    int connected = connect(fd, (struct sockaddr*)&server_sockaddr, sizeof(server_sockaddr));
    EXPECT_EQ(connected, -1);

    int closed = close(fd);
    EXPECT_EQ(closed, 0);

    unlink("/tmp/tmp-client.test");
    unlink("/tmp/tmp.test");
}
