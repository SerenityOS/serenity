/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GlyphMapWidget.h"
#include <AK/MemoryStream.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Painter.h>
#include <LibGfx/BitmapFont.h>
#include <LibGfx/Emoji.h>
#include <LibGfx/Palette.h>

GlyphMapWidget::Selection GlyphMapWidget::Selection::normalized() const
{
    if (m_size > 0)
        return *this;
    return { m_start + m_size, -m_size + 1 };
}

void GlyphMapWidget::Selection::resize_by(int i)
{
    m_size += i;
    if (m_size == 0) {
        if (i < 0)
            m_size--;
        else
            m_size++;
    }
}

bool GlyphMapWidget::Selection::contains(int i) const
{
    auto this_normalized = normalized();
    return i >= this_normalized.m_start && i < this_normalized.m_start + this_normalized.m_size;
}

void GlyphMapWidget::Selection::extend_to(int glyph)
{
    m_size = glyph - m_start;
    if (m_size > 0)
        m_size++;
}

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
    set_active_glyph('A');
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

    scroll_to_glyph(m_active_glyph);

    AbstractScrollableWidget::resize_event(event);
}

void GlyphMapWidget::set_active_glyph(int glyph, ShouldResetSelection should_reset_selection)
{
    if (m_active_glyph == glyph)
        return;
    m_active_glyph = glyph;
    if (should_reset_selection == ShouldResetSelection::Yes) {
        m_selection.set_start(glyph);
        m_selection.set_size(1);
    }
    if (on_active_glyph_changed)
        on_active_glyph_changed(glyph);
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
        if (m_selection.contains(glyph)) {
            painter.fill_rect(outer_rect, is_focused() ? palette().selection() : palette().inactive_selection());
            if (m_font->contains_raw_glyph(glyph))
                painter.draw_glyph(inner_rect.location(), glyph, is_focused() ? palette().selection_text() : palette().inactive_selection_text());
            else if (auto* emoji = Gfx::Emoji::emoji_for_code_point(glyph))
                painter.draw_emoji(inner_rect.location(), *emoji, *m_font);
        } else if (m_font->contains_raw_glyph(glyph)) {
            painter.fill_rect(outer_rect, palette().base());
            painter.draw_glyph(inner_rect.location(), glyph, palette().base_text());
        } else if (auto* emoji = Gfx::Emoji::emoji_for_code_point(glyph)) {
            painter.fill_rect(outer_rect, Gfx::Color { 255, 150, 150 });
            painter.draw_emoji(inner_rect.location(), *emoji, *m_font);
        }
    }
    painter.draw_focus_rect(get_outer_rect(m_active_glyph), Gfx::Color::Black);
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
        if (event.shift())
            m_selection.extend_to(glyph);
        else {
            m_selection.set_size(1);
            m_selection.set_start(glyph);
        }
        set_active_glyph(glyph, ShouldResetSelection::No);
    }
}

void GlyphMapWidget::keydown_event(GUI::KeyEvent& event)
{
    GUI::Frame::keydown_event(event);

    if (!event.ctrl() && !event.shift()) {
        m_selection.set_size(1);
        m_selection.set_start(m_active_glyph);
    }

    if (event.key() == KeyCode::Key_Up) {
        if (m_selection.start() >= m_columns) {
            if (event.shift())
                m_selection.resize_by(-m_columns);
            else
                m_selection.set_start(m_selection.start() - m_columns);
            set_active_glyph(m_active_glyph - m_columns, ShouldResetSelection::No);
            scroll_to_glyph(m_active_glyph);
            return;
        }
    }
    if (event.key() == KeyCode::Key_Down) {
        if (m_selection.start() < m_glyph_count - m_columns) {
            if (event.shift())
                m_selection.resize_by(m_columns);
            else
                m_selection.set_start(m_selection.start() + m_columns);
            set_active_glyph(m_active_glyph + m_columns, ShouldResetSelection::No);
            scroll_to_glyph(m_active_glyph);
            return;
        }
    }
    if (event.key() == KeyCode::Key_Left) {
        if (m_selection.start() > 0) {
            if (event.shift())
                m_selection.resize_by(-1);
            else
                m_selection.set_start(m_selection.start() - 1);
            set_active_glyph(m_active_glyph - 1, ShouldResetSelection::No);
            scroll_to_glyph(m_active_glyph);
            return;
        }
    }
    if (event.key() == KeyCode::Key_Right) {
        if (m_selection.start() < m_glyph_count - 1) {
            if (event.shift())
                m_selection.resize_by(1);
            else
                m_selection.set_start(m_selection.start() + 1);
            set_active_glyph(m_active_glyph + 1, ShouldResetSelection::No);
            scroll_to_glyph(m_active_glyph);
            return;
        }
    }

    // FIXME: Support selection for these.
    if (event.ctrl() && event.key() == KeyCode::Key_Home) {
        set_active_glyph(0);
        scroll_to_glyph(m_active_glyph);
        return;
    }
    if (event.ctrl() && event.key() == KeyCode::Key_End) {
        set_active_glyph(m_glyph_count - 1);
        scroll_to_glyph(m_active_glyph);
        return;
    }
    if (!event.ctrl() && event.key() == KeyCode::Key_Home) {
        set_active_glyph(m_active_glyph / m_columns * m_columns);
        return;
    }
    if (!event.ctrl() && event.key() == KeyCode::Key_End) {
        int new_selection = m_active_glyph / m_columns * m_columns + (m_columns - 1);
        int max = m_glyph_count - 1;
        new_selection = clamp(new_selection, 0, max);
        set_active_glyph(new_selection);
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
