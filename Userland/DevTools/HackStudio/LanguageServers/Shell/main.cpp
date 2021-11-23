/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClientConnection.h"
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibIPC/ClientConnection.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop event_loop;
    TRY(Core::System::pledge("stdio unix rpath recvfd", nullptr));

    auto socket = TRY(Core::LocalSocket::take_over_accepted_socket_from_system_server());
    IPC::new_client_connection<LanguageServers::Shell::ClientConnection>(move(socket), 1);
    TRY(Core::System::pledge("stdio rpath recvfd", nullptr));
    TRY(Core::System::unveil("/etc/passwd", "r"));

    return event_loop.exec();
}
