/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibIPC/MultiServer.h>
#include <LibMain/Main.h>
#include <SQLServer/ConnectionFromClient.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio accept unix rpath wpath cpath"));
    TRY(Core::System::mkdir("/home/anon/sql", 0700, Core::System::TreatExistingDirectoryAsError::No, Core::System::CreateParentDirectories::No));
    TRY(Core::System::unveil("/home/anon/sql", "rwc"));
    TRY(Core::System::unveil(nullptr, nullptr));

    Core::EventLoop event_loop;

    auto server = TRY(IPC::MultiServer<SQLServer::ConnectionFromClient>::try_create());
    return event_loop.exec();
}
