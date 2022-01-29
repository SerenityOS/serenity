/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GameSizeDialog.h"
#include "Game.h"
#include <AK/IntegralMath.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/SpinBox.h>

GameSizeDialog::GameSizeDialog(GUI::Window* parent, size_t board_size, size_t target, bool evil_ai)
    : GUI::Dialog(parent)
    , m_board_size(board_size)
    , m_target_tile_power(AK::log2(target) - 1)
    , m_evil_ai(evil_ai)
{
    set_rect({ 0, 0, 250, 150 });
    set_title("New Game");
    set_icon(parent->icon());
    set_resizable(false);

    auto& main_widget = set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);

    auto& layout = main_widget.set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins(4);

    auto& board_size_box = main_widget.add<GUI::Widget>();
    auto& input_layout = board_size_box.set_layout<GUI::HorizontalBoxLayout>();
    input_layout.set_spacing(4);

    board_size_box.add<GUI::Label>("Board size").set_text_alignment(Gfx::TextAlignment::CenterLeft);
    auto& spinbox = board_size_box.add<GUI::SpinBox>();

    auto& target_box = main_widget.add<GUI::Widget>();
    auto& target_layout = target_box.set_layout<GUI::HorizontalBoxLayout>();
    target_layout.set_spacing(4);
    spinbox.set_min(2);
    spinbox.set_value(m_board_size);

    target_box.add<GUI::Label>("Target tile").set_text_alignment(Gfx::TextAlignment::CenterLeft);
    auto& tile_value_label = target_box.add<GUI::Label>(String::number(target_tile()));
    tile_value_label.set_text_alignment(Gfx::TextAlignment::CenterRight);
    auto& target_spinbox = target_box.add<GUI::SpinBox>();
    target_spinbox.set_max(Game::max_power_for_board(m_board_size));
    target_spinbox.set_min(3);
    target_spinbox.set_value(m_target_tile_power);

    spinbox.on_change = [&](auto value) {
        m_board_size = value;
        target_spinbox.set_max(Game::max_power_for_board(m_board_size));
    };

    target_spinbox.on_change = [&](auto value) {
        m_target_tile_power = value;
        tile_value_label.set_text(String::number(target_tile()));
    };

    auto& evil_ai_checkbox = main_widget.add<GUI::CheckBox>("Evil AI");
    evil_ai_checkbox.set_checked(m_evil_ai);
    evil_ai_checkbox.on_checked = [this](auto checked) { m_evil_ai = checked; };

    auto& temp_checkbox = main_widget.add<GUI::CheckBox>("Temporarily apply changes");
    temp_checkbox.set_checked(m_temporary);
    temp_checkbox.on_checked = [this](auto checked) { m_temporary = checked; };

    auto& buttonbox = main_widget.add<GUI::Widget>();
    auto& button_layout = buttonbox.set_layout<GUI::HorizontalBoxLayout>();
    button_layout.set_spacing(10);

    buttonbox.add<GUI::Button>("Cancel").on_click = [this](auto) {
        done(Dialog::ExecCancel);
    };

    buttonbox.add<GUI::Button>("OK").on_click = [this](auto) {
        done(Dialog::ExecOK);
    };
}
