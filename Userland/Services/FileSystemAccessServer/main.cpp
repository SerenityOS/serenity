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

ErrorOr<int> serenity_main(Main::Arguments)
{
    using enum Kernel::Pledge;
    TRY((Core::System::Promise<stdio, Kernel::Pledge::recvfd, Kernel::Pledge::sendfd, rpath, cpath, wpath, unix, thread>::pledge()));

    auto app = GUI::Application::construct(0, nullptr);
    app->set_quit_when_last_window_deleted(false);

    auto client = TRY(IPC::take_over_accepted_client_from_system_server<FileSystemAccessServer::ConnectionFromClient>());
    return app->exec();
}
