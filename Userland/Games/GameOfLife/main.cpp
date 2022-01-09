/*
 * Copyright (c) 2021, Andres Crucitti <dasc495@gmail.com>
 * Copyright (c) 2021, Jakob-Niklas See <git@nwex.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BoardWidget.h"
#include <AK/URL.h>
#include <Games/GameOfLife/GameOfLifeGML.h>
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

const char* click_tip = "Tip: click the board to toggle individual cells, or click+drag to toggle multiple cells";

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath recvfd sendfd unix"));

    auto app = TRY(GUI::Application::try_create(arguments));

    TRY(Desktop::Launcher::add_allowed_handler_with_only_specific_urls("/bin/Help", { URL::create_with_file_protocol("/usr/share/man/man6/GameOfLife.md") }));
    TRY(Desktop::Launcher::seal_allowlist());

    TRY(Core::System::pledge("stdio rpath recvfd sendfd"));

    TRY(Core::System::unveil("/res", "r"));
    TRY(Core::System::unveil("/tmp/portal/launch", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));

    auto app_icon = TRY(GUI::Icon::try_create_default_icon("app-gameoflife"));

    auto window = TRY(GUI::Window::try_create());
    window->set_icon(app_icon.bitmap_for_size(16));

    size_t board_columns = 35;
    size_t board_rows = 35;

    window->set_double_buffering_enabled(false);
    window->set_title("Game Of Life");

    auto main_widget = TRY(window->try_set_main_widget<GUI::Widget>());
    main_widget->load_from_gml(game_of_life_gml);
    main_widget->set_fill_with_background_color(true);

    auto& main_toolbar = *main_widget->find_descendant_of_type_named<GUI::Toolbar>("toolbar");
    main_toolbar.layout()->set_margins({ 0, 6 });

    auto& board_widget_container = *main_widget->find_descendant_of_type_named<GUI::Widget>("board_widget_container");
    auto board_layout = TRY(board_widget_container.try_set_layout<GUI::VerticalBoxLayout>());
    board_layout->set_spacing(0);
    auto board_widget = TRY(board_widget_container.try_add<BoardWidget>(board_rows, board_columns));
    board_widget->randomize_cells();

    auto& statusbar = *main_widget->find_descendant_of_type_named<GUI::Statusbar>("statusbar");
    statusbar.set_text(click_tip);

    auto& columns_spinbox = *main_widget->find_descendant_of_type_named<GUI::SpinBox>("columns_spinbox");
    auto& rows_spinbox = *main_widget->find_descendant_of_type_named<GUI::SpinBox>("rows_spinbox");

    columns_spinbox.set_value(board_columns);
    rows_spinbox.set_value(board_rows);

    auto size_changed_function = [&] {
        statusbar.set_text(click_tip);
        board_widget->resize_board(rows_spinbox.value(), columns_spinbox.value());
        board_widget->update();
    };

    rows_spinbox.on_change = [&](auto) { size_changed_function(); };
    columns_spinbox.on_change = [&](auto) { size_changed_function(); };

    auto& interval_spinbox = *main_widget->find_descendant_of_type_named<GUI::SpinBox>("interval_spinbox");

    interval_spinbox.on_change = [&](auto value) {
        board_widget->set_running_timer_interval(value);
    };

    interval_spinbox.set_value(150);

    auto paused_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/pause.png").release_value_but_fixme_should_propagate_errors();
    auto play_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/play.png").release_value_but_fixme_should_propagate_errors();

    auto toggle_running_action = GUI::Action::create("&Toggle Running", { Mod_None, Key_Return }, *play_icon, [&](GUI::Action&) {
        board_widget->set_running(!board_widget->is_running());
    });

    toggle_running_action->set_checkable(true);
    auto toggle_running_toolbar_button = TRY(main_toolbar.try_add_action(toggle_running_action));

    auto run_one_generation_action = GUI::Action::create("Run &Next Generation", { Mod_Ctrl, Key_Equal }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png").release_value_but_fixme_should_propagate_errors(), [&](const GUI::Action&) {
        statusbar.set_text(click_tip);
        board_widget->run_generation();
    });
    (void)TRY(main_toolbar.try_add_action(run_one_generation_action));

    auto clear_board_action = GUI::Action::create("&Clear board", { Mod_Ctrl, Key_N }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/delete.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        statusbar.set_text(click_tip);
        board_widget->clear_cells();
        board_widget->update();
    });
    (void)TRY(main_toolbar.try_add_action(clear_board_action));

    auto randomize_cells_action = GUI::Action::create("&Randomize board", { Mod_Ctrl, Key_R }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/reload.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        statusbar.set_text(click_tip);
        board_widget->randomize_cells();
        board_widget->update();
    });
    (void)TRY(main_toolbar.try_add_action(randomize_cells_action));

    auto rotate_pattern_action = GUI::Action::create("&Rotate pattern", { 0, Key_R }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/redo.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        board_widget->selected_pattern()->rotate_clockwise();
    });
    rotate_pattern_action->set_enabled(false);
    (void)TRY(main_toolbar.try_add_action(rotate_pattern_action));

    auto game_menu = TRY(window->try_add_menu("&Game"));

    TRY(game_menu->try_add_action(clear_board_action));
    TRY(game_menu->try_add_action(randomize_cells_action));
    TRY(game_menu->try_add_separator());
    TRY(game_menu->try_add_action(toggle_running_action));
    TRY(game_menu->try_add_action(run_one_generation_action));
    TRY(game_menu->try_add_separator());
    TRY(game_menu->try_add_action(GUI::CommonActions::make_quit_action([](auto&) {
        GUI::Application::the()->quit();
    })));

    auto help_menu = TRY(window->try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_help_action([](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol("/usr/share/man/man6/GameOfLife.md"), "/bin/Help");
    })));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Game Of Life", app_icon, window)));

    board_widget->on_running_state_change = [&]() {
        if (board_widget->is_running()) {
            statusbar.set_text("Running...");
            toggle_running_toolbar_button->set_icon(*paused_icon);
            main_widget->set_override_cursor(Gfx::StandardCursor::None);
        } else {
            statusbar.set_text(click_tip);
            toggle_running_toolbar_button->set_icon(*play_icon);
            main_widget->set_override_cursor(Gfx::StandardCursor::Drag);
        }

        interval_spinbox.set_value(board_widget->running_timer_interval());

        rows_spinbox.set_enabled(!board_widget->is_running());
        columns_spinbox.set_enabled(!board_widget->is_running());
        interval_spinbox.set_enabled(!board_widget->is_running());

        run_one_generation_action->set_enabled(!board_widget->is_running());
        clear_board_action->set_enabled(!board_widget->is_running());
        randomize_cells_action->set_enabled(!board_widget->is_running());

        board_widget->update();
    };

    board_widget->on_stall = [&] {
        toggle_running_action->activate();
        statusbar.set_text("Stalled...");
    };

    board_widget->on_cell_toggled = [&](auto, auto, auto) {
        statusbar.set_text(click_tip);
    };

    board_widget->on_pattern_selection_state_change = [&] {
        rotate_pattern_action->set_enabled(board_widget->selected_pattern() != nullptr);
    };

    window->resize(500, 420);
    window->show();

    return app->exec();
}
