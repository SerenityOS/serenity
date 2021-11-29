/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DHCPv4Client.h"
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio unix inet cpath rpath"));
    Core::EventLoop event_loop;

    TRY(Core::System::unveil("/proc/net/", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto client = TRY(DHCPv4Client::try_create());

    TRY(Core::System::pledge("stdio inet cpath rpath"));
    return event_loop.exec();
}
