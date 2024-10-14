/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GameSizeDialog.h"
#include "Game.h"
#include "GameSizeDialogWidget.h"
#include <AK/IntegralMath.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/SpinBox.h>

namespace TwentyFourtyEight {
GameSizeDialog::GameSizeDialog(GUI::Window* parent, size_t board_size, size_t target, bool evil_ai)
    : GUI::Dialog(parent)
    , m_board_size(board_size)
    , m_target_tile_power(AK::log2(target))
    , m_evil_ai(evil_ai)
{
    set_rect({ 0, 0, 250, 150 });
    set_title("New Game");
    set_icon(parent->icon());
    set_resizable(false);

    auto main_widget = GameSizeDialogWidget::try_create().release_value_but_fixme_should_propagate_errors();
    set_main_widget(main_widget);

    auto board_size_spinbox = main_widget->find_descendant_of_type_named<GUI::SpinBox>("board_size_spinbox");
    board_size_spinbox->set_value(m_board_size);

    auto tile_value_label = main_widget->find_descendant_of_type_named<GUI::Label>("tile_value_label");
    tile_value_label->set_text(String::number(target_tile()));
    auto target_spinbox = main_widget->find_descendant_of_type_named<GUI::SpinBox>("target_spinbox");
    target_spinbox->set_max(Game::max_power_for_board(m_board_size));
    target_spinbox->set_value(m_target_tile_power);

    board_size_spinbox->on_change = [this, target_spinbox](auto value) {
        m_board_size = value;
        target_spinbox->set_max(Game::max_power_for_board(m_board_size));
    };

    target_spinbox->on_change = [this, tile_value_label](auto value) {
        m_target_tile_power = value;
        tile_value_label->set_text(String::number(target_tile()));
    };

    auto evil_ai_checkbox = main_widget->find_descendant_of_type_named<GUI::CheckBox>("evil_ai_checkbox");
    evil_ai_checkbox->set_checked(m_evil_ai);
    evil_ai_checkbox->on_checked = [this](auto checked) { m_evil_ai = checked; };

    auto temporary_checkbox = main_widget->find_descendant_of_type_named<GUI::CheckBox>("temporary_checkbox");
    temporary_checkbox->set_checked(m_temporary);
    temporary_checkbox->on_checked = [this](auto checked) { m_temporary = checked; };

    auto cancel_button = main_widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
    cancel_button->on_click = [this](auto) {
        done(ExecResult::Cancel);
    };

    auto ok_button = main_widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button->on_click = [this](auto) {
        done(ExecResult::OK);
    };
}
}
