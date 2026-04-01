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
#include <SSHServer/ServerConfiguration.h>
#include <SSHServer/TCPClient.h>

static constexpr auto DEFAULT_LISTEN_ADDRESS = "0.0.0.0"sv;
static constexpr auto DEFAULT_PORT = 22;

namespace {

// This is always null in the client process.
RefPtr<Core::TCPServer> g_tcp_server;

// This is always null in the listener process.
OwnPtr<SSH::Server::TCPClient> g_client;

ErrorOr<void> accept_connection()
{
    auto client_socket = TRY(g_tcp_server->accept());

    if (auto pid = TRY(Core::System::fork()); pid == 0) {
        // Close the server listening socket. This is deferred to not close
        // the server from within itself.
        Core::EventLoop::current().deferred_invoke([socket = move(client_socket)]() mutable {
            g_tcp_server = nullptr;
            auto on_quit = []() {
                g_client = nullptr;
                Core::EventLoop::current().quit(0);
            };
            g_client = SSH::Server::TCPClient::create(move(socket), move(on_quit));
        });
    } else {
        // The client socket will be close when leaving this function.
        // The server goes back to listening again.
    }
    return {};
}

}

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio accept inet unix rpath proc exec"));

    // FIXME: Audit the server architecture and add veils wherever possible.

    Optional<u32> port {};
    bool unsafe_stub_private_key { false };
    Optional<StringView> user_authorized_keys_file {};

    Core::ArgsParser parser;
    parser.add_option(port, "Port to listen on", "port", 'p', "port");
    parser.add_option(user_authorized_keys_file, "File to read the user's authorized keys from", "user-authorized-keys-file", 0, "FILE");
    parser.add_option(unsafe_stub_private_key, "Stub the server's private key - UNSAFE", "unsafe-stub-private-key");

    parser.parse(args);

    if (port.has_value() && *port != static_cast<u16>(*port))
        return Error::from_string_literal("Invalid port number");

    if (unsafe_stub_private_key)
        SSH::Server::ServerConfiguration::the().use_unsafe_stubbed_private_key();

    if (user_authorized_keys_file.has_value())
        SSH::Server::ServerConfiguration::the().set_user_authorized_keys_file(*user_authorized_keys_file);

    Core::EventLoop loop;

    g_tcp_server = TRY(Core::TCPServer::try_create());

    g_tcp_server->on_ready_to_accept = [] {
        auto maybe_error = accept_connection();
        if (maybe_error.is_error())
            warnln("Failed to accept client connection: {}", maybe_error.error());
    };

    TRY(g_tcp_server->listen(*IPv4Address::from_string(DEFAULT_LISTEN_ADDRESS), port.value_or(DEFAULT_PORT)));

    outln("Listening on {}:{}", g_tcp_server->local_address().value(), g_tcp_server->local_port());

    TRY(Core::System::pledge("stdio accept rpath proc exec"));
    return loop.exec();
}
