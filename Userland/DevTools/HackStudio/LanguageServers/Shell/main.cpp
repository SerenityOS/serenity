/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClientConnection.h"
#include <YAK/LexicalPath.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/LocalServer.h>
#include <LibIPC/ClientConnection.h>
#include <sys/stat.h>
#include <unistd.h>

int main(int, char**)
{
    Core::EventLoop event_loop;
    if (pledge("stdio unix rpath recvfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto socket = Core::LocalSocket::take_over_accepted_socket_from_system_server();
    IPC::new_client_connection<LanguageServers::Shell::ClientConnection>(socket.release_nonnull(), 1);
    if (pledge("stdio rpath recvfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    if (unveil("/etc/passwd", "r") < 0) {
        perror("unveil");
        return 1;
    }

    return event_loop.exec();
}
