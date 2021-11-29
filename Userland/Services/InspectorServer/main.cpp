/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InspectableProcess.h"
#include <InspectorServer/ClientConnection.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibIPC/ClientConnection.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop event_loop;
    auto server = TRY(Core::LocalServer::try_create());

    TRY(Core::System::pledge("stdio unix accept"));

    bool ok = server->take_over_from_system_server("/tmp/portal/inspector");
    VERIFY(ok);
    server->on_accept = [&](auto client_socket) {
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        IPC::new_client_connection<InspectorServer::ClientConnection>(move(client_socket), client_id);
    };

    auto inspectables_server = TRY(Core::LocalServer::try_create());
    if (!inspectables_server->take_over_from_system_server("/tmp/portal/inspectables"))
        VERIFY_NOT_REACHED();

    inspectables_server->on_accept = [&](auto client_socket) {
        auto pid = client_socket->peer_pid();
        InspectorServer::g_processes.set(pid, make<InspectorServer::InspectableProcess>(pid, move(client_socket)));
    };

    return event_loop.exec();
}
