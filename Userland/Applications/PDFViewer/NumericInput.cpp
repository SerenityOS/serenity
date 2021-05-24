/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NumericInput.h"
#include "ctype.h"

NumericInput::NumericInput()
{
    set_text("0");

    on_change = [&] {
        auto number_opt = text().to_int();
        if (number_opt.has_value()) {
            set_current_number(number_opt.value(), false);
            return;
        }

        StringBuilder builder;
        bool first = true;
        for (auto& ch : text()) {
            if (isdigit(ch) || (first && ((ch == '-' && m_min_number < 0) || ch == '+')))
                builder.append(ch);
            first = false;
        }

        auto new_number_opt = builder.to_string().to_int();
        if (!new_number_opt.has_value()) {
            m_needs_text_reset = true;
            set_text(builder.to_string());
            return;
        } else {
            m_needs_text_reset = false;
        }

        set_text(builder.to_string());
        set_current_number(new_number_opt.value(), false);
    };

    on_up_pressed = [&] {
        if (m_current_number < m_max_number)
            set_current_number(m_current_number + 1);
    };

    on_down_pressed = [&] {
        if (m_current_number > m_min_number)
            set_current_number(m_current_number - 1);
    };

    on_focusout = [&] { on_focus_lost(); };
    on_return_pressed = [&] { on_focus_lost(); };
    on_escape_pressed = [&] { on_focus_lost(); };
}

void NumericInput::set_min_number(i32 number)
{
    m_min_number = number;
    if (m_current_number < number)
        set_current_number(number);
}

void NumericInput::set_max_number(i32 number)
{
    m_max_number = number;
    if (m_current_number > number)
        set_current_number(number);
}

void NumericInput::on_focus_lost()
{
    if (m_needs_text_reset) {
        set_text(String::number(m_current_number));
        m_needs_text_reset = false;
    }
    if (on_number_changed)
        on_number_changed(m_current_number);
}

void NumericInput::set_current_number(i32 number, bool call_change_handler)
{
    if (number == m_current_number)
        return;

    m_current_number = clamp(number, m_min_number, m_max_number);
    set_text(String::number(m_current_number));
    if (on_number_changed && call_change_handler)
        on_number_changed(m_current_number);
}
