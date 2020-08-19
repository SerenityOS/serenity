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

#include "GameSizeDialog.h"
#include "Game.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/SpinBox.h>

GameSizeDialog::GameSizeDialog(GUI::Window* parent)
    : GUI::Dialog(parent)
{
    set_rect({ 0, 0, 200, 150 });
    set_title("New Game");
    set_icon(parent->icon());
    set_resizable(false);

    auto& main_widget = set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);

    auto& layout = main_widget.set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins({ 4, 4, 4, 4 });

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

    auto& temp_checkbox = main_widget.add<GUI::CheckBox>("Temporary");
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
