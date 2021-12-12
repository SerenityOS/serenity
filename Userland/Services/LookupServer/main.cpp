/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LookupServer.h"
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

const char* serenity_get_initial_promises()
{
    return "stdio accept unix inet rpath";
}

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop event_loop;
    auto server = TRY(LookupServer::LookupServer::try_create());
    TRY(Core::System::retract("unix"));

    TRY(Core::System::unveil("/proc/net/adapters", "r"));
    TRY(Core::System::unveil("/etc/hosts", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));
    return event_loop.exec();
}
