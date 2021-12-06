/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <FileSystemAccessServer/ClientConnection.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibIPC/SingleServer.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath cpath wpath unix thread"));

    auto app = GUI::Application::construct(0, nullptr);
    app->set_quit_when_last_window_deleted(false);

    auto client = TRY(IPC::take_over_accepted_client_from_system_server<FileSystemAccessServer::ClientConnection>());
    return app->exec();
}
