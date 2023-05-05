/*
 * Copyright (c) 2021, timmot <tiwwot@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <FileSystemAccessServer/ConnectionFromClient.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibIPC/SingleServer.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath cpath wpath unix thread"));

    auto app = TRY(GUI::Application::create(arguments));
    app->set_quit_when_last_window_deleted(false);

    auto client = TRY(IPC::take_over_accepted_client_from_system_server<FileSystemAccessServer::ConnectionFromClient>());
    return app->exec();
}
