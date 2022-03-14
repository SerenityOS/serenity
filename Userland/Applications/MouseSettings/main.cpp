/*
 * Copyright (c) 2020, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MouseWidget.h"
#include "ThemeWidget.h"
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/SettingsWindow.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    using enum Kernel::Pledge;
    TRY((Core::System::Promise<stdio, cpath, rpath, Kernel::Pledge::recvfd, Kernel::Pledge::sendfd, unix>::pledge()));

    auto app = TRY(GUI::Application::try_create(arguments));

    TRY((Core::System::Promise<stdio, cpath, rpath, Kernel::Pledge::recvfd, Kernel::Pledge::sendfd>::pledge()));

    auto app_icon = GUI::Icon::default_icon("app-mouse");

    auto window = TRY(GUI::SettingsWindow::create("Mouse Settings", GUI::SettingsWindow::ShowDefaultsButton::Yes));
    (void)TRY(window->add_tab<MouseWidget>("Mouse"));
    (void)TRY(window->add_tab<ThemeWidget>("Cursor Theme"));
    window->set_icon(app_icon.bitmap_for_size(16));

    window->show();
    return app->exec();
}
