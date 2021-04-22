/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClientConnection.h"
#include <AK/LexicalPath.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/LocalServer.h>
#include <LibIPC/ClientConnection.h>
#include <sys/stat.h>
#include <unistd.h>

int main(int, char**)
{
    Core::EventLoop event_loop;
    if (pledge("stdio unix recvfd rpath ", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto socket = Core::LocalSocket::take_over_accepted_socket_from_system_server();
    IPC::new_client_connection<LanguageServers::Cpp::ClientConnection>(socket.release_nonnull(), 1);
    if (pledge("stdio recvfd rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/usr/include", "r") < 0)
        perror("unveil");

    // unveil will be sealed later, when we know the project's root path.
    return event_loop.exec();
}
