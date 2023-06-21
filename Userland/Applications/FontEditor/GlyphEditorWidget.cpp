/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GlyphEditorWidget.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Font/BitmapFont.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(FontEditor, GlyphEditorWidget);

namespace FontEditor {

void GlyphEditorWidget::initialize(Gfx::BitmapFont* mutable_font)
{
    if (m_font == mutable_font)
        return;
    m_font = mutable_font;
    update();
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
    if (!m_font)
        return;

    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(frame_inner_rect(), palette().base());
    painter.translate(frame_thickness(), frame_thickness());

    painter.translate(-1, -1);
    for (int y = 1; y < m_font->glyph_height(); ++y) {
        int y_below = y - 1;
        bool bold_line = y_below == m_font->baseline() || y_below == m_font->mean_line();
        painter.draw_line({ 0, y * m_scale }, { m_font->max_glyph_width() * m_scale, y * m_scale }, palette().threed_shadow2(), bold_line ? 2 : 1);
    }

    for (int x = 1; x < m_font->max_glyph_width(); ++x)
        painter.draw_line({ x * m_scale, 0 }, { x * m_scale, m_font->glyph_height() * m_scale }, palette().threed_shadow2());

    auto bitmap = m_font->raw_glyph(m_glyph).glyph_bitmap();

    for (int y = 0; y < m_font->glyph_height(); ++y) {
        for (int x = 0; x < m_font->max_glyph_width(); ++x) {
            Gfx::IntRect rect { x * m_scale, y * m_scale, m_scale, m_scale };
            if (x >= m_font->raw_glyph_width(m_glyph)) {
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
    auto bitmap = m_font->raw_glyph(m_glyph).glyph_bitmap();
    for (int x = 0; x < m_font->max_glyph_width(); x++)
        for (int y = 0; y < m_font->glyph_height(); y++)
            if (bitmap.bit_at(x, y))
                return false;
    return true;
}

void GlyphEditorWidget::mousedown_event(GUI::MouseEvent& event)
{
    if ((event.x() - 1) / m_scale + 1 > m_font->raw_glyph_width(m_glyph))
        return;
    if (mode() == Move && is_glyph_empty())
        return;
    m_is_clicking_valid_cell = true;
    if (mode() == Paint) {
        draw_at_mouse(event);
    } else {
        memset(m_movable_bits, 0, sizeof(m_movable_bits));
        auto bitmap = m_font->raw_glyph(m_glyph).glyph_bitmap();
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
    m_is_clicking_valid_cell = false;
    m_is_altering_glyph = false;
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
    auto bitmap = m_font->raw_glyph(m_glyph).glyph_bitmap();
    if (x < 0 || x >= bitmap.width())
        return;
    if (y < 0 || y >= bitmap.height())
        return;
    if (bitmap.bit_at(x, y) == set)
        return;
    if (on_undo_event && !m_is_altering_glyph)
        on_undo_event("Paint Glyph"sv);
    m_is_altering_glyph = true;
    bitmap.set_bit_at(x, y, set);
    if (on_glyph_altered)
        on_glyph_altered(m_glyph);
    update();
}

void GlyphEditorWidget::move_at_mouse(GUI::MouseEvent const& event)
{
    int x_delta = ((event.x() - 1) / m_scale) - m_scaled_offset_x;
    int y_delta = ((event.y() - 1) / m_scale) - m_scaled_offset_y;
    if (x_delta == 0 && y_delta == 0 && !m_is_altering_glyph)
        return;
    auto bitmap = m_font->raw_glyph(m_glyph).glyph_bitmap();
    if (abs(x_delta) > bitmap.width() || abs(y_delta) > bitmap.height())
        return;
    if (on_undo_event && !m_is_altering_glyph)
        on_undo_event("Move Glyph"sv);
    m_is_altering_glyph = true;
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

void GlyphEditorWidget::rotate_90(Gfx::RotationDirection direction)
{
    auto clockwise = direction == Gfx::RotationDirection::Clockwise;
    auto action_text = clockwise ? "Rotate Glyph Clockwise"sv : "Rotate Glyph Counterclockwise"sv;
    if (on_undo_event)
        on_undo_event(action_text);

    auto bitmap = m_font->raw_glyph(m_glyph).glyph_bitmap();
    auto matrix = glyph_as_matrix(bitmap);

    for (int y = 0; y < bitmap.height(); y++) {
        for (int x = 0; x < bitmap.width(); x++) {
            int source_x = clockwise ? y : max(bitmap.width() - 1 - y, 0);
            int source_y = clockwise ? bitmap.width() - 1 - x : x;
            bool value = false;
            if (source_x < bitmap.width() && source_y < bitmap.height()) {
                value = (!clockwise && y >= bitmap.width()) ? false : matrix[source_y][source_x];
            }
            bitmap.set_bit_at(x, y, value);
        }
    }

    if (on_glyph_altered)
        on_glyph_altered(m_glyph);
    update();
}

void GlyphEditorWidget::flip(Gfx::Orientation orientation)
{
    auto vertical = orientation == Gfx::Orientation::Vertical;
    auto action_text = vertical ? "Flip Glyph Vertically"sv : "Flip Glyph Horizontally"sv;
    if (on_undo_event)
        on_undo_event(action_text);

    auto bitmap = m_font->raw_glyph(m_glyph).glyph_bitmap();
    auto matrix = glyph_as_matrix(bitmap);

    for (int y = 0; y < bitmap.height(); y++) {
        for (int x = 0; x < bitmap.width(); x++) {
            int source_x = vertical ? x : bitmap.width() - 1 - x;
            int source_y = vertical ? bitmap.height() - 1 - y : y;
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
    return frame_thickness() * 2 + m_font->max_glyph_width() * m_scale - 1;
}

int GlyphEditorWidget::preferred_height() const
{
    return frame_thickness() * 2 + m_font->glyph_height() * m_scale - 1;
}

void GlyphEditorWidget::set_scale(int scale)
{
    if (m_scale == scale)
        return;
    m_scale = clamp(scale, 1, 15);
    update();
}

}
