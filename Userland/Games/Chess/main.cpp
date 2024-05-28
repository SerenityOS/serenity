/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 * Copyright (c) 2024, Daniel Gaston <tfd@tuta.io>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ChessWidget.h"
#include "MainWidget.h"
#include "NewGameDialog.h"
#include <LibConfig/Client.h>
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
#include <LibGUI/Process.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>

struct EngineDetails {
    StringView command;
    StringView name { command };
    String path {};
};

static Vector<EngineDetails> s_all_engines {
    { "ChessEngine"sv },
    { "stockfish"sv, "Stockfish"sv },
};

static ErrorOr<Vector<EngineDetails>> available_engines()
{
    Vector<EngineDetails> available_engines;
    for (auto& engine : s_all_engines) {
        auto path_or_error = Core::System::resolve_executable_from_environment(engine.command);
        if (path_or_error.is_error())
            continue;

        engine.path = path_or_error.release_value();
        TRY(available_engines.try_append(engine));
    }

    return available_engines;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd thread proc exec unix"));

    auto app = TRY(GUI::Application::create(arguments));

    Config::pledge_domain("Games");
    Config::monitor_domain("Games");

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man6/Chess.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-chess"sv));

    auto window = GUI::Window::construct();
    auto main_widget = TRY(Chess::MainWidget::try_create());

    auto& chess_widget = *main_widget->find_descendant_of_type_named<Chess::ChessWidget>("chess_widget");
    auto& move_display_widget = *main_widget->find_descendant_of_type_named<GUI::TextEditor>("move_display_widget");
    chess_widget.set_move_display_widget(move(move_display_widget));
    auto& white_time_label = *main_widget->find_descendant_of_type_named<GUI::Label>("white_time_label");
    chess_widget.set_white_time_label(move(white_time_label));
    auto& black_time_label = *main_widget->find_descendant_of_type_named<GUI::Label>("black_time_label");
    chess_widget.set_black_time_label(move(black_time_label));

    window->set_main_widget(main_widget);
    window->set_focused_widget(&chess_widget);

    auto engines = TRY(available_engines());
    for (auto const& engine : engines)
        TRY(Core::System::unveil(engine.path, "x"sv));

    TRY(Core::System::unveil("/etc/passwd", "r"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/bin/GamesSettings", "x"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/tmp/session/%sid/portal/filesystemaccess", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    window->set_title("Chess");
    window->set_base_size({ 4, 4 });
    window->set_size_increment({ 8, 8 });
    window->resize(668, 508);

    window->set_icon(app_icon.bitmap_for_size(16));

    chess_widget.set_piece_set(Config::read_string("Games"sv, "Chess"sv, "PieceSet"sv, "Classic"sv));
    chess_widget.set_board_theme(Config::read_string("Games"sv, "Chess"sv, "BoardTheme"sv, "Beige"sv));
    chess_widget.set_coordinates(Config::read_bool("Games"sv, "Chess"sv, "ShowCoordinates"sv, true));
    chess_widget.set_show_available_moves(Config::read_bool("Games"sv, "Chess"sv, "ShowAvailableMoves"sv, true));
    chess_widget.set_highlight_checks(Config::read_bool("Games"sv, "Chess"sv, "HighlightChecks"sv, true));
    chess_widget.set_unlimited_time_control(Config::read_bool("Games"sv, "Chess"sv, "UnlimitedTimeControl"sv, true));
    chess_widget.set_time_control_seconds(Config::read_i32("Games"sv, "Chess"sv, "TimeControlSeconds"sv, 300));
    chess_widget.set_time_control_increment(Config::read_i32("Games"sv, "Chess"sv, "TimeControlIncrement"sv, 3));
    chess_widget.initialize_timer();

    auto game_menu = window->add_menu("&Game"_string);

    game_menu->add_action(GUI::Action::create("&Resign", { Mod_None, Key_F3 }, [&](auto&) {
        chess_widget.resign();
    }));
    game_menu->add_action(GUI::Action::create("&Flip Board", { Mod_Ctrl, Key_F }, [&](auto&) {
        chess_widget.flip_board();
    }));
    game_menu->add_separator();

    game_menu->add_action(GUI::Action::create("&Import PGN...", { Mod_Ctrl, Key_O }, [&](auto&) {
        FileSystemAccessClient::OpenFileOptions options {
            .allowed_file_types = Vector {
                GUI::FileTypeFilter { "PGN Files", { { "pgn" } } },
                GUI::FileTypeFilter::all_files(),
            }
        };
        auto result = FileSystemAccessClient::Client::the().open_file(window, options);
        if (result.is_error())
            return;

        if (auto maybe_error = chess_widget.import_pgn(*result.value().release_stream()); maybe_error.is_error()) {
            auto error_message = maybe_error.release_error().message();
            dbgln("Failed to import PGN: {}", error_message);
            GUI::MessageBox::show(window, error_message, "Import Error"sv, GUI::MessageBox::Type::Information);
        } else {
            dbgln("Imported PGN file from {}", result.value().filename());
        }
    }));
    game_menu->add_action(GUI::Action::create("&Export PGN...", { Mod_Ctrl, Key_S }, [&](auto&) {
        auto result = FileSystemAccessClient::Client::the().save_file(window, "Untitled", "pgn");
        if (result.is_error())
            return;

        if (auto maybe_error = chess_widget.export_pgn(*result.value().release_stream()); maybe_error.is_error())
            dbgln("Failed to export PGN: {}", maybe_error.release_error());
        else
            dbgln("Exported PGN file to {}", result.value().filename());
    }));
    game_menu->add_action(GUI::Action::create("&Copy FEN", { Mod_Ctrl, Key_C }, [&](auto&) {
        GUI::Clipboard::the().set_data(chess_widget.get_fen().release_value_but_fixme_should_propagate_errors().bytes());
        GUI::MessageBox::show(window, "Board state copied to clipboard as FEN."sv, "Copy FEN"sv, GUI::MessageBox::Type::Information);
    }));
    game_menu->add_separator();

    game_menu->add_action(GUI::Action::create("&New Game", { Mod_None, Key_F2 }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/reload.png"sv)), [&](auto&) {
        if (chess_widget.board().game_result() == Chess::Board::Result::NotFinished) {
            if (chess_widget.resign() < 0)
                return;
        }

        auto new_game_dialog_or_error = Chess::NewGameDialog::try_create(window, chess_widget.unlimited_time_control(), chess_widget.time_control_seconds(), chess_widget.time_control_increment());
        if (new_game_dialog_or_error.is_error()) {
            GUI::MessageBox::show(window, "Failed to load the new game window"sv, "Unable to Open New Game Dialog"sv, GUI::MessageBox::Type::Error);
            return;
        }

        auto new_game_dialog = new_game_dialog_or_error.release_value();
        if (new_game_dialog->exec() != GUI::Dialog::ExecResult::OK)
            return;

        chess_widget.set_unlimited_time_control(new_game_dialog->unlimited_time_control());
        chess_widget.set_time_control_seconds(new_game_dialog->time_control_seconds());
        chess_widget.set_time_control_increment(new_game_dialog->time_control_increment());

        Config::write_bool("Games"sv, "Chess"sv, "UnlimitedTimeControl"sv, new_game_dialog->unlimited_time_control());
        Config::write_i32("Games"sv, "Chess"sv, "TimeControlSeconds"sv, new_game_dialog->time_control_seconds());
        Config::write_i32("Games"sv, "Chess"sv, "TimeControlIncrement"sv, new_game_dialog->time_control_increment());
        chess_widget.reset();
    }));
    game_menu->add_separator();

    auto settings_action = GUI::Action::create(
        "Chess &Settings", {}, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/games.png"sv)), [window](auto&) {
            GUI::Process::spawn_or_show_error(window, "/bin/GamesSettings"sv, Array { "--open-tab", "chess" });
        },
        window);
    settings_action->set_status_tip("Open the Game Settings for Chess"_string);
    game_menu->add_action(settings_action);

    auto show_available_moves_action = GUI::Action::create_checkable("Show Available Moves", [&](auto& action) {
        chess_widget.set_show_available_moves(action.is_checked());
        chess_widget.update();
        Config::write_bool("Games"sv, "Chess"sv, "ShowAvailableMoves"sv, action.is_checked());
    });
    show_available_moves_action->set_checked(chess_widget.show_available_moves());
    game_menu->add_action(show_available_moves_action);
    game_menu->add_separator();

    game_menu->add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto engine_menu = window->add_menu("&Engine"_string);

    GUI::ActionGroup engines_action_group;
    engines_action_group.set_exclusive(true);
    auto engine_submenu = engine_menu->add_submenu("&Engine"_string);
    auto human_engine_checkbox = GUI::Action::create_checkable("Human", [&](auto&) {
        chess_widget.set_engine(nullptr);
    });
    human_engine_checkbox->set_checked(true);
    engines_action_group.add_action(human_engine_checkbox);
    engine_submenu->add_action(human_engine_checkbox);

    for (auto const& engine : engines) {
        auto action = GUI::Action::create_checkable(engine.name, [&](auto&) {
            auto new_engine = Engine::construct(engine.path);
            new_engine->on_connection_lost = [&]() {
                if (!chess_widget.want_engine_move())
                    return;

                auto rc = GUI::MessageBox::show(window, "Connection to the chess engine was lost while waiting for a move. Do you want to try again?"sv, "Chess"sv, GUI::MessageBox::Type::Question, GUI::MessageBox::InputType::YesNo);
                if (rc == GUI::Dialog::ExecResult::Yes)
                    chess_widget.input_engine_move();
                else
                    human_engine_checkbox->activate();
            };
            chess_widget.set_engine(move(new_engine));
            chess_widget.input_engine_move();
        });
        engines_action_group.add_action(*action);
        engine_submenu->add_action(*action);
    }

    auto view_menu = window->add_menu("&View"_string);
    view_menu->add_action(GUI::CommonActions::make_fullscreen_action([&](auto&) {
        window->set_fullscreen(!window->is_fullscreen());
    }));

    auto help_menu = window->add_menu("&Help"_string);
    help_menu->add_action(GUI::CommonActions::make_command_palette_action(window));
    help_menu->add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man6/Chess.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Chess"_string, app_icon, window));

    window->show();
    chess_widget.reset();

    return app->exec();
}
