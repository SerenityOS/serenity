/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <FileSystemAccessServer/ClientConnection.h>
#include <LibCore/EventLoop.h>
#include <LibGUI/Application.h>
#include <LibIPC/ClientConnection.h>

int main(int, char**)
{
    if (pledge("stdio recvfd sendfd rpath cpath wpath unix thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(0, nullptr);
    app->set_quit_when_last_window_deleted(false);

    auto socket = Core::LocalSocket::take_over_accepted_socket_from_system_server();
    IPC::new_client_connection<FileSystemAccessServer::ClientConnection>(socket.release_nonnull(), 1);
    return app->exec();
}
