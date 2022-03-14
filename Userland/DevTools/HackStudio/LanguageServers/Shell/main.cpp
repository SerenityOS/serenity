/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionFromClient.h"
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibIPC/SingleServer.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop event_loop;
    using enum Kernel::Pledge;
    TRY((Core::System::Promise<stdio, unix, rpath, Kernel::Pledge::recvfd>::pledge()));

    auto client = TRY(IPC::take_over_accepted_client_from_system_server<LanguageServers::Shell::ConnectionFromClient>());

    TRY((Core::System::Promise<stdio, rpath, Kernel::Pledge::recvfd>::pledge()));
    TRY(Core::System::unveil("/etc/passwd", "r"));

    return event_loop.exec();
}
