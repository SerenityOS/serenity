/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GlyphMapWidget.h"
#include <LibGUI/Painter.h>
#include <LibGfx/BitmapFont.h>
#include <LibGfx/Emoji.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(GUI, GlyphMapWidget);

namespace GUI {

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
    set_focus_policy(FocusPolicy::StrongFocus);
    horizontal_scrollbar().set_visible(false);
    did_change_font();
    set_active_glyph('A');
}

GlyphMapWidget::~GlyphMapWidget()
{
}

void GlyphMapWidget::resize_event(ResizeEvent& event)
{
    recalculate_content_size();
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
    glyph -= m_active_range.first;
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

void GlyphMapWidget::paint_event(PaintEvent& event)
{
    Frame::paint_event(event);

    Painter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());

    painter.set_font(font());
    painter.fill_rect(widget_inner_rect(), palette().inactive_window_title());

    int scroll_steps = vertical_scrollbar().value() / vertical_scrollbar().step();
    int first_visible_glyph = scroll_steps * columns();
    int range_offset = m_active_range.first;
    int last_glyph = m_active_range.last + 1;

    for (int glyph = first_visible_glyph + range_offset; glyph <= first_visible_glyph + m_visible_glyphs + range_offset && glyph < last_glyph; ++glyph) {
        Gfx::IntRect outer_rect = get_outer_rect(glyph);
        Gfx::IntRect inner_rect(
            outer_rect.x() + m_horizontal_spacing / 2,
            outer_rect.y() + m_vertical_spacing / 2,
            font().max_glyph_width(),
            font().glyph_height());
        if (m_selection.contains(glyph)) {
            painter.fill_rect(outer_rect, is_focused() ? palette().selection() : palette().inactive_selection());
            if (font().contains_glyph(glyph))
                painter.draw_glyph(inner_rect.location(), glyph, is_focused() ? palette().selection_text() : palette().inactive_selection_text());
            else if (auto* emoji = Gfx::Emoji::emoji_for_code_point(glyph))
                painter.draw_emoji(inner_rect.location(), *emoji, font());
        } else if (font().contains_glyph(glyph)) {
            painter.fill_rect(outer_rect, palette().base());
            painter.draw_glyph(inner_rect.location(), glyph, palette().base_text());
        } else if (auto* emoji = Gfx::Emoji::emoji_for_code_point(glyph)) {
            painter.fill_rect(outer_rect, Gfx::Color { 255, 150, 150 });
            painter.draw_emoji(inner_rect.location(), *emoji, font());
        }
    }
    painter.draw_focus_rect(get_outer_rect(m_active_glyph), Gfx::Color::Black);
}

Optional<int> GlyphMapWidget::glyph_at_position(Gfx::IntPoint position) const
{
    Gfx::IntPoint map_offset { frame_thickness() - horizontal_scrollbar().value(), frame_thickness() - vertical_scrollbar().value() };
    auto map_position = position - map_offset;
    auto col = (map_position.x() - 1) / ((font().max_glyph_width() + m_horizontal_spacing));
    auto row = (map_position.y() - 1) / ((font().glyph_height() + m_vertical_spacing));
    auto glyph = row * columns() + col + m_active_range.first;
    if (row >= 0 && row < rows() && col >= 0 && col < columns() && glyph < m_glyph_count + m_active_range.first)
        return glyph;

    return {};
}

void GlyphMapWidget::mousedown_event(MouseEvent& event)
{
    Frame::mousedown_event(event);

    if (auto maybe_glyph = glyph_at_position(event.position()); maybe_glyph.has_value()) {
        auto glyph = maybe_glyph.value();
        if (event.shift())
            m_selection.extend_to(glyph);
        else {
            m_selection.set_size(1);
            m_selection.set_start(glyph);
        }
        m_in_drag_select = true;
        set_active_glyph(glyph, ShouldResetSelection::No);
    }
}

void GlyphMapWidget::mouseup_event(GUI::MouseEvent& event)
{
    Frame::mouseup_event(event);

    if (!m_in_drag_select)
        return;

    if (auto maybe_glyph = glyph_at_position(event.position()); maybe_glyph.has_value()) {
        auto glyph = maybe_glyph.value();
        m_selection.extend_to(glyph);
        m_in_drag_select = false;
        set_active_glyph(glyph, ShouldResetSelection::No);
    }
}

void GlyphMapWidget::mousemove_event(GUI::MouseEvent& event)
{
    Frame::mousemove_event(event);

    if (!m_in_drag_select)
        return;

    if (auto maybe_glyph = glyph_at_position(event.position()); maybe_glyph.has_value()) {
        auto glyph = maybe_glyph.value();
        m_selection.extend_to(glyph);
        scroll_to_glyph(glyph);
        update();
    }
}

void GlyphMapWidget::doubleclick_event(MouseEvent& event)
{
    Widget::doubleclick_event(event);
    if (on_glyph_double_clicked) {
        if (auto maybe_glyph = glyph_at_position(event.position()); maybe_glyph.has_value())
            on_glyph_double_clicked(maybe_glyph.value());
    }
}

void GlyphMapWidget::keydown_event(KeyEvent& event)
{
    Frame::keydown_event(event);

    int range_offset = m_active_range.first;

    if (!event.ctrl() && !event.shift()) {
        m_selection.set_size(1);
        m_selection.set_start(m_active_glyph);
    }

    if (event.key() == KeyCode::Key_Up) {
        if (m_selection.start() - range_offset >= m_columns) {
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
        if (m_selection.start() < m_glyph_count + range_offset - m_columns) {
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
        if (m_selection.start() > range_offset) {
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
        if (m_selection.start() < m_glyph_count + range_offset - 1) {
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
        set_active_glyph(m_active_range.first);
        scroll_to_glyph(m_active_glyph);
        return;
    }
    if (event.ctrl() && event.key() == KeyCode::Key_End) {
        set_active_glyph(m_active_range.last);
        scroll_to_glyph(m_active_glyph);
        return;
    }
    if (!event.ctrl() && event.key() == KeyCode::Key_Home) {
        auto start_of_row = (m_active_glyph - range_offset) / m_columns * m_columns;
        set_active_glyph(start_of_row + range_offset);
        return;
    }
    if (!event.ctrl() && event.key() == KeyCode::Key_End) {
        auto end_of_row = (m_active_glyph - range_offset) / m_columns * m_columns + (m_columns - 1);
        end_of_row = clamp(end_of_row + range_offset, m_active_range.first, m_active_range.last);
        set_active_glyph(end_of_row);
        return;
    }
}

void GlyphMapWidget::did_change_font()
{
    recalculate_content_size();
    vertical_scrollbar().set_step(font().glyph_height() + m_vertical_spacing);
}

void GlyphMapWidget::scroll_to_glyph(int glyph)
{
    glyph -= m_active_range.first;
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

void GlyphMapWidget::select_previous_existing_glyph()
{
    bool search_wrapped = false;
    int first_glyph = m_active_range.first;
    int last_glyph = m_active_range.last;
    for (int i = active_glyph() - 1;; --i) {
        if (i < first_glyph && !search_wrapped) {
            i = last_glyph;
            search_wrapped = true;
        } else if (i < first_glyph && search_wrapped) {
            break;
        }
        if (font().contains_glyph(i)) {
            set_focus(true);
            set_active_glyph(i);
            scroll_to_glyph(i);
            break;
        }
    }
}

void GlyphMapWidget::select_next_existing_glyph()
{
    bool search_wrapped = false;
    int first_glyph = m_active_range.first;
    int last_glyph = m_active_range.last;
    for (int i = active_glyph() + 1;; ++i) {
        if (i > last_glyph && !search_wrapped) {
            i = first_glyph;
            search_wrapped = true;
        } else if (i > last_glyph && search_wrapped) {
            break;
        }
        if (font().contains_glyph(i)) {
            set_focus(true);
            set_active_glyph(i);
            scroll_to_glyph(i);
            break;
        }
    }
}

void GlyphMapWidget::recalculate_content_size()
{
    auto inner_rect = frame_inner_rect();
    int event_width = inner_rect.width() - vertical_scrollbar().width() - m_horizontal_spacing;
    int event_height = inner_rect.height();
    m_visible_glyphs = (event_width * event_height) / (font().max_glyph_width() * font().glyph_height());
    m_columns = max(event_width / (font().max_glyph_width() + m_horizontal_spacing), 1);
    m_rows = ceil_div(m_glyph_count, m_columns);

    int content_width = columns() * (font().max_glyph_width() + m_horizontal_spacing);
    int content_height = rows() * (font().glyph_height() + m_vertical_spacing) + frame_thickness();
    set_content_size({ content_width, content_height });

    scroll_to_glyph(m_active_glyph);
}

void GlyphMapWidget::set_active_range(Unicode::CodePointRange range)
{
    if (m_active_range.first == range.first && m_active_range.last == range.last)
        return;
    m_active_range = range;
    m_glyph_count = range.last - range.first + 1;
    set_active_glyph(range.first);
    recalculate_content_size();
    update();
}

}
