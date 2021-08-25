/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClientConnection.h"
#include <LibCore/LocalServer.h>
#include <LibCore/StandardPaths.h>
#include <unistd.h>

int main(int, char**)
{
    if (pledge("stdio accept rpath wpath cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil(Core::StandardPaths::config_directory().characters(), "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    Core::EventLoop event_loop;
    auto server = Core::LocalServer::construct();

    bool ok = server->take_over_from_system_server();
    VERIFY(ok);
    server->on_ready_to_accept = [&] {
        auto client_socket = server->accept();
        if (!client_socket) {
            dbgln("ConfigServer: accept failed.");
            return;
        }
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        IPC::new_client_connection<ConfigServer::ClientConnection>(client_socket.release_nonnull(), client_id);
    };

    return event_loop.exec();
}
