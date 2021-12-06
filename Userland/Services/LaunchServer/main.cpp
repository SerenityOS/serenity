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
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop event_loop;
    auto server = TRY(Core::LocalServer::try_create());

    auto launcher = LaunchServer::Launcher();
    launcher.load_handlers();
    launcher.load_config(Core::ConfigFile::open_for_app("LaunchServer"));

    TRY(Core::System::pledge("stdio accept rpath proc exec"));

    bool ok = server->take_over_from_system_server();
    VERIFY(ok);
    server->on_accept = [&](auto client_socket) {
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        (void)IPC::new_client_connection<LaunchServer::ClientConnection>(move(client_socket), client_id);
    };

    return event_loop.exec();
}
