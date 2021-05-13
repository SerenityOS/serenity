/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Mixer.h"
#include <LibCore/File.h>
#include <LibCore/LocalServer.h>

int main(int, char**)
{
    if (pledge("stdio recvfd thread accept cpath rpath wpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Core::EventLoop event_loop;
    AudioServer::Mixer mixer;

    auto server = Core::LocalServer::construct();
    bool ok = server->take_over_from_system_server();
    VERIFY(ok);
    server->on_ready_to_accept = [&] {
        auto client_socket = server->accept();
        if (!client_socket) {
            dbgln("AudioServer: accept failed.");
            return;
        }
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        IPC::new_client_connection<AudioServer::ClientConnection>(client_socket.release_nonnull(), client_id, mixer);
    };

    if (pledge("stdio recvfd thread accept", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    unveil(nullptr, nullptr);

    return event_loop.exec();
}
