/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    if (pledge("stdio cpath unix fattr inet id accept", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/tmp/rpc", "rwc") < 0) {
        perror("unveil");
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
            clients.remove(id);
            outln("Client {} disconnected", id);
        };
        clients.set(id, client);
    };

    outln("Listening on 0.0.0.0:{}", port);

    return event_loop.exec();
}
