/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "InspectableProcess.h"
#include <InspectorServer/ConnectionFromClient.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibIPC/ConnectionFromClient.h>
#include <LibIPC/MultiServer.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop event_loop;

    TRY(Core::System::pledge("stdio unix accept"));

    auto server = TRY(IPC::MultiServer<InspectorServer::ConnectionFromClient>::try_create("/tmp/portal/inspector"));

    auto inspectables_server = TRY(Core::LocalServer::try_create());
    TRY(inspectables_server->take_over_from_system_server("/tmp/portal/inspectables"));

    inspectables_server->on_accept = [&](auto client_socket) {
        auto pid = client_socket->peer_pid().release_value_but_fixme_should_propagate_errors();
        InspectorServer::g_processes.set(pid, make<InspectorServer::InspectableProcess>(pid, move(client_socket)));
    };

    return event_loop.exec();
}
