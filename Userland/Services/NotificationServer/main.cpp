/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClientConnection.h"
#include <LibCore/LocalServer.h>
#include <LibGUI/Application.h>
#include <LibGUI/WindowServerConnection.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd accept rpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);
    auto server = Core::LocalServer::construct();

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

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    if (pledge("stdio recvfd sendfd accept rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    return app->exec();
}
