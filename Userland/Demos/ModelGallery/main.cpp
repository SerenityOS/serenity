/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GalleryWidget.h"
#include <LibGUI/Application.h>
#include <LibGUI/MessageBox.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd rpath wpath cpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-model-gallery");

    auto window = GUI::Window::construct();
    window->set_title("Model Gallery");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(430, 480);
    window->set_main_widget<GalleryWidget>();

    window->show();
    return app->exec();
}
