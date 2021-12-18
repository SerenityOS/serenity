/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GalleryWidget.h"
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Frame.h>
#include <LibGUI/MessageBox.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath wpath cpath unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-model-gallery"));

    auto window = TRY(GUI::Window::try_create());
    window->set_title("Model Gallery");
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(430, 480);
    (void)TRY(window->try_set_main_widget<GalleryWidget>());

    window->show();
    return app->exec();
}
