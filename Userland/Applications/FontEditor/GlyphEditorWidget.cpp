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
#include <AK/StringBuilder.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Painter.h>
#include <LibGfx/BitmapFont.h>
#include <LibGfx/Palette.h>

GlyphEditorWidget::~GlyphEditorWidget()
{
}

void GlyphEditorWidget::initialize(Gfx::BitmapFont& mutable_font)
{
    if (m_font == mutable_font)
        return;
    m_font = mutable_font;
    set_relative_rect({ 0, 0, preferred_width(), preferred_height() });
    m_glyph = 0;
}

void GlyphEditorWidget::set_glyph(int glyph)
{
    if (m_glyph == glyph)
        return;
    m_glyph = glyph;
    update();
}

void GlyphEditorWidget::delete_glyph()
{
    auto bitmap = font().glyph(m_glyph).glyph_bitmap();
    for (int x = 0; x < bitmap.width(); x++)
        for (int y = 0; y < bitmap.height(); y++)
            bitmap.set_bit_at(x, y, false);
    if (on_glyph_altered)
        on_glyph_altered(m_glyph);
    update();
}

void GlyphEditorWidget::cut_glyph()
{
    copy_glyph();
    delete_glyph();
}

void GlyphEditorWidget::copy_glyph()
{
    auto bitmap = font().glyph(m_glyph).glyph_bitmap();
    u8 bits[bitmap.width()][bitmap.height()];
    for (int x = 0; x < bitmap.width(); x++) {
        for (int y = 0; y < bitmap.height(); y++) {
            bits[x][y] = bitmap.bit_at(x, y);
        }
    }

    StringBuilder glyph_builder;
    if (m_glyph < 128) {
        glyph_builder.append(m_glyph);
    } else {
        glyph_builder.append(128 | 64 | (m_glyph / 64));
        glyph_builder.append(128 | (m_glyph % 64));
    }

    HashMap<String, String> metadata;
    metadata.set("char", glyph_builder.to_string());
    metadata.set("width", String::format("%d", bitmap.width()));
    metadata.set("height", String::format("%d", bitmap.height()));

    auto data = ByteBuffer::copy(&bits[0], bitmap.width() * bitmap.height());
    GUI::Clipboard::the().set_data(data, "glyph/x-fonteditor", metadata);
}

void GlyphEditorWidget::paste_glyph()
{
    auto mime_type = GUI::Clipboard::the().mime_type();
    if (!mime_type.starts_with("glyph/"))
        return;

    auto byte_buffer = GUI::Clipboard::the().data();
    auto buffer_height = GUI::Clipboard::the().data_and_type().metadata.get("height").value().to_int();
    auto buffer_width = GUI::Clipboard::the().data_and_type().metadata.get("width").value().to_int();

    u8 bits[buffer_width.value()][buffer_height.value()];
    int i = 0;
    for (int x = 0; x < buffer_width.value(); x++) {
        for (int y = 0; y < buffer_height.value(); y++) {
            bits[x][y] = byte_buffer[i];
            i++;
        }
    }

    auto bitmap = font().glyph(m_glyph).glyph_bitmap();
    for (int x = 0; x < min(bitmap.width(), buffer_width.value()); x++) {
        for (int y = 0; y < min(bitmap.height(), buffer_height.value()); y++) {
            if (bits[x][y])
                bitmap.set_bit_at(x, y, bits[x][y]);
        }
    }
    if (on_glyph_altered)
        on_glyph_altered(m_glyph);
    update();
}

void GlyphEditorWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(frame_inner_rect(), palette().base());
    painter.translate(frame_thickness(), frame_thickness());

    painter.translate(-1, -1);
    for (int y = 1; y < font().glyph_height(); ++y) {
        int y_below = y - 1;
        bool bold_line = y_below == font().baseline() || y_below == font().mean_line();
        painter.draw_line({ 0, y * m_scale }, { font().max_glyph_width() * m_scale, y * m_scale }, palette().threed_shadow2(), bold_line ? 2 : 1);
    }

    for (int x = 1; x < font().max_glyph_width(); ++x)
        painter.draw_line({ x * m_scale, 0 }, { x * m_scale, font().glyph_height() * m_scale }, palette().threed_shadow2());

    auto bitmap = font().glyph(m_glyph).glyph_bitmap();

    for (int y = 0; y < font().glyph_height(); ++y) {
        for (int x = 0; x < font().max_glyph_width(); ++x) {
            Gfx::IntRect rect { x * m_scale, y * m_scale, m_scale, m_scale };
            if (x >= font().glyph_width(m_glyph)) {
                painter.fill_rect(rect, palette().threed_shadow1());
            } else {
                if (bitmap.bit_at(x, y))
                    painter.fill_rect(rect, palette().base_text());
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
    auto bitmap = font().glyph(m_glyph).glyph_bitmap();
    if (x < 0 || x >= bitmap.width())
        return;
    if (y < 0 || y >= bitmap.height())
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
