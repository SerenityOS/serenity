/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Slider.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, HorizontalSlider)
REGISTER_WIDGET(GUI, Slider)
REGISTER_WIDGET(GUI, VerticalSlider)

namespace GUI {

Slider::Slider(Orientation orientation)
    : AbstractSlider(orientation)
{
    REGISTER_ENUM_PROPERTY("knob_size_mode", knob_size_mode, set_knob_size_mode, KnobSizeMode,
        { KnobSizeMode::Fixed, "Fixed" },
        { KnobSizeMode::Proportional, "Proportional" });
}

void Slider::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    Gfx::IntRect track_rect;

    if (orientation() == Orientation::Horizontal) {
        track_rect = { inner_rect().x(), 0, inner_rect().width(), track_size() };
        track_rect.center_vertically_within(inner_rect());
    } else {
        track_rect = { 0, inner_rect().y(), track_size(), inner_rect().height() };
        track_rect.center_horizontally_within(inner_rect());
    }
    Gfx::StylePainter::paint_frame(painter, track_rect, palette(), Gfx::FrameShape::Panel, Gfx::FrameShadow::Sunken, 1);
    if (is_enabled())
        Gfx::StylePainter::paint_button(painter, knob_rect(), palette(), Gfx::ButtonStyle::Normal, false, m_knob_hovered);
    else
        Gfx::StylePainter::paint_button(painter, knob_rect(), palette(), Gfx::ButtonStyle::Normal, true, m_knob_hovered);
}

Gfx::IntRect Slider::knob_rect() const
{
    auto inner_rect = this->inner_rect();
    Gfx::IntRect rect;
    rect.set_secondary_offset_for_orientation(orientation(), 0);
    rect.set_secondary_size_for_orientation(orientation(), knob_secondary_size());

    if (knob_size_mode() == KnobSizeMode::Fixed) {
        if (max() - min()) {
            float scale = (float)inner_rect.primary_size_for_orientation(orientation()) / (float)(max() - min());
            rect.set_primary_offset_for_orientation(orientation(), inner_rect.primary_offset_for_orientation(orientation()) + ((int)((value() - min()) * scale)) - (knob_fixed_primary_size() / 2));
        } else
            rect.set_primary_size_for_orientation(orientation(), 0);
        rect.set_primary_size_for_orientation(orientation(), knob_fixed_primary_size());
    } else {
        float scale = (float)inner_rect.primary_size_for_orientation(orientation()) / (float)(max() - min() + 1);
        rect.set_primary_offset_for_orientation(orientation(), inner_rect.primary_offset_for_orientation(orientation()) + ((int)((value() - min()) * scale)));
        if (max() - min())
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
    if (event.button() == MouseButton::Primary) {
        if (knob_rect().contains(event.position())) {
            m_dragging = true;
            m_drag_origin = event.position();
            m_drag_origin_value = value();
            return;
        }

        auto const mouse_offset = event.position().primary_offset_for_orientation(orientation());

        if (jump_to_cursor()) {
            float normalized_mouse_offset = 0.0f;
            if (orientation() == Orientation::Vertical) {
                normalized_mouse_offset = static_cast<float>(mouse_offset - track_margin()) / static_cast<float>(inner_rect().height());
            } else {
                normalized_mouse_offset = static_cast<float>(mouse_offset - track_margin()) / static_cast<float>(inner_rect().width());
            }

            int new_value = static_cast<int>(min() + ((max() - min()) * normalized_mouse_offset));
            set_value(new_value);
        } else {
            auto knob_first_edge = knob_rect().first_edge_for_orientation(orientation());
            auto knob_last_edge = knob_rect().last_edge_for_orientation(orientation());
            if (mouse_offset > knob_last_edge)
                increase_slider_by_page_steps(1);
            else if (mouse_offset < knob_first_edge)
                decrease_slider_by_page_steps(1);
        }
    }
    return Widget::mousedown_event(event);
}

void Slider::mousemove_event(MouseEvent& event)
{
    set_knob_hovered(knob_rect().contains(event.position()));
    if (m_dragging) {
        float delta = event.position().primary_offset_for_orientation(orientation()) - m_drag_origin.primary_offset_for_orientation(orientation());
        float scrubbable_range = inner_rect().primary_size_for_orientation(orientation());
        float value_steps_per_scrubbed_pixel = (max() - min()) / scrubbable_range;
        float new_value = m_drag_origin_value + (value_steps_per_scrubbed_pixel * delta);
        set_value((int)new_value);
        return;
    }
    return Widget::mousemove_event(event);
}

void Slider::mouseup_event(MouseEvent& event)
{
    if (event.button() == MouseButton::Primary) {
        m_dragging = false;
        return;
    }

    return Widget::mouseup_event(event);
}

void Slider::mousewheel_event(MouseEvent& event)
{
    auto acceleration_modifier = step();
    auto wheel_delta = event.wheel_delta_y();

    if (event.modifiers() == KeyModifier::Mod_Ctrl)
        acceleration_modifier *= 6;
    if (knob_size_mode() == KnobSizeMode::Proportional)
        wheel_delta /= abs(wheel_delta);

    if (orientation() == Orientation::Horizontal)
        decrease_slider_by(wheel_delta * acceleration_modifier);
    else
        increase_slider_by(wheel_delta * acceleration_modifier);

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
