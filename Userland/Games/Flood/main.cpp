/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BoardWidget.h"
#include "MainWidget.h"
#include "SettingsDialog.h"
#include <AK/String.h>
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Icon.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Painter.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>

// FIXME: Improve this AI.
// Currently, this AI always chooses a move that gets the most cells flooded immediately.
// This far from being able to generate an optimal solution, and is something that needs to be improved
// if a user-facing auto-solver is implemented or a harder difficulty is wanted.
// A fairly simple way to improve this would be to test deeper moves and then choose the most efficient sequence.
static int get_number_of_moves_from_ai(Board const& board)
{
    Board ai_board { board };
    auto const color_scheme = ai_board.get_color_scheme();
    ai_board.set_current_value(ai_board.cell(0, 0));
    int moves { 0 };
    while (!ai_board.is_flooded()) {
        ++moves;
        int most_painted = 0;
        int best_value = ai_board.cell(0, 0);
        for (size_t i = 0; i < color_scheme.size(); ++i) {
            Board test_board { ai_board };
            test_board.set_current_value(i);
            // The first update applies the current value, and the second update is done to obtain the new area.
            test_board.update_values();
            int new_area = test_board.update_values(true);
            if (new_area > most_painted) {
                most_painted = new_area;
                best_value = i;
            }
        }
        ai_board.set_current_value(best_value);
        ai_board.update_values();
    }
    return moves;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));
    auto app = TRY(GUI::Application::create(arguments));
    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-flood"sv));

    auto window = GUI::Window::construct();

    Config::pledge_domain("Flood");

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man6/Flood.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));

    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    size_t board_rows = Config::read_i32("Flood"sv, ""sv, "board_rows"sv, 16);
    size_t board_columns = Config::read_i32("Flood"sv, ""sv, "board_columns"sv, 16);

    Config::write_i32("Flood"sv, ""sv, "board_rows"sv, board_rows);
    Config::write_i32("Flood"sv, ""sv, "board_columns"sv, board_columns);

    window->set_double_buffering_enabled(false);
    window->set_title("Flood");
    window->resize(304, 325);

    auto main_widget = TRY(Flood::MainWidget::try_create());
    window->set_main_widget(main_widget);

    auto board_widget = TRY(main_widget->find_descendant_of_type_named<GUI::Widget>("board_widget_container")->try_add<BoardWidget>(board_rows, board_columns));
    board_widget->board()->randomize();
    int ai_moves = get_number_of_moves_from_ai(*board_widget->board());
    int moves_made = 0;

    auto statusbar = main_widget->find_descendant_of_type_named<GUI::Statusbar>("statusbar");

    app->on_action_enter = [&](GUI::Action& action) {
        statusbar->set_override_text(action.status_tip());
    };

    app->on_action_leave = [&](GUI::Action&) {
        statusbar->set_override_text({});
    };

    auto update = [&]() {
        board_widget->update();
        statusbar->set_text(String::formatted("Moves remaining: {}", ai_moves - moves_made).release_value_but_fixme_should_propagate_errors());
    };

    update();

    auto change_settings = [&] {
        auto settings_dialog_or_error = SettingsDialog::try_create(window, board_rows, board_columns);
        if (settings_dialog_or_error.is_error()) {
            GUI::MessageBox::show(window, "Failed to load the settings window"sv, "Unable to Open Settings"sv, GUI::MessageBox::Type::Error);
            return;
        }

        auto settings_dialog = settings_dialog_or_error.release_value();
        if (settings_dialog->exec() != GUI::Dialog::ExecResult::OK)
            return;

        board_rows = settings_dialog->board_rows();
        board_columns = settings_dialog->board_columns();

        Config::write_i32("Flood"sv, ""sv, "board_rows"sv, board_rows);
        Config::write_i32("Flood"sv, ""sv, "board_columns"sv, board_columns);

        GUI::MessageBox::show(settings_dialog, "New settings have been saved and will be applied on a new game"sv, "Settings Changed Successfully"sv, GUI::MessageBox::Type::Information);
    };

    auto start_a_new_game = [&] {
        board_widget->resize_board(board_rows, board_columns);
        board_widget->board()->reset();
        board_widget->board()->randomize();
        ai_moves = get_number_of_moves_from_ai(*board_widget->board());
        moves_made = 0;
        update();
        window->update();
    };

    board_widget->on_move = [&](Board::RowAndColumn row_and_column) {
        auto const [row, column] = row_and_column;
        board_widget->board()->set_current_value(board_widget->board()->cell(row, column));
        if (board_widget->board()->get_previous_value() != board_widget->board()->get_current_value()) {
            ++moves_made;
            board_widget->board()->update_values();
            update();
            if (board_widget->board()->is_flooded()) {
                auto dialog_text = "You have tied with the AI."_string;
                auto dialog_title("Congratulations!"sv);
                if (ai_moves - moves_made == 1)
                    dialog_text = "You defeated the AI by 1 move."_string;
                else if (ai_moves - moves_made > 1)
                    dialog_text = String::formatted("You defeated the AI by {} moves.", ai_moves - moves_made).release_value_but_fixme_should_propagate_errors();
                else
                    dialog_title = "Game over!"sv;
                GUI::MessageBox::show(window,
                    dialog_text,
                    dialog_title,
                    GUI::MessageBox::Type::Information,
                    GUI::MessageBox::InputType::OK);
                start_a_new_game();
            } else if (moves_made == ai_moves) {
                GUI::MessageBox::show(window,
                    StringView("You have no more moves left."sv),
                    "You lost!"sv,
                    GUI::MessageBox::Type::Information,
                    GUI::MessageBox::InputType::OK);
                start_a_new_game();
            }
        }
    };

    auto game_menu = window->add_menu("&Game"_string);

    game_menu->add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/reload.png"sv)), [&](auto&) {
        start_a_new_game();
    }));

    game_menu->add_separator();
    game_menu->add_action(GUI::Action::create("&Settings", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/settings.png"sv)), [&](auto&) {
        change_settings();
    }));

    game_menu->add_separator();
    game_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man6/Flood.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Flood"_string, app_icon, window));

    window->show();

    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
