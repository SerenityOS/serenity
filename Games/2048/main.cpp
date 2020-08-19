/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include "BoardView.h"
#include "Game.h"
#include "GameSizeDialog.h"
#include <LibCore/ConfigFile.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/StatusBar.h>
#include <LibGUI/Window.h>
#include <stdio.h>
#include <time.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath wpath cpath shared_buffer accept cpath unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    srand(time(nullptr));

    auto app = GUI::Application::construct(argc, argv);

    auto window = GUI::Window::construct();

    auto config = Core::ConfigFile::get_for_app("2048");

    size_t board_size = config->read_num_entry("", "board_size", 4);
    u32 target_tile = config->read_num_entry("", "target_tile", 0);

    config->write_num_entry("", "board_size", board_size);
    config->write_num_entry("", "target_tile", target_tile);

    config->sync();

    if (pledge("stdio rpath shared_buffer wpath cpath accept", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

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

    window->set_double_buffering_enabled(false);
    window->set_title("2048");
    window->resize(315, 336);

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_layout<GUI::VerticalBoxLayout>();
    main_widget.set_fill_with_background_color(true);

    Game game { board_size, target_tile };

    auto& board_view = main_widget.add<BoardView>(&game.board());
    board_view.set_focus(true);
    auto& statusbar = main_widget.add<GUI::StatusBar>();

    auto update = [&]() {
        board_view.set_board(&game.board());
        board_view.update();
        statusbar.set_text(String::format("Score: %d", game.score()));
    };

    update();

    Vector<Game> undo_stack;

    auto change_settings = [&] {
        auto size_dialog = GameSizeDialog::construct(window);
        if (size_dialog->exec() || size_dialog->result() != GUI::Dialog::ExecOK)
            return;

        board_size = size_dialog->board_size();
        target_tile = size_dialog->target_tile();

        if (!size_dialog->temporary()) {

            config->write_num_entry("", "board_size", board_size);
            config->write_num_entry("", "target_tile", target_tile);

            if (!config->sync()) {
                GUI::MessageBox::show(window, "Configuration could not be synced", "Error", GUI::MessageBox::Type::Error);
                return;
            }
            GUI::MessageBox::show(window, "New settings have been saved and will be applied on a new game", "Settings Changed Successfully", GUI::MessageBox::Type::Information);
            return;
        }

        GUI::MessageBox::show(window, "New settings have been set and will be applied on the next game", "Settings Changed Successfully", GUI::MessageBox::Type::Information);
    };
    auto start_a_new_game = [&] {
        // Do not leak game states between games.
        undo_stack.clear();

        game = Game(board_size, target_tile);

        // This ensures that the sizes are correct.
        board_view.set_board(nullptr);
        board_view.set_board(&game.board());

        update();
        window->update();
    };

    board_view.on_move = [&](Game::Direction direction) {
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
        case Game::MoveOutcome::Won:
            update();
            GUI::MessageBox::show(window,
                String::format("You reached %d in %zu turns with a score of %d", game.target_tile(), game.turns(), game.score()),
                "You won!",
                GUI::MessageBox::Type::Information);
            start_a_new_game();
            break;
        case Game::MoveOutcome::GameOver:
            update();
            GUI::MessageBox::show(window,
                String::format("You reached %d in %zu turns with a score of %d", game.largest_tile(), game.turns(), game.score()),
                "You lost!",
                GUI::MessageBox::Type::Information);
            start_a_new_game();
            break;
        }
    };

    auto menubar = GUI::MenuBar::construct();

    auto& app_menu = menubar->add_menu("2048");

    app_menu.add_action(GUI::Action::create("New game", { Mod_None, Key_F2 }, [&](auto&) {
        start_a_new_game();
    }));

    app_menu.add_action(GUI::CommonActions::make_undo_action([&](auto&) {
        if (undo_stack.is_empty())
            return;
        game = undo_stack.take_last();
        update();
    }));

    app_menu.add_separator();

    app_menu.add_action(GUI::Action::create("Settings", [&](auto&) {
        change_settings();
    }));

    app_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [&](auto&) {
        GUI::AboutDialog::show("2048", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-2048.png"), window);
    }));

    app->set_menubar(move(menubar));

    window->show();

    window->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-2048.png"));

    return app->exec();
}
