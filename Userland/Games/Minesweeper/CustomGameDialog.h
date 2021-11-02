/*
 * Copyright (c) 2021, Pedro Pereira <pmh.pereira@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/SpinBox.h>

class Field;

class CustomGameDialog : public GUI::Dialog {
    C_OBJECT(CustomGameDialog);

public:
    static int show(GUI::Window* parent_window, Field& field);

private:
    CustomGameDialog(GUI::Window* parent_window);
    virtual ~CustomGameDialog() override;

    void set_max_mines();

    RefPtr<GUI::Button> m_ok_button;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::SpinBox> m_columns_spinbox;
    RefPtr<GUI::SpinBox> m_rows_spinbox;
    RefPtr<GUI::SpinBox> m_mines_spinbox;
};
