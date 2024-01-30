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

    auto app = TRY(GUI::Application::create(arguments));
    auto window = TRY(Run::RunWindow::try_create());

    constexpr int margin = 16;
    window->move_to(margin, GUI::Desktop::the().rect().bottom() - 1 - GUI::Desktop::the().taskbar_height() - margin - window->height());
    window->show();

    return app->exec();
}
