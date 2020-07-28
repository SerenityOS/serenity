/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibCore/ArgsParser.h>
#include <arpa/inet.h>
#include <errno.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    bool should_listen = false;
    bool verbose = false;
    bool should_close = false;
    const char* addr = nullptr;
    int port = 0;

    Core::ArgsParser args_parser;
    args_parser.add_option(should_listen, "Listen instead of connecting", "listen", 'l');
    args_parser.add_option(verbose, "Log everything that's happening", "verbose", 'v');
    args_parser.add_option(should_close, "Close connection after reading stdin to the end", nullptr, 'N');
    args_parser.add_positional_argument(addr, "Address to connect to or listen on", "address");
    args_parser.add_positional_argument(port, "Port to connect to or listen on", "port");
    args_parser.parse(argc, argv);

    int fd;

    if (should_listen) {
        int listen_fd = socket(AF_INET, SOCK_STREAM, 0);
        if (listen_fd < 0) {
            perror("socket");
            return 1;
        }

        struct sockaddr_in sa;
        memset(&sa, 0, sizeof sa);
        sa.sin_family = AF_INET;
        sa.sin_port = htons(port);
        sa.sin_addr.s_addr = htonl(INADDR_ANY);
        if (addr) {
            if (inet_pton(AF_INET, addr, &sa.sin_addr) < 0) {
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

        char addr_str[100];

        struct sockaddr_in sin;
        socklen_t len;

        len = sizeof(sin);
        if (getsockname(listen_fd, (struct sockaddr*)&sin, &len) == -1) {
            perror("getsockname");
            return 1;
        }
        if (verbose)
            fprintf(stderr, "waiting for a connection on %s:%d\n", inet_ntop(sin.sin_family, &sin.sin_addr, addr_str, sizeof(addr_str) - 1), ntohs(sin.sin_port));

        len = sizeof(sin);
        fd = accept(listen_fd, (struct sockaddr*)&sin, &len);
        if (fd == -1) {
            perror("accept");
            return 1;
        }

        if (verbose)
            fprintf(stderr, "got connection from %s:%d\n", inet_ntop(sin.sin_family, &sin.sin_addr, addr_str, sizeof(addr_str) - 1), ntohs(sin.sin_port));

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

        char addr_str[100];

        struct sockaddr_in dst_addr;
        memset(&dst_addr, 0, sizeof(dst_addr));

        dst_addr.sin_family = AF_INET;
        dst_addr.sin_port = htons(port);
        if (inet_pton(AF_INET, addr, &dst_addr.sin_addr) < 0) {
            perror("inet_pton");
            return 1;
        }

        if (verbose)
            fprintf(stderr, "connecting to %s:%d\n", inet_ntop(dst_addr.sin_family, &dst_addr.sin_addr, addr_str, sizeof(addr_str) - 1), ntohs(dst_addr.sin_port));
        if (connect(fd, (struct sockaddr*)&dst_addr, sizeof(dst_addr)) < 0) {
            perror("connect");
            return 1;
        }
        if (verbose)
            fprintf(stderr, "connected!\n");
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

        int ready = select(highest_fd + 1, &readfds, &writefds, &exceptfds, NULL);
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
                    fprintf(stderr, "stdin closed\n");
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
                    fprintf(stderr, "remote closed\n");
            } else if (write(STDOUT_FILENO, buf, nread) < 0) {
                perror("write(STDOUT_FILENO)");
                return 1;
            }
        }
    }

    return 0;
}
