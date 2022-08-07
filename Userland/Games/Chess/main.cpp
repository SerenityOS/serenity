/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ChessWidget.h"
#include <LibConfig/Client.h>
#include <LibCore/DirIterator.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd thread proc exec unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    Config::pledge_domain("Chess");

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_protocol("/usr/share/man/man6/Chess.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-chess"sv));

    auto window = TRY(GUI::Window::try_create());
    auto widget = TRY(window->try_set_main_widget<ChessWidget>());

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/bin/ChessEngine", "x"));
    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil("/tmp/user/%uid/portal/launch", "rw"));
    TRY(Core::System::unveil("/tmp/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto size = Config::read_i32("Chess"sv, "Display"sv, "size"sv, 512);
    window->set_title("Chess");
    window->set_base_size({ 4, 4 });
    window->set_size_increment({ 8, 8 });
    window->resize(size - 4, size - 4);

    window->set_icon(app_icon.bitmap_for_size(16));

    widget->set_piece_set(Config::read_string("Chess"sv, "Style"sv, "PieceSet"sv, "stelar7"sv));
    widget->set_board_theme(Config::read_string("Chess"sv, "Style"sv, "BoardTheme"sv, "Beige"sv));
    widget->set_coordinates(Config::read_bool("Chess"sv, "Style"sv, "Coordinates"sv, true));
    widget->set_show_available_moves(Config::read_bool("Chess"sv, "Style"sv, "ShowAvailableMoves"sv, true));

    auto game_menu = TRY(window->try_add_menu("&Game"));

    TRY(game_menu->try_add_action(GUI::Action::create("&Resign", { Mod_None, Key_F3 }, [&](auto&) {
        widget->resign();
    })));
    TRY(game_menu->try_add_action(GUI::Action::create("&Flip Board", { Mod_Ctrl, Key_F }, [&](auto&) {
        widget->flip_board();
    })));
    TRY(game_menu->try_add_separator());

    TRY(game_menu->try_add_action(GUI::Action::create("&Import PGN...", { Mod_Ctrl, Key_O }, [&](auto&) {
        auto result = FileSystemAccessClient::Client::the().try_open_file(window);
        if (result.is_error())
            return;

        widget->import_pgn(result.value());
        dbgln("Imported PGN file from {}", result.value()->filename());
    })));
    TRY(game_menu->try_add_action(GUI::Action::create("&Export PGN...", { Mod_Ctrl, Key_S }, [&](auto&) {
        auto result = FileSystemAccessClient::Client::the().try_save_file(window, "Untitled", "pgn");
        if (result.is_error())
            return;

        widget->export_pgn(result.value());
        dbgln("Exported PGN file to {}", result.value()->filename());
    })));
    TRY(game_menu->try_add_action(GUI::Action::create("&Copy FEN", { Mod_Ctrl, Key_C }, [&](auto&) {
        GUI::Clipboard::the().set_data(widget->get_fen().bytes());
        GUI::MessageBox::show(window, "Board state copied to clipboard as FEN."sv, "Copy FEN"sv, GUI::MessageBox::Type::Information);
    })));
    TRY(game_menu->try_add_separator());

    TRY(game_menu->try_add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/reload.png"sv)), [&](auto&) {
        if (widget->board().game_result() == Chess::Board::Result::NotFinished) {
            if (widget->resign() < 0)
                return;
        }
        widget->reset();
    })));
    TRY(game_menu->try_add_separator());
    TRY(game_menu->try_add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    })));

    auto style_menu = TRY(window->try_add_menu("&Style"));
    GUI::ActionGroup piece_set_action_group;
    piece_set_action_group.set_exclusive(true);
    auto piece_set_menu = TRY(style_menu->try_add_submenu("&Piece Set"));
    piece_set_menu->set_icon(app_icon.bitmap_for_size(16));

    Core::DirIterator di("/res/icons/chess/sets/", Core::DirIterator::SkipParentAndBaseDir);
    while (di.has_next()) {
        auto set = di.next_path();
        auto action = GUI::Action::create_checkable(set, [&](auto& action) {
            widget->set_piece_set(action.text());
            widget->update();
            Config::write_string("Chess"sv, "Style"sv, "PieceSet"sv, action.text());
        });

        piece_set_action_group.add_action(*action);
        if (widget->piece_set() == set)
            action->set_checked(true);
        TRY(piece_set_menu->try_add_action(*action));
    }

    GUI::ActionGroup board_theme_action_group;
    board_theme_action_group.set_exclusive(true);
    auto board_theme_menu = TRY(style_menu->try_add_submenu("Board Theme"));
    board_theme_menu->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/chess/mini-board.png"sv).release_value_but_fixme_should_propagate_errors());

    for (auto const& theme : { "Beige", "Green", "Blue" }) {
        auto action = GUI::Action::create_checkable(theme, [&](auto& action) {
            widget->set_board_theme(action.text());
            widget->update();
            Config::write_string("Chess"sv, "Style"sv, "BoardTheme"sv, action.text());
        });
        board_theme_action_group.add_action(*action);
        if (widget->board_theme().name == theme)
            action->set_checked(true);
        TRY(board_theme_menu->try_add_action(*action));
    }

    auto coordinates_action = GUI::Action::create_checkable("Coordinates", [&](auto& action) {
        widget->set_coordinates(action.is_checked());
        widget->update();
        Config::write_bool("Chess"sv, "Style"sv, "Coordinates"sv, action.is_checked());
    });
    coordinates_action->set_checked(widget->coordinates());
    TRY(style_menu->try_add_action(coordinates_action));

    auto show_available_moves_action = GUI::Action::create_checkable("Show Available Moves", [&](auto& action) {
        widget->set_show_available_moves(action.is_checked());
        widget->update();
        Config::write_bool("Chess"sv, "Style"sv, "ShowAvailableMoves"sv, action.is_checked());
    });
    show_available_moves_action->set_checked(widget->show_available_moves());
    TRY(style_menu->try_add_action(show_available_moves_action));

    auto engine_menu = TRY(window->try_add_menu("&Engine"));

    GUI::ActionGroup engines_action_group;
    engines_action_group.set_exclusive(true);
    auto engine_submenu = TRY(engine_menu->try_add_submenu("&Engine"));
    for (auto const& engine : { "Human", "ChessEngine" }) {
        auto action = GUI::Action::create_checkable(engine, [&](auto& action) {
            if (action.text() == "Human") {
                widget->set_engine(nullptr);
            } else {
                widget->set_engine(Engine::construct(action.text()));
                widget->input_engine_move();
            }
        });
        engines_action_group.add_action(*action);
        if (engine == String("Human"))
            action->set_checked(true);

        TRY(engine_submenu->try_add_action(*action));
    }

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man6/Chess.md"), "/bin/Help");
    })));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Chess", app_icon, window)));

    window->show();
    widget->reset();

    return app->exec();
}
