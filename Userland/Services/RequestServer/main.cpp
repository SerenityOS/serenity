/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibIPC/ClientConnection.h>
#include <LibTLS/Certificate.h>
#include <RequestServer/ClientConnection.h>
#include <RequestServer/GeminiProtocol.h>
#include <RequestServer/HttpProtocol.h>
#include <RequestServer/HttpsProtocol.h>

int main(int, char**)
{
    if (pledge("stdio inet accept unix rpath sendfd recvfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    // Ensure the certificates are read out here.
    [[maybe_unused]] auto& certs = DefaultRootCACertificates::the();

    Core::EventLoop event_loop;
    // FIXME: Establish a connection to LookupServer and then drop "unix"?
    if (pledge("stdio inet accept unix sendfd recvfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    if (unveil("/tmp/portal/lookup", "rw") < 0) {
        perror("unveil");
        return 1;
    }
    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    [[maybe_unused]] auto gemini = new RequestServer::GeminiProtocol;
    [[maybe_unused]] auto http = new RequestServer::HttpProtocol;
    [[maybe_unused]] auto https = new RequestServer::HttpsProtocol;

    auto socket = Core::LocalSocket::take_over_accepted_socket_from_system_server();
    VERIFY(socket);
    IPC::new_client_connection<RequestServer::ClientConnection>(socket.release_nonnull(), 1);
    return event_loop.exec();
}
