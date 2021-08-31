/*
 * Copyright (c) 2021, the SerenityOS Developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ScreenshotWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/ArgsParser.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd cpath wpath rpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Core::ArgsParser args_parser;

    String output_path;
    args_parser.add_positional_argument(output_path, "Output filename", "output", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto app = GUI::Application::construct(argc, argv);

    Config::pledge_domains("Screenshot");

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/home", "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/tmp/portal/notify", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-screenshot");

    auto window = GUI::Window::construct();
    window->resize(380, 150);
    window->center_on_screen();
    window->set_resizable(false);

    window->set_title("Screenshot");
    window->set_icon(app_icon.bitmap_for_size(16));
    RefPtr<ScreenshotWidget> screenshotwidget = window->set_main_widget<ScreenshotWidget>();
    if (!output_path.is_empty()) {
        dbgln("Path: {}", output_path);
        screenshotwidget->set_path(output_path);
    }

    window->show();

    return app->exec();
}
