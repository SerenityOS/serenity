/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CustomGameDialog.h"
#include "Field.h"
#include "MainWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>
#include <stdio.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::create(arguments));

    Config::pledge_domain("Minesweeper");

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man6/Minesweeper.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));

    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-minesweeper"sv));

    auto window = GUI::Window::construct();
    window->set_resizable(false);
    window->set_title("Minesweeper");
    window->set_auto_shrink(true);

    auto main_widget = TRY(Minesweeper::MainWidget::try_create());
    window->set_main_widget(main_widget);

    auto& flag_label = *main_widget->find_descendant_of_type_named<GUI::Label>("flag_label");
    auto& time_label = *main_widget->find_descendant_of_type_named<GUI::Label>("time_label");
    auto& face_button = *main_widget->find_descendant_of_type_named<GUI::Button>("face_button");
    auto field = TRY(Field::create(flag_label, time_label, face_button));
    TRY(main_widget->try_add_child(field));

    auto game_menu = window->add_menu("&Game"_string);

    game_menu->add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/reload.png"sv)), [&](auto&) {
        field->reset();
    }));

    game_menu->add_separator();

    auto chord_toggler_action = GUI::Action::create_checkable("&Single-click Chording", [&](auto& action) {
        field->set_single_chording(action.is_checked());
    });
    chord_toggler_action->set_checked(field->is_single_chording());

    game_menu->add_action(*chord_toggler_action);
    game_menu->add_separator();

    // Put Fullscreen in Game rather than View
    // When in beginner mode it can only show 3 menus. Adding View makes 4
    game_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));
    game_menu->add_separator();

    game_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto difficulty_menu = window->add_menu("&Difficulty"_string);
    GUI::ActionGroup difficulty_actions;
    difficulty_actions.set_exclusive(true);

    auto action = GUI::Action::create_checkable("&Beginner", { Mod_Ctrl, Key_B }, [&](auto&) {
        field->set_field_difficulty(Field::Difficulty::Beginner);
    });
    action->set_checked(field->difficulty() == Field::Difficulty::Beginner);
    difficulty_menu->add_action(action);
    difficulty_actions.add_action(action);

    action = GUI::Action::create_checkable("&Intermediate", { Mod_Ctrl, Key_I }, [&](auto&) {
        field->set_field_difficulty(Field::Difficulty::Intermediate);
    });
    action->set_checked(field->difficulty() == Field::Difficulty::Intermediate);
    difficulty_menu->add_action(action);
    difficulty_actions.add_action(action);

    action = GUI::Action::create_checkable("&Expert", { Mod_Ctrl, Key_E }, [&](auto&) {
        field->set_field_difficulty(Field::Difficulty::Expert);
    });
    action->set_checked(field->difficulty() == Field::Difficulty::Expert);
    difficulty_menu->add_action(action);
    difficulty_actions.add_action(action);

    action = GUI::Action::create_checkable("&Madwoman", { Mod_Ctrl, Key_M }, [&](auto&) {
        field->set_field_difficulty(Field::Difficulty::Madwoman);
    });
    action->set_checked(field->difficulty() == Field::Difficulty::Madwoman);
    difficulty_menu->add_action(action);
    difficulty_actions.add_action(action);

    difficulty_menu->add_separator();
    action = GUI::Action::create_checkable("&Custom Game...", { Mod_Ctrl, Key_C }, [&](auto&) {
        Minesweeper::CustomGameDialog::show(window, field);
    });
    action->set_checked(field->difficulty() == Field::Difficulty::Custom);
    difficulty_menu->add_action(action);
    difficulty_actions.add_action(action);

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man6/Minesweeper.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Minesweeper"_string, app_icon, window));

    window->show();

    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
