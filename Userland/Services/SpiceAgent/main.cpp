/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SpiceAgent.h"
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibIPC/ConnectionToServer.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>
#include <fcntl.h>

static constexpr auto SPICE_DEVICE = "/dev/hvc0p1"sv;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    if (!FileSystem::exists(SPICE_DEVICE)) {
        return Error::from_string_literal("Failed to find spice device file!");
    }

    // We use the application to be able to easily write to the user's clipboard.
    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::pledge("unix rpath wpath cpath stdio sendfd recvfd"));
    TRY(Core::System::unveil(SPICE_DEVICE, "rw"sv));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/notify", "rw"));
    TRY(Core::System::unveil(Core::StandardPaths::downloads_directory(), "rwc"sv));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto agent = TRY(SpiceAgent::SpiceAgent::create(SPICE_DEVICE));

    agent->on_disconnected_from_spice_server = [&]() {
        app->quit();
    };

    TRY(agent->start());
    return app->exec();
}
