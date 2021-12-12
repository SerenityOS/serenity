/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibIPC/SingleServer.h>
#include <LibMain/Main.h>
#include <LibTLS/Certificate.h>
#include <WebSocket/ClientConnection.h>

const char* serenity_get_initial_promises()
{
    return "stdio inet unix rpath sendfd recvfd";
}

ErrorOr<int> serenity_main(Main::Arguments)
{
    // Ensure the certificates are read out here.
    [[maybe_unused]] auto& certs = DefaultRootCACertificates::the();

    Core::EventLoop event_loop;
    // FIXME: Establish a connection to LookupServer and then drop "unix"?
    TRY(Core::System::retract("rpath"));
    TRY(Core::System::unveil("/tmp/portal/lookup", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto client = TRY(IPC::take_over_accepted_client_from_system_server<WebSocket::ClientConnection>());

    return event_loop.exec();
}
