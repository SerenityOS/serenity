/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NumericLimits.h>
#include <LibGUI/TextBox.h>

class NumericInput final : public GUI::TextBox {
    C_OBJECT(NumericInput)
public:
    virtual ~NumericInput() override = default;

    Function<void(i32)> on_number_changed;

    void set_min_number(i32 number);
    void set_max_number(i32 number);
    void set_current_number(i32 number, bool call_change_handler = true);

private:
    NumericInput();
    void on_focus_lost();

    bool m_needs_text_reset { false };
    i32 m_current_number { 0 };
    i32 m_min_number { NumericLimits<i32>::min() };
    i32 m_max_number { NumericLimits<i32>::max() };
};
