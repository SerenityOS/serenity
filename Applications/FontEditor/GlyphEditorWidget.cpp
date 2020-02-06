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

#include "GlyphEditorWidget.h"
#include <LibGUI/GPainter.h>

GlyphEditorWidget::GlyphEditorWidget(Gfx::Font& mutable_font, GUI::Widget* parent)
    : GUI::Frame(parent)
    , m_font(mutable_font)
{
    set_frame_thickness(2);
    set_frame_shadow(Gfx::FrameShadow::Sunken);
    set_frame_shape(Gfx::FrameShape::Container);
    set_relative_rect({ 0, 0, preferred_width(), preferred_height() });
}

GlyphEditorWidget::~GlyphEditorWidget()
{
}

void GlyphEditorWidget::set_glyph(u8 glyph)
{
    if (m_glyph == glyph)
        return;
    m_glyph = glyph;
    update();
}

void GlyphEditorWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(frame_inner_rect(), Color::White);
    painter.translate(frame_thickness(), frame_thickness());

    painter.translate(-1, -1);
    for (int y = 1; y < font().glyph_height(); ++y)
        painter.draw_line({ 0, y * m_scale }, { font().max_glyph_width() * m_scale, y * m_scale }, Color::Black);

    for (int x = 1; x < font().max_glyph_width(); ++x)
        painter.draw_line({ x * m_scale, 0 }, { x * m_scale, font().glyph_height() * m_scale }, Color::Black);

    auto bitmap = font().glyph_bitmap(m_glyph);

    for (int y = 0; y < font().glyph_height(); ++y) {
        for (int x = 0; x < font().max_glyph_width(); ++x) {
            Gfx::Rect rect { x * m_scale, y * m_scale, m_scale, m_scale };
            if (x >= font().glyph_width(m_glyph)) {
                painter.fill_rect(rect, Color::MidGray);
            } else {
                if (bitmap.bit_at(x, y))
                    painter.fill_rect(rect, Color::Black);
            }
        }
    }
}

void GlyphEditorWidget::mousedown_event(GUI::MouseEvent& event)
{
    draw_at_mouse(event);
}

void GlyphEditorWidget::mousemove_event(GUI::MouseEvent& event)
{
    if (event.buttons() & (GUI::MouseButton::Left | GUI::MouseButton::Right))
        draw_at_mouse(event);
}

void GlyphEditorWidget::draw_at_mouse(const GUI::MouseEvent& event)
{
    bool set = event.buttons() & GUI::MouseButton::Left;
    bool unset = event.buttons() & GUI::MouseButton::Right;
    if (!(set ^ unset))
        return;
    int x = (event.x() - 1) / m_scale;
    int y = (event.y() - 1) / m_scale;
    auto bitmap = font().glyph_bitmap(m_glyph);
    if (x >= bitmap.width())
        return;
    if (y >= bitmap.height())
        return;
    if (bitmap.bit_at(x, y) == set)
        return;
    bitmap.set_bit_at(x, y, set);
    if (on_glyph_altered)
        on_glyph_altered(m_glyph);
    update();
}

int GlyphEditorWidget::preferred_width() const
{
    return frame_thickness() * 2 + font().max_glyph_width() * m_scale - 1;
}

int GlyphEditorWidget::preferred_height() const
{
    return frame_thickness() * 2 + font().glyph_height() * m_scale - 1;
}
