/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGUI/Button.h>
#include <LibGUI/SpinBox.h>
#include <LibGUI/TextBox.h>

REGISTER_WIDGET(GUI, SpinBox)

namespace GUI {

SpinBox::SpinBox()
{
    set_min_width(32);
    set_fixed_height(22);
    m_editor = add<TextBox>();
    m_editor->set_text("0");
    m_editor->on_change = [this] {
        auto value = m_editor->text().to_uint();
        if (value.has_value())
            set_value(value.value());
        else
            m_editor->set_text(String::number(m_value));
    };
    m_editor->on_up_pressed = [this] {
        set_value(m_value + 1);
    };
    m_editor->on_down_pressed = [this] {
        set_value(m_value - 1);
    };

    m_increment_button = add<Button>();
    m_increment_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/upward-triangle.png"));
    m_increment_button->set_focus_policy(GUI::FocusPolicy::NoFocus);
    m_increment_button->on_click = [this](auto) { set_value(m_value + 1); };
    m_increment_button->set_auto_repeat_interval(150);
    m_decrement_button = add<Button>();
    m_decrement_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/downward-triangle.png"));
    m_decrement_button->set_focus_policy(GUI::FocusPolicy::NoFocus);
    m_decrement_button->on_click = [this](auto) { set_value(m_value - 1); };
    m_decrement_button->set_auto_repeat_interval(150);

    REGISTER_INT_PROPERTY("min", min, set_min);
    REGISTER_INT_PROPERTY("max", max, set_max);
}

SpinBox::~SpinBox()
{
}

void SpinBox::set_value(int value)
{
    value = clamp(value, m_min, m_max);
    if (m_value == value)
        return;
    m_value = value;
    m_editor->set_text(String::number(value));
    update();
    if (on_change)
        on_change(value);
}

void SpinBox::set_range(int min, int max)
{
    VERIFY(min <= max);
    if (m_min == min && m_max == max)
        return;

    m_min = min;
    m_max = max;

    int old_value = m_value;
    m_value = clamp(m_value, m_min, m_max);
    if (m_value != old_value) {
        m_editor->set_text(String::number(m_value));
        if (on_change)
            on_change(m_value);
    }

    update();
}

void SpinBox::mousewheel_event(MouseEvent& event)
{
    set_value(m_value - event.wheel_delta());
}

void SpinBox::resize_event(ResizeEvent& event)
{
    int frame_thickness = m_editor->frame_thickness();
    int button_height = (event.size().height() / 2) - frame_thickness;
    int button_width = 15;
    m_increment_button->set_relative_rect(width() - button_width - frame_thickness, frame_thickness, button_width, button_height);
    m_decrement_button->set_relative_rect(width() - button_width - frame_thickness, frame_thickness + button_height, button_width, button_height);
    m_editor->set_relative_rect(0, 0, width(), height());
}

}
