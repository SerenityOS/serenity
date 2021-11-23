/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibIPC/ClientConnection.h>
#include <LibMain/Main.h>
#include <LibTLS/Certificate.h>
#include <WebSocket/ClientConnection.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio inet unix rpath sendfd recvfd", nullptr));

    // Ensure the certificates are read out here.
    [[maybe_unused]] auto& certs = DefaultRootCACertificates::the();

    Core::EventLoop event_loop;
    // FIXME: Establish a connection to LookupServer and then drop "unix"?
    TRY(Core::System::pledge("stdio inet unix sendfd recvfd", nullptr));
    TRY(Core::System::unveil("/tmp/portal/lookup", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto socket = TRY(Core::LocalSocket::take_over_accepted_socket_from_system_server());
    IPC::new_client_connection<WebSocket::ClientConnection>(move(socket), 1);
    return event_loop.exec();
}
