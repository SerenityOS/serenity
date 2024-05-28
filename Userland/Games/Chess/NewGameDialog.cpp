/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NewGameDialog.h"
#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/SpinBox.h>

namespace Chess {

ErrorOr<NonnullRefPtr<NewGameDialog>> NewGameDialog::try_create(GUI::Window* parent_window, bool unlimited_time_control, i32 time_control_seconds, i32 time_control_increment)
{
    auto new_game_widget = TRY(Chess::NewGameWidget::try_create());
    auto new_game_dialog = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) NewGameDialog(move(new_game_widget), move(parent_window), unlimited_time_control, time_control_seconds, time_control_increment)));
    return new_game_dialog;
}

NewGameDialog::NewGameDialog(NonnullRefPtr<Chess::NewGameWidget> new_game_widget, GUI::Window* parent_window, bool unlimited_time_control, i32 time_control_seconds, i32 time_control_increment)
    : GUI::Dialog(parent_window)
    , m_unlimited_time_control(unlimited_time_control)
    , m_time_control_seconds(time_control_seconds)
    , m_time_control_increment(time_control_increment)
{
    set_title("New Game");
    set_main_widget(new_game_widget);

    m_minutes_spinbox_value = m_time_control_seconds / 60;
    m_seconds_spinbox_value = m_time_control_seconds % 60;

    m_minutes_spinbox = new_game_widget->find_descendant_of_type_named<GUI::SpinBox>("minutes_spinbox");
    m_minutes_spinbox->set_value(m_minutes_spinbox_value);
    m_minutes_spinbox->on_change = [&](auto value) {
        m_minutes_spinbox_value = value;
        m_time_control_seconds = m_minutes_spinbox_value * 60 + m_seconds_spinbox_value;
    };

    m_seconds_spinbox = new_game_widget->find_descendant_of_type_named<GUI::SpinBox>("seconds_spinbox");
    m_seconds_spinbox->set_value(m_seconds_spinbox_value);
    m_seconds_spinbox->on_change = [&](auto value) {
        m_seconds_spinbox_value = value;
        m_time_control_seconds = m_minutes_spinbox_value * 60 + m_seconds_spinbox_value;
    };

    m_increment_spinbox = new_game_widget->find_descendant_of_type_named<GUI::SpinBox>("increment_spinbox");
    m_increment_spinbox->set_value(m_time_control_increment);
    m_increment_spinbox->on_change = [&](auto value) {
        m_time_control_increment = value;
    };

    auto unlimited_checkbox = new_game_widget->find_descendant_of_type_named<GUI::CheckBox>("unlimited_time_control");
    unlimited_checkbox->set_checked(m_unlimited_time_control);
    unlimited_checkbox->on_checked = [&](bool checked) {
        m_unlimited_time_control = checked;
        m_minutes_spinbox->set_enabled(!checked);
        m_seconds_spinbox->set_enabled(!checked);
        m_increment_spinbox->set_enabled(!checked);
    };

    m_minutes_spinbox->set_enabled(!m_unlimited_time_control);
    m_seconds_spinbox->set_enabled(!m_unlimited_time_control);
    m_increment_spinbox->set_enabled(!m_unlimited_time_control);

    auto start_button = new_game_widget->find_descendant_of_type_named<GUI::Button>("start_button");
    start_button->on_click = [this](auto) {
        done(ExecResult::OK);
    };
}

}
