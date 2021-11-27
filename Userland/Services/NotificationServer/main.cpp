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

    bool ok = server->take_over_from_system_server();
    VERIFY(ok);
    server->on_ready_to_accept = [&] {
        auto client_socket = server->accept();
        if (!client_socket) {
            dbgln("NotificationServer: accept failed.");
            return;
        }
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        IPC::new_client_connection<NotificationServer::ClientConnection>(client_socket.release_nonnull(), client_id);
    };

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));
    TRY(Core::System::pledge("stdio recvfd sendfd accept rpath"));

    return app->exec();
}
