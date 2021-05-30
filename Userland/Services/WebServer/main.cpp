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
    String default_listen_address = "0.0.0.0";
    u16 default_port = 8000;
    const char* root_path = "/www";

    String listen_address = default_listen_address;
    int port = default_port;

    Core::ArgsParser args_parser;
    args_parser.add_option(listen_address, "IP address to listen on", "listen-address", 'l', "listen_address");
    args_parser.add_option(port, "Port to listen on", "port", 'p', "port");
    args_parser.add_positional_argument(root_path, "Path to serve the contents of", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto ipv4_address = IPv4Address::from_string(listen_address);
    if (!ipv4_address.has_value()) {
        warnln("Invalid listen address: {}", listen_address);
        return 1;
    }

    if ((u16)port != port) {
        warnln("Invalid port number: {}", port);
        return 1;
    }

    auto real_root_path = Core::File::real_path_for(root_path);

    if (!Core::File::exists(real_root_path)) {
        warnln("Root path does not exist: '{}'", root_path);
        return 1;
    }

    if (pledge("stdio accept rpath inet unix", nullptr) < 0) {
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

    if (!server->listen(ipv4_address.value(), port)) {
        warnln("Failed to listen on {}:{}", ipv4_address.value(), port);
        return 1;
    }

    outln("Listening on {}:{}", ipv4_address.value(), port);

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
