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
#include <WebSocket/ConnectionFromClient.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio inet unix rpath sendfd recvfd"));

    // Ensure the certificates are read out here.
    [[maybe_unused]] auto& certs = DefaultRootCACertificates::the();

    Core::EventLoop event_loop;
    // FIXME: Establish a connection to LookupServer and then drop "unix"?
    TRY(Core::System::unveil("/tmp/portal/lookup", "rw"));
    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto client = TRY(IPC::take_over_accepted_client_from_system_server<WebSocket::ConnectionFromClient>());

    return event_loop.exec();
}
