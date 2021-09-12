/*
 * Copyright (c) 2020, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MouseSettingsWindow.h"
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio cpath rpath recvfd sendfd unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio cpath rpath recvfd sendfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-mouse");

    auto window = MouseSettingsWindow::construct();
    window->set_title("Mouse Settings");
    window->resize(400, 480);
    window->set_resizable(false);
    window->set_minimizable(false);
    window->set_icon(app_icon.bitmap_for_size(16));

    window->show();
    return app->exec();
}
