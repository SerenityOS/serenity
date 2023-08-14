/*
 * Copyright (c) 2021, Erlend Høier <Erlend@ReasonablePanic.com>
 * Copyright (c) 2021, Julius Heijmen <julius.heijmen@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AnalogClock.h"
#include <LibCore/DateTime.h>
#include <LibCore/System.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));
    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-analog-clock"sv));
    auto window = TRY(GUI::Window::try_create());
    window->set_title(Core::DateTime::now().to_deprecated_string("%Y-%m-%d"sv));
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(170, 170);
    window->set_resizable(false);
    auto clock = TRY(window->set_main_widget<AnalogClock>());

    auto show_window_frame_action = GUI::Action::create_checkable(
        "Show Window &Frame", { Mod_Alt, KeyCode::Key_F }, [&](auto& action) {
            clock->set_show_window_frame(action.is_checked());
        });
    show_window_frame_action->set_checked(clock->show_window_frame());
    auto menu = TRY(GUI::Menu::try_create());
    menu->add_action(*show_window_frame_action);

    clock->on_context_menu_request = [&](auto& event) {
        menu->popup(event.screen_position());
    };

    window->show();
    return app->exec();
}
