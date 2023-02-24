/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionFromClient.h"
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <LibIPC/MultiServer.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio accept rpath wpath cpath"));
    TRY(Core::System::unveil(Core::StandardPaths::config_directory(), "rwc"sv));
    TRY(Core::System::unveil(Core::StandardPaths::home_directory(), "rwc"sv));
    TRY(Core::System::unveil(nullptr, nullptr));

    Core::EventLoop event_loop;

    auto server = TRY(IPC::MultiServer<ConfigServer::ConnectionFromClient>::try_create());
    return event_loop.exec();
}
