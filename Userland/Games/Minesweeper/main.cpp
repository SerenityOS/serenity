/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CustomGameDialog.h"
#include "Field.h"
#include <AK/URL.h>
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

    Config::pledge_domains("Minesweeper");

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_protocol("/usr/share/man/man6/Minesweeper.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/portal/launch", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-minesweeper"));

    auto window = TRY(GUI::Window::try_create());
    window->set_resizable(false);
    window->set_title("Minesweeper");
    window->resize(139, 175);

    auto widget = TRY(window->try_set_main_widget<GUI::Widget>());
    (void)TRY(widget->try_set_layout<GUI::VerticalBoxLayout>());
    widget->layout()->set_spacing(0);

    auto top_line = TRY(widget->try_add<GUI::SeparatorWidget>(Gfx::Orientation::Horizontal));
    top_line->set_fixed_height(2);

    auto container = TRY(widget->try_add<GUI::Widget>());
    container->set_fill_with_background_color(true);
    container->set_fixed_height(36);
    (void)TRY(container->try_set_layout<GUI::HorizontalBoxLayout>());

    container->layout()->add_spacer();

    auto flag_image = TRY(container->try_add<GUI::Label>());
    flag_image->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/minesweeper/flag.png").release_value_but_fixme_should_propagate_errors());
    flag_image->set_fixed_width(16);

    auto flag_label = TRY(container->try_add<GUI::Label>());
    flag_label->set_autosize(true);
    flag_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);

    container->layout()->add_spacer();

    auto face_button = TRY(container->try_add<GUI::Button>());
    face_button->set_focus_policy(GUI::FocusPolicy::TabFocus);
    face_button->set_button_style(Gfx::ButtonStyle::Coolbar);
    face_button->set_fixed_size(36, 36);

    container->layout()->add_spacer();

    auto time_image = TRY(container->try_add<GUI::Label>());
    time_image->set_fixed_width(16);
    time_image->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/minesweeper/timer.png").release_value_but_fixme_should_propagate_errors());

    auto time_label = TRY(container->try_add<GUI::Label>());
    time_label->set_fixed_width(50);
    time_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);

    container->layout()->add_spacer();

    auto field = TRY(widget->try_add<Field>(flag_label, time_label, face_button, [&](auto size) {
        size.set_height(size.height() + container->min_size().height());
        window->resize(size);
    }));

    auto game_menu = TRY(window->try_add_menu("&Game"));

    TRY(game_menu->try_add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, [&](auto&) {
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
    TRY(help_menu->try_add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man6/Minesweeper.md"), "/bin/Help");
    })));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Minesweeper", app_icon, window)));

    window->show();

    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
