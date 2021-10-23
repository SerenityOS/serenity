/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Client.h"
#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/TCPServer.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <unistd.h>

static void run_command(int ptm_fd, String command)
{
    pid_t pid = fork();
    if (pid == 0) {
        const char* tty_name = ptsname(ptm_fd);
        if (!tty_name) {
            perror("ptsname");
            exit(1);
        }
        close(ptm_fd);
        int pts_fd = open(tty_name, O_RDWR);
        if (pts_fd < 0) {
            perror("open");
            exit(1);
        }

        // NOTE: It's okay if this fails.
        [[maybe_unused]] auto rc = ioctl(0, TIOCNOTTY);

        close(0);
        close(1);
        close(2);

        rc = dup2(pts_fd, 0);
        if (rc < 0) {
            perror("dup2");
            exit(1);
        }
        rc = dup2(pts_fd, 1);
        if (rc < 0) {
            perror("dup2");
            exit(1);
        }
        rc = dup2(pts_fd, 2);
        if (rc < 0) {
            perror("dup2");
            exit(1);
        }
        rc = close(pts_fd);
        if (rc < 0) {
            perror("close");
            exit(1);
        }
        rc = ioctl(0, TIOCSCTTY);
        if (rc < 0) {
            perror("ioctl(TIOCSCTTY)");
            exit(1);
        }
        const char* args[4] = { "/bin/Shell", nullptr, nullptr, nullptr };
        if (!command.is_empty()) {
            args[1] = "-c";
            args[2] = command.characters();
        }
        const char* envs[] = { "TERM=xterm", "PATH=/usr/local/bin:/usr/bin:/bin", nullptr };
        rc = execve("/bin/Shell", const_cast<char**>(args), const_cast<char**>(envs));
        if (rc < 0) {
            perror("execve");
            exit(1);
        }
        VERIFY_NOT_REACHED();
    }
}

int main(int argc, char** argv)
{
    int port = 23;
    const char* command = "";

    Core::ArgsParser args_parser;
    args_parser.add_option(port, "Port to listen on", nullptr, 'p', "port");
    args_parser.add_option(command, "Program to run on connection", nullptr, 'c', "command");
    args_parser.parse(argc, argv);

    if ((u16)port != port) {
        warnln("Invalid port number: {}", port);
        exit(1);
    }

    Core::EventLoop event_loop;
    auto server = Core::TCPServer::construct();

    if (!server->listen({}, port)) {
        warnln("Listening on 0.0.0.0:{} failed", port);
        exit(1);
    }

    HashMap<int, NonnullRefPtr<Client>> clients;
    int next_id = 0;

    server->on_ready_to_accept = [&next_id, &clients, &server, command] {
        int id = next_id++;

        auto client_socket = server->accept();
        if (!client_socket) {
            perror("accept");
            return;
        }

        int ptm_fd = posix_openpt(O_RDWR);
        if (ptm_fd < 0) {
            perror("posix_openpt");
            client_socket->close();
            return;
        }
        if (grantpt(ptm_fd) < 0) {
            perror("grantpt");
            client_socket->close();
            return;
        }
        if (unlockpt(ptm_fd) < 0) {
            perror("unlockpt");
            client_socket->close();
            return;
        }

        run_command(ptm_fd, command);

        auto client = Client::create(id, move(client_socket), ptm_fd);
        client->on_exit = [&clients, id] {
            Core::deferred_invoke([&clients, id] { clients.remove(id); });
        };
        clients.set(id, client);
    };

    int rc = event_loop.exec();
    if (rc != 0) {
        fprintf(stderr, "event loop exited badly; rc=%d", rc);
        exit(1);
    }

    return 0;
}
