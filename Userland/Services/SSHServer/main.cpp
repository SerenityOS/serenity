/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibCore/TCPServer.h>
#include <LibMain/Main.h>
#include <SSHServer/TCPClient.h>

static constexpr auto DEFAULT_LISTEN_ADDRESS = "0.0.0.0"sv;
static constexpr auto DEFAULT_PORT = 22;

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio accept inet unix"));
    TRY(Core::System::unveil(nullptr, nullptr));

    Optional<u32> port {};

    Core::ArgsParser parser;
    parser.add_option(port, "Port to listen on", "port", 'p', "port");

    parser.parse(args);

    if (port.has_value() && *port != static_cast<u16>(*port))
        return Error::from_string_literal("Invalid port number");

    Core::EventLoop loop;

    auto tcp_server = TRY(Core::TCPServer::try_create());

    tcp_server->on_ready_to_accept = [&] {
        auto maybe_client_socket = tcp_server->accept();
        if (maybe_client_socket.is_error()) {
            warnln("Failed to accept the client: {}", maybe_client_socket.error());
            return;
        }

        auto client = SSH::Server::TCPClient::create(maybe_client_socket.release_value(), tcp_server);
    };

    TRY(tcp_server->listen(*IPv4Address::from_string(DEFAULT_LISTEN_ADDRESS), port.value_or(DEFAULT_PORT)));

    outln("Listening on {}:{}", tcp_server->local_address().value(), tcp_server->local_port());

    TRY(Core::System::pledge("stdio accept"));
    return loop.exec();
}
