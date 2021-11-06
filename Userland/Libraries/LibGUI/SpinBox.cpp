/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    m_increment_button->set_button_style(Gfx::ButtonStyle::ThickCap);
    m_increment_button->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/upward-triangle.png").release_value_but_fixme_should_propagate_errors());
    m_increment_button->set_focus_policy(GUI::FocusPolicy::NoFocus);
    m_increment_button->on_click = [this](auto) { set_value(m_value + 1); };
    m_increment_button->set_auto_repeat_interval(150);
    m_decrement_button = add<Button>();
    m_decrement_button->set_button_style(Gfx::ButtonStyle::ThickCap);
    m_decrement_button->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/downward-triangle.png").release_value_but_fixme_should_propagate_errors());
    m_decrement_button->set_focus_policy(GUI::FocusPolicy::NoFocus);
    m_decrement_button->on_click = [this](auto) { set_value(m_value - 1); };
    m_decrement_button->set_auto_repeat_interval(150);

    REGISTER_INT_PROPERTY("min", min, set_min);
    REGISTER_INT_PROPERTY("max", max, set_max);
}

SpinBox::~SpinBox()
{
}

void SpinBox::set_value(int value, AllowCallback allow_callback)
{
    value = clamp(value, m_min, m_max);
    if (m_value == value)
        return;
    m_value = value;
    m_editor->set_text(String::number(value));
    update();
    if (on_change && allow_callback == AllowCallback::Yes)
        on_change(value);
}

void SpinBox::set_range(int min, int max, AllowCallback allow_callback)
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
        if (on_change && allow_callback == AllowCallback::Yes)
            on_change(m_value);
    }

    update();
}

void SpinBox::mousewheel_event(MouseEvent& event)
{
    auto wheel_delta = event.wheel_delta() / abs(event.wheel_delta());
    if (event.modifiers() == KeyModifier::Mod_Ctrl)
        wheel_delta *= 6;
    set_value(m_value - wheel_delta);
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
