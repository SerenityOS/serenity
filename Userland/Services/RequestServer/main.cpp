/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibIPC/SingleServer.h>
#include <LibMain/Main.h>
#include <LibTLS/Certificate.h>
#include <RequestServer/ClientConnection.h>
#include <RequestServer/GeminiProtocol.h>
#include <RequestServer/HttpProtocol.h>
#include <RequestServer/HttpsProtocol.h>
#include <signal.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio inet accept unix rpath sendfd recvfd sigaction"));
    signal(SIGINFO, [](int) { RequestServer::ConnectionCache::dump_jobs(); });
    TRY(Core::System::pledge("stdio inet accept unix rpath sendfd recvfd"));

    // Ensure the certificates are read out here.
    [[maybe_unused]] auto& certs = DefaultRootCACertificates::the();

    Core::EventLoop event_loop;
    // FIXME: Establish a connection to LookupServer and then drop "unix"?
    TRY(Core::System::unveil("/tmp/portal/lookup", "rw"));
    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    [[maybe_unused]] auto gemini = make<RequestServer::GeminiProtocol>();
    [[maybe_unused]] auto http = make<RequestServer::HttpProtocol>();
    [[maybe_unused]] auto https = make<RequestServer::HttpsProtocol>();

    auto client = TRY(IPC::take_over_accepted_client_from_system_server<RequestServer::ClientConnection>());

    auto result = event_loop.exec();

    // FIXME: We exit instead of returning, so that protocol destructors don't get called.
    //        The Protocol base class should probably do proper de-registration instead of
    //        just VERIFY_NOT_REACHED().
    exit(result);
}
