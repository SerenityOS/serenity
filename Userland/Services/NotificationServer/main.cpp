/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClientConnection.h"
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd accept rpath unix"));

    auto app = TRY(GUI::Application::try_create(arguments));
    auto server = TRY(Core::LocalServer::try_create());

    TRY(server->take_over_from_system_server());
    server->on_accept = [&](auto client_socket) {
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        (void)IPC::new_client_connection<NotificationServer::ClientConnection>(move(client_socket), client_id);
    };

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));
    TRY(Core::System::pledge("stdio recvfd sendfd accept rpath"));

    return app->exec();
}
