/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ChessWidget.h"
#include "NewGameWidget.h"
#include <LibGUI/Dialog.h>

namespace Chess {

class NewGameDialog final : public GUI::Dialog {
    C_OBJECT_ABSTRACT(NewGameDialog)
public:
    static ErrorOr<NonnullRefPtr<NewGameDialog>> try_create(GUI::Window* parent_window, bool unlimited_time_control, i32 time_control_seconds, i32 time_control_increment);
    bool unlimited_time_control() { return m_unlimited_time_control; }
    i32 time_control_seconds() { return m_time_control_seconds; }
    i32 time_control_increment() { return m_time_control_increment; }

private:
    NewGameDialog(NonnullRefPtr<Chess::NewGameWidget> new_game_widget_widget, GUI::Window* parent_window, bool unlimited_time_control, i32 time_control_seconds, i32 time_control_increment);

    bool m_unlimited_time_control;
    i32 m_time_control_seconds;
    i32 m_time_control_increment;
    i32 m_minutes_spinbox_value;
    i32 m_seconds_spinbox_value;

    RefPtr<GUI::SpinBox> m_minutes_spinbox;
    RefPtr<GUI::SpinBox> m_seconds_spinbox;
    RefPtr<GUI::SpinBox> m_increment_spinbox;
};

}
