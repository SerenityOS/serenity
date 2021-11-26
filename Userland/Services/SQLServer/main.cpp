/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <SQLServer/ClientConnection.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

int main([[maybe_unused]] int argc, [[maybe_unused]] char** argv)
{
    if (pledge("stdio accept unix rpath wpath cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (mkdir("/home/anon/sql", 0700) < 0 && errno != EEXIST) {
        perror("mkdir");
        return 1;
    }

    if (unveil("/home/anon/sql", "rwc") < 0) {
        perror("unveil");
        return 1;
    }
    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    Core::EventLoop event_loop;
    auto server = Core::LocalServer::construct();
    bool ok = server->take_over_from_system_server();
    VERIFY(ok);

    server->on_ready_to_accept = [&] {
        auto client_socket = server->accept();
        if (!client_socket) {
            dbgln("SQLServer: accept failed.");
            return;
        }
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        IPC::new_client_connection<SQLServer::ClientConnection>(client_socket.release_nonnull(), client_id);
    };

    return event_loop.exec();
}
