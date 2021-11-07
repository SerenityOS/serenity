/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

static int x_offset;
static int y_offset;

GlyphEditorWidget::~GlyphEditorWidget()
{
}

void GlyphEditorWidget::initialize(Gfx::BitmapFont& mutable_font)
{
    if (m_font == mutable_font)
        return;
    m_font = mutable_font;
    set_relative_rect({ 0, 0, preferred_width(), preferred_height() });
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
    if (on_undo_event)
        on_undo_event();
    auto bitmap = font().raw_glyph(m_glyph).glyph_bitmap();
    for (int x = 0; x < font().max_glyph_width(); x++)
        for (int y = 0; y < font().glyph_height(); y++)
            bitmap.set_bit_at(x, y, false);
    font().set_glyph_width(m_glyph, 0);
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
    auto bitmap = font().raw_glyph(m_glyph).glyph_bitmap();
    u8 bits[bitmap.width()][bitmap.height()];
    for (int x = 0; x < bitmap.width(); x++) {
        for (int y = 0; y < bitmap.height(); y++) {
            bits[x][y] = bitmap.bit_at(x, y);
        }
    }

    StringBuilder glyph_builder;
    if (AK::UnicodeUtils::is_unicode_control_code_point(m_glyph))
        glyph_builder.append(AK::UnicodeUtils::get_unicode_control_code_point_alias(m_glyph).value());
    else if (Gfx::get_char_bidi_class(m_glyph) == Gfx::BidirectionalClass::STRONG_RTL)
        glyph_builder.append_code_point(0xFFFD);
    else
        glyph_builder.append_code_point(m_glyph);

    HashMap<String, String> metadata;
    metadata.set("char", glyph_builder.to_string());
    metadata.set("width", String::number(bitmap.width()));
    metadata.set("height", String::number(bitmap.height()));

    GUI::Clipboard::the().set_data(ReadonlyBytes(&bits[0], bitmap.width() * bitmap.height()), "glyph/x-fonteditor", metadata);
}

void GlyphEditorWidget::paste_glyph()
{
    auto mime_type = GUI::Clipboard::the().mime_type();
    if (!mime_type.starts_with("glyph/"))
        return;

    if (on_undo_event)
        on_undo_event();

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

    auto bitmap = font().raw_glyph(m_glyph).glyph_bitmap();
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
        for (int x = s_max_width; x < s_max_width + bitmap.width(); x++)
            for (int y = s_max_height; y < s_max_height + bitmap.height(); y++)
                m_movable_bits[x][y] = bitmap.bit_at(x - s_max_width, y - s_max_height);
        x_offset = (event.x() - 1) / m_scale;
        y_offset = (event.y() - 1) / m_scale;
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

void GlyphEditorWidget::draw_at_mouse(const GUI::MouseEvent& event)
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

void GlyphEditorWidget::move_at_mouse(const GUI::MouseEvent& event)
{
    int x_delta = ((event.x() - 1) / m_scale) - x_offset;
    int y_delta = ((event.y() - 1) / m_scale) - y_offset;
    auto bitmap = font().raw_glyph(m_glyph).glyph_bitmap();
    if (abs(x_delta) > bitmap.width() || abs(y_delta) > bitmap.height())
        return;
    for (int x = 0; x < bitmap.width(); x++) {
        for (int y = 0; y < bitmap.height(); y++) {
            bitmap.set_bit_at(x, y, m_movable_bits[s_max_width + x - x_delta][s_max_height + y - y_delta]);
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
