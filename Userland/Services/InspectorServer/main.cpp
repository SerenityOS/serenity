/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InspectableProcess.h"
#include <InspectorServer/ClientConnection.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibIPC/ClientConnection.h>

int main(int, char**)
{
    Core::EventLoop event_loop;
    auto server = Core::LocalServer::construct();

    if (pledge("stdio unix accept", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    bool ok = server->take_over_from_system_server("/tmp/portal/inspector");
    VERIFY(ok);
    server->on_ready_to_accept = [&] {
        auto client_socket = server->accept();
        if (!client_socket) {
            dbgln("accept failed.");
            return;
        }
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        IPC::new_client_connection<InspectorServer::ClientConnection>(client_socket.release_nonnull(), client_id);
    };

    auto inspectables_server = Core::LocalServer::construct();
    if (!inspectables_server->take_over_from_system_server("/tmp/portal/inspectables"))
        VERIFY_NOT_REACHED();

    inspectables_server->on_ready_to_accept = [&] {
        auto client_socket = inspectables_server->accept();
        if (!client_socket) {
            dbgln("backdoor accept failed.");
            return;
        }
        struct ucred creds = {};
        socklen_t creds_size = sizeof(creds);
        if (getsockopt(client_socket->fd(), SOL_SOCKET, SO_PEERCRED, &creds, &creds_size) < 0) {
            dbgln("SO_PEERCRED failed");
            return;
        }

        InspectorServer::g_processes.set(creds.pid, make<InspectorServer::InspectableProcess>(creds.pid, client_socket.release_nonnull()));
    };

    return event_loop.exec();
}
