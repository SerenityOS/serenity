/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Client.h"
#include <AK/HashMap.h>
#include <AK/IPv4Address.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibCore/TCPServer.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio unix inet id accept"));
    TRY(Core::System::unveil(nullptr, nullptr));

    int port = 7;

    Core::ArgsParser args_parser;
    args_parser.add_option(port, "Port to listen on", "port", 'p', "port");
    args_parser.parse(arguments);

    if ((u16)port != port) {
        warnln("Invalid port number: {}", port);
        exit(1);
    }

    if (port < 1024 && geteuid() != 0) {
        warnln("Listening on port {} requires root privileges", port);
        exit(1);
    }

    Core::EventLoop event_loop;

    auto server = TRY(Core::TCPServer::try_create());
    TRY(server->listen({}, port));

    IGNORE_USE_IN_ESCAPING_LAMBDA HashMap<int, NonnullRefPtr<Client>> clients;
    int next_id = 0;

    server->on_ready_to_accept = [&next_id, &clients, &server] {
        int id = next_id++;

        auto maybe_client_socket = server->accept();
        if (maybe_client_socket.is_error()) {
            warnln("accept: {}", maybe_client_socket.error());
            return;
        }

        outln("Client {} connected", id);

        auto client = Client::create(id, maybe_client_socket.release_value());
        client->on_exit = [&clients, id] {
            Core::deferred_invoke([&clients, id] {
                clients.remove(id);
                outln("Client {} disconnected", id);
            });
        };
        clients.set(id, client);
    };

    outln("Listening on 0.0.0.0:{}...", port);

    return event_loop.exec();
}
