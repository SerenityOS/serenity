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

#include <LibDraw/StylePainter.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GSlider.h>

GSlider::GSlider(GWidget* parent)
    : GSlider(Orientation::Horizontal, parent)
{
}

GSlider::GSlider(Orientation orientation, GWidget* parent)
    : GWidget(parent)
    , m_orientation(orientation)
{
}

GSlider::~GSlider()
{
}

void GSlider::set_range(int min, int max)
{
    ASSERT(min <= max);
    if (m_min == min && m_max == max)
        return;
    m_min = min;
    m_max = max;

    if (m_value > max)
        m_value = max;
    if (m_value < min)
        m_value = min;
    update();
}

void GSlider::set_value(int value)
{
    if (value > m_max)
        value = m_max;
    if (value < m_min)
        value = m_min;
    if (m_value == value)
        return;
    m_value = value;
    update();

    if (on_value_changed)
        on_value_changed(m_value);
}

void GSlider::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    Rect track_rect;

    if (orientation() == Orientation::Horizontal) {
        track_rect = { inner_rect().x(), 0, inner_rect().width(), track_size() };
        track_rect.center_vertically_within(inner_rect());
    } else {
        track_rect = { 0, inner_rect().y(), track_size(), inner_rect().height() };
        track_rect.center_horizontally_within(inner_rect());
    }

    StylePainter::paint_frame(painter, track_rect, palette(), FrameShape::Panel, FrameShadow::Sunken, 1);
    StylePainter::paint_button(painter, knob_rect(), palette(), ButtonStyle::Normal, false, m_knob_hovered);
}

Rect GSlider::knob_rect() const
{
    auto inner_rect = this->inner_rect();
    Rect rect;
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

void GSlider::mousedown_event(GMouseEvent& event)
{
    if (!is_enabled())
        return;
    if (event.button() == GMouseButton::Left) {
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
    return GWidget::mousedown_event(event);
}

void GSlider::mousemove_event(GMouseEvent& event)
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
    return GWidget::mousemove_event(event);
}

void GSlider::mouseup_event(GMouseEvent& event)
{
    if (!is_enabled())
        return;
    if (event.button() == GMouseButton::Left) {
        m_dragging = false;
        return;
    }

    return GWidget::mouseup_event(event);
}

void GSlider::leave_event(CEvent& event)
{
    if (!is_enabled())
        return;
    set_knob_hovered(false);
    GWidget::leave_event(event);
}

void GSlider::change_event(GEvent& event)
{
    if (event.type() == GEvent::Type::EnabledChange) {
        if (!is_enabled())
            m_dragging = false;
    }
    GWidget::change_event(event);
}

void GSlider::set_knob_hovered(bool hovered)
{
    if (m_knob_hovered == hovered)
        return;
    m_knob_hovered = hovered;
    update(knob_rect());
}
