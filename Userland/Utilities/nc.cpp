/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/HashTable.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <arpa/inet.h>
#include <errno.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>

// NOTE: `warnln` is used instead of `outln` because we want to redirect all
// output to stderr to allow for commands like:
//
// nc -l someport > out.file
//
// Below man page was considered to come up default bounds
// for SO_RCVBUF
// https://man7.org/linux/man-pages/man7/socket.7.html
static constexpr size_t maximum_tcp_receive_buffer_size_upper_bound = 212992;
static constexpr size_t maximum_tcp_receive_buffer_size_lower_bound = 256;

static size_t get_maximum_tcp_buffer_size(size_t input_buf_size)
{
    if (input_buf_size < maximum_tcp_receive_buffer_size_lower_bound)
        return maximum_tcp_receive_buffer_size_lower_bound;
    if (input_buf_size > maximum_tcp_receive_buffer_size_upper_bound)
        return maximum_tcp_receive_buffer_size_upper_bound;
    return input_buf_size;
};

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool should_listen = false;
    bool verbose = false;
    bool should_close = false;
    bool udp_mode = false;
    const char* target = nullptr;
    int port = 0;
    int maximum_tcp_receive_buffer_size_input = -1;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Network cat: Connect to network sockets as if it were a file.");
    args_parser.add_option(should_listen, "Listen instead of connecting", "listen", 'l');
    args_parser.add_option(verbose, "Log everything that's happening", "verbose", 'v');
    args_parser.add_option(udp_mode, "UDP mode", "udp", 'u');
    args_parser.add_option(should_close, "Close connection after reading stdin to the end", nullptr, 'N');
    args_parser.add_option(maximum_tcp_receive_buffer_size_input, "Set maximum tcp receive buffer size", "length", 'I', nullptr);
    args_parser.add_positional_argument(target, "Address to listen on, or the address or hostname to connect to", "target");
    args_parser.add_positional_argument(port, "Port to connect to or listen on", "port");
    args_parser.parse(arguments);

    if (udp_mode) {
        if (should_listen) {
            warnln("listening on UDP not yet supported");
            return 1;
        }

        Core::EventLoop loop;
        auto socket = TRY(Core::Stream::UDPSocket::connect(target, port));

        if (verbose)
            warnln("connected to {}:{}", target, port);

        Array<u8, 1024> buffer;
        for (;;) {
            Bytes buffer_span = buffer.span();
            auto nread = TRY(Core::System::read(STDIN_FILENO, buffer_span));
            buffer_span = buffer_span.trim(nread);

            TRY(socket->write({ buffer_span.data(), static_cast<size_t>(nread) }));
        }
    }

    int fd = -1;
    int listen_fd = -1;

    if (should_listen) {
        listen_fd = TRY(Core::System::socket(AF_INET, SOCK_STREAM, 0));

        sockaddr_in sa {};
        sa.sin_family = AF_INET;
        sa.sin_port = htons(port);
        sa.sin_addr.s_addr = htonl(INADDR_ANY);
        if (target) {
            if (inet_pton(AF_INET, target, &sa.sin_addr) <= 0) {
                perror("inet_pton");
                return 1;
            }
        }

        TRY(Core::System::bind(listen_fd, (struct sockaddr*)&sa, sizeof(sa)));
        TRY(Core::System::listen(listen_fd, 1));

        char addr_str[INET_ADDRSTRLEN];
        sockaddr_in sin;
        socklen_t len;

        len = sizeof(sin);
        TRY(Core::System::getsockname(listen_fd, (struct sockaddr*)&sin, &len));

        if (verbose)
            warnln("waiting for a connection on {}:{}", inet_ntop(sin.sin_family, &sin.sin_addr, addr_str, sizeof(addr_str) - 1), ntohs(sin.sin_port));

    } else {
        fd = TRY(Core::System::socket(AF_INET, SOCK_STREAM, 0));

        struct timeval timeout {
            3, 0
        };
        TRY(Core::System::setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout)));
        TRY(Core::System::setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, &timeout, sizeof(timeout)));

        auto* hostent = gethostbyname(target);
        if (!hostent) {
            warnln("Socket::connect: Unable to resolve '{}'", target);
            return 1;
        }

        sockaddr_in dst_addr {};
        dst_addr.sin_family = AF_INET;
        dst_addr.sin_port = htons(port);
        dst_addr.sin_addr.s_addr = *(const in_addr_t*)hostent->h_addr_list[0];

        if (verbose) {
            char addr_str[INET_ADDRSTRLEN];
            warnln("connecting to {}:{}", inet_ntop(dst_addr.sin_family, &dst_addr.sin_addr, addr_str, sizeof(addr_str) - 1), ntohs(dst_addr.sin_port));
        }

        TRY(Core::System::connect(fd, (struct sockaddr*)&dst_addr, sizeof(dst_addr)));
        if (verbose)
            warnln("connected!");
    }

    HashTable<int> connected_clients;

    bool stdin_closed = false;
    bool fd_closed = false;
    bool listen_fd_closed = false;

    fd_set readfds, writefds, exceptfds;

    size_t receive_buffer_size = get_maximum_tcp_buffer_size(maximum_tcp_receive_buffer_size_input);
    if (verbose && (maximum_tcp_receive_buffer_size_input != -1)) {
        warnln("receive_buffer_size set to {}", receive_buffer_size);
    }

    while (!stdin_closed || !fd_closed || !listen_fd_closed) {
        FD_ZERO(&readfds);
        FD_ZERO(&writefds);
        FD_ZERO(&exceptfds);

        int highest_fd = 0;

        if (!stdin_closed) {
            FD_SET(STDIN_FILENO, &readfds);
            FD_SET(STDIN_FILENO, &exceptfds);
            highest_fd = max(highest_fd, STDIN_FILENO);
        }
        if (!fd_closed && fd) {
            FD_SET(fd, &readfds);
            FD_SET(fd, &exceptfds);
            highest_fd = max(highest_fd, fd);
        }

        if (!listen_fd_closed && listen_fd) {
            FD_SET(listen_fd, &readfds);
            FD_SET(listen_fd, &exceptfds);
            highest_fd = max(highest_fd, listen_fd);
        }

        bool has_clients = (should_listen && !connected_clients.is_empty());
        if (has_clients) {
            for (auto const& client_fd : connected_clients) {
                FD_SET(client_fd, &readfds);
                FD_SET(client_fd, &exceptfds);
                highest_fd = max(highest_fd, client_fd);
            }
        }

        int ready = select(highest_fd + 1, &readfds, &writefds, &exceptfds, nullptr);
        if (ready == -1) {
            if (errno == EINTR)
                continue;

            perror("select");
            return 1;
        }

        if (!stdin_closed && FD_ISSET(STDIN_FILENO, &readfds)) {
            Array<u8, 1024> buffer;
            Bytes buffer_span = buffer.span();
            auto nread = TRY(Core::System::read(STDIN_FILENO, buffer_span));
            buffer_span = buffer_span.trim(nread);

            // stdin closed
            if (nread == 0) {
                stdin_closed = true;
                if (verbose)
                    warnln("stdin closed");
                if (should_close) {
                    if (should_listen) {
                        TRY(Core::System::close(listen_fd));
                        listen_fd_closed = true;
                    } else {
                        TRY(Core::System::close(fd));
                        fd_closed = true;
                    }
                }
            } else {
                if (should_listen && has_clients) {
                    for (auto const& client_fd : connected_clients)
                        TRY(Core::System::write(client_fd, buffer_span));
                } else {
                    TRY(Core::System::write(fd, buffer_span));
                }
            }
        }

        if (!fd_closed && FD_ISSET(fd, &readfds)) {
            auto buffer = TRY(ByteBuffer::create_uninitialized(receive_buffer_size));
            Bytes buffer_span = buffer.bytes();
            auto nread = TRY(Core::System::read(fd, buffer_span));
            buffer_span = buffer_span.trim(nread);

            // remote end closed
            if (nread == 0) {
                close(STDIN_FILENO);
                stdin_closed = true;
                fd_closed = true;
                if (verbose)
                    warnln("remote closed");
            } else {
                TRY(Core::System::write(STDOUT_FILENO, buffer_span));
            }
        }

        if (!listen_fd_closed && FD_ISSET(listen_fd, &readfds)) {
            char client_str[INET_ADDRSTRLEN];
            sockaddr_in client;
            socklen_t clientlen = sizeof(client);

            int new_client = TRY(Core::System::accept(listen_fd, (struct sockaddr*)&client, &clientlen));
            connected_clients.set(new_client);

            if (verbose)
                warnln("got connection from {}:{}", inet_ntop(client.sin_family, &client.sin_addr, client_str, sizeof(client_str) - 1), ntohs(client.sin_port));
        }

        if (has_clients) {
            for (auto const client_fd : connected_clients) {
                if (FD_ISSET(client_fd, &readfds)) {
                    Array<u8, 1024> buffer;
                    Bytes buffer_span = buffer.span();
                    auto nread = TRY(Core::System::read(client_fd, buffer_span));
                    buffer_span = buffer_span.trim(nread);

                    if (nread == 0) {
                        if (verbose) {
                            struct sockaddr_in client;
                            socklen_t clientlen = sizeof(client);
                            TRY(Core::System::getpeername(client_fd, (struct sockaddr*)&client, &clientlen));
                            warnln("remote connection closed {}:{}", inet_ntoa(client.sin_addr), ntohs(client.sin_port));
                        }
                        connected_clients.remove(client_fd);
                        close(client_fd);
                        FD_CLR(client_fd, &readfds);
                        FD_CLR(client_fd, &exceptfds);
                    } else {
                        TRY(Core::System::write(STDOUT_FILENO, buffer_span));
                    }
                }
            }
        }
    }

    return 0;
}
