/*
 * Copyright (c) 2020, Nicholas Hollett <niax@niax.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionFromClient.h"
#include "Launcher.h"
#include <LibCore/ConfigFile.h>
#include <LibCore/EventLoop.h>
#include <LibCore/System.h>
#include <LibIPC/MultiServer.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    Core::EventLoop event_loop;
    auto server = TRY(IPC::MultiServer<LaunchServer::ConnectionFromClient>::try_create());

    auto launcher = LaunchServer::Launcher();
    launcher.load_handlers();
    launcher.load_config(TRY(Core::ConfigFile::open_for_app("LaunchServer")));

    TRY(Core::System::pledge("stdio accept rpath proc exec"));

    return event_loop.exec();
}
