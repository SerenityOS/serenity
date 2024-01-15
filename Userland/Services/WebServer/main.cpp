/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 * Copyright (c) 2022, Thomas Keppler <serenity@tkeppler.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <LibCore/TCPServer.h>
#include <LibFileSystem/FileSystem.h>
#include <LibHTTP/HttpRequest.h>
#include <LibMain/Main.h>
#include <WebServer/Client.h>
#include <WebServer/Configuration.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    static auto const default_listen_address = "0.0.0.0"_string;
    static auto const default_port = 8000;
    static auto const default_document_root_path = "/www"_string;

    ByteString listen_address = default_listen_address.to_byte_string();
    int port = default_port;
    ByteString username;
    ByteString password;
    ByteString document_root_path = default_document_root_path.to_byte_string();

    Core::ArgsParser args_parser;
    args_parser.add_option(listen_address, "IP address to listen on", "listen-address", 'l', "listen_address");
    args_parser.add_option(port, "Port to listen on", "port", 'p', "port");
    args_parser.add_option(username, "HTTP basic authentication username", "user", 'U', "username");
    args_parser.add_option(password, "HTTP basic authentication password", "pass", 'P', "password");
    args_parser.add_positional_argument(document_root_path, "Path to serve the contents of", "path", Core::ArgsParser::Required::No);
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

    auto real_document_root_path = TRY(FileSystem::real_path(document_root_path));
    if (!FileSystem::exists(real_document_root_path)) {
        warnln("Root path does not exist: '{}'", document_root_path);
        return 1;
    }

    TRY(Core::System::pledge("stdio accept rpath inet unix"));

    Optional<HTTP::HttpRequest::BasicAuthenticationCredentials> credentials;
    if (!username.is_empty() && !password.is_empty())
        credentials = HTTP::HttpRequest::BasicAuthenticationCredentials { username, password };

    // FIXME: This should accept a ByteString for the path instead.
    WebServer::Configuration configuration(TRY(String::from_byte_string(real_document_root_path)), credentials);

    Core::EventLoop loop;

    auto server = TRY(Core::TCPServer::try_create());

    server->on_ready_to_accept = [&] {
        auto maybe_client_socket = server->accept();
        if (maybe_client_socket.is_error()) {
            warnln("Failed to accept the client: {}", maybe_client_socket.error());
            return;
        }

        auto maybe_buffered_socket = Core::BufferedTCPSocket::create(maybe_client_socket.release_value());
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

    out("Listening on ");
    out("\033]8;;http://{}:{}\033\\", ipv4_address.value(), server->local_port());
    out("{}:{}", ipv4_address.value(), server->local_port());
    outln("\033]8;;\033\\");

    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil("/res/icons", "r"));
    TRY(Core::System::unveil(real_document_root_path, "r"sv));
    TRY(Core::System::unveil(nullptr, nullptr));

    TRY(Core::System::pledge("stdio accept rpath"));
    return loop.exec();
}
