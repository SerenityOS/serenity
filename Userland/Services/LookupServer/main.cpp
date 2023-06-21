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

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio accept unix inet rpath"));
    Core::EventLoop event_loop;
    auto server = TRY(LookupServer::LookupServer::try_create());

    TRY(Core::System::pledge("stdio accept inet rpath"));
    TRY(Core::System::unveil("/sys/kernel/net/adapters", "r"));
    TRY(Core::System::unveil("/etc/hosts", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));
    return event_loop.exec();
}
