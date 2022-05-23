/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BoardView.h"
#include "Game.h"
#include "GameSizeDialog.h"
#include <AK/URL.h>
#include <LibConfig/Client.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/Window.h>
#include <LibGfx/Painter.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <time.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    srand(time(nullptr));

    auto app = TRY(GUI::Application::try_create(arguments));
    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-2048"));

    auto window = TRY(GUI::Window::try_create());

    Config::pledge_domain("2048");

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_protocol("/usr/share/man/man6/2048.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/portal/launch", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    size_t board_size = Config::read_i32("2048", "", "board_size", 4);
    u32 target_tile = Config::read_i32("2048", "", "target_tile", 2048);
    bool evil_ai = Config::read_bool("2048", "", "evil_ai", false);

    if ((target_tile & (target_tile - 1)) != 0) {
        // If the target tile is not a power of 2, reset to its default value.
        target_tile = 2048;
    }

    Config::write_i32("2048", "", "board_size", board_size);
    Config::write_i32("2048", "", "target_tile", target_tile);
    Config::write_bool("2048", "", "evil_ai", evil_ai);

    window->set_double_buffering_enabled(false);
    window->set_title("2048");
    window->resize(315, 336);

    auto main_widget = TRY(window->try_set_main_widget<GUI::Widget>());
    (void)TRY(main_widget->try_set_layout<GUI::VerticalBoxLayout>());
    main_widget->set_fill_with_background_color(true);

    Game game { board_size, target_tile, evil_ai };

    auto board_view = TRY(main_widget->try_add<BoardView>(&game.board()));
    board_view->set_focus(true);
    auto statusbar = TRY(main_widget->try_add<GUI::Statusbar>());

    app->on_action_enter = [&](GUI::Action& action) {
        auto text = action.status_tip();
        if (text.is_empty())
            text = Gfx::parse_ampersand_string(action.text());
        statusbar->set_override_text(move(text));
    };

    app->on_action_leave = [&](GUI::Action&) {
        statusbar->set_override_text({});
    };

    auto update = [&]() {
        board_view->set_board(&game.board());
        board_view->update();
        statusbar->set_text(String::formatted("Score: {}", game.score()));
    };

    update();

    Vector<Game> undo_stack;
    Vector<Game> redo_stack;

    auto change_settings = [&] {
        auto size_dialog = GameSizeDialog::construct(window, board_size, target_tile, evil_ai);
        if (size_dialog->exec() != GUI::Dialog::ExecResult::OK)
            return;

        board_size = size_dialog->board_size();
        target_tile = size_dialog->target_tile();
        evil_ai = size_dialog->evil_ai();

        if (!size_dialog->temporary()) {

            Config::write_i32("2048", "", "board_size", board_size);
            Config::write_i32("2048", "", "target_tile", target_tile);
            Config::write_bool("2048", "", "evil_ai", evil_ai);

            GUI::MessageBox::show(window, "New settings have been saved and will be applied on a new game", "Settings Changed Successfully", GUI::MessageBox::Type::Information);
            return;
        }

        GUI::MessageBox::show(window, "New settings have been set and will be applied on the next game", "Settings Changed Successfully", GUI::MessageBox::Type::Information);
    };
    auto start_a_new_game = [&] {
        // Do not leak game states between games.
        undo_stack.clear();
        redo_stack.clear();

        game = Game(board_size, target_tile, evil_ai);

        // This ensures that the sizes are correct.
        board_view->set_board(nullptr);
        board_view->set_board(&game.board());

        update();
        window->update();
    };

    board_view->on_move = [&](Game::Direction direction) {
        undo_stack.append(game);
        auto outcome = game.attempt_move(direction);
        switch (outcome) {
        case Game::MoveOutcome::OK:
            if (undo_stack.size() >= 16)
                undo_stack.take_first();
            update();
            break;
        case Game::MoveOutcome::InvalidMove:
            undo_stack.take_last();
            break;
        case Game::MoveOutcome::Won: {
            update();
            auto want_to_continue = GUI::MessageBox::show(window,
                String::formatted("You won the game in {} turns with a score of {}. Would you like to continue?", game.turns(), game.score()),
                "Congratulations!",
                GUI::MessageBox::Type::Question,
                GUI::MessageBox::InputType::YesNo);
            if (want_to_continue == GUI::MessageBox::ExecResult::Yes)
                game.set_want_to_continue();
            else
                start_a_new_game();
            break;
        }
        case Game::MoveOutcome::GameOver:
            update();
            GUI::MessageBox::show(window,
                String::formatted("You reached {} in {} turns with a score of {}", game.largest_tile(), game.turns(), game.score()),
                "You lost!",
                GUI::MessageBox::Type::Information);
            start_a_new_game();
            break;
        }
    };

    auto game_menu = TRY(window->try_add_menu("&Game"));

    TRY(game_menu->try_add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/reload.png")), [&](auto&) {
        start_a_new_game();
    })));

    TRY(game_menu->try_add_action(GUI::CommonActions::make_undo_action([&](auto&) {
        if (undo_stack.is_empty())
            return;
        redo_stack.append(game);
        game = undo_stack.take_last();
        update();
    })));

    TRY(game_menu->try_add_action(GUI::CommonActions::make_redo_action([&](auto&) {
        if (redo_stack.is_empty())
            return;
        undo_stack.append(game);
        game = redo_stack.take_last();
        update();
    })));

    TRY(game_menu->try_add_separator());
    TRY(game_menu->try_add_action(GUI::Action::create("&Settings", TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/settings.png")), [&](auto&) {
        change_settings();
    })));

    TRY(game_menu->try_add_separator());
    TRY(game_menu->try_add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    })));

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man6/2048.md"), "/bin/Help");
    })));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("2048", app_icon, window)));

    window->show();

    window->set_icon(app_icon.bitmap_for_size(16));

    return app->exec();
}
