/*
 * Copyright (c) 2021, Andres Crucitti <dasc495@gmail.com>
 * Copyright (c) 2021, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BoardWidget.h"
#include "MainWidget.h"
#include <AK/Try.h>
#include <LibCore/System.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/Statusbar.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/Window.h>
#include <LibMain/Main.h>
#include <LibURL/URL.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::create(arguments));

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_scheme("/usr/share/man/man6/GameOfLife.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));

    TRY(Core::System::unveil("/tmp/session/%sid/portal/launch", "rw"));
    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto toggle_cells_tip = "Tip: click the board to toggle individual cells, or click+drag to toggle multiple cells"_string;
    auto pattern_place_tip = "Tip: hold Ctrl to place multiple patterns"_string;

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-gameoflife"sv));

    auto window = GUI::Window::construct();
    window->set_icon(app_icon.bitmap_for_size(16));

    size_t board_columns = 35;
    size_t board_rows = 35;

    window->set_double_buffering_enabled(false);
    window->set_title("Game of Life");

    auto main_widget = TRY(GameOfLife::MainWidget::try_create());
    window->set_main_widget(main_widget);
    main_widget->set_fill_with_background_color(true);

    auto& main_toolbar = *main_widget->find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    main_toolbar.layout()->set_margins({ 0, 6 });

    auto& board_widget_container = *main_widget->find_descendant_of_type_named<GUI::Widget>("board_widget_container");
    board_widget_container.set_layout<GUI::VerticalBoxLayout>(GUI::Margins {}, 0);
    auto& board_widget = board_widget_container.add<BoardWidget>(board_rows, board_columns);
    board_widget.set_focus_policy(GUI::FocusPolicy::StrongFocus);
    board_widget.set_focus(true);
    board_widget.randomize_cells();
    board_widget.set_min_size(board_columns, board_rows);

    auto& statusbar = *main_widget->find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    auto width = board_widget.font().width("Ticks: 000,000,000"sv) + board_widget.font().max_glyph_width();
    statusbar.segment(1).set_fixed_width(ceil(width));

    auto show_statusbar_hint = [&]() {
        auto tip = board_widget.selected_pattern() ? pattern_place_tip : toggle_cells_tip;
        statusbar.segment(0).set_text(tip);
    };
    show_statusbar_hint();

    auto& columns_spinbox = *main_widget->find_descendant_of_type_named<GUI::SpinBox>("columns_spinbox");
    auto& rows_spinbox = *main_widget->find_descendant_of_type_named<GUI::SpinBox>("rows_spinbox");

    columns_spinbox.set_value(board_columns);
    rows_spinbox.set_value(board_rows);

    auto size_changed_function = [&] {
        show_statusbar_hint();
        board_widget.resize_board(rows_spinbox.value(), columns_spinbox.value());
        board_widget.update();
    };

    rows_spinbox.on_change = [&](auto) { size_changed_function(); };
    columns_spinbox.on_change = [&](auto) { size_changed_function(); };

    auto& interval_spinbox = *main_widget->find_descendant_of_type_named<GUI::SpinBox>("interval_spinbox");

    interval_spinbox.on_change = [&](auto value) {
        board_widget.set_running_timer_interval(value);
    };

    interval_spinbox.set_value(150);

    auto paused_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/pause.png"sv));
    auto play_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/play.png"sv));

    auto play_pause_action = GUI::Action::create("&Play", { Mod_None, Key_Return }, *play_icon, [&](GUI::Action&) {
        board_widget.set_running(!board_widget.is_running());
    });

    main_toolbar.add_action(play_pause_action);

    auto run_one_generation_action = GUI::Action::create("Run &Next Generation", { Mod_Ctrl, Key_Equal }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"sv)), [&](const GUI::Action&) {
        show_statusbar_hint();
        board_widget.run_generation();
    });
    main_toolbar.add_action(run_one_generation_action);

    auto clear_board_action = GUI::Action::create("&Clear board", { Mod_Ctrl, Key_N }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/delete.png"sv)), [&](auto&) {
        show_statusbar_hint();
        statusbar.segment(1).set_text({});
        board_widget.clear_cells();
        board_widget.update();
    });
    main_toolbar.add_action(clear_board_action);

    auto randomize_cells_action = GUI::Action::create("&Randomize board", { Mod_Ctrl, Key_R }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/reload.png"sv)), [&](auto&) {
        show_statusbar_hint();
        statusbar.segment(1).set_text({});
        board_widget.randomize_cells();
        board_widget.update();
    });
    main_toolbar.add_action(randomize_cells_action);

    auto rotate_pattern_action = GUI::Action::create("&Rotate pattern", { 0, Key_R }, TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/redo.png"sv)), [&](auto&) {
        board_widget.selected_pattern()->rotate_clockwise();
    });
    rotate_pattern_action->set_enabled(false);
    main_toolbar.add_action(rotate_pattern_action);

    auto game_menu = window->add_menu("&Game"_string);

    game_menu->add_action(clear_board_action);
    game_menu->add_action(randomize_cells_action);
    game_menu->add_separator();
    game_menu->add_action(play_pause_action);
    game_menu->add_action(run_one_generation_action);
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
        Desktop::Launcher::open(URL::create_with_file_scheme("/usr/share/man/man6/GameOfLife.md"), "/bin/Help");
    }));
    help_menu->add_action(GUI::CommonActions::make_about_action("Game of Life"_string, app_icon, window));

    board_widget.on_tick = [&](u64 ticks) {
        statusbar.segment(1).set_text(String::formatted("Ticks: {:'}", ticks).release_value_but_fixme_should_propagate_errors());
    };

    board_widget.on_running_state_change = [&]() {
        if (board_widget.is_running()) {
            statusbar.segment(0).set_text("Running..."_string);
            play_pause_action->set_icon(paused_icon);
            play_pause_action->set_text("&Pause");
            main_widget->set_override_cursor(Gfx::StandardCursor::None);
        } else {
            statusbar.segment(0).set_text("Paused"_string);
            play_pause_action->set_icon(play_icon);
            play_pause_action->set_text("&Play");
            main_widget->set_override_cursor(Gfx::StandardCursor::Drag);
        }

        interval_spinbox.set_value(board_widget.running_timer_interval());

        rows_spinbox.set_enabled(!board_widget.is_running());
        columns_spinbox.set_enabled(!board_widget.is_running());
        interval_spinbox.set_enabled(!board_widget.is_running());

        run_one_generation_action->set_enabled(!board_widget.is_running());
        clear_board_action->set_enabled(!board_widget.is_running());
        randomize_cells_action->set_enabled(!board_widget.is_running());

        board_widget.update();
    };

    board_widget.on_stall = [&] {
        play_pause_action->activate();
        statusbar.segment(0).set_text("Stalled"_string);
    };

    board_widget.on_cell_toggled = [&](auto, auto, auto) {
        show_statusbar_hint();
        statusbar.segment(1).set_text({});
    };

    board_widget.on_pattern_selection_state_change = [&] {
        show_statusbar_hint();
        rotate_pattern_action->set_enabled(board_widget.selected_pattern() != nullptr);
    };

    window->resize(600, 500);
    window->show();

    return app->exec();
}
