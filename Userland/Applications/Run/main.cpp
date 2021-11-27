/*
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RunWindow.h"
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Desktop.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd thread cpath rpath wpath unix proc exec"));

    auto app = TRY(GUI::Application::try_create(arguments));
    auto window = TRY(RunWindow::try_create());

    window->move_to(16, GUI::Desktop::the().rect().bottom() - GUI::Desktop::the().taskbar_height() - 16 - window->height());
    window->show();

    return app->exec();
}
