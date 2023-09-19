/*
 * Copyright (c) 2021, Pedro Pereira <pmh.pereira@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CustomGameDialog.h"
#include "Field.h"
#include <Games/Minesweeper/MinesweeperCustomGameWindowGML.h>

GUI::Dialog::ExecResult CustomGameDialog::show(GUI::Window* parent_window, Field& field)
{
    auto dialog = CustomGameDialog::construct(parent_window);

    if (parent_window) {
        dialog->set_icon(parent_window->icon());
        dialog->center_within(*parent_window);
    }

    dialog->m_columns_spinbox->set_value(field.columns());
    dialog->m_rows_spinbox->set_value(field.rows());
    dialog->m_mines_spinbox->set_value(field.mine_count());

    auto result = dialog->exec();

    if (result != ExecResult::OK)
        return result;

    field.set_field_size(Field::Difficulty::Custom, dialog->m_rows_spinbox->value(), dialog->m_columns_spinbox->value(), dialog->m_mines_spinbox->value());

    return ExecResult::OK;
}

void CustomGameDialog::set_max_mines()
{
    // NOTE: this is the maximum number of mines possible in a given minesweeper board
    m_mines_spinbox->set_max((m_rows_spinbox->value() * m_columns_spinbox->value()) - 9);
}

CustomGameDialog::CustomGameDialog(Window* parent_window)
    : Dialog(parent_window)
{
    resize(300, 82);
    set_resizable(false);
    set_title("Custom Game");

    auto main_widget = set_main_widget<GUI::Widget>();
    main_widget->load_from_gml(minesweeper_custom_game_window_gml).release_value_but_fixme_should_propagate_errors();

    m_columns_spinbox = *main_widget->find_descendant_of_type_named<GUI::SpinBox>("columns_spinbox");
    m_rows_spinbox = *main_widget->find_descendant_of_type_named<GUI::SpinBox>("rows_spinbox");
    m_mines_spinbox = *main_widget->find_descendant_of_type_named<GUI::SpinBox>("mines_spinbox");
    m_ok_button = *main_widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    m_cancel_button = *main_widget->find_descendant_of_type_named<GUI::Button>("cancel_button");

    m_columns_spinbox->on_change = [this](auto) {
        set_max_mines();
    };

    m_rows_spinbox->on_change = [this](auto) {
        set_max_mines();
    };

    m_ok_button->on_click = [this](auto) {
        done(ExecResult::OK);
    };

    m_cancel_button->on_click = [this](auto) {
        done(ExecResult::Cancel);
    };

    set_max_mines();
}
