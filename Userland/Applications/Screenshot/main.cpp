/*
 * Copyright (c) 2024, circl <circl.lastname@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWindow.h"
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd thread cpath rpath wpath unix proc exec"));

    auto app = TRY(GUI::Application::create(arguments));
    app->set_config_domain("Screenshot"_string);

    auto window = TRY(Screenshot::MainWindow::try_create());

    window->show();

    return app->exec();
}
