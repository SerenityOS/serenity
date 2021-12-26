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
#include <LibGUI/Painter.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

GlyphMapWidget::GlyphMapWidget(Gfx::Font& mutable_font)
    : m_font(mutable_font)
{
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

Gfx::Rect GlyphMapWidget::get_outer_rect(u8 glyph) const
{
    int row = glyph / columns();
    int column = glyph % columns();
    return Gfx::Rect {
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
    painter.fill_rect(frame_inner_rect(), palette().base());

    for (int glyph = 0; glyph < m_glyph_count; ++glyph) {
        Gfx::Rect outer_rect = get_outer_rect(glyph);
        Gfx::Rect inner_rect(
            outer_rect.x() + m_horizontal_spacing / 2,
            outer_rect.y() + m_vertical_spacing / 2,
            font().max_glyph_width(),
            font().glyph_height());
        if (glyph == m_selected_glyph) {
            painter.fill_rect(outer_rect, is_focused() ? palette().selection() : palette().inactive_selection());
            painter.draw_glyph(inner_rect.location(), glyph, is_focused() ? palette().selection_text() : palette().inactive_selection_text());
        } else {
            painter.draw_glyph(inner_rect.location(), glyph, palette().base_text());
        }
    }
}

void GlyphMapWidget::mousedown_event(GUI::MouseEvent& event)
{
    GUI::Frame::mousedown_event(event);

    // FIXME: This is a silly loop.
    for (int glyph = 0; glyph < m_glyph_count; ++glyph) {
        if (get_outer_rect(glyph).contains(event.position())) {
            set_selected_glyph(glyph);
            break;
        }
    }
}

void GlyphMapWidget::keydown_event(GUI::KeyEvent& event)
{
    GUI::Frame::keydown_event(event);

    if (event.key() == KeyCode::Key_Up) {
        if (selected_glyph() >= m_columns) {
            set_selected_glyph(selected_glyph() - m_columns);
            return;
        }
    }
    if (event.key() == KeyCode::Key_Down) {
        if (selected_glyph() < m_glyph_count - m_columns) {
            set_selected_glyph(selected_glyph() + m_columns);
            return;
        }
    }
    if (event.key() == KeyCode::Key_Left) {
        if (selected_glyph() > 0) {
            set_selected_glyph(selected_glyph() - 1);
            return;
        }
    }
    if (event.key() == KeyCode::Key_Right) {
        if (selected_glyph() < m_glyph_count - 1) {
            set_selected_glyph(selected_glyph() + 1);
            return;
        }
    }
    if (event.ctrl() && event.key() == KeyCode::Key_Home) {
        set_selected_glyph(0);
        return;
    }
    if (event.ctrl() && event.key() == KeyCode::Key_End) {
        set_selected_glyph(m_glyph_count - 1);
        return;
    }
    if (!event.ctrl() && event.key() == KeyCode::Key_Home) {
        set_selected_glyph(selected_glyph() / m_columns * m_columns);
        return;
    }
    if (!event.ctrl() && event.key() == KeyCode::Key_End) {
        int new_selection = selected_glyph() / m_columns * m_columns + (m_columns - 1);
        new_selection = clamp(new_selection, 0, m_glyph_count - 1);
        set_selected_glyph(new_selection);
        return;
    }
}
