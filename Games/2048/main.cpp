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

    if (pledge("stdio rpath shared_buffer wpath accept", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
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

    Game game { 4, 4 };

    auto& board_view = main_widget.add<BoardView>(&game.board());
    board_view.set_focus(true);
    auto& statusbar = main_widget.add<GUI::StatusBar>();

    auto update = [&]() {
        board_view.set_board(&game.board());
        board_view.update();
        statusbar.set_text(String::format("Score: %d", game.score()));
    };

    update();

    auto start_a_new_game = [&]() {
        game = Game(4, 4);
        update();
    };

    Vector<Game> undo_stack;

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
                String::format("Score = %d in %zu turns", game.score(), game.turns()),
                "You won!",
                GUI::MessageBox::Type::Information);
            start_a_new_game();
            break;
        case Game::MoveOutcome::GameOver:
            update();
            GUI::MessageBox::show(window,
                String::format("Score = %d in %zu turns", game.score(), game.turns()),
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
