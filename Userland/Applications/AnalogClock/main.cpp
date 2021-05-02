/*
 * Copyright (c) 2021, Erlend Høier <Erlend@ReasonablePanic.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AnalogClock.h"
#include <LibCore/DateTime.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <unistd.h>

int main(int argc, char** argv)
{

    if (pledge("stdio recvfd sendfd accept rpath unix cpath wpath fattr thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd accept rpath cpath wpath thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-analog-clock");
    auto window = GUI::Window::construct();

    window->set_main_widget<AnalogClock>();
    window->set_title(Core::DateTime::now().to_string("Clock %d-%m-%Y"));
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(170, 170);
    window->set_resizable(false);
    window->set_double_buffering_enabled(true);

    window->show();
    return app->exec();
}
