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

#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Slider.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

namespace GUI {

Slider::Slider(Orientation orientation)
    : m_orientation(orientation)
{
}

Slider::~Slider()
{
}

void Slider::set_range(int min, int max)
{
    ASSERT(min <= max);
    if (m_min == min && m_max == max)
        return;
    m_min = min;
    m_max = max;
    m_value = clamp(m_value, m_min, m_max);
    update();
}

void Slider::set_value(int value)
{
    value = clamp(value, m_min, m_max);
    if (m_value == value)
        return;
    m_value = value;
    update();

    if (on_value_changed)
        on_value_changed(m_value);
}

void Slider::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    Gfx::Rect track_rect;

    if (orientation() == Orientation::Horizontal) {
        track_rect = { inner_rect().x(), 0, inner_rect().width(), track_size() };
        track_rect.center_vertically_within(inner_rect());
    } else {
        track_rect = { 0, inner_rect().y(), track_size(), inner_rect().height() };
        track_rect.center_horizontally_within(inner_rect());
    }

    Gfx::StylePainter::paint_frame(painter, track_rect, palette(), Gfx::FrameShape::Panel, Gfx::FrameShadow::Sunken, 1);
    Gfx::StylePainter::paint_button(painter, knob_rect(), palette(), Gfx::ButtonStyle::Normal, false, m_knob_hovered);
}

Gfx::Rect Slider::knob_rect() const
{
    auto inner_rect = this->inner_rect();
    Gfx::Rect rect;
    rect.set_secondary_offset_for_orientation(orientation(), 0);
    rect.set_secondary_size_for_orientation(orientation(), knob_secondary_size());

    if (knob_size_mode() == KnobSizeMode::Fixed) {
        if (m_max - m_min) {
            float scale = (float)inner_rect.primary_size_for_orientation(orientation()) / (float)(m_max - m_min);
            rect.set_primary_offset_for_orientation(orientation(), inner_rect.primary_offset_for_orientation(orientation()) + ((int)(m_value * scale)) - (knob_fixed_primary_size() / 2));
        } else
            rect.set_primary_size_for_orientation(orientation(), 0);
        rect.set_primary_size_for_orientation(orientation(), knob_fixed_primary_size());
    } else {
        float scale = (float)inner_rect.primary_size_for_orientation(orientation()) / (float)(m_max - m_min + 1);
        rect.set_primary_offset_for_orientation(orientation(), inner_rect.primary_offset_for_orientation(orientation()) + ((int)(m_value * scale)));
        if (m_max - m_min)
            rect.set_primary_size_for_orientation(orientation(), ::max((int)(scale), knob_fixed_primary_size()));
        else
            rect.set_primary_size_for_orientation(orientation(), inner_rect.primary_size_for_orientation(orientation()));
    }
    if (orientation() == Orientation::Horizontal)
        rect.center_vertically_within(inner_rect);
    else
        rect.center_horizontally_within(inner_rect);
    return rect;
}

void Slider::mousedown_event(MouseEvent& event)
{
    if (!is_enabled())
        return;
    if (event.button() == MouseButton::Left) {
        if (knob_rect().contains(event.position())) {
            m_dragging = true;
            m_drag_origin = event.position();
            m_drag_origin_value = m_value;
            return;
        } else {
            if (event.position().primary_offset_for_orientation(orientation()) > knob_rect().last_edge_for_orientation(orientation()))
                set_value(m_value + 1);
            else if (event.position().primary_offset_for_orientation(orientation()) < knob_rect().first_edge_for_orientation(orientation()))
                set_value(m_value - 1);
        }
    }
    return Widget::mousedown_event(event);
}

void Slider::mousemove_event(MouseEvent& event)
{
    if (!is_enabled())
        return;
    set_knob_hovered(knob_rect().contains(event.position()));
    if (m_dragging) {
        float delta = event.position().primary_offset_for_orientation(orientation()) - m_drag_origin.primary_offset_for_orientation(orientation());
        float scrubbable_range = inner_rect().primary_size_for_orientation(orientation());
        float value_steps_per_scrubbed_pixel = (m_max - m_min) / scrubbable_range;
        float new_value = m_drag_origin_value + (value_steps_per_scrubbed_pixel * delta);
        set_value((int)new_value);
        return;
    }
    return Widget::mousemove_event(event);
}

void Slider::mouseup_event(MouseEvent& event)
{
    if (!is_enabled())
        return;
    if (event.button() == MouseButton::Left) {
        m_dragging = false;
        return;
    }

    return Widget::mouseup_event(event);
}

void Slider::mousewheel_event(MouseEvent& event)
{
    if (!is_enabled())
        return;

    if (orientation() == Orientation::Horizontal)
        set_value(value() - event.wheel_delta() * m_step);
    else
        set_value(value() + event.wheel_delta() * m_step);

    Widget::mousewheel_event(event);
}

void Slider::leave_event(Core::Event& event)
{
    if (!is_enabled())
        return;
    set_knob_hovered(false);
    Widget::leave_event(event);
}

void Slider::change_event(Event& event)
{
    if (event.type() == Event::Type::EnabledChange) {
        if (!is_enabled())
            m_dragging = false;
    }
    Widget::change_event(event);
}

void Slider::set_knob_hovered(bool hovered)
{
    if (m_knob_hovered == hovered)
        return;
    m_knob_hovered = hovered;
    update(knob_rect());
}

}
