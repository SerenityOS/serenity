/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Mixer.h"
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibCore/LocalServer.h>

int main(int, char**)
{
    if (pledge("stdio recvfd thread accept cpath rpath wpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto config = Core::ConfigFile::get_for_app("Audio", Core::ConfigFile::AllowWriting::Yes);
    if (unveil(config->filename().characters(), "rwc") < 0) {
        perror("unveil");
        return 1;
    }
    if (unveil("/dev/audio", "wc") < 0) {
        perror("unveil");
        return 1;
    }
    unveil(nullptr, nullptr);

    Core::EventLoop event_loop;
    AudioServer::Mixer mixer { config };

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

    if (pledge("stdio recvfd thread accept cpath rpath wpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    unveil(nullptr, nullptr);

    return event_loop.exec();
}
