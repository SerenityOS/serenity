/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DHCPv4Client.h"
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

const char* serenity_get_initial_promises()
{
    return "stdio unix inet cpath rpath";
}

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop event_loop;

    TRY(Core::System::unveil("/proc/net/", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto client = TRY(DHCPv4Client::try_create());
    TRY(Core::System::retract("unix"));

    return event_loop.exec();
}
