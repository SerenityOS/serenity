/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Application.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::try_create(arguments));
    auto window = GUI::Window::construct();

    window->set_title("Hello LibGUI World");
    window->resize(400, 600);
    window->show();
    return app->exec();
}
