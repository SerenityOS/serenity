/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2024, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NumericInput.h"
#include <ctype.h>

REGISTER_WIDGET(GUI, NumericInput);

namespace GUI {

NumericInput::NumericInput()
{
    set_text("0"sv);

    on_change = [&] {
        auto number_opt = text().to_number<int>();
        if (number_opt.has_value()) {
            set_value(number_opt.value(), GUI::AllowCallback::No);
            return;
        }

        StringBuilder builder;
        bool first = true;
        for (auto& ch : text()) {
            if (isdigit(ch) || (first && ((ch == '-' && m_min < 0) || ch == '+')))
                builder.append(ch);
            first = false;
        }

        auto new_number_opt = builder.to_byte_string().to_number<int>();
        if (!new_number_opt.has_value()) {
            m_needs_text_reset = true;
            return;
        } else {
            m_needs_text_reset = false;
        }

        set_text(builder.to_byte_string());
        set_value(new_number_opt.value(), GUI::AllowCallback::No);
    };

    on_up_pressed = [&] {
        if (m_value < m_max)
            set_value(m_value + 1);
    };

    on_down_pressed = [&] {
        if (m_value > m_min)
            set_value(m_value - 1);
    };

    on_focusout = [&] { on_focus_lost(); };
    on_return_pressed = [&] { on_focus_lost(); };
    on_escape_pressed = [&] { on_focus_lost(); };

    REGISTER_INT_PROPERTY("min", min, set_min);
    REGISTER_INT_PROPERTY("max", max, set_max);
    REGISTER_INT_PROPERTY("value", value, set_value);
}

void NumericInput::set_min(i64 number)
{
    m_min = number;
    if (m_value < number)
        set_value(number);
}

void NumericInput::set_max(i64 number)
{
    m_max = number;
    if (m_value > number)
        set_value(number);
}

void NumericInput::on_focus_lost()
{
    if (m_needs_text_reset) {
        set_text(ByteString::number(m_value));
        m_needs_text_reset = false;
    }
    if (on_number_changed)
        on_number_changed(m_value);
}

void NumericInput::set_value(i64 number, GUI::AllowCallback allow_callback)
{
    if (number == m_value)
        return;

    m_value = clamp(number, ::min(m_min, m_max), ::max(m_min, m_max));
    set_text(ByteString::number(m_value));
    if (on_number_changed && allow_callback == GUI::AllowCallback::Yes)
        on_number_changed(m_value);
}

void NumericInput::mousewheel_event(GUI::MouseEvent& event)
{
    auto wheel_delta = event.wheel_delta_y() / abs(event.wheel_delta_y());
    if (event.modifiers() == KeyModifier::Mod_Ctrl)
        wheel_delta *= 6;
    set_value(m_value - wheel_delta);
    event.accept();
}

}
