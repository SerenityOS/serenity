/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClientConnection.h"
#include <LibCore/LocalServer.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio accept rpath wpath cpath"));
    TRY(Core::System::unveil(Core::StandardPaths::config_directory(), "rwc"));
    TRY(Core::System::unveil(nullptr, nullptr));

    Core::EventLoop event_loop;
    auto server = TRY(Core::LocalServer::try_create());

    bool ok = server->take_over_from_system_server();
    VERIFY(ok);
    server->on_accept = [&](auto client_socket) {
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        (void)IPC::new_client_connection<ConfigServer::ClientConnection>(move(client_socket), client_id);
    };

    return event_loop.exec();
}
