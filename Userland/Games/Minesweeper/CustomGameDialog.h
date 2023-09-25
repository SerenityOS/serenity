/*
 * Copyright (c) 2021, Pedro Pereira <pmh.pereira@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "CustomGameWidget.h"
#include <LibGUI/Button.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/SpinBox.h>

class Field;

namespace Minesweeper {

class CustomGameDialog : public GUI::Dialog {
    C_OBJECT_ABSTRACT(CustomGameDialog);

public:
    static ExecResult show(GUI::Window* parent_window, Field& field);
    static ErrorOr<NonnullRefPtr<CustomGameDialog>> try_create(GUI::Window* parent);

private:
    CustomGameDialog(NonnullRefPtr<CustomGameWidget> custom_game_widget, GUI::Window* parent_window);
    virtual ~CustomGameDialog() override = default;

    void set_max_mines();

    RefPtr<GUI::Button> m_ok_button;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::SpinBox> m_columns_spinbox;
    RefPtr<GUI::SpinBox> m_rows_spinbox;
    RefPtr<GUI::SpinBox> m_mines_spinbox;
};

}
