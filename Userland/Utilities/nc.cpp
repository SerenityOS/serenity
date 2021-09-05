/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/UDPSocket.h>
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

int main(int argc, char** argv)
{
    bool should_listen = false;
    bool verbose = false;
    bool should_close = false;
    bool udp_mode = false;
    const char* target = nullptr;
    int port = 0;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Network cat: Connect to network sockets as if it were a file.");
    args_parser.add_option(should_listen, "Listen instead of connecting", "listen", 'l');
    args_parser.add_option(verbose, "Log everything that's happening", "verbose", 'v');
    args_parser.add_option(udp_mode, "UDP mode", "udp", 'u');
    args_parser.add_option(should_close, "Close connection after reading stdin to the end", nullptr, 'N');
    args_parser.add_positional_argument(target, "Address to listen on, or the address or hostname to connect to", "target");
    args_parser.add_positional_argument(port, "Port to connect to or listen on", "port");
    args_parser.parse(argc, argv);

    if (udp_mode) {
        if (should_listen) {
            warnln("listening on UDP not yet supported");
            return 1;
        }

        Core::EventLoop loop;
        auto socket = Core::UDPSocket::construct();

        socket->on_connected = [&]() {
            if (verbose)
                warnln("connected to {}:{}", target, port);
        };
        socket->connect(target, port);

        for (;;) {
            char buf[1024];
            int nread = read(STDIN_FILENO, buf, sizeof(buf));
            if (nread < 0) {
                perror("read");
                return 1;
            }

            socket->send({ buf, static_cast<size_t>(nread) });
        }
    }

    int fd;

    if (should_listen) {
        int listen_fd = socket(AF_INET, SOCK_STREAM, 0);
        if (listen_fd < 0) {
            perror("socket");
            return 1;
        }

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

        if (bind(listen_fd, (struct sockaddr*)&sa, sizeof(sa)) == -1) {
            perror("bind");
            return 1;
        }

        if (listen(listen_fd, 1) == -1) {
            perror("listen");
            return 1;
        }

        char addr_str[INET_ADDRSTRLEN];

        sockaddr_in sin;
        socklen_t len;

        len = sizeof(sin);
        if (getsockname(listen_fd, (struct sockaddr*)&sin, &len) == -1) {
            perror("getsockname");
            return 1;
        }
        if (verbose)
            warnln("waiting for a connection on {}:{}", inet_ntop(sin.sin_family, &sin.sin_addr, addr_str, sizeof(addr_str) - 1), ntohs(sin.sin_port));

        len = sizeof(sin);
        fd = accept(listen_fd, (struct sockaddr*)&sin, &len);
        if (fd == -1) {
            perror("accept");
            return 1;
        }

        if (verbose)
            warnln("got connection from {}:{}", inet_ntop(sin.sin_family, &sin.sin_addr, addr_str, sizeof(addr_str) - 1), ntohs(sin.sin_port));

        if (close(listen_fd) == -1) {
            perror("close");
            return 1;
        };
    } else {
        fd = socket(AF_INET, SOCK_STREAM, 0);
        if (fd < 0) {
            perror("socket");
            return 1;
        }

        struct timeval timeout {
            3, 0
        };
        if (setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout)) < 0) {
            perror("setsockopt");
            return 1;
        }
        if (setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, &timeout, sizeof(timeout)) < 0) {
            perror("setsockopt");
            return 1;
        }

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
        if (connect(fd, (struct sockaddr*)&dst_addr, sizeof(dst_addr)) < 0) {
            perror("connect");
            return 1;
        }
        if (verbose)
            warnln("connected!");
    }

    bool stdin_closed = false;
    bool fd_closed = false;

    fd_set readfds, writefds, exceptfds;

    while (!stdin_closed || !fd_closed) {
        FD_ZERO(&readfds);
        FD_ZERO(&writefds);
        FD_ZERO(&exceptfds);

        int highest_fd = 0;

        if (!stdin_closed) {
            FD_SET(STDIN_FILENO, &readfds);
            FD_SET(STDIN_FILENO, &exceptfds);
            highest_fd = max(highest_fd, STDIN_FILENO);
        }
        if (!fd_closed) {
            FD_SET(fd, &readfds);
            FD_SET(fd, &exceptfds);
            highest_fd = max(highest_fd, fd);
        }

        int ready = select(highest_fd + 1, &readfds, &writefds, &exceptfds, nullptr);
        if (ready == -1) {
            if (errno == EINTR)
                continue;

            perror("select");
            return 1;
        }

        if (!stdin_closed && FD_ISSET(STDIN_FILENO, &readfds)) {
            char buf[1024];
            int nread = read(STDIN_FILENO, buf, sizeof(buf));
            if (nread < 0) {
                perror("read(STDIN_FILENO)");
                return 1;
            }

            // stdin closed
            if (nread == 0) {
                stdin_closed = true;
                if (verbose)
                    warnln("stdin closed");
                if (should_close) {
                    close(fd);
                    fd_closed = true;
                }
            } else if (write(fd, buf, nread) < 0) {
                perror("write(fd)");
                return 1;
            }
        }

        if (!fd_closed && FD_ISSET(fd, &readfds)) {
            char buf[1024];
            int nread = read(fd, buf, sizeof(buf));
            if (nread < 0) {
                perror("read(fd)");
                return 1;
            }

            // remote end closed
            if (nread == 0) {
                close(STDIN_FILENO);
                stdin_closed = true;
                fd_closed = true;
                if (verbose)
                    warnln("remote closed");
            } else if (write(STDOUT_FILENO, buf, nread) < 0) {
                perror("write(STDOUT_FILENO)");
                return 1;
            }
        }
    }

    return 0;
}
