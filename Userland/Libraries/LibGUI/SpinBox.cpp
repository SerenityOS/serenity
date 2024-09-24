/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
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
    set_min_size({ SpecialDimension::Shrink });
    set_preferred_size({ SpecialDimension::OpportunisticGrow, SpecialDimension::Shrink });
    m_editor = add<TextBox>();
    m_editor->set_text("0"sv);
    m_editor->on_change = [this, weak_this = make_weak_ptr()] {
        if (!weak_this)
            return;

        auto value = m_editor->text().to_number<signed>();
        if (!value.has_value() && m_editor->text().length() > 0 && m_editor->text() != "-")
            m_editor->do_delete();
    };
    m_editor->on_focusout = [this] {
        set_value_from_current_text();
    };
    m_editor->on_up_pressed = [this] {
        set_value(m_value + 1);
    };
    m_editor->on_down_pressed = [this] {
        set_value(m_value - 1);
    };
    m_editor->on_return_pressed = [this] {
        set_value_from_current_text();
        if (on_return_pressed)
            on_return_pressed();
    };

    m_increment_button = add<Button>();
    m_increment_button->set_button_style(Gfx::ButtonStyle::ThickCap);
    m_increment_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/upward-triangle.png"sv).release_value_but_fixme_should_propagate_errors());
    m_increment_button->set_focus_policy(GUI::FocusPolicy::NoFocus);
    m_increment_button->on_click = [this](auto) { set_value(m_value + 1); };
    m_increment_button->set_auto_repeat_interval(150);
    m_decrement_button = add<Button>();
    m_decrement_button->set_button_style(Gfx::ButtonStyle::ThickCap);
    m_decrement_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/downward-triangle.png"sv).release_value_but_fixme_should_propagate_errors());
    m_decrement_button->set_focus_policy(GUI::FocusPolicy::NoFocus);
    m_decrement_button->on_click = [this](auto) { set_value(m_value - 1); };
    m_decrement_button->set_auto_repeat_interval(150);

    REGISTER_INT_PROPERTY("min", min, set_min);
    REGISTER_INT_PROPERTY("max", max, set_max);
}

void SpinBox::set_value(int value, AllowCallback allow_callback)
{
    value = clamp(value, m_min, m_max);
    m_editor->set_text(ByteString::number(value));
    if (m_value == value)
        return;
    m_value = value;

    m_increment_button->set_enabled(m_value < m_max);
    m_decrement_button->set_enabled(m_value > m_min);

    update();
    if (on_change && allow_callback == AllowCallback::Yes)
        on_change(value);
}

void SpinBox::set_value_from_current_text()
{
    if (m_editor->text().is_empty())
        return;

    auto value = m_editor->text().to_number<int>();
    if (value.has_value())
        set_value(value.value());
    else if (m_editor->text() == "-")
        set_value(-1 * abs(m_value));
    else
        set_value(min());
}

void SpinBox::set_text(StringView text, AllowCallback allow_callback)
{
    m_editor->set_text(text, allow_callback);
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
        m_editor->set_text(ByteString::number(m_value));
        if (on_change && allow_callback == AllowCallback::Yes)
            on_change(m_value);
    }

    m_increment_button->set_enabled(m_value < m_max);
    m_decrement_button->set_enabled(m_value > m_min);

    update();
}

void SpinBox::mousewheel_event(MouseEvent& event)
{
    auto wheel_delta = event.wheel_delta_y() / abs(event.wheel_delta_y());
    if (event.modifiers() == KeyModifier::Mod_Ctrl)
        wheel_delta *= 6;
    set_value(m_value - wheel_delta);
    event.accept();
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

Optional<UISize> SpinBox::calculated_min_size() const
{
    auto constexpr button_width = 15;
    auto width = m_editor->effective_min_size().width().as_int() + button_width;
    auto height = m_editor->effective_min_size().height().as_int();

    return UISize { width, height };
}

}
