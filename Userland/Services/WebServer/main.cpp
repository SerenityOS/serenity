/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Client.h"
#include <AK/MappedFile.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/TCPServer.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    u16 default_port = 8000;
    const char* root_path = "/www";

    int port = default_port;

    Core::ArgsParser args_parser;
    args_parser.add_option(port, "Port to listen on", "port", 'p', "port");
    args_parser.add_positional_argument(root_path, "Path to serve the contents of", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if ((u16)port != port) {
        printf("Warning: invalid port number: %d\n", port);
        port = default_port;
    }

    auto real_root_path = Core::File::real_path_for(root_path);

    if (!Core::File::exists(real_root_path)) {
        fprintf(stderr, "Root path does not exist: '%s'\n", root_path);
        return 1;
    }

    if (pledge("stdio accept rpath inet unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Core::EventLoop loop;

    auto server = Core::TCPServer::construct();

    server->on_ready_to_accept = [&] {
        auto client_socket = server->accept();
        VERIFY(client_socket);
        auto client = WebServer::Client::construct(client_socket.release_nonnull(), real_root_path, server);
        client->start();
    };

    if (!server->listen({}, port)) {
        warnln("Failed to listen on 0.0.0.0:{}", port);
        return 1;
    }

    outln("Listening on 0.0.0.0:{}", port);

    if (unveil("/res/icons", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(real_root_path.characters(), "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    if (pledge("stdio accept rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    return loop.exec();
}
