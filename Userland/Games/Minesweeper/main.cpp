/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Window.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath accept wpath cpath recvfd sendfd unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio rpath accept wpath cpath recvfd sendfd", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto config = Core::ConfigFile::get_for_app("Minesweeper");

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(config->file_name().characters(), "crw") < 0) {
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

    auto& container = widget.add<GUI::Widget>();
    container.set_fill_with_background_color(true);
    container.set_fixed_height(36);
    container.set_layout<GUI::HorizontalBoxLayout>();

    auto& flag_image = container.add<GUI::ImageWidget>();
    flag_image.load_from_file("/res/icons/minesweeper/flag.png");

    auto& flag_label = container.add<GUI::Label>();
    auto& face_button = container.add<GUI::Button>();
    face_button.set_focus_policy(GUI::FocusPolicy::TabFocus);
    face_button.set_button_style(Gfx::ButtonStyle::Coolbar);
    face_button.set_fixed_size(36, 36);

    auto& time_image = container.add<GUI::ImageWidget>();
    time_image.load_from_file("/res/icons/minesweeper/timer.png");

    auto& time_label = container.add<GUI::Label>();
    auto& field = widget.add<Field>(flag_label, time_label, face_button, [&](auto size) {
        size.set_height(size.height() + container.min_size().height());
        window->resize(size);
    });

    auto menubar = GUI::Menubar::construct();

    auto& app_menu = menubar->add_menu("Game");

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
    help_menu.add_action(GUI::CommonActions::make_about_action("Minesweeper", app_icon, window));

    window->set_menubar(move(menubar));

    window->show();

    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
