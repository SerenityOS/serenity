/*
 * Copyright (c) 2021, Kyle Pereira <kyle@xylepereira.me>
 * Copyright (c) 2023, Caoimhe Byrne <caoimhebyrne06@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SpiceAgent.h"
#include <AK/URL.h>
#include <LibCore/StandardPaths.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibIPC/ConnectionToServer.h>
#include <LibMain/Main.h>
#include <fcntl.h>

static constexpr auto SPICE_DEVICE = "/dev/hvc0p1"sv;

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    // We use the application to be able to easily write to the user's clipboard.
    auto app = TRY(GUI::Application::create(arguments));

    TRY(Desktop::Launcher::add_allowed_url(URL::create_with_file_scheme(Core::StandardPaths::downloads_directory())));
    TRY(Desktop::Launcher::seal_allowlist());

    // FIXME: Make Core::File support reading and writing, but without creating:
    //        By default, Core::File opens the file descriptor with O_CREAT when using OpenMode::Write (and subsequently, OpenMode::ReadWrite).
    //        To minimise confusion for people that have already used Core::File, we can probably just do `OpenMode::ReadWrite | OpenMode::DontCreate`.
    TRY(Core::System::pledge("unix rpath wpath stdio sendfd recvfd cpath"));
    TRY(Core::System::unveil(SPICE_DEVICE, "rwc"sv));
    TRY(Core::System::unveil(Core::StandardPaths::downloads_directory(), "rwc"sv));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto agent = TRY(SpiceAgent::SpiceAgent::create(SPICE_DEVICE));

    agent->on_disconnected_from_spice_server = [&]() {
        app->quit();
    };

    TRY(agent->start());
    return app->exec();
}
