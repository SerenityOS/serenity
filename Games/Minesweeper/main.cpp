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
#include <LibCore/ConfigFile.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/Window.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath accept wpath cpath shared_buffer unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio rpath accept wpath cpath shared_buffer", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto window = GUI::Window::construct();
    window->set_resizable(false);
    window->set_title("Minesweeper");
    window->resize(139, 175);

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.set_layout<GUI::VerticalBoxLayout>();
    widget.layout()->set_spacing(0);

    auto& container = widget.add<GUI::Widget>();
    container.set_fill_with_background_color(true);
    container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    container.set_preferred_size(0, 36);
    container.set_layout<GUI::HorizontalBoxLayout>();

    auto& flag_image = container.add<GUI::ImageWidget>();
    flag_image.load_from_file("/res/icons/minesweeper/flag.png");

    auto& flag_label = container.add<GUI::Label>();
    auto& face_button = container.add<GUI::Button>();
    face_button.set_button_style(Gfx::ButtonStyle::CoolBar);
    face_button.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    face_button.set_preferred_size(36, 0);

    auto& time_image = container.add<GUI::ImageWidget>();
    time_image.load_from_file("/res/icons/minesweeper/timer.png");

    auto& time_label = container.add<GUI::Label>();
    auto& field = widget.add<Field>(flag_label, time_label, face_button, [&](auto size) {
        size.set_height(size.height() + container.preferred_size().height());
        window->resize(size);
    });

    auto menubar = GUI::MenuBar::construct();

    auto& app_menu = menubar->add_menu("Minesweeper");

    app_menu.add_action(GUI::Action::create("New game", { Mod_None, Key_F2 }, [&](auto&) {
        field.reset();
    }));

    app_menu.add_separator();

    auto chord_toggler_action = GUI::Action::create_checkable("Single-click chording", [&](auto& action) {
        field.set_single_chording(action.is_checked());
    });
    chord_toggler_action->set_checked(field.is_single_chording());

    app_menu.add_action(*chord_toggler_action);
    app_menu.add_separator();

    app_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
        return;
    }));

    auto& difficulty_menu = menubar->add_menu("Difficulty");
    difficulty_menu.add_action(GUI::Action::create("Beginner", { Mod_Ctrl, Key_B }, [&](auto&) {
        field.set_field_size(9, 9, 10);
    }));
    difficulty_menu.add_action(GUI::Action::create("Intermediate", { Mod_Ctrl, Key_I }, [&](auto&) {
        field.set_field_size(16, 16, 40);
    }));
    difficulty_menu.add_action(GUI::Action::create("Expert", { Mod_Ctrl, Key_E }, [&](auto&) {
        field.set_field_size(16, 30, 99);
    }));
    difficulty_menu.add_action(GUI::Action::create("Madwoman", { Mod_Ctrl, Key_M }, [&](auto&) {
        field.set_field_size(32, 60, 350);
    }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [&](auto&) {
        GUI::AboutDialog::show("Minesweeper", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-minesweeper.png"), window);
    }));

    app->set_menubar(move(menubar));

    window->show();

    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/minesweeper/mine.png"));

    return app->exec();
}
