/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <FileSystemAccessServer/ClientConnection.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibIPC/ClientConnection.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath cpath wpath unix thread"));

    auto app = GUI::Application::construct(0, nullptr);
    app->set_quit_when_last_window_deleted(false);

    auto socket = TRY(Core::LocalSocket::take_over_accepted_socket_from_system_server());
    (void)IPC::new_client_connection<FileSystemAccessServer::ClientConnection>(move(socket), 1);
    return app->exec();
}
