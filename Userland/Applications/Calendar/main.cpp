/*
 * Copyright (c) 2019-2020, Ryan Grieb <ryan.m.grieb@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AddEventDialog.h"
#include <Applications/Calendar/CalendarWindowGML.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Calendar.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/Window.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio recvfd sendfd rpath unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    auto app_icon = GUI::Icon::default_icon("app-calendar");
    auto window = GUI::Window::construct();
    window->set_title("Calendar");
    window->resize(600, 480);
    window->set_minimum_size(171, 141);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.load_from_gml(calendar_window_gml);

    auto toolbar = main_widget.find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    auto calendar = main_widget.find_descendant_of_type_named<GUI::Calendar>("calendar");

    auto prev_date_action = GUI::Action::create({}, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-back.png").release_value_but_fixme_should_propagate_errors(), [&](const GUI::Action&) {
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

    auto next_date_action = GUI::Action::create({}, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png").release_value_but_fixme_should_propagate_errors(), [&](const GUI::Action&) {
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

    auto add_event_action = GUI::Action::create("&Add Event", {}, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/add-event.png").release_value_but_fixme_should_propagate_errors(), [&](const GUI::Action&) {
        AddEventDialog::show(calendar->selected_date(), window);
    });

    auto jump_to_action = GUI::Action::create("Jump to &Today", {}, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/calendar-date.png").release_value_but_fixme_should_propagate_errors(), [&](const GUI::Action&) {
        calendar->set_selected_date(Core::DateTime::now());
        calendar->update_tiles(Core::DateTime::now().year(), Core::DateTime::now().month());
    });

    auto view_month_action = GUI::Action::create_checkable("&Month View", { Mod_Ctrl, KeyCode::Key_1 }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/calendar-month-view.png").release_value_but_fixme_should_propagate_errors(), [&](const GUI::Action&) {
        if (calendar->mode() == GUI::Calendar::Year)
            calendar->toggle_mode();
    });
    view_month_action->set_checked(true);

    auto view_year_action = GUI::Action::create_checkable("&Year View", { Mod_Ctrl, KeyCode::Key_2 }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/icon-view.png").release_value_but_fixme_should_propagate_errors(), [&](const GUI::Action&) {
        if (calendar->mode() == GUI::Calendar::Month)
            calendar->toggle_mode();
    });

    auto view_type_action_group = make<GUI::ActionGroup>();
    view_type_action_group->set_exclusive(true);
    view_type_action_group->add_action(*view_month_action);
    view_type_action_group->add_action(*view_year_action);

    toolbar->add_action(prev_date_action);
    toolbar->add_action(next_date_action);
    toolbar->add_separator();
    toolbar->add_action(jump_to_action);
    toolbar->add_action(add_event_action);
    toolbar->add_separator();
    toolbar->add_action(view_month_action);
    toolbar->add_action(view_year_action);

    calendar->on_tile_doubleclick = [&] {
        AddEventDialog::show(calendar->selected_date(), window);
    };

    calendar->on_month_click = [&] {
        view_month_action->set_checked(true);
    };

    auto& file_menu = window->add_menu("&File");
    file_menu.add_action(GUI::Action::create("&Add Event", { Mod_Ctrl | Mod_Shift, Key_E }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/add-event.png").release_value_but_fixme_should_propagate_errors(),
        [&](const GUI::Action&) {
            AddEventDialog::show(calendar->selected_date(), window);
        }));

    file_menu.add_separator();

    file_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& view_menu = window->add_menu("&View");
    view_menu.add_action(*view_month_action);
    view_menu.add_action(*view_year_action);

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Calendar", app_icon, window));

    window->show();
    app->exec();
}
