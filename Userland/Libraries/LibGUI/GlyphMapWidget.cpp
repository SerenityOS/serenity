/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GlyphMapWidget.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Font/BitmapFont.h>
#include <LibGfx/Font/Emoji.h>
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
    if (m_size >= 0)
        m_size++;
}

GlyphMapWidget::GlyphMapWidget()
    : AbstractScrollableWidget()
{
    set_focus_policy(FocusPolicy::StrongFocus);
    horizontal_scrollbar().set_visible(false);
    did_change_font();
    set_active_glyph('A');
}

void GlyphMapWidget::resize_event(ResizeEvent& event)
{
    recalculate_content_size();
    AbstractScrollableWidget::resize_event(event);
}

void GlyphMapWidget::set_active_glyph(int glyph, ShouldResetSelection should_reset_selection)
{
    if (m_active_glyph == glyph && should_reset_selection == ShouldResetSelection::No)
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

void GlyphMapWidget::set_selection(int start, int size, Optional<u32> active_glyph)
{
    m_selection.set_start(start);
    m_selection.set_size(size);
    if (active_glyph.has_value())
        set_active_glyph(active_glyph.value(), ShouldResetSelection::No);
}

void GlyphMapWidget::restore_selection(int start, int size, int active_glyph)
{
    if (start == active_glyph && size > 1) {
        start = active_glyph + size - 1;
        size = -size + 1;
    }
    m_selection.set_start(start);
    m_selection.set_size(size);
    set_active_glyph(active_glyph, ShouldResetSelection::No);
}

Gfx::IntRect GlyphMapWidget::get_outer_rect(int glyph) const
{
    glyph -= m_active_range.first;
    int row = glyph / columns();
    int column = glyph % columns();
    return Gfx::IntRect {
        column * (font().max_glyph_width() + m_horizontal_spacing),
        row * (font().pixel_size_rounded_up() + m_vertical_spacing),
        font().max_glyph_width() + m_horizontal_spacing,
        font().pixel_size_rounded_up() + m_vertical_spacing
    }
        .translated(frame_thickness() - horizontal_scrollbar().value(), frame_thickness() - vertical_scrollbar().value());
}

void GlyphMapWidget::update_glyph(int glyph)
{
    set_glyph_modified(glyph, true);
    update(get_outer_rect(glyph));
}

void GlyphMapWidget::paint_event(PaintEvent& event)
{
    Frame::paint_event(event);

    if (!is_enabled())
        return;

    Painter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());

    painter.set_font(font());
    painter.fill_rect(widget_inner_rect(), palette().window().darkened(0.8f));

    auto first_row = vertical_scrollbar().value() / vertical_scrollbar().step();
    auto first_glyph = first_row * columns() + m_active_range.first;
    auto last_glyph = m_active_range.last;

    for (u32 glyph = first_glyph; glyph <= first_glyph + m_visible_glyphs && glyph <= last_glyph; ++glyph) {
        Gfx::IntRect outer_rect = get_outer_rect(glyph);
        Gfx::IntRect inner_rect(
            outer_rect.x() + m_horizontal_spacing / 2,
            outer_rect.y() + m_vertical_spacing / 2,
            font().max_glyph_width(),
            font().pixel_size_rounded_up());
        if (m_selection.contains(glyph)) {
            painter.fill_rect(outer_rect, is_focused() ? palette().selection() : palette().inactive_selection());
            if (font().contains_glyph(glyph))
                painter.draw_glyph(inner_rect.location(), glyph, is_focused() ? palette().selection_text() : palette().inactive_selection_text());
            else if (auto* emoji = Gfx::Emoji::emoji_for_code_point(glyph); emoji && m_show_system_emoji)
                painter.draw_emoji(inner_rect.location(), *emoji, font());
        } else if (font().contains_glyph(glyph)) {
            if (m_highlight_modifications && m_modified_glyphs.contains(glyph)) {
                if (m_original_font && m_original_font->contains_glyph(glyph)) {
                    // Modified
                    if (palette().is_dark())
                        painter.fill_rect(outer_rect, Gfx::Color { 0, 65, 159 });
                    else
                        painter.fill_rect(outer_rect, Gfx::Color { 138, 185, 252 });
                } else {
                    // Newly created
                    if (palette().is_dark())
                        painter.fill_rect(outer_rect, Gfx::Color { 8, 127, 0 });
                    else
                        painter.fill_rect(outer_rect, Gfx::Color { 133, 251, 116 });
                }
            } else {
                painter.fill_rect(outer_rect, palette().base());
            }
            painter.draw_glyph(inner_rect.location(), glyph, palette().base_text());
        } else if (auto* emoji = Gfx::Emoji::emoji_for_code_point(glyph); emoji && m_show_system_emoji) {
            painter.draw_emoji(inner_rect.location(), *emoji, font());
        } else {
            if (m_highlight_modifications && m_original_font && m_original_font->contains_glyph(glyph)) {
                // Deleted
                if (palette().is_dark())
                    painter.fill_rect(outer_rect, Gfx::Color { 127, 0, 0 });
                else
                    painter.fill_rect(outer_rect, Gfx::Color { 255, 150, 150 });
            } else {
                painter.fill_rect(outer_rect, palette().window());
            }
        }
    }
    painter.draw_focus_rect(get_outer_rect(m_active_glyph), palette().focus_outline());
}

Optional<int> GlyphMapWidget::glyph_at_position(Gfx::IntPoint position) const
{
    Gfx::IntPoint map_offset { frame_thickness() - horizontal_scrollbar().value(), frame_thickness() - vertical_scrollbar().value() };
    auto map_position = position - map_offset;
    auto col = (map_position.x() - 1) / ((font().max_glyph_width() + m_horizontal_spacing));
    auto row = (map_position.y() - 1) / ((font().pixel_size_rounded_up() + m_vertical_spacing));
    auto glyph = row * columns() + col + m_active_range.first;
    if (row >= 0 && row < rows() && col >= 0 && col < columns() && glyph < m_glyph_count + m_active_range.first)
        return glyph;

    return {};
}

int GlyphMapWidget::glyph_at_position_clamped(Gfx::IntPoint position) const
{
    Gfx::IntPoint map_offset { frame_thickness() - horizontal_scrollbar().value(), frame_thickness() - vertical_scrollbar().value() };
    auto map_position = position - map_offset;
    auto col = clamp((map_position.x() - 1) / ((font().max_glyph_width() + m_horizontal_spacing)), 0, columns() - 1);
    auto row = clamp((map_position.y() - 1) / ((font().pixel_size_rounded_up() + m_vertical_spacing)), 0, rows() - 1);
    auto glyph = row * columns() + col + m_active_range.first;
    if (row == rows() - 1)
        glyph = min(glyph, m_glyph_count + m_active_range.first - 1);
    return glyph;
}

void GlyphMapWidget::context_menu_event(GUI::ContextMenuEvent& event)
{
    if (on_context_menu_request)
        on_context_menu_request(event);
}

void GlyphMapWidget::mousedown_event(MouseEvent& event)
{
    if (event.button() == MouseButton::Secondary)
        return;

    if (auto maybe_glyph = glyph_at_position(event.position()); maybe_glyph.has_value()) {
        auto glyph = maybe_glyph.value();
        if (event.shift())
            m_selection.extend_to(glyph);
        m_in_drag_select = true;
        set_active_glyph(glyph, event.shift() ? ShouldResetSelection::No : ShouldResetSelection::Yes);
    }
}

void GlyphMapWidget::mouseup_event(GUI::MouseEvent& event)
{
    if (event.button() == MouseButton::Secondary)
        return;

    if (!m_in_drag_select)
        return;
    auto constrained = event.position().constrained(widget_inner_rect());
    if (auto maybe_glyph = glyph_at_position(constrained); maybe_glyph.has_value()) {
        auto glyph = maybe_glyph.value();
        m_selection.extend_to(glyph);
        set_active_glyph(glyph, ShouldResetSelection::No);
    }
    m_in_drag_select = false;
}

void GlyphMapWidget::mousemove_event(GUI::MouseEvent& event)
{
    m_last_mousemove_position = event.position();
    if (auto maybe_glyph = glyph_at_position(event.position()); maybe_glyph.has_value() && maybe_glyph != m_tooltip_glyph) {
        m_tooltip_glyph = maybe_glyph.value();
        auto draw_tooltip = [this]() -> ErrorOr<void> {
            StringBuilder builder;
            TRY(builder.try_appendff("U+{:04X}", m_tooltip_glyph));
            set_tooltip(TRY(builder.to_string()));
            return {};
        }();
        if (draw_tooltip.is_error())
            warnln("Failed to draw tooltip");
    }
    if (m_in_drag_select) {
        auto constrained = event.position().constrained(widget_inner_rect());
        auto glyph = glyph_at_position_clamped(constrained);
        m_selection.extend_to(glyph);
        set_active_glyph(glyph, ShouldResetSelection::No);
        scroll_to_glyph(glyph);
        update();
    }
}

void GlyphMapWidget::automatic_scrolling_timer_did_fire()
{
    if (!m_in_drag_select) {
        set_automatic_scrolling_timer_active(false);
        return;
    }
    auto glyph = glyph_at_position_clamped(m_last_mousemove_position);
    m_selection.extend_to(glyph);
    set_active_glyph(glyph, ShouldResetSelection::No);
    scroll_to_glyph(glyph);
    update();
}

void GlyphMapWidget::doubleclick_event(MouseEvent& event)
{
    if (on_glyph_double_clicked) {
        if (auto maybe_glyph = glyph_at_position(event.position()); maybe_glyph.has_value())
            on_glyph_double_clicked(maybe_glyph.value());
    }
}

void GlyphMapWidget::keydown_event(KeyEvent& event)
{
    if (event.key() == KeyCode::Key_Tab) {
        AbstractScrollableWidget::keydown_event(event);
        return;
    }

    if (event.key() == KeyCode::Key_Escape) {
        m_selection.set_size(1);
        m_selection.set_start(m_active_glyph);
        if (on_escape_pressed)
            on_escape_pressed();
        return;
    }

    if (!event.modifiers() && event.is_arrow_key()) {
        m_selection.set_size(1);
        m_selection.set_start(m_active_glyph);
    }

    if (event.shift() && event.is_arrow_key()) {
        auto resizing_end = m_selection.start() + m_selection.size() - (m_selection.size() > 0 ? 1 : 0);
        set_active_glyph(resizing_end, ShouldResetSelection::No);
        scroll_to_glyph(resizing_end);
    }

    int first_glyph = m_active_range.first;
    int last_glyph = m_active_range.last;
    auto selection = m_selection.normalized();

    if (event.key() == KeyCode::Key_Up) {
        if (m_active_glyph - m_columns < first_glyph)
            return;
        if (event.ctrl() && selection.start() - m_columns < first_glyph)
            return;
        if (event.shift())
            m_selection.extend_to(m_active_glyph - m_columns);
        else
            m_selection.set_start(m_selection.start() - m_columns);
        set_active_glyph(m_active_glyph - m_columns, ShouldResetSelection::No);
        scroll_to_glyph(m_active_glyph);
        return;
    }

    if (event.key() == KeyCode::Key_Down) {
        if (m_active_glyph + m_columns > last_glyph)
            return;
        if (event.ctrl() && selection.start() + selection.size() - 1 + m_columns > last_glyph)
            return;
        if (event.shift())
            m_selection.extend_to(m_active_glyph + m_columns);
        else
            m_selection.set_start(m_selection.start() + m_columns);
        set_active_glyph(m_active_glyph + m_columns, ShouldResetSelection::No);
        scroll_to_glyph(m_active_glyph);
        return;
    }

    if (event.key() == KeyCode::Key_Left) {
        if (event.alt())
            return event.ignore();
        if (m_active_glyph - 1 < first_glyph)
            return;
        if (event.ctrl() && selection.start() - 1 < first_glyph)
            return;
        if (event.shift())
            m_selection.resize_by(-1);
        else
            m_selection.set_start(m_selection.start() - 1);
        set_active_glyph(m_active_glyph - 1, ShouldResetSelection::No);
        scroll_to_glyph(m_active_glyph);
        return;
    }

    if (event.key() == KeyCode::Key_Right) {
        if (event.alt())
            return event.ignore();
        if (m_active_glyph + 1 > last_glyph)
            return;
        if (event.ctrl() && selection.start() + selection.size() > last_glyph)
            return;
        if (event.shift())
            m_selection.resize_by(1);
        else
            m_selection.set_start(m_selection.start() + 1);
        set_active_glyph(m_active_glyph + 1, ShouldResetSelection::No);
        scroll_to_glyph(m_active_glyph);
        return;
    }

    if (event.key() == KeyCode::Key_Home) {
        if (event.alt()) {
            set_active_glyph(first_glyph);
            scroll_to_glyph(m_active_glyph);
            return;
        }
        if (event.ctrl() && event.shift()) {
            m_selection.extend_to(first_glyph);
            set_active_glyph(first_glyph, ShouldResetSelection::No);
            scroll_to_glyph(m_active_glyph);
            return;
        }
        auto start_of_row = (m_active_glyph - first_glyph) / m_columns * m_columns;
        if (event.shift())
            m_selection.extend_to(start_of_row + first_glyph);
        set_active_glyph(start_of_row + first_glyph, event.shift() ? ShouldResetSelection::No : ShouldResetSelection::Yes);
        return;
    }

    if (event.key() == KeyCode::Key_End) {
        if (event.alt()) {
            set_active_glyph(last_glyph);
            scroll_to_glyph(m_active_glyph);
            return;
        }
        if (event.ctrl() && event.shift()) {
            m_selection.extend_to(last_glyph);
            set_active_glyph(last_glyph, ShouldResetSelection::No);
            scroll_to_glyph(m_active_glyph);
            return;
        }
        auto end_of_row = (m_active_glyph - first_glyph) / m_columns * m_columns + (m_columns - 1);
        end_of_row = clamp(end_of_row + first_glyph, first_glyph, last_glyph);
        if (event.shift())
            m_selection.extend_to(end_of_row);
        set_active_glyph(end_of_row, event.shift() ? ShouldResetSelection::No : ShouldResetSelection::Yes);
        return;
    }

    {
        auto first_visible_row = vertical_scrollbar().value() / vertical_scrollbar().step();
        auto last_visible_row = first_visible_row + m_visible_rows;
        auto current_row = (m_active_glyph - first_glyph) / columns();
        auto page = m_active_glyph;

        if (event.key() == KeyCode::Key_PageDown) {
            auto current_page = m_active_glyph + m_columns * (last_visible_row - current_row);
            auto next_page = m_active_glyph + m_columns * m_visible_rows;
            auto remainder = m_active_glyph + m_columns * ((last_glyph - first_glyph) / columns() - current_row);
            if (current_row < last_visible_row && current_page <= last_glyph)
                page = current_page;
            else if (next_page <= last_glyph)
                page = next_page;
            else if (remainder <= last_glyph)
                page = remainder;
            else
                page = remainder - m_columns; // Bottom rows do not always extend across all columns
            if (event.shift())
                m_selection.extend_to(page);
            set_active_glyph(page, event.shift() ? ShouldResetSelection::No : ShouldResetSelection::Yes);
            scroll_to_glyph(m_active_glyph);
            return;
        }

        if (event.key() == KeyCode::Key_PageUp) {
            auto current_page = m_active_glyph - m_columns * (current_row - first_visible_row);
            auto previous_page = m_active_glyph - m_columns * m_visible_rows;
            auto remainder = m_active_glyph - m_columns * current_row;
            if (current_row > first_visible_row && current_page >= first_glyph)
                page = current_page;
            else if (previous_page >= first_glyph)
                page = previous_page;
            else
                page = remainder;
            if (event.shift())
                m_selection.extend_to(page);
            set_active_glyph(page, event.shift() ? ShouldResetSelection::No : ShouldResetSelection::Yes);
            scroll_to_glyph(m_active_glyph);
            return;
        }
    }

    event.ignore();
}

void GlyphMapWidget::did_change_font()
{
    recalculate_content_size();
    vertical_scrollbar().set_step(font().pixel_size_rounded_up() + m_vertical_spacing);
}

void GlyphMapWidget::scroll_to_glyph(int glyph)
{
    glyph -= m_active_range.first;
    int row = glyph / columns();
    int column = glyph % columns();
    auto scroll_rect = Gfx::IntRect {
        column * (font().max_glyph_width() + m_horizontal_spacing),
        row * (font().pixel_size_rounded_up() + m_vertical_spacing),
        font().max_glyph_width() + m_horizontal_spacing,
        font().pixel_size_rounded_up() + m_vertical_spacing
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
    auto event_width = widget_inner_rect().width();
    auto event_height = widget_inner_rect().height();
    m_columns = max(event_width / (font().max_glyph_width() + m_horizontal_spacing), 1);
    m_rows = ceil_div(m_glyph_count, m_columns);

    constexpr auto overdraw_margins = 2;
    auto max_visible_rows = event_height / (font().pixel_size_rounded_up() + m_vertical_spacing);
    m_visible_rows = min(max_visible_rows, m_rows);
    m_visible_glyphs = (m_visible_rows + overdraw_margins) * m_columns;

    int content_width = columns() * (font().max_glyph_width() + m_horizontal_spacing);
    int content_height = rows() * (font().pixel_size_rounded_up() + m_vertical_spacing);
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
    vertical_scrollbar().set_value(0);
    recalculate_content_size();
    update();
}

void GlyphMapWidget::set_highlight_modifications(bool highlight_modifications)
{
    if (m_highlight_modifications == highlight_modifications)
        return;

    m_highlight_modifications = highlight_modifications;
    update();
}

void GlyphMapWidget::set_show_system_emoji(bool show)
{
    if (m_show_system_emoji == show)
        return;
    m_show_system_emoji = show;
    update();
}

void GlyphMapWidget::set_glyph_modified(u32 glyph, bool modified)
{
    if (modified)
        m_modified_glyphs.set(glyph);
    else
        m_modified_glyphs.remove(glyph);
}

bool GlyphMapWidget::glyph_is_modified(u32 glyph)
{
    return m_modified_glyphs.contains(glyph);
}

ErrorOr<void> GlyphMapWidget::initialize(Gfx::Font const* font)
{
    if (font)
        m_original_font = TRY(font->try_clone());
    else
        m_original_font = nullptr;
    m_modified_glyphs.clear();
    AbstractScrollableWidget::set_font(font);
    return {};
}

void GlyphMapWidget::enter_event(Core::Event&)
{
    set_automatic_scrolling_timer_active(false);
}

void GlyphMapWidget::leave_event(Core::Event&)
{
    if (m_in_drag_select)
        set_automatic_scrolling_timer_active(true);
}

Optional<UISize> GlyphMapWidget::calculated_min_size() const
{
    auto scrollbar = vertical_scrollbar().effective_min_size().height().as_int();
    auto min_height = max(font().pixel_size_rounded_up() + m_vertical_spacing, scrollbar);
    auto min_width = font().max_glyph_width() + m_horizontal_spacing + width_occupied_by_vertical_scrollbar();
    return { { min_width + frame_thickness() * 2, min_height + frame_thickness() * 2 } };
}

}
