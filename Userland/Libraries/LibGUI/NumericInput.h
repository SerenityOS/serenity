/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NumericLimits.h>
#include <LibGUI/TextBox.h>

namespace GUI {

class NumericInput final : public GUI::TextBox {
    C_OBJECT(NumericInput)
public:
    virtual ~NumericInput() override = default;

    Function<void(i64)> on_number_changed;

    void set_min_number(i64 number);
    void set_max_number(i64 number);
    void set_current_number(i64 number, GUI::AllowCallback allow_callback = GUI::AllowCallback::Yes);

    virtual void mousewheel_event(GUI::MouseEvent&) override;

private:
    NumericInput();
    void on_focus_lost();

    bool m_needs_text_reset { false };
    i64 m_current_number { 0 };
    i64 m_min_number { NumericLimits<i64>::min() };
    i64 m_max_number { NumericLimits<i64>::max() };
};

}
