/*
 * Copyright (c) 2019-2020, Ryan Grieb <ryan.m.grieb@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "AddEventDialog.h"
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Calendar.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/ToolBar.h>
#include <LibGUI/ToolBarContainer.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Color.h>
#include <LibGfx/Font.h>
#include <stdio.h>

int main(int argc, char** argv)
{

    if (pledge("stdio shared_buffer rpath accept unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio shared_buffer rpath accept", nullptr) < 0) {
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
    window->set_icon(app_icon.bitmap_for_size(16));

    auto& root_container = window->set_main_widget<GUI::Widget>();
    root_container.set_fill_with_background_color(true);
    root_container.set_layout<GUI::VerticalBoxLayout>();

    auto& toolbar_container = root_container.add<GUI::ToolBarContainer>();
    auto& toolbar = toolbar_container.add<GUI::ToolBar>();

    auto& calendar_container = root_container.add<GUI::Frame>();
    calendar_container.set_layout<GUI::VerticalBoxLayout>();
    calendar_container.layout()->set_margins({ 2, 2, 2, 2 });
    auto& calendar_widget = calendar_container.add<GUI::Calendar>(Core::DateTime::now());

    RefPtr<GUI::Button> selected_calendar_button;

    auto prev_date_action = GUI::Action::create("Previous date", { Mod_Alt, Key_Left }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-back.png"), [&](const GUI::Action&) {
        unsigned int target_month = calendar_widget.selected_month();
        unsigned int target_year = calendar_widget.selected_year();

        if (calendar_widget.mode() == GUI::Calendar::Month) {
            target_month--;
            if (calendar_widget.selected_month() <= 1) {
                target_month = 12;
                target_year--;
            }
        } else {
            target_year--;
        }

        calendar_widget.update_tiles(target_year, target_month);
        selected_calendar_button->set_text(calendar_widget.selected_calendar_text());
    });

    auto next_date_action = GUI::Action::create("Next date", { Mod_Alt, Key_Right }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"), [&](const GUI::Action&) {
        unsigned int target_month = calendar_widget.selected_month();
        unsigned int target_year = calendar_widget.selected_year();

        if (calendar_widget.mode() == GUI::Calendar::Month) {
            target_month++;
            if (calendar_widget.selected_month() >= 12) {
                target_month = 1;
                target_year++;
            }
        } else {
            target_year++;
        }

        calendar_widget.update_tiles(target_year, target_month);
        selected_calendar_button->set_text(calendar_widget.selected_calendar_text());
    });

    auto add_event_action = GUI::Action::create("Add event", {}, Gfx::Bitmap::load_from_file("/res/icons/16x16/add-event.png"), [&](const GUI::Action&) {
        AddEventDialog::show(calendar_widget.selected_date(), window);
    });

    auto jump_to_action = GUI::Action::create("Jump to today", {}, Gfx::Bitmap::load_from_file("/res/icons/16x16/calendar-date.png"), [&](const GUI::Action&) {
        if (calendar_widget.mode() == GUI::Calendar::Year)
            calendar_widget.toggle_mode();
        calendar_widget.set_selected_date(Core::DateTime::now());
        calendar_widget.update_tiles(Core::DateTime::now().year(), Core::DateTime::now().month());
        selected_calendar_button->set_text(calendar_widget.selected_calendar_text());
    });

    toolbar.add_action(prev_date_action);
    selected_calendar_button = toolbar.add<GUI::Button>(calendar_widget.selected_calendar_text());
    selected_calendar_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    selected_calendar_button->set_preferred_size(70, 0);
    selected_calendar_button->set_button_style(Gfx::ButtonStyle::CoolBar);
    selected_calendar_button->set_font(Gfx::Font::default_bold_fixed_width_font());
    selected_calendar_button->on_click = [&](auto) {
        calendar_widget.toggle_mode();
        selected_calendar_button->set_text(calendar_widget.selected_calendar_text());
    };
    toolbar.add_action(next_date_action);
    toolbar.add_separator();
    toolbar.add_action(jump_to_action);
    toolbar.add_action(add_event_action);

    calendar_widget.on_calendar_tile_click = [&] {
        selected_calendar_button->set_text(calendar_widget.selected_calendar_text());
    };

    calendar_widget.on_calendar_tile_doubleclick = [&] {
        AddEventDialog::show(calendar_widget.selected_date(), window);
    };

    calendar_widget.on_month_tile_click = [&] {
        selected_calendar_button->set_text(calendar_widget.selected_calendar_text());
    };

    auto menubar = GUI::MenuBar::construct();
    auto& app_menu = menubar->add_menu("Calendar");
    app_menu.add_action(GUI::Action::create("Add Event", { Mod_Ctrl | Mod_Shift, Key_E }, Gfx::Bitmap::load_from_file("/res/icons/16x16/add-event.png"),
        [&](const GUI::Action&) {
            AddEventDialog::show(calendar_widget.selected_date(), window);
            return;
        }));

    app_menu.add_separator();

    app_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
        return;
    }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [&](auto&) {
        GUI::AboutDialog::show("Calendar", app_icon.bitmap_for_size(32), window);
    }));

    app->set_menubar(move(menubar));
    window->show();
    app->exec();
}
