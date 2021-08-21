/*
 * Copyright (c) 2020, Nicholas Hollett <niax@niax.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClientConnection.h"
#include "Launcher.h"
#include <LibCore/ConfigFile.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <stdio.h>
#include <unistd.h>

int main([[maybe_unused]] int argc, [[maybe_unused]] char** argv)
{
    Core::EventLoop event_loop;
    auto server = Core::LocalServer::construct();

    auto launcher = LaunchServer::Launcher();

    launcher.load_handlers();
    launcher.load_config(Core::ConfigFile::open_for_app("LaunchServer"));

    if (pledge("stdio accept rpath proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool ok = server->take_over_from_system_server();
    VERIFY(ok);
    server->on_ready_to_accept = [&] {
        auto client_socket = server->accept();
        if (!client_socket) {
            dbgln("LaunchServer: accept failed.");
            return;
        }
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        IPC::new_client_connection<LaunchServer::ClientConnection>(client_socket.release_nonnull(), client_id);
    };

    return event_loop.exec();
}
