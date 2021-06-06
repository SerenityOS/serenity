/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Clipboard/ClientConnection.h>
#include <Clipboard/Storage.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibIPC/ClientConnection.h>

int main(int, char**)
{
    if (pledge("stdio recvfd sendfd accept", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
    Core::EventLoop event_loop;
    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto server = Core::LocalServer::construct();
    bool ok = server->take_over_from_system_server();
    VERIFY(ok);

    server->on_ready_to_accept = [&] {
        auto client_socket = server->accept();
        if (!client_socket) {
            dbgln("Clipboard: accept failed.");
            return;
        }
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        IPC::new_client_connection<Clipboard::ClientConnection>(client_socket.release_nonnull(), client_id);
    };

    Clipboard::Storage::the().on_content_change = [&] {
        Clipboard::ClientConnection::for_each_client([&](auto& client) {
            client.notify_about_clipboard_change();
        });
    };

    return event_loop.exec();
}
