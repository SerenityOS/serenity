/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CustomGameDialog.h"
#include "Field.h"
#include <AK/URL.h>
#include <Games/Minesweeper/MinesweeperWindowGML.h>
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
#include <stdio.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    Config::pledge_domain("Minesweeper");

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man6/Minesweeper.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));

    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-minesweeper"sv));

    auto window = TRY(GUI::Window::try_create());
    window->set_resizable(false);
    window->set_title("Minesweeper");
    window->resize(139, 177);

    auto widget = TRY(window->try_set_main_widget<GUI::Widget>());
    widget->load_from_gml(minesweeper_window_gml);

    auto& separator = *widget->find_descendant_of_type_named<GUI::HorizontalSeparator>("separator");
    auto& container = *widget->find_descendant_of_type_named<GUI::Widget>("container");
    auto& flag_label = *widget->find_descendant_of_type_named<GUI::Label>("flag_label");
    auto& time_label = *widget->find_descendant_of_type_named<GUI::Label>("time_label");
    auto& face_button = *widget->find_descendant_of_type_named<GUI::Button>("face_button");
    auto field = TRY(widget->try_add<Field>(flag_label, time_label, face_button, [&](auto size) {
        size.set_height(size.height() + separator.height() + container.height());
        window->resize(size);
    }));

    auto game_menu = TRY(window->try_add_menu("&Game"));

    TRY(game_menu->try_add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/reload.png"sv)), [&](auto&) {
        field->reset();
    })));

    TRY(game_menu->try_add_separator());

    auto chord_toggler_action = GUI::Action::create_checkable("Single-click chording", [&](auto& action) {
        field->set_single_chording(action.is_checked());
    });
    chord_toggler_action->set_checked(field->is_single_chording());

    TRY(game_menu->try_add_action(*chord_toggler_action));
    TRY(game_menu->try_add_separator());

    TRY(game_menu->try_add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    })));

    auto difficulty_menu = TRY(window->try_add_menu("&Difficulty"));
    GUI::ActionGroup difficulty_actions;
    difficulty_actions.set_exclusive(true);

    auto action = GUI::Action::create_checkable("&Beginner", { Mod_Ctrl, Key_B }, [&](auto&) {
        field->set_field_difficulty(Field::Difficulty::Beginner);
    });
    action->set_checked(field->difficulty() == Field::Difficulty::Beginner);
    TRY(difficulty_menu->try_add_action(action));
    difficulty_actions.add_action(action);

    action = GUI::Action::create_checkable("&Intermediate", { Mod_Ctrl, Key_I }, [&](auto&) {
        field->set_field_difficulty(Field::Difficulty::Intermediate);
    });
    action->set_checked(field->difficulty() == Field::Difficulty::Intermediate);
    TRY(difficulty_menu->try_add_action(action));
    difficulty_actions.add_action(action);

    action = GUI::Action::create_checkable("&Expert", { Mod_Ctrl, Key_E }, [&](auto&) {
        field->set_field_difficulty(Field::Difficulty::Expert);
    });
    action->set_checked(field->difficulty() == Field::Difficulty::Expert);
    TRY(difficulty_menu->try_add_action(action));
    difficulty_actions.add_action(action);

    action = GUI::Action::create_checkable("&Madwoman", { Mod_Ctrl, Key_M }, [&](auto&) {
        field->set_field_difficulty(Field::Difficulty::Madwoman);
    });
    action->set_checked(field->difficulty() == Field::Difficulty::Madwoman);
    TRY(difficulty_menu->try_add_action(action));
    difficulty_actions.add_action(action);

    TRY(difficulty_menu->try_add_separator());
    action = GUI::Action::create_checkable("&Custom game...", { Mod_Ctrl, Key_C }, [&](auto&) {
        CustomGameDialog::show(window, field);
    });
    action->set_checked(field->difficulty() == Field::Difficulty::Custom);
    TRY(difficulty_menu->try_add_action(action));
    difficulty_actions.add_action(action);

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_command_palette_action(window)));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man6/Minesweeper.md"), "/bin/Help");
    })));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Minesweeper", app_icon, window)));

    window->show();

    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
