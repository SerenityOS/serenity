/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibIPC/ClientConnection.h>
#include <WebContent/ClientConnection.h>

int main(int, char**)
{
    Core::EventLoop event_loop;
    if (pledge("stdio recvfd sendfd accept unix rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }
    if (unveil("/tmp/portal/request", "rw") < 0) {
        perror("unveil");
        return 1;
    }
    if (unveil("/tmp/portal/image", "rw") < 0) {
        perror("unveil");
        return 1;
    }
    if (unveil("/tmp/portal/websocket", "rw") < 0) {
        perror("unveil");
        return 1;
    }
    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto socket = Core::LocalSocket::take_over_accepted_socket_from_system_server();
    VERIFY(socket);
    IPC::new_client_connection<WebContent::ClientConnection>(socket.release_nonnull(), 1);
    return event_loop.exec();
}
