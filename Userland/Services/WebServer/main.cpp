/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <LibCore/TCPServer.h>
#include <LibHTTP/HttpRequest.h>
#include <LibMain/Main.h>
#include <WebServer/Client.h>
#include <WebServer/Configuration.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    String default_listen_address = "0.0.0.0";
    u16 default_port = 8000;
    String root_path = "/www";

    String listen_address = default_listen_address;
    int port = default_port;
    String username;
    String password;

    Core::ArgsParser args_parser;
    args_parser.add_option(listen_address, "IP address to listen on", "listen-address", 'l', "listen_address");
    args_parser.add_option(port, "Port to listen on", "port", 'p', "port");
    args_parser.add_option(username, "HTTP basic authentication username", "user", 'U', "username");
    args_parser.add_option(password, "HTTP basic authentication password", "pass", 'P', "password");
    args_parser.add_positional_argument(root_path, "Path to serve the contents of", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto ipv4_address = IPv4Address::from_string(listen_address);
    if (!ipv4_address.has_value()) {
        warnln("Invalid listen address: {}", listen_address);
        return 1;
    }

    if ((u16)port != port) {
        warnln("Invalid port number: {}", port);
        return 1;
    }

    if (username.is_empty() != password.is_empty()) {
        warnln("Both username and password are required for HTTP basic authentication.");
        return 1;
    }

    auto real_root_path = Core::File::real_path_for(root_path);

    if (!Core::File::exists(real_root_path)) {
        warnln("Root path does not exist: '{}'", root_path);
        return 1;
    }

    TRY(Core::System::pledge("stdio accept rpath inet unix"));

    WebServer::Configuration configuration(real_root_path);

    if (!username.is_empty() && !password.is_empty())
        configuration.set_credentials(HTTP::HttpRequest::BasicAuthenticationCredentials { username, password });

    Core::EventLoop loop;

    auto server = TRY(Core::TCPServer::try_create());

    server->on_ready_to_accept = [&] {
        auto maybe_client_socket = server->accept();
        if (maybe_client_socket.is_error()) {
            warnln("Failed to accept the client: {}", maybe_client_socket.error());
            return;
        }

        auto maybe_buffered_socket = Core::Stream::BufferedTCPSocket::create(maybe_client_socket.release_value());
        if (maybe_buffered_socket.is_error()) {
            warnln("Could not obtain a buffered socket for the client: {}", maybe_buffered_socket.error());
            return;
        }

        // FIXME: Propagate errors
        MUST(maybe_buffered_socket.value()->set_blocking(true));
        auto client = WebServer::Client::construct(maybe_buffered_socket.release_value(), server);
        client->start();
    };

    TRY(server->listen(ipv4_address.value(), port));

    outln("Listening on {}:{}", ipv4_address.value(), port);

    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil("/res/icons", "r"));
    TRY(Core::System::unveil(real_root_path.characters(), "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    TRY(Core::System::pledge("stdio accept rpath"));
    return loop.exec();
}
