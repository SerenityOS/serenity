/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DashboardWindow.h"
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd proc exec rpath unix"));
    auto app = TRY(GUI::Application::try_create(arguments));
    TRY(Core::System::pledge("stdio recvfd sendfd proc exec rpath"));

    auto app_icon = GUI::Icon::default_icon("app-dashboard"sv);
    auto window = TRY(DashboardWindow::try_create());
    window->show();
    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
