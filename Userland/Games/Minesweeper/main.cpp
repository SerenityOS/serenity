/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Field.h"
#include <LibCore/ConfigFile.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/Window.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath wpath cpath recvfd sendfd unix", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio rpath wpath cpath recvfd sendfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto config = Core::ConfigFile::get_for_app("Minesweeper");

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(config->filename().characters(), "crw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-minesweeper");

    auto window = GUI::Window::construct();
    window->set_resizable(false);
    window->set_title("Minesweeper");
    window->resize(139, 175);

    auto& widget = window->set_main_widget<GUI::Widget>();
    widget.set_layout<GUI::VerticalBoxLayout>();
    widget.layout()->set_spacing(0);

    auto& top_line = widget.add<GUI::SeparatorWidget>(Gfx::Orientation::Horizontal);
    top_line.set_fixed_height(2);

    auto& container = widget.add<GUI::Widget>();
    container.set_fill_with_background_color(true);
    container.set_fixed_height(36);
    container.set_layout<GUI::HorizontalBoxLayout>();

    container.layout()->add_spacer();

    auto& flag_image = container.add<GUI::Label>();
    flag_image.set_icon(Gfx::Bitmap::load_from_file("/res/icons/minesweeper/flag.png"));
    flag_image.set_fixed_width(16);

    auto& flag_label = container.add<GUI::Label>();
    flag_label.set_autosize(true);
    flag_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    container.layout()->add_spacer();

    auto& face_button = container.add<GUI::Button>();
    face_button.set_focus_policy(GUI::FocusPolicy::TabFocus);
    face_button.set_button_style(Gfx::ButtonStyle::Coolbar);
    face_button.set_fixed_size(36, 36);

    container.layout()->add_spacer();

    auto& time_image = container.add<GUI::Label>();
    time_image.set_fixed_width(16);
    time_image.set_icon(Gfx::Bitmap::load_from_file("/res/icons/minesweeper/timer.png"));

    auto& time_label = container.add<GUI::Label>();
    time_label.set_autosize(true);
    time_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    container.layout()->add_spacer();

    auto& field = widget.add<Field>(flag_label, time_label, face_button, [&](auto size) {
        size.set_height(size.height() + container.min_size().height());
        window->resize(size);
    });

    auto menubar = GUI::Menubar::construct();

    auto& game_menu = menubar->add_menu("&Game");

    game_menu.add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, [&](auto&) {
        field.reset();
    }));

    game_menu.add_separator();

    auto chord_toggler_action = GUI::Action::create_checkable("Single-click chording", [&](auto& action) {
        field.set_single_chording(action.is_checked());
    });
    chord_toggler_action->set_checked(field.is_single_chording());

    game_menu.add_action(*chord_toggler_action);
    game_menu.add_separator();

    game_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& difficulty_menu = menubar->add_menu("&Difficulty");
    difficulty_menu.add_action(GUI::Action::create("&Beginner", { Mod_Ctrl, Key_B }, [&](auto&) {
        field.set_field_size(9, 9, 10);
    }));
    difficulty_menu.add_action(GUI::Action::create("&Intermediate", { Mod_Ctrl, Key_I }, [&](auto&) {
        field.set_field_size(16, 16, 40);
    }));
    difficulty_menu.add_action(GUI::Action::create("&Expert", { Mod_Ctrl, Key_E }, [&](auto&) {
        field.set_field_size(16, 30, 99);
    }));
    difficulty_menu.add_action(GUI::Action::create("&Madwoman", { Mod_Ctrl, Key_M }, [&](auto&) {
        field.set_field_size(32, 60, 350);
    }));

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Minesweeper", app_icon, window));

    window->set_menubar(move(menubar));

    window->show();

    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
