/*
 * Copyright (c) 2021, Peter Elliott <pelliott@ualberta.ca>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Client.h"
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/LocalSocket.h>

int main()
{
    dbgln("Starting XServer");
    Core::EventLoop loop;

    auto server = Core::LocalServer::construct();

    bool ok = server->take_over_from_system_server();
    VERIFY(ok);

    server->on_ready_to_accept = [&] {
        auto client_socket = server->accept();
        if (!client_socket) {
            dbgln("XServer: accept failed");
            return;
        }
        auto client = X::Client::construct(client_socket.release_nonnull(), server);
        client->do_handshake();
    };

    return loop.exec();
}
