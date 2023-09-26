/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MainWidget.h"
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int>
serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::create(arguments));

    auto window = GUI::Window::construct();
    window->set_title("GL Clock Demo");
    window->resize(600, 250);
    window->set_resizable(false);
    window->set_double_buffering_enabled(true);

    auto main_widget = TRY(GLClockDemo::MainWidget::try_create());
    window->set_main_widget(main_widget);
    window->show();

    return app->exec();
}
