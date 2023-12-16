/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Directory.h>
#include <LibCore/EventLoop.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <LibIPC/MultiServer.h>
#include <LibMain/Main.h>
#include <SQLServer/ConnectionFromClient.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio accept unix rpath wpath cpath"));

    auto database_path = ByteString::formatted("{}/sql", Core::StandardPaths::data_directory());
    TRY(Core::Directory::create(database_path, Core::Directory::CreateDirectories::Yes));

    TRY(Core::System::unveil(database_path, "rwc"sv));
    TRY(Core::System::unveil(nullptr, nullptr));

    Core::EventLoop event_loop;

    auto server = TRY(IPC::MultiServer<SQLServer::ConnectionFromClient>::try_create());
    return event_loop.exec();
}
