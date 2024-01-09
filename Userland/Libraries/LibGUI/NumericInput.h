/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
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

    i64 min() const { return m_min; }
    void set_min(i64 number);
    i64 max() const { return m_max; }
    void set_max(i64 number);
    i64 value() const { return m_value; }
    void set_value(i64 number, GUI::AllowCallback allow_callback = GUI::AllowCallback::Yes);

    virtual void mousewheel_event(GUI::MouseEvent&) override;

private:
    NumericInput();
    void on_focus_lost();

    bool m_needs_text_reset { false };
    i64 m_value { 0 };
    i64 m_min { NumericLimits<i64>::min() };
    i64 m_max { NumericLimits<i64>::max() };
};

}
