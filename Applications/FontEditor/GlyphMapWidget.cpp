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

#include "GlyphMapWidget.h"
#include <LibGfx/Palette.h>
#include <LibGUI/GPainter.h>

GlyphMapWidget::GlyphMapWidget(Gfx::Font& mutable_font, GUI::Widget* parent)
    : GUI::Frame(parent)
    , m_font(mutable_font)
{
    set_frame_thickness(2);
    set_frame_shape(Gfx::FrameShape::Container);
    set_frame_shadow(Gfx::FrameShadow::Sunken);
    set_relative_rect({ 0, 0, preferred_width(), preferred_height() });
}

GlyphMapWidget::~GlyphMapWidget()
{
}

int GlyphMapWidget::preferred_width() const
{
    return columns() * (font().max_glyph_width() + m_horizontal_spacing) + 2 + frame_thickness() * 2;
}

int GlyphMapWidget::preferred_height() const
{
    return rows() * (font().glyph_height() + m_vertical_spacing) + 2 + frame_thickness() * 2;
}

void GlyphMapWidget::set_selected_glyph(u8 glyph)
{
    if (m_selected_glyph == glyph)
        return;
    m_selected_glyph = glyph;
    if (on_glyph_selected)
        on_glyph_selected(glyph);
    update();
}

Rect GlyphMapWidget::get_outer_rect(u8 glyph) const
{
    int row = glyph / columns();
    int column = glyph % columns();
    return Rect {
        column * (font().max_glyph_width() + m_horizontal_spacing) + 1,
        row * (font().glyph_height() + m_vertical_spacing) + 1,
        font().max_glyph_width() + m_horizontal_spacing,
        font().glyph_height() + m_horizontal_spacing
    }
        .translated(frame_thickness(), frame_thickness());
}

void GlyphMapWidget::update_glyph(u8 glyph)
{
    update(get_outer_rect(glyph));
}

void GlyphMapWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.set_font(font());
    painter.fill_rect(frame_inner_rect(), Color::White);

    u8 glyph = 0;

    for (int row = 0; row < rows(); ++row) {
        for (int column = 0; column < columns(); ++column, ++glyph) {
            Rect outer_rect = get_outer_rect(glyph);
            Rect inner_rect(
                outer_rect.x() + m_horizontal_spacing / 2,
                outer_rect.y() + m_vertical_spacing / 2,
                font().max_glyph_width(),
                font().glyph_height());
            if (glyph == m_selected_glyph) {
                painter.fill_rect(outer_rect, palette().selection());
                painter.draw_glyph(inner_rect.location(), glyph, Color::White);
            } else {
                painter.draw_glyph(inner_rect.location(), glyph, Color::Black);
            }
        }
    }
}

void GlyphMapWidget::mousedown_event(GUI::MouseEvent& event)
{
    // FIXME: This is a silly loop.
    for (unsigned glyph = 0; glyph < 256; ++glyph) {
        if (get_outer_rect(glyph).contains(event.position())) {
            set_selected_glyph(glyph);
            break;
        }
    }
}
