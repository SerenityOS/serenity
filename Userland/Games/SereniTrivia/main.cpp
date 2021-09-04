/*
 * Copyright (c) 2021, xSlendiX <gamingxslendix@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TriviaWidget.h"
#include <LibConfig/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd rpath unix proc exec", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    Config::pledge_domains("SereniTrivia");

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/home", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/webcontent", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/bin/Help", "x") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-serenitrivia");

    auto window = GUI::Window::construct();
    window->resize(480, 200);
    window->set_resizable(false);
    window->center_on_screen();

    window->set_title("SereniTrivia");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->set_main_widget<SereniTrivia::TriviaWidget>();

    window->show();

    return app->exec();
}
