/*
 * Copyright (c) 2022, Lucas Chollet <lucas.chollet@free.fr>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGUI/Dialog.h>

class RoundingDialog : public GUI::Dialog {
    C_OBJECT(RoundingDialog);

public:
    static ExecResult show(GUI::Window* parent_window, unsigned& rounding_value);

private:
    RoundingDialog(GUI::Window* parent_window);
    virtual ~RoundingDialog() override = default;

    RefPtr<GUI::SpinBox> m_rounding_spinbox;
    RefPtr<GUI::Widget> m_buttons_container;
    RefPtr<GUI::DialogButton> m_ok_button;
    RefPtr<GUI::DialogButton> m_cancel_button;

    static constexpr unsigned m_dialog_length = 200;
    static constexpr unsigned m_dialog_height = 54;
};
