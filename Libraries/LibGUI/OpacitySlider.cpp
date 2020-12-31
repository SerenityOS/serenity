/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibGUI/OpacitySlider.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

namespace GUI {

OpacitySlider::OpacitySlider(Gfx::Orientation orientation)
    : AbstractSlider(orientation)
{
    // FIXME: Implement vertical mode.
    ASSERT(orientation == Gfx::Orientation::Horizontal);

    set_min(0);
    set_max(100);
    set_value(100);
    set_fixed_height(20);
}

OpacitySlider::~OpacitySlider()
{
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
    for (int x = inner_rect.left(); x <= inner_rect.right(); ++x) {
        float relative_offset = (float)x / (float)width();
        float alpha = relative_offset * 255.0f;
        Color color { 0, 0, 0, (u8)alpha };
        painter.fill_rect({ x, inner_rect.y(), 1, inner_rect.height() }, color);
    }

    constexpr int notch_size = 3;
    int notch_y_top = inner_rect.top() + notch_size;
    int notch_y_bottom = inner_rect.bottom() - notch_size;
    int notch_x = inner_rect.left() + ((float)value() / (float)max() * (float)inner_rect.width());

    // Top notch
    painter.set_pixel(notch_x, notch_y_top, palette().threed_shadow2());
    for (int i = notch_size; i >= 0; --i) {
        painter.set_pixel(notch_x - (i + 1), notch_y_top - i - 1, palette().threed_highlight());
        for (int j = 0; j < i * 2; ++j) {
            painter.set_pixel(notch_x - (i + 1) + j + 1, notch_y_top - i - 1, palette().button());
        }
        painter.set_pixel(notch_x + (i + 0), notch_y_top - i - 1, palette().threed_shadow1());
        painter.set_pixel(notch_x + (i + 1), notch_y_top - i - 1, palette().threed_shadow2());
    }

    // Bottom notch
    painter.set_pixel(notch_x, notch_y_bottom, palette().threed_shadow2());
    for (int i = 0; i < notch_size; ++i) {
        painter.set_pixel(notch_x - (i + 1), notch_y_bottom + i + 1, palette().threed_highlight());
        for (int j = 0; j < i * 2; ++j) {
            painter.set_pixel(notch_x - (i + 1) + j + 1, notch_y_bottom + i + 1, palette().button());
        }
        painter.set_pixel(notch_x + (i + 0), notch_y_bottom + i + 1, palette().threed_shadow1());
        painter.set_pixel(notch_x + (i + 1), notch_y_bottom + i + 1, palette().threed_shadow2());
    }

    // Hairline
    // NOTE: If we're in the whiter part of the gradient, the notch is painted as shadow between the notches.
    //       If we're in the darker part, the notch is painted as highlight.
    //       We adjust the hairline's x position so it lines up with the shadow/highlight of the notches.
    u8 h = ((float)value() / (float)max()) * 255.0f;
    if (h < 128)
        painter.draw_line({ notch_x, notch_y_top }, { notch_x, notch_y_bottom }, Color(h, h, h, h));
    else
        painter.draw_line({ notch_x - 1, notch_y_top }, { notch_x - 1, notch_y_bottom }, Color(h, h, h, h));

    // Text label
    auto percent_text = String::formatted("{}%", (int)((float)value() / (float)max() * 100.0f));
    painter.draw_text(inner_rect.translated(1, 1), percent_text, Gfx::TextAlignment::Center, Color::Black);
    painter.draw_text(inner_rect, percent_text, Gfx::TextAlignment::Center, Color::White);

    // Frame
    Gfx::StylePainter::paint_frame(painter, rect(), palette(), Gfx::FrameShape::Container, Gfx::FrameShadow::Sunken, 2);
}

int OpacitySlider::value_at(const Gfx::IntPoint& position) const
{
    auto inner_rect = frame_inner_rect();
    if (position.x() < inner_rect.left())
        return min();
    if (position.x() > inner_rect.right())
        return max();
    float relative_offset = (float)(position.x() - inner_rect.x()) / (float)inner_rect.width();
    return relative_offset * (float)max();
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
    set_value(value() - event.wheel_delta());
}

}
