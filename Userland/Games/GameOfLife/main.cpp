/*
 * Copyright (c) 2021, Andres Crucitti <dasc495@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BoardWidget.h"
#include <Games/GameOfLife/GameOfLifeGML.h>
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

const char* click_tip = "Tip: click the board to toggle individual cells, or click+drag to toggle multiple cells";

int main(int argc, char** argv)
{
    auto app = GUI::Application::construct(argc, argv);
    auto app_icon = GUI::Icon::default_icon("app-gameoflife");

    auto window = GUI::Window::construct();
    window->set_icon(app_icon.bitmap_for_size(16));

    size_t board_columns = 35;
    size_t board_rows = 35;

    window->set_double_buffering_enabled(false);
    window->set_title("Game Of Life");

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.load_from_gml(game_of_life_gml);
    main_widget.set_fill_with_background_color(true);

    auto& main_toolbar = *main_widget.find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    main_toolbar.layout()->set_margins({ 6, 0, 6, 0 });

    auto& board_widget_container = *main_widget.find_descendant_of_type_named<GUI::Widget>("board_widget_container");
    auto& board_layout = board_widget_container.set_layout<GUI::VerticalBoxLayout>();
    board_layout.set_spacing(0);
    auto& board_widget = board_widget_container.add<BoardWidget>(board_rows, board_columns);
    board_widget.randomize_cells();

    auto& statusbar = *main_widget.find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    statusbar.set_text(click_tip);

    auto& columns_spinbox = *main_widget.find_descendant_of_type_named<GUI::SpinBox>("columns_spinbox");
    auto& rows_spinbox = *main_widget.find_descendant_of_type_named<GUI::SpinBox>("rows_spinbox");

    columns_spinbox.set_value(board_columns);
    rows_spinbox.set_value(board_rows);

    auto size_changed_function = [&] {
        statusbar.set_text(click_tip);
        board_widget.resize_board(rows_spinbox.value(), columns_spinbox.value());
        board_widget.update();
    };

    rows_spinbox.on_change = [&](auto) { size_changed_function(); };
    columns_spinbox.on_change = [&](auto) { size_changed_function(); };

    auto& interval_spinbox = *main_widget.find_descendant_of_type_named<GUI::SpinBox>("interval_spinbox");

    interval_spinbox.on_change = [&](auto value) {
        board_widget.set_running_timer_interval(value);
    };

    interval_spinbox.set_value(150);

    auto paused_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/pause.png");
    auto play_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/play.png");

    auto toggle_running_action = GUI::Action::create("&Toggle Running", { Mod_None, Key_Return }, *play_icon, [&](GUI::Action&) {
        board_widget.set_running(!board_widget.is_running());
    });

    toggle_running_action->set_checkable(true);
    main_toolbar.add_action(toggle_running_action);

    auto run_one_generation_action = GUI::Action::create("Run &Next Generation", { Mod_Ctrl, Key_Equal }, Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"), [&](const GUI::Action&) {
        statusbar.set_text(click_tip);
        board_widget.run_generation();
    });
    main_toolbar.add_action(run_one_generation_action);

    auto clear_board_action = GUI::Action::create("&Clear board", { Mod_Ctrl, Key_N }, Gfx::Bitmap::load_from_file("/res/icons/16x16/delete.png"), [&](auto&) {
        statusbar.set_text(click_tip);
        board_widget.clear_cells();
        board_widget.update();
    });
    main_toolbar.add_action(clear_board_action);

    auto randomize_cells_action = GUI::Action::create("&Randomize board", { Mod_Ctrl, Key_R }, Gfx::Bitmap::load_from_file("/res/icons/16x16/reload.png"), [&](auto&) {
        statusbar.set_text(click_tip);
        board_widget.randomize_cells();
        board_widget.update();
    });
    main_toolbar.add_action(randomize_cells_action);

    auto rotate_pattern_action = GUI::Action::create("&Rotate pattern", { 0, Key_R }, Gfx::Bitmap::load_from_file("/res/icons/16x16/redo.png"), [&](auto&) {
        if (board_widget.selected_pattern() != nullptr)
            board_widget.selected_pattern()->rotate_clockwise();
    });
    main_toolbar.add_action(rotate_pattern_action);

    auto menubar = GUI::Menubar::construct();
    auto& game_menu = menubar->add_menu("&Game");

    game_menu.add_action(clear_board_action);
    game_menu.add_action(randomize_cells_action);
    game_menu.add_separator();
    game_menu.add_action(toggle_running_action);
    game_menu.add_action(run_one_generation_action);
    game_menu.add_separator();
    game_menu.add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    }));

    auto& help_menu = menubar->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Game Of Life", app_icon, window));

    window->set_menubar(move(menubar));

    board_widget.on_running_state_change = [&]() {
        if (board_widget.is_running()) {
            statusbar.set_text("Running...");
            toggle_running_action->set_icon(paused_icon);
            main_widget.set_override_cursor(Gfx::StandardCursor::None);
        } else {
            statusbar.set_text(click_tip);
            toggle_running_action->set_icon(play_icon);
            main_widget.set_override_cursor(Gfx::StandardCursor::Drag);
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
        toggle_running_action->activate();
        statusbar.set_text("Stalled...");
    };

    board_widget.on_cell_toggled = [&](auto, auto, auto) {
        statusbar.set_text(click_tip);
    };

    window->resize(500, 420);
    window->show();

    return app->exec();
}
