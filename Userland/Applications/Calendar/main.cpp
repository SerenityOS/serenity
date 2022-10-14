/*
 * Copyright (c) 2019-2020, Ryan Grieb <ryan.m.grieb@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AddEventDialog.h"
#include <Applications/Calendar/CalendarWindowGML.h>
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Calendar.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Process.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio recvfd sendfd rpath proc exec unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    Config::pledge_domain("Calendar");
    Config::monitor_domain("Calendar");

    TRY(Core::System::pledge("stdio recvfd sendfd rpath proc exec"));
    TRY(Core::System::unveil("/etc/timezone", "r"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/bin/CalendarSettings", "x"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-calendar"sv));
    auto window = TRY(GUI::Window::try_create());
    window->set_title("Calendar");
    window->resize(600, 480);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto main_widget = TRY(window->try_set_main_widget<GUI::Widget>());
    main_widget->load_from_gml(calendar_window_gml);

    auto toolbar = main_widget->find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    auto calendar = main_widget->find_descendant_of_type_named<GUI::Calendar>("calendar");

    auto prev_date_action = GUI::Action::create({}, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-back.png"sv)), [&](const GUI::Action&) {
        unsigned view_month = calendar->view_month();
        unsigned view_year = calendar->view_year();
        if (calendar->mode() == GUI::Calendar::Month) {
            view_month--;
            if (calendar->view_month() == 1) {
                view_month = 12;
                view_year--;
            }
        } else {
            view_year--;
        }
        calendar->update_tiles(view_year, view_month);
    });

    auto next_date_action = GUI::Action::create({}, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png"sv)), [&](const GUI::Action&) {
        unsigned view_month = calendar->view_month();
        unsigned view_year = calendar->view_year();
        if (calendar->mode() == GUI::Calendar::Month) {
            view_month++;
            if (calendar->view_month() == 12) {
                view_month = 1;
                view_year++;
            }
        } else {
            view_year++;
        }
        calendar->update_tiles(view_year, view_month);
    });

    auto add_event_action = GUI::Action::create("&Add Event", {}, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/add-event.png"sv)), [&](const GUI::Action&) {
        AddEventDialog::show(calendar->selected_date(), window);
    });

    auto jump_to_action = GUI::Action::create("Jump to &Today", {}, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/calendar-date.png"sv)), [&](const GUI::Action&) {
        calendar->set_selected_date(Core::DateTime::now());
        calendar->update_tiles(Core::DateTime::now().year(), Core::DateTime::now().month());
    });

    auto view_month_action = GUI::Action::create_checkable("&Month View", { Mod_Ctrl, KeyCode::Key_1 }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/calendar-month-view.png"sv)), [&](const GUI::Action&) {
        if (calendar->mode() == GUI::Calendar::Year)
            calendar->toggle_mode();
    });
    view_month_action->set_checked(true);

    auto view_year_action = GUI::Action::create_checkable("&Year View", { Mod_Ctrl, KeyCode::Key_2 }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/icon-view.png"sv)), [&](const GUI::Action&) {
        if (calendar->mode() == GUI::Calendar::Month)
            calendar->toggle_mode();
    });

    auto view_type_action_group = make<GUI::ActionGroup>();
    view_type_action_group->set_exclusive(true);
    view_type_action_group->add_action(*view_month_action);
    view_type_action_group->add_action(*view_year_action);
    auto default_view = Config::read_string("Calendar"sv, "View"sv, "DefaultView"sv, "Month"sv);
    if (default_view == "Year")
        view_year_action->set_checked(true);

    auto open_settings_action = GUI::Action::create("&Settings", {}, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-settings.png"sv)), [&](GUI::Action const&) {
        GUI::Process::spawn_or_show_error(window, "/bin/CalendarSettings"sv);
    });

    (void)TRY(toolbar->try_add_action(prev_date_action));
    (void)TRY(toolbar->try_add_action(next_date_action));
    TRY(toolbar->try_add_separator());
    (void)TRY(toolbar->try_add_action(jump_to_action));
    (void)TRY(toolbar->try_add_action(add_event_action));
    TRY(toolbar->try_add_separator());
    (void)TRY(toolbar->try_add_action(view_month_action));
    (void)TRY(toolbar->try_add_action(view_year_action));
    (void)TRY(toolbar->try_add_action(open_settings_action));

    calendar->on_tile_doubleclick = [&] {
        AddEventDialog::show(calendar->selected_date(), window);
    };

    calendar->on_month_click = [&] {
        view_month_action->set_checked(true);
    };

    auto& file_menu = window->add_menu("&File");
    file_menu.add_action(GUI::Action::create("&Add Event", { Mod_Ctrl | Mod_Shift, Key_E }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/add-event.png"sv)),
        [&](const GUI::Action&) {
            AddEventDialog::show(calendar->selected_date(), window);
        }));
    file_menu.add_action(open_settings_action);

    TRY(file_menu.try_add_separator());

    TRY(file_menu.try_add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    })));

    auto view_menu = TRY(window->try_add_menu("&View"));
    TRY(view_menu->try_add_action(*view_month_action));
    TRY(view_menu->try_add_action(*view_year_action));

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_command_palette_action(window)));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Calendar", app_icon, window)));

    window->show();
    app->exec();

    return 0;
}
