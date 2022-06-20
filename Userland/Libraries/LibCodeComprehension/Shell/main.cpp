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
    TRY(Core::System::pledge("stdio unix rpath recvfd"));

    auto client = TRY(IPC::take_over_accepted_client_from_system_server<LanguageServers::Shell::ConnectionFromClient>());

    TRY(Core::System::pledge("stdio rpath recvfd"));
    TRY(Core::System::unveil("/etc/passwd", "r"));

    return event_loop.exec();
}
