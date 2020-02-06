/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "Field.h"
#include <LibCore/CConfigFile.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath accept wpath cpath shared_buffer unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GUI::Application app(argc, argv);

    if (pledge("stdio rpath accept wpath cpath shared_buffer", nullptr) < 0) {
        perror("pledge");
        return 1;
    }


    auto window = GUI::Window::construct();
    window->set_resizable(false);
    window->set_title("Minesweeper");
    window->set_rect(100, 100, 139, 175);

    auto widget = GUI::Widget::construct();
    window->set_main_widget(widget);
    widget->set_layout(make<GUI::VerticalBoxLayout>());
    widget->layout()->set_spacing(0);

    auto container = GUI::Widget::construct(widget.ptr());
    container->set_fill_with_background_color(true);
    container->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    container->set_preferred_size(0, 36);
    container->set_layout(make<GUI::HorizontalBoxLayout>());
    auto flag_icon_label = GUI::Label::construct(container);
    flag_icon_label->set_icon(Gfx::Bitmap::load_from_file("/res/icons/minesweeper/flag.png"));
    auto flag_label = GUI::Label::construct(container);
    auto face_button = GUI::Button::construct(container);
    face_button->set_button_style(Gfx::ButtonStyle::CoolBar);
    face_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    face_button->set_preferred_size(36, 0);
    auto time_icon_label = GUI::Label::construct(container);
    time_icon_label->set_icon(Gfx::Bitmap::load_from_file("/res/icons/minesweeper/timer.png"));
    auto time_label = GUI::Label::construct(container);
    auto field = Field::construct(*flag_label, *time_label, *face_button, widget, [&](auto size) {
        size.set_height(size.height() + container->preferred_size().height());
        window->resize(size);
    });

    auto menubar = make<GUI::MenuBar>();

    auto app_menu = GUI::Menu::construct("Minesweeper");

    app_menu->add_action(GUI::Action::create("New game", { Mod_None, Key_F2 }, [&](const GUI::Action&) {
        field->reset();
    }));


    app_menu->add_separator();


    NonnullRefPtr<GUI::Action> chord_toggler_action = GUI::Action::create("Single-click chording", [&](const GUI::Action&) {
        bool toggled = !field->is_single_chording();
        field->set_single_chording(toggled);
        chord_toggler_action->set_checked(toggled);
    });
    chord_toggler_action->set_checkable(true);
    chord_toggler_action->set_checked(field->is_single_chording());

    app_menu->add_action(*chord_toggler_action);
    app_menu->add_separator();

    app_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the().quit(0);
        return;
    }));
    menubar->add_menu(move(app_menu));

    auto difficulty_menu = GUI::Menu::construct("Difficulty");
    difficulty_menu->add_action(GUI::Action::create("Beginner", { Mod_Ctrl, Key_B }, [&](const GUI::Action&) {
        field->set_field_size(9, 9, 10);
    }));
    difficulty_menu->add_action(GUI::Action::create("Intermediate", { Mod_Ctrl, Key_I }, [&](const GUI::Action&) {
        field->set_field_size(16, 16, 40);
    }));
    difficulty_menu->add_action(GUI::Action::create("Expert", { Mod_Ctrl, Key_E }, [&](const GUI::Action&) {
        field->set_field_size(16, 30, 99);
    }));
    difficulty_menu->add_action(GUI::Action::create("Madwoman", { Mod_Ctrl, Key_M }, [&](const GUI::Action&) {
        field->set_field_size(32, 60, 350);
    }));
    menubar->add_menu(move(difficulty_menu));

    auto help_menu = GUI::Menu::construct("Help");
    help_menu->add_action(GUI::Action::create("About", [&](const GUI::Action&) {
        GUI::AboutDialog::show("Minesweeper", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-minesweeper.png"), window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    window->show();

    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/minesweeper/mine.png"));

    return app.exec();
}
