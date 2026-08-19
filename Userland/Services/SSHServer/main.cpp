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
    auto fd = client_socket->fd();

    if (auto pid = TRY(Core::System::fork()); pid == 0) {
        // Close the server listening socket. This is deferred to not close
        // the server from within itself.

        // We need to ensure the socket survives until the deferred_invoke, but
        // fd will be closed at the end of the scope with client_socket.
        auto fd_copy = TRY(Core::System::dup(fd));
        TRY(Core::System::fcntl(fd_copy, F_SETFD, FD_CLOEXEC));

        Core::EventLoop::current().deferred_invoke([fd_copy]() mutable {
            g_tcp_server = nullptr;
            auto on_quit = []() {
                g_client = nullptr;
                Core::EventLoop::current().quit(0);
            };

            Core::EventLoop::notify_forked(Core::EventLoop::ForkEvent::Child);

            auto socket = MUST(Core::TCPSocket::adopt_fd(fd_copy));
            g_client = SSH::Server::TCPClient::create(move(socket), move(on_quit));
        });
    } else {
        // The client socket will be closed when leaving this function.
        // The server goes back to listening again.
    }
    return {};
}

}

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio accept inet unix rpath wpath cpath proc exec sigaction id"));

    // FIXME: Make the server use more low-privilege processes.
    //        When receiving a connection, the main server forks and the child handles
    //        the request. As an SSH server needs to perform many powerful actions, the
    //        child remains a high-privilege process. This design is quite dangerous as
    //        the probably easily abusable network code is done by this same high-privilege
    //        process. When running the server as root a simple bug in the network handling
    //        code can result in a compromised root-running binary.
    //        Splitting the server in more low-privilege processes should solve this
    //        safety hazard. What is proposed here is similar to the OpenSSH
    //        architecture:
    //         - The main process (this file) should first set up the PrivilegedWorker,
    //           and then only accept connections and spawn NetworkParser processes for
    //           them.
    //         - NetworkParser processes should only parse SSH packets and defer any
    //           privileged actions to the PrivilegedWorker.
    //         - The PrivilegedWorker should be responsible for performing privileged
    //           actions requested by the NetworkParser processes (reading configuration
    //           files, authentification, spawning child processes etc…).
    //        After setup, the privileges of the processes should be as follow:
    //         - Main process: pledge(stdio accept spawn), unveil(NetworkParser binary)
    //         - NetworkParser: pledge(stdio recvfd unix), unveil()
    //         - PrivilegedWorker: pledge(stdio rpath wpath cpath sendfd id proc exec sigaction unix), veil-free.

    // FIXME: Audit the server architecture and add veils wherever possible.

    Optional<u32> port {};
    bool unsafe_stub_private_key { false };
    Optional<StringView> user_authorized_keys_file {};
    Optional<StringView> keylog_file {};

    Core::ArgsParser parser;
    parser.add_option(port, "Port to listen on", "port", 'p', "port");
    parser.add_option(user_authorized_keys_file, "File to read the user's authorized keys from", "user-authorized-keys-file", 0, "FILE");
    parser.add_option(keylog_file, "File to log the connections keys to - UNSAFE", "unsafe-keylog-file", 0, "FILE");
    parser.add_option(unsafe_stub_private_key, "Stub the server's private key - UNSAFE", "unsafe-stub-private-key");

    parser.parse(args);

    if (port.has_value() && *port != static_cast<u16>(*port))
        return Error::from_string_literal("Invalid port number");

    if (unsafe_stub_private_key)
        SSH::Server::ServerConfiguration::the().use_unsafe_stubbed_private_key();

    if (user_authorized_keys_file.has_value())
        SSH::Server::ServerConfiguration::the().set_user_authorized_keys_file(*user_authorized_keys_file);

    if (keylog_file.has_value())
        SSH::Server::ServerConfiguration::the().set_keylog_file(*keylog_file);

    Core::EventLoop loop;

    Core::EventLoop::register_signal(SIGCHLD, [&](int) {
        while (true) {
            auto maybe_result = Core::System::waitpid(-1, WNOHANG);
            if (maybe_result.is_error()) {
                auto error = maybe_result.release_error();
                if (error.code() == EINTR)
                    continue;
                if (error.code() == ECHILD)
                    return;
                dbgln("Error while reaping child processes: {}", error);
                loop.quit(-1);
            }
            auto wait_result = maybe_result.value();
            if (wait_result.pid == 0)
                return;

            if (WIFSIGNALED(wait_result.status))
                warnln("Connection process exited with: signal({})", WTERMSIG(wait_result.status));
            else if (WIFEXITED(wait_result.status) && WEXITSTATUS(wait_result.status) > 0)
                warnln("Connection process exited with: exit code({})", WEXITSTATUS(wait_result.status));
        }
    });

    g_tcp_server = TRY(Core::TCPServer::try_create());

    g_tcp_server->on_ready_to_accept = [] {
        auto maybe_error = accept_connection();
        if (maybe_error.is_error())
            warnln("Failed to accept client connection: {}", maybe_error.error());
    };

    TRY(g_tcp_server->listen(*IPv4Address::from_string(DEFAULT_LISTEN_ADDRESS), port.value_or(DEFAULT_PORT)));

    outln("Listening on {}:{}", g_tcp_server->local_address().value(), g_tcp_server->local_port());

    TRY(Core::System::pledge("stdio accept rpath wpath cpath proc exec sigaction id"));
    return loop.exec();
}
