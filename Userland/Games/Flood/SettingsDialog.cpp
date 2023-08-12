/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SettingsDialog.h"
#include <AK/IntegralMath.h>
#include <AK/QuickSort.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/SpinBox.h>

ErrorOr<NonnullRefPtr<SettingsDialog>> SettingsDialog::try_create(GUI::Window* parent, size_t board_rows, size_t board_columns)
{
    auto settings_widget = TRY(Flood::SettingsWidget::try_create());
    auto settings_dialog = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) SettingsDialog(move(settings_widget), move(parent), move(board_rows), move(board_columns))));
    return settings_dialog;
}

SettingsDialog::SettingsDialog(NonnullRefPtr<Flood::SettingsWidget> settings_widget, GUI::Window* parent, size_t board_rows, size_t board_columns)
    : GUI::Dialog(parent)
    , m_board_rows(board_rows)
    , m_board_columns(board_columns)
{
    set_rect({ 0, 0, 250, 150 });
    set_title("New Game");
    set_icon(parent->icon());
    set_resizable(false);

    set_main_widget(settings_widget);

    auto board_rows_spinbox = settings_widget->find_descendant_of_type_named<GUI::SpinBox>("board_rows_spinbox");
    board_rows_spinbox->set_value(m_board_rows);

    board_rows_spinbox->on_change = [&](auto value) {
        m_board_rows = value;
    };

    auto board_columns_spinbox = settings_widget->find_descendant_of_type_named<GUI::SpinBox>("board_columns_spinbox");
    board_columns_spinbox->set_value(m_board_columns);

    board_columns_spinbox->on_change = [&](auto value) {
        m_board_columns = value;
    };

    auto cancel_button = settings_widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
    cancel_button->on_click = [this](auto) {
        done(ExecResult::Cancel);
    };

    auto ok_button = settings_widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button->on_click = [this](auto) {
        done(ExecResult::OK);
    };
}
