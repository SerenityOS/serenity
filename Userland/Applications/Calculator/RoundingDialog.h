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
    RoundingDialog();
    virtual ~RoundingDialog() override = default;

    RefPtr<GUI::Label> m_error_label;
    RefPtr<GUI::TextEditor> m_text_editor;
    RefPtr<GUI::Button> m_ok_button;

    static constexpr unsigned m_dialog_length = 200;
    static constexpr unsigned m_dialog_height = 40;
    static constexpr unsigned m_dialog_height_on_error = 80;
};
