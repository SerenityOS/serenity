/*
 * Copyright (c) 2023, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Painter.h>
#include <LibGUI/RangeSlider.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, RangeSlider)
REGISTER_WIDGET(GUI, HorizontalRangeSlider)

namespace GUI {

RangeSlider::RangeSlider(Gfx::Orientation orientation)
    : AbstractSlider(orientation)

{
    REGISTER_INT_PROPERTY("lower_range", lower_range, set_lower_range);
    REGISTER_INT_PROPERTY("upper_range", upper_range, set_upper_range);
    REGISTER_BOOL_PROPERTY("show_label", show_label, set_show_label);

    set_min(0);
    set_max(100);
    set_lower_range(0);
    set_upper_range(100);
    set_preferred_size(SpecialDimension::Fit);
}

Gfx::IntRect RangeSlider::frame_inner_rect() const
{
    return rect().shrunken(4, 4);
}

void RangeSlider::paint_event(PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    auto inner_rect = frame_inner_rect();

    // Grid pattern
    Gfx::StylePainter::paint_transparency_grid(painter, inner_rect, palette());

    // Alpha gradient
    painter.fill_rect_with_linear_gradient(inner_rect, m_background_gradient, orientation() == Orientation::Horizontal ? 90.0f : 180.0f);

    Gfx::StylePainter::paint_button(painter, knob_rect_for_value(lower_range()), palette(), Gfx::ButtonStyle::Normal, false, m_hovered_lower_knob);
    Gfx::StylePainter::paint_button(painter, knob_rect_for_value(upper_range()), palette(), Gfx::ButtonStyle::Normal, false, m_hovered_upper_knob);

    // Text label
    if (m_show_label) {
        auto range_text = ByteString::formatted("{} to {}", lower_range(), upper_range());
        painter.draw_text(inner_rect.translated(1, 1), range_text, Gfx::TextAlignment::Center, Color::Black);
        painter.draw_text(inner_rect, range_text, Gfx::TextAlignment::Center, Color::White);
    }

    // Frame
    Gfx::StylePainter::paint_frame(painter, rect(), palette(), Gfx::FrameStyle::SunkenContainer);
}

int RangeSlider::value_at(Gfx::IntPoint position) const
{
    auto inner_rect = frame_inner_rect();
    auto relevant_position = position.primary_offset_for_orientation(orientation()),
         begin_position = inner_rect.first_edge_for_orientation(orientation()),
         end_position = inner_rect.last_edge_for_orientation(orientation());
    if (relevant_position < begin_position)
        return min();
    if (relevant_position > end_position)
        return max();

    float relative_offset = static_cast<float>(relevant_position - begin_position) / static_cast<float>(inner_rect.primary_size_for_orientation(orientation()));
    return min() + (relative_offset * static_cast<float>(max() - min()));
}

void RangeSlider::set_gradient_color(Gfx::Color from_color, Gfx::Color to_color)
{
    m_background_gradient = Vector { Gfx::ColorStop { from_color, 0 }, Gfx::ColorStop { to_color, 1 } };
    update();
}

void RangeSlider::set_gradient_colors(Vector<Gfx::ColorStop> colors)
{
    VERIFY(colors.size());
    m_background_gradient = colors;
    update();
}

void RangeSlider::mousedown_event(MouseEvent& event)
{
    if (event.button() == MouseButton::Primary) {
        m_dragging = true;
        int clicked_value = value_at(event.position());
        if (m_hovered_lower_knob)
            set_lower_range(clicked_value);
        if (m_hovered_upper_knob)
            set_upper_range(clicked_value);
        if (!m_hovered_lower_knob && !m_hovered_upper_knob) {
            if (clicked_value < lower_range())
                set_lower_range(lower_range() - AK::min(page_step(), lower_range() - clicked_value));
            if (clicked_value > upper_range())
                set_upper_range(upper_range() + AK::min(page_step(), clicked_value - upper_range()));
            if (clicked_value > lower_range() && clicked_value < upper_range()) {
                set_lower_range(lower_range() + page_step());
                set_upper_range(upper_range() - page_step());
            }
        }

        return;
    }
    AbstractSlider::mousedown_event(event);
}

void RangeSlider::mousemove_event(MouseEvent& event)
{
    if (m_dragging) {
        if (m_hovered_lower_knob)
            set_lower_range(value_at(event.position()));
        if (m_hovered_upper_knob)
            set_upper_range(value_at(event.position()));

        return;
    } else {
        m_hovered_lower_knob = knob_rect_for_value(lower_range()).contains(event.position());
        m_hovered_upper_knob = knob_rect_for_value(upper_range()).contains(event.position());
    }
    AbstractSlider::mousemove_event(event);
}

void RangeSlider::mouseup_event(MouseEvent& event)
{
    if (event.button() == MouseButton::Primary) {
        m_dragging = false;
        m_hovered_lower_knob = false;
        m_hovered_upper_knob = false;
        return;
    }
    AbstractSlider::mouseup_event(event);
}

void RangeSlider::mousewheel_event(MouseEvent& event)
{
    set_lower_range(lower_range() + event.wheel_delta_y());

    if (event.ctrl())
        set_upper_range(upper_range() + event.wheel_delta_y());
    else
        set_upper_range(upper_range() - event.wheel_delta_y());
}

Optional<UISize> RangeSlider::calculated_min_size() const
{
    if (orientation() == Gfx::Orientation::Vertical)
        return { { 33, 40 } };
    return { { 40, 22 } };
}

Optional<UISize> RangeSlider::calculated_preferred_size() const
{
    if (orientation() == Gfx::Orientation::Vertical)
        return { { SpecialDimension::Shrink, SpecialDimension::OpportunisticGrow } };
    return { { SpecialDimension::OpportunisticGrow, SpecialDimension::Shrink } };
}

Gfx::IntRect RangeSlider::knob_rect_for_value(int value) const
{
    auto knob_rect = frame_inner_rect();
    knob_rect.set_left(knob_rect.left() + (static_cast<float>(value + AK::abs(min())) / static_cast<float>((max() - min())) * (knob_rect.width() - c_knob_width)));
    knob_rect.set_width(c_knob_width);

    return knob_rect;
}

void RangeSlider::set_lower_range(int value, AllowCallback allow_callback)
{
    if (lower_range() == value)
        return;

    if (value > upper_range())
        m_lower_range = upper_range();
    else
        m_lower_range = clamp(value, min(), max());

    if (on_range_change && allow_callback == AllowCallback::Yes)
        on_range_change(lower_range(), upper_range());

    update();
}

int RangeSlider::lower_range()
{
    return m_lower_range;
}

void RangeSlider::set_upper_range(int value, AllowCallback allow_callback)
{
    if (upper_range() == value)
        return;
    if (value < lower_range())
        m_upper_range = lower_range();
    else
        m_upper_range = clamp(value, min(), max());

    if (on_range_change && allow_callback == AllowCallback::Yes)
        on_range_change(lower_range(), upper_range());

    update();
}

int RangeSlider::upper_range()
{
    return m_upper_range;
}

void RangeSlider::set_range(int min, int max)
{
    AbstractSlider::set_range(min, max);
    set_lower_range(clamp(lower_range(), AbstractSlider::min(), AbstractSlider::max()), AllowCallback::No);
    set_upper_range(clamp(upper_range(), AbstractSlider::min(), AbstractSlider::max()), AllowCallback::No);
}

void RangeSlider::set_show_label(bool show_label)
{
    m_show_label = show_label;
}

bool RangeSlider::show_label()
{
    return m_show_label;
}

}
