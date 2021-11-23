/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CustomGameDialog.h"
#include "Field.h"
#include <LibConfig/Client.h>
#include <LibCore/System.h>
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
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix", nullptr));

    auto app = GUI::Application::construct(arguments);

    Config::pledge_domains("Minesweeper");

    TRY(Core::System::pledge("stdio rpath recvfd sendfd", nullptr));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

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
    flag_image.set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/minesweeper/flag.png").release_value_but_fixme_should_propagate_errors());
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
    time_image.set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/minesweeper/timer.png").release_value_but_fixme_should_propagate_errors());

    auto& time_label = container.add<GUI::Label>();
    time_label.set_fixed_width(50);
    time_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);

    container.layout()->add_spacer();

    auto& field = widget.add<Field>(flag_label, time_label, face_button, [&](auto size) {
        size.set_height(size.height() + container.min_size().height());
        window->resize(size);
    });

    auto& game_menu = window->add_menu("&Game");

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

    auto& difficulty_menu = window->add_menu("&Difficulty");
    GUI::ActionGroup difficulty_actions;
    difficulty_actions.set_exclusive(true);

    auto action = GUI::Action::create_checkable("&Beginner", { Mod_Ctrl, Key_B }, [&](auto&) {
        field.set_field_difficulty(Field::Difficulty::Beginner);
    });
    action->set_checked(field.difficulty() == Field::Difficulty::Beginner);
    difficulty_menu.add_action(action);
    difficulty_actions.add_action(action);

    action = GUI::Action::create_checkable("&Intermediate", { Mod_Ctrl, Key_I }, [&](auto&) {
        field.set_field_difficulty(Field::Difficulty::Intermediate);
    });
    action->set_checked(field.difficulty() == Field::Difficulty::Intermediate);
    difficulty_menu.add_action(action);
    difficulty_actions.add_action(action);

    action = GUI::Action::create_checkable("&Expert", { Mod_Ctrl, Key_E }, [&](auto&) {
        field.set_field_difficulty(Field::Difficulty::Expert);
    });
    action->set_checked(field.difficulty() == Field::Difficulty::Expert);
    difficulty_menu.add_action(action);
    difficulty_actions.add_action(action);

    action = GUI::Action::create_checkable("&Madwoman", { Mod_Ctrl, Key_M }, [&](auto&) {
        field.set_field_difficulty(Field::Difficulty::Madwoman);
    });
    action->set_checked(field.difficulty() == Field::Difficulty::Madwoman);
    difficulty_menu.add_action(action);
    difficulty_actions.add_action(action);

    difficulty_menu.add_separator();
    action = GUI::Action::create_checkable("&Custom game...", { Mod_Ctrl, Key_C }, [&](auto&) {
        CustomGameDialog::show(window, field);
    });
    action->set_checked(field.difficulty() == Field::Difficulty::Custom);
    difficulty_menu.add_action(action);
    difficulty_actions.add_action(action);

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Minesweeper", app_icon, window));

    window->show();

    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
