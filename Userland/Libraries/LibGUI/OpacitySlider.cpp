/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/OpacitySlider.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, HorizontalOpacitySlider)
REGISTER_WIDGET(GUI, VerticalOpacitySlider)

namespace GUI {

OpacitySlider::OpacitySlider(Gfx::Orientation orientation)
    : AbstractSlider(orientation)
{
    set_min(0);
    set_max(100);
    set_value(100);
    set_preferred_size(SpecialDimension::Fit);
}

Gfx::IntRect OpacitySlider::frame_inner_rect() const
{
    return rect().shrunken(4, 4);
}

void OpacitySlider::paint_event(PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    auto inner_rect = frame_inner_rect();

    // Grid pattern
    Gfx::StylePainter::paint_transparency_grid(painter, inner_rect, palette());

    // Alpha gradient
    painter.fill_rect_with_linear_gradient(inner_rect, Array { Gfx::ColorStop { Color::Transparent, 0 }, Gfx::ColorStop { m_base_color.with_alpha(255), 1 } },
        orientation() == Orientation::Horizontal ? 90.0f : 180.0f);

    constexpr int notch_size = 3;
    if (orientation() == Gfx::Orientation::Horizontal) {
        int notch_y_top = inner_rect.top() + notch_size;
        int notch_y_bottom = inner_rect.bottom() - 1 - notch_size;
        int notch_x = inner_rect.left() + ((float)value() / (float)max() * (float)inner_rect.width());

        // Top notch
        painter.set_pixel(notch_x, notch_y_top, palette().threed_shadow2());
        for (int i = notch_size; i >= 0; --i) {
            painter.set_pixel(notch_x - (i + 1), notch_y_top - i - 1, palette().threed_highlight());
            for (int j = 0; j < i * 2; ++j)
                painter.set_pixel(notch_x - (i + 1) + j + 1, notch_y_top - i - 1, palette().button());
            painter.set_pixel(notch_x + (i + 0), notch_y_top - i - 1, palette().threed_shadow1());
            painter.set_pixel(notch_x + (i + 1), notch_y_top - i - 1, palette().threed_shadow2());
        }

        // Bottom notch
        painter.set_pixel(notch_x, notch_y_bottom, palette().threed_shadow2());
        for (int i = 0; i < notch_size; ++i) {
            painter.set_pixel(notch_x - (i + 1), notch_y_bottom + i + 1, palette().threed_highlight());
            for (int j = 0; j < i * 2; ++j)
                painter.set_pixel(notch_x - (i + 1) + j + 1, notch_y_bottom + i + 1, palette().button());
            painter.set_pixel(notch_x + (i + 0), notch_y_bottom + i + 1, palette().threed_shadow1());
            painter.set_pixel(notch_x + (i + 1), notch_y_bottom + i + 1, palette().threed_shadow2());
        }

        // Hairline
        // NOTE: If we're in the whiter part of the gradient, the notch is painted as shadow between the notches.
        //       If we're in the darker part, the notch is painted as highlight.
        //       We adjust the hairline's x position so it lines up with the shadow/highlight of the notches.
        u8 h = ((float)value() / (float)max()) * 255.0f;
        if (h < 128)
            painter.draw_line({ notch_x, notch_y_top }, { notch_x, notch_y_bottom }, Color(h, h, h, 255));
        else
            painter.draw_line({ notch_x - 1, notch_y_top }, { notch_x - 1, notch_y_bottom }, Color(h, h, h, 255));
    } else {
        int notch_x_left = inner_rect.left() + notch_size;
        int notch_x_right = inner_rect.right() - 1 - notch_size;
        int notch_y = inner_rect.top() + ((float)value() / (float)max() * (float)inner_rect.height());

        // Left notch
        painter.set_pixel(notch_x_left, notch_y, palette().threed_shadow2());
        for (int i = notch_size; i >= 0; --i) {
            painter.set_pixel(notch_x_left - i - 1, notch_y - (i + 1), palette().threed_highlight());
            for (int j = 0; j < i * 2; ++j)
                painter.set_pixel(notch_x_left - i - 1, notch_y - (i + 1) + j + 1, palette().button());
            painter.set_pixel(notch_x_left - i - 1, notch_y + (i + 0), palette().threed_shadow1());
            painter.set_pixel(notch_x_left - i - 1, notch_y + (i + 1), palette().threed_shadow2());
        }

        // Bottom notch
        painter.set_pixel(notch_x_right, notch_y, palette().threed_shadow2());
        for (int i = 0; i < notch_size; ++i) {
            painter.set_pixel(notch_x_right + i + 1, notch_y - (i + 1), palette().threed_highlight());
            for (int j = 0; j < i * 2; ++j)
                painter.set_pixel(notch_x_right + i + 1, notch_y - (i + 1) + j + 1, palette().button());
            painter.set_pixel(notch_x_right + i + 1, notch_y + (i + 0), palette().threed_shadow1());
            painter.set_pixel(notch_x_right + i + 1, notch_y + (i + 1), palette().threed_shadow2());
        }

        // Hairline
        // NOTE: See above
        u8 h = ((float)value() / (float)max()) * 255.0f;
        if (h < 128)
            painter.draw_line({ notch_x_left, notch_y }, { notch_x_right, notch_y }, Color(h, h, h, 255));
        else
            painter.draw_line({ notch_x_left, notch_y - 1 }, { notch_x_right, notch_y - 1 }, Color(h, h, h, 255));
    }

    // Text label
    // FIXME: better support text in vertical orientation, either by having a vertical option for draw_text, or only showing when there is enough space
    auto percent_text = ByteString::formatted("{}%", (int)((float)value() / (float)max() * 100.0f));
    painter.draw_text(inner_rect.translated(1, 1), percent_text, Gfx::TextAlignment::Center, Color::Black);
    painter.draw_text(inner_rect, percent_text, Gfx::TextAlignment::Center, Color::White);

    // Frame
    Gfx::StylePainter::paint_frame(painter, rect(), palette(), Gfx::FrameStyle::SunkenContainer);
}

int OpacitySlider::value_at(Gfx::IntPoint position) const
{
    auto inner_rect = frame_inner_rect();
    auto relevant_position = position.primary_offset_for_orientation(orientation()),
         begin_position = inner_rect.first_edge_for_orientation(orientation()),
         end_position = inner_rect.last_edge_for_orientation(orientation());
    if (relevant_position < begin_position)
        return min();
    if (relevant_position > end_position)
        return max();
    float relative_offset = (float)(relevant_position - begin_position) / (float)inner_rect.primary_size_for_orientation(orientation());

    int range = max() - min();
    return min() + (int)(relative_offset * (float)range);
}

void OpacitySlider::set_base_color(Gfx::Color base_color)
{
    m_base_color = base_color;
    update();
}

void OpacitySlider::mousedown_event(MouseEvent& event)
{
    if (event.button() == MouseButton::Primary) {
        m_dragging = true;
        set_value(value_at(event.position()));
        return;
    }
    AbstractSlider::mousedown_event(event);
}

void OpacitySlider::mousemove_event(MouseEvent& event)
{
    if (m_dragging) {
        set_value(value_at(event.position()));
        return;
    }
    AbstractSlider::mousemove_event(event);
}

void OpacitySlider::mouseup_event(MouseEvent& event)
{
    if (event.button() == MouseButton::Primary) {
        m_dragging = false;
        return;
    }
    AbstractSlider::mouseup_event(event);
}

void OpacitySlider::mousewheel_event(MouseEvent& event)
{
    decrease_slider_by(event.wheel_delta_y());
}

Optional<UISize> OpacitySlider::calculated_min_size() const
{
    if (orientation() == Gfx::Orientation::Vertical)
        return { { 33, 40 } };
    return { { 40, 22 } };
}

Optional<UISize> OpacitySlider::calculated_preferred_size() const
{
    if (orientation() == Gfx::Orientation::Vertical)
        return { { SpecialDimension::Shrink, SpecialDimension::OpportunisticGrow } };
    return { { SpecialDimension::OpportunisticGrow, SpecialDimension::Shrink } };
}

}
