/*
 * Copyright (c) 2021, Erlend Høier <Erlend@ReasonablePanic.com>
 * Copyright (c) 2021, Julius Heijmen <julius.heijmen@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AnalogClock.h"
#include <LibCore/DateTime.h>
#include <LibCore/System.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <LibTimeZone/TimeZone.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto app = TRY(GUI::Application::create(arguments));

    TRY(Core::System::pledge("stdio recvfd sendfd rpath"));
    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-analog-clock"sv));
    auto window = GUI::Window::construct();
    window->set_title(Core::DateTime::now().to_byte_string("%Y-%m-%d"sv));
    window->set_icon(app_icon.bitmap_for_size(16));
    window->resize(170, 170);
    window->set_resizable(false);
    auto clock = window->set_main_widget<AnalogClock>();

    auto show_window_frame_action = GUI::Action::create_checkable(
        "Show Window &Frame", { Mod_Alt, KeyCode::Key_F }, [&](auto& action) {
            clock->set_show_window_frame(action.is_checked());
        });
    show_window_frame_action->set_checked(clock->show_window_frame());
    auto menu = GUI::Menu::construct();
    menu->add_action(*show_window_frame_action);

    menu->add_separator();

    menu->add_action(GUI::Action::create_checkable(
        "Show Time Zone",
        [&clock](auto& action) {
            clock->set_show_time_zone(action.is_checked());
        }));

    auto timezone_submenu = menu->add_submenu("Time Zone"_string);

    GUI::ActionGroup timezone_action_group;
    timezone_action_group.set_exclusive(true);

    for (auto time_zone : TimeZone::all_time_zones()) {
        auto timezone_action = GUI::Action::create_checkable(
            time_zone.name,
            [&clock](auto& action) {
                clock->set_time_zone(action.text());
            });

        timezone_action_group.add_action(timezone_action);
        timezone_submenu->add_action(timezone_action);
    }

    auto reset_to_system_time_zone_action = GUI::Action::create(
        "Reset to System Time Zone",
        [&timezone_action_group](auto&) {
            auto system_time_zone = TimeZone::system_time_zone();

            timezone_action_group.for_each_action([&system_time_zone](auto& action) {
                if (action.text() == system_time_zone) {
                    action.activate();
                    return IterationDecision::Break;
                }
                return IterationDecision::Continue;
            });
        });

    menu->add_action(reset_to_system_time_zone_action);
    reset_to_system_time_zone_action->activate();

    clock->on_context_menu_request = [&](auto& event) {
        menu->popup(event.screen_position());
    };

    window->show();
    return app->exec();
}
