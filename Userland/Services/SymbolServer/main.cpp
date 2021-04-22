/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibIPC/ClientConnection.h>
#include <SymbolServer/ClientConnection.h>

int main(int, char**)
{
    Core::EventLoop event_loop;
    auto server = Core::LocalServer::construct();

    if (pledge("stdio rpath accept", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/bin", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/usr/lib", "r") < 0) {
        perror("unveil");
        return 1;
    }

    // NOTE: Developers can opt into kernel symbolication by making /boot/Kernel accessible to the "symbol" user.
    if (access("/boot/Kernel", F_OK) == 0) {
        if (unveil("/boot/Kernel", "r") < 0) {
            perror("unveil");
            return 1;
        }
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    bool ok = server->take_over_from_system_server();
    VERIFY(ok);
    server->on_ready_to_accept = [&] {
        auto client_socket = server->accept();
        if (!client_socket) {
            dbgln("LaunchServer: accept failed.");
            return;
        }
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        IPC::new_client_connection<SymbolServer::ClientConnection>(client_socket.release_nonnull(), client_id);
    };

    return event_loop.exec();
}
