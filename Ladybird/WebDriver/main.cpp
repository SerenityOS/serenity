/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#define AK_DONT_REPLACE_STD

#include "../Utilities.h"
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibCore/TCPServer.h>
#include <LibMain/Main.h>
#include <WebDriver/Client.h>

extern String s_serenity_resource_root;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto listen_address = "0.0.0.0"sv;
    int port = 8000;

    Core::ArgsParser args_parser;
    args_parser.add_option(listen_address, "IP address to listen on", "listen-address", 'l', "listen_address");
    args_parser.add_option(port, "Port to listen on", "port", 'p', "port");
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

    platform_init();

    Core::EventLoop loop;
    auto server = TRY(Core::TCPServer::try_create());

    // FIXME: Propagate errors
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

        auto maybe_client = WebDriver::Client::try_create(maybe_buffered_socket.release_value(), server);
        if (maybe_client.is_error()) {
            warnln("Could not create a WebDriver client: {}", maybe_client.error());
            return;
        }
    };

    TRY(server->listen(ipv4_address.value(), port, Core::TCPServer::AllowAddressReuse::Yes));
    outln("Listening on {}:{}", ipv4_address.value(), port);

    return loop.exec();
}
