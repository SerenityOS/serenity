/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GalleryWidget.h"
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath unix thread"));
    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/etc/FileIconProvider.ini", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));
    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-widget-gallery"sv));

    auto window = GUI::Window::construct();
    window->resize(430, 480);
    window->set_title("Widget Gallery");
    window->set_icon(app_icon.bitmap_for_size(16));
    (void)window->set_main_widget<GalleryWidget>();
    window->show();

    return app->exec();
}
