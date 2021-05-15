/*
 * Copyright (c) 2021, Erlend Høier <Erlend@ReasonablePanic.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AnalogClock.h"
#include <LibCore/DateTime.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd rpath", nullptr) < 0) {
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
    window->set_title(Core::DateTime::now().to_string("%Y-%m-%d"));
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(170, 170);
    window->set_resizable(false);
    auto& clock = window->set_main_widget<AnalogClock>();

    auto show_window_frame_action = GUI::Action::create_checkable(
        "Show Window &Frame", { Mod_Alt, KeyCode::Key_F }, [&](auto& action) {
            clock.set_show_window_frame(action.is_checked());
        });
    show_window_frame_action->set_checked(clock.show_window_frame());
    auto menu = GUI::Menu::construct();
    menu->add_action(move(show_window_frame_action));
    clock.on_context_menu_request = [&](auto& event) {
        menu->popup(event.screen_position());
    };

    window->show();
    return app->exec();
}
