/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibIPC/ClientConnection.h>
#include <LibTLS/Certificate.h>
#include <WebSocket/ClientConnection.h>

int main(int, char**)
{
    if (pledge("stdio inet unix rpath sendfd recvfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    // Ensure the certificates are read out here.
    [[maybe_unused]] auto& certs = DefaultRootCACertificates::the();

    Core::EventLoop event_loop;
    // FIXME: Establish a connection to LookupServer and then drop "unix"?
    if (pledge("stdio inet unix sendfd recvfd", nullptr) < 0) {
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

    auto socket = Core::LocalSocket::take_over_accepted_socket_from_system_server();
    VERIFY(socket);
    IPC::new_client_connection<WebSocket::ClientConnection>(socket.release_nonnull(), 1);
    return event_loop.exec();
}
