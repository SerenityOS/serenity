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
#include <LibCore/TCPServer.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio unix inet id accept", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    int port = 7;

    Core::ArgsParser args_parser;
    args_parser.add_option(port, "Port to listen on", "port", 'p', "port");
    args_parser.parse(argc, argv);

    if ((u16)port != port) {
        warnln("Invalid port number: {}", port);
        exit(1);
    }

    Core::EventLoop event_loop;

    auto server = Core::TCPServer::construct();

    if (!server->listen({}, port)) {
        warnln("Listening on 0.0.0.0:{} failed", port);
        exit(1);
    }

    HashMap<int, NonnullRefPtr<Client>> clients;
    int next_id = 0;

    server->on_ready_to_accept = [&next_id, &clients, &server] {
        int id = next_id++;

        auto client_socket = server->accept();
        if (!client_socket) {
            perror("accept");
            return;
        }

        outln("Client {} connected", id);

        auto client = Client::create(id, move(client_socket));
        client->on_exit = [&clients, id] {
            Core::deferred_invoke([&clients, id] {
                clients.remove(id);
                outln("Client {} disconnected", id);
            });
        };
        clients.set(id, client);
    };

    outln("Listening on 0.0.0.0:{}", port);

    return event_loop.exec();
}
