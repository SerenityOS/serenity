/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <ImageDecoder/ClientConnection.h>
#include <LibCore/EventLoop.h>
#include <LibIPC/ClientConnection.h>

int main(int, char**)
{
    Core::EventLoop event_loop;
    if (pledge("stdio recvfd sendfd unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto socket = Core::LocalSocket::take_over_accepted_socket_from_system_server();
    IPC::new_client_connection<ImageDecoder::ClientConnection>(socket.release_nonnull(), 1);
    if (pledge("stdio recvfd sendfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    return event_loop.exec();
}
