/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GlyphMapWidget.h"
#include <LibGUI/Painter.h>
#include <LibGfx/BitmapFont.h>
#include <LibGfx/Palette.h>

GlyphMapWidget::GlyphMapWidget()
{
    set_focus_policy(GUI::FocusPolicy::StrongFocus);
    horizontal_scrollbar().set_visible(false);
}

GlyphMapWidget::~GlyphMapWidget()
{
}

void GlyphMapWidget::initialize(Gfx::BitmapFont& mutable_font)
{
    if (m_font == mutable_font)
        return;
    m_font = mutable_font;
    vertical_scrollbar().set_step(font().glyph_height() + m_vertical_spacing);
    set_selected_glyph('A');
}

void GlyphMapWidget::resize_event(GUI::ResizeEvent& event)
{
    if (!m_font)
        return;

    int event_width = event.size().width() - vertical_scrollbar().width() - (frame_thickness() * 2) - m_horizontal_spacing;
    int event_height = event.size().height() - (frame_thickness() * 2);
    m_visible_glyphs = (event_width * event_height) / (font().max_glyph_width() * font().glyph_height());
    m_columns = max(event_width / (font().max_glyph_width() + m_horizontal_spacing), 1);
    m_rows = ceil_div(m_glyph_count, m_columns);

    int content_width = columns() * (font().max_glyph_width() + m_horizontal_spacing);
    int content_height = rows() * (font().glyph_height() + m_vertical_spacing) + frame_thickness();
    set_content_size({ content_width, content_height });

    scroll_to_glyph(m_selected_glyph);

    AbstractScrollableWidget::resize_event(event);
}

void GlyphMapWidget::set_selected_glyph(int glyph)
{
    if (m_selected_glyph == glyph)
        return;
    m_selected_glyph = glyph;
    if (on_glyph_selected)
        on_glyph_selected(glyph);
    update();
}

Gfx::IntRect GlyphMapWidget::get_outer_rect(int glyph) const
{
    int row = glyph / columns();
    int column = glyph % columns();
    return Gfx::IntRect {
        column * (font().max_glyph_width() + m_horizontal_spacing) + 1,
        row * (font().glyph_height() + m_vertical_spacing) + 1,
        font().max_glyph_width() + m_horizontal_spacing,
        font().glyph_height() + m_horizontal_spacing
    }
        .translated(frame_thickness() - horizontal_scrollbar().value(), frame_thickness() - vertical_scrollbar().value());
}

void GlyphMapWidget::update_glyph(int glyph)
{
    update(get_outer_rect(glyph));
}

void GlyphMapWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());

    painter.set_font(font());
    painter.fill_rect(widget_inner_rect(), palette().inactive_window_title());

    int scroll_steps = vertical_scrollbar().value() / vertical_scrollbar().step();
    int first_visible_glyph = scroll_steps * columns();

    for (int glyph = first_visible_glyph; glyph <= first_visible_glyph + m_visible_glyphs && glyph < m_glyph_count; ++glyph) {
        Gfx::IntRect outer_rect = get_outer_rect(glyph);
        Gfx::IntRect inner_rect(
            outer_rect.x() + m_horizontal_spacing / 2,
            outer_rect.y() + m_vertical_spacing / 2,
            font().max_glyph_width(),
            font().glyph_height());
        if (glyph == m_selected_glyph) {
            painter.fill_rect(outer_rect, is_focused() ? palette().selection() : palette().inactive_selection());
            if (m_font->contains_raw_glyph(glyph))
                painter.draw_glyph(inner_rect.location(), glyph, is_focused() ? palette().selection_text() : palette().inactive_selection_text());
        } else if (m_font->contains_raw_glyph(glyph)) {
            painter.fill_rect(outer_rect, palette().base());
            painter.draw_glyph(inner_rect.location(), glyph, palette().base_text());
        }
    }
}

void GlyphMapWidget::mousedown_event(GUI::MouseEvent& event)
{
    GUI::Frame::mousedown_event(event);

    Gfx::IntPoint map_offset { frame_thickness() - horizontal_scrollbar().value(), frame_thickness() - vertical_scrollbar().value() };
    auto map_position = event.position() - map_offset;
    auto col = (map_position.x() - 1) / ((font().max_glyph_width() + m_horizontal_spacing));
    auto row = (map_position.y() - 1) / ((font().glyph_height() + m_vertical_spacing));
    auto glyph = row * columns() + col;
    if (row >= 0 && row < rows() && col >= 0 && col < columns() && glyph < m_glyph_count) {
        set_selected_glyph(glyph);
    }
}

void GlyphMapWidget::keydown_event(GUI::KeyEvent& event)
{
    GUI::Frame::keydown_event(event);

    if (event.key() == KeyCode::Key_Up) {
        if (selected_glyph() >= m_columns) {
            set_selected_glyph(selected_glyph() - m_columns);
            scroll_to_glyph(selected_glyph());
            return;
        }
    }
    if (event.key() == KeyCode::Key_Down) {
        if (selected_glyph() < m_glyph_count - m_columns) {
            set_selected_glyph(selected_glyph() + m_columns);
            scroll_to_glyph(selected_glyph());
            return;
        }
    }
    if (event.key() == KeyCode::Key_Left) {
        if (selected_glyph() > 0) {
            set_selected_glyph(selected_glyph() - 1);
            scroll_to_glyph(selected_glyph());
            return;
        }
    }
    if (event.key() == KeyCode::Key_Right) {
        if (selected_glyph() < m_glyph_count - 1) {
            set_selected_glyph(selected_glyph() + 1);
            scroll_to_glyph(selected_glyph());
            return;
        }
    }
    if (event.ctrl() && event.key() == KeyCode::Key_Home) {
        set_selected_glyph(0);
        scroll_to_glyph(selected_glyph());
        return;
    }
    if (event.ctrl() && event.key() == KeyCode::Key_End) {
        set_selected_glyph(m_glyph_count - 1);
        scroll_to_glyph(selected_glyph());
        return;
    }
    if (!event.ctrl() && event.key() == KeyCode::Key_Home) {
        set_selected_glyph(selected_glyph() / m_columns * m_columns);
        return;
    }
    if (!event.ctrl() && event.key() == KeyCode::Key_End) {
        int new_selection = selected_glyph() / m_columns * m_columns + (m_columns - 1);
        int max = m_glyph_count - 1;
        new_selection = clamp(new_selection, 0, max);
        set_selected_glyph(new_selection);
        return;
    }
}

void GlyphMapWidget::scroll_to_glyph(int glyph)
{
    int row = glyph / columns();
    int column = glyph % columns();
    auto scroll_rect = Gfx::IntRect {
        column * (font().max_glyph_width() + m_horizontal_spacing) + 1,
        row * (font().glyph_height() + m_vertical_spacing) + 1,
        font().max_glyph_width() + m_horizontal_spacing,
        font().glyph_height() + m_horizontal_spacing
    };
    scroll_into_view(scroll_rect, true, true);
}
