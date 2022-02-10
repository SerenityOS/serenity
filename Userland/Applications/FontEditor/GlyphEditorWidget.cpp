/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GlyphEditorWidget.h"
#include <AK/StringBuilder.h>
#include <AK/UnicodeUtils.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Painter.h>
#include <LibGfx/BitmapFont.h>
#include <LibGfx/Palette.h>
#include <string.h>

void GlyphEditorWidget::initialize(Gfx::BitmapFont& mutable_font)
{
    if (m_font == mutable_font)
        return;
    m_font = mutable_font;
}

void GlyphEditorWidget::set_glyph(int glyph)
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

    auto bitmap = font().raw_glyph(m_glyph).glyph_bitmap();

    for (int y = 0; y < font().glyph_height(); ++y) {
        for (int x = 0; x < font().max_glyph_width(); ++x) {
            Gfx::IntRect rect { x * m_scale, y * m_scale, m_scale, m_scale };
            if (x >= font().raw_glyph_width(m_glyph)) {
                painter.fill_rect(rect, palette().threed_shadow1());
            } else {
                if (bitmap.bit_at(x, y))
                    painter.fill_rect(rect, palette().base_text());
            }
        }
    }
}

bool GlyphEditorWidget::is_glyph_empty()
{
    auto bitmap = font().raw_glyph(m_glyph).glyph_bitmap();
    for (int x = 0; x < font().max_glyph_width(); x++)
        for (int y = 0; y < font().glyph_height(); y++)
            if (bitmap.bit_at(x, y))
                return false;
    return true;
}

void GlyphEditorWidget::mousedown_event(GUI::MouseEvent& event)
{
    if ((event.x() - 1) / m_scale + 1 > font().raw_glyph_width(m_glyph))
        return;
    if (mode() == Move && is_glyph_empty())
        return;
    m_is_clicking_valid_cell = true;
    if (on_undo_event)
        on_undo_event();
    if (mode() == Paint) {
        draw_at_mouse(event);
    } else {
        memset(m_movable_bits, 0, sizeof(m_movable_bits));
        auto bitmap = font().raw_glyph(m_glyph).glyph_bitmap();
        for (int x = 0; x < bitmap.width(); x++) {
            for (int y = 0; y < bitmap.height(); y++) {
                int movable_x = Gfx::GlyphBitmap::max_width() + x;
                int movable_y = Gfx::GlyphBitmap::max_height() + y;
                m_movable_bits[movable_x][movable_y] = bitmap.bit_at(x, y);
            }
        }
        m_scaled_offset_x = (event.x() - 1) / m_scale;
        m_scaled_offset_y = (event.y() - 1) / m_scale;
        move_at_mouse(event);
    }
}

void GlyphEditorWidget::mouseup_event(GUI::MouseEvent&)
{
    if (!m_is_clicking_valid_cell)
        return;
    m_is_clicking_valid_cell = false;
}

void GlyphEditorWidget::mousemove_event(GUI::MouseEvent& event)
{
    if (!m_is_clicking_valid_cell)
        return;
    if (!(event.buttons() & (GUI::MouseButton::Primary | GUI::MouseButton::Secondary)))
        return;
    if (mode() == Paint)
        draw_at_mouse(event);
    else
        move_at_mouse(event);
}

void GlyphEditorWidget::enter_event(Core::Event&)
{
    if (mode() == Move)
        set_override_cursor(Gfx::StandardCursor::Move);
    else
        set_override_cursor(Gfx::StandardCursor::None);
}

void GlyphEditorWidget::draw_at_mouse(GUI::MouseEvent const& event)
{
    bool set = event.buttons() & GUI::MouseButton::Primary;
    bool unset = event.buttons() & GUI::MouseButton::Secondary;
    if (!(set ^ unset))
        return;
    int x = (event.x() - 1) / m_scale;
    int y = (event.y() - 1) / m_scale;
    auto bitmap = font().raw_glyph(m_glyph).glyph_bitmap();
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

void GlyphEditorWidget::move_at_mouse(GUI::MouseEvent const& event)
{
    int x_delta = ((event.x() - 1) / m_scale) - m_scaled_offset_x;
    int y_delta = ((event.y() - 1) / m_scale) - m_scaled_offset_y;
    auto bitmap = font().raw_glyph(m_glyph).glyph_bitmap();
    if (abs(x_delta) > bitmap.width() || abs(y_delta) > bitmap.height())
        return;
    for (int x = 0; x < bitmap.width(); x++) {
        for (int y = 0; y < bitmap.height(); y++) {
            int movable_x = Gfx::GlyphBitmap::max_width() + x - x_delta;
            int movable_y = Gfx::GlyphBitmap::max_height() + y - y_delta;
            bitmap.set_bit_at(x, y, m_movable_bits[movable_x][movable_y]);
        }
    }
    if (on_glyph_altered)
        on_glyph_altered(m_glyph);
    update();
}

static Vector<Vector<u8>> glyph_as_matrix(Gfx::GlyphBitmap const& bitmap)
{
    Vector<Vector<u8>> result;
    result.ensure_capacity(bitmap.height());

    for (int y = 0; y < bitmap.height(); y++) {
        result.empend();
        auto& row = result.last();
        row.ensure_capacity(bitmap.width());
        for (int x = 0; x < bitmap.width(); x++) {
            row.append(bitmap.bit_at(x, y));
        }
    }

    return result;
}

void GlyphEditorWidget::rotate_90(Direction direction)
{
    if (on_undo_event)
        on_undo_event();

    auto bitmap = font().raw_glyph(m_glyph).glyph_bitmap();
    auto matrix = glyph_as_matrix(bitmap);

    for (int y = 0; y < bitmap.height(); y++) {
        for (int x = 0; x < bitmap.width(); x++) {
            int source_x = (direction == Counterclockwise) ? max(bitmap.width() - 1 - y, 0) : y;
            int source_y = (direction == Counterclockwise) ? x : bitmap.width() - 1 - x;
            bool value = false;
            if (source_x < bitmap.width() && source_y < bitmap.height()) {
                value = (direction == Counterclockwise && y >= bitmap.width()) ? false : matrix[source_y][source_x];
            }
            bitmap.set_bit_at(x, y, value);
        }
    }

    if (on_glyph_altered)
        on_glyph_altered(m_glyph);
    update();
}

void GlyphEditorWidget::flip_vertically()
{
    if (on_undo_event)
        on_undo_event();

    auto bitmap = font().raw_glyph(m_glyph).glyph_bitmap();
    auto matrix = glyph_as_matrix(bitmap);

    for (int y = 0; y < bitmap.height(); y++) {
        for (int x = 0; x < bitmap.width(); x++) {
            int source_x = x;
            int source_y = bitmap.height() - 1 - y;
            bool value = matrix[source_y][source_x];
            bitmap.set_bit_at(x, y, value);
        }
    }

    if (on_glyph_altered)
        on_glyph_altered(m_glyph);
    update();
}

void GlyphEditorWidget::flip_horizontally()
{
    if (on_undo_event)
        on_undo_event();

    auto bitmap = font().raw_glyph(m_glyph).glyph_bitmap();
    auto matrix = glyph_as_matrix(bitmap);

    for (int y = 0; y < bitmap.height(); y++) {
        for (int x = 0; x < bitmap.width(); x++) {
            int source_x = bitmap.width() - 1 - x;
            int source_y = y;
            bool value = matrix[source_y][source_x];
            bitmap.set_bit_at(x, y, value);
        }
    }

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

void GlyphEditorWidget::set_scale(int scale)
{
    if (m_scale == scale)
        return;
    m_scale = clamp(scale, 1, 15);
    update();
}
