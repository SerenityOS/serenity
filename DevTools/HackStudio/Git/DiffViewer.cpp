/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
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

#include "DiffViewer.h"
#include <LibDiff/Hunks.h>
#include <LibGUI/AbstractView.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <LibGfx/Color.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

// #define DEBUG_DIFF

namespace HackStudio {

DiffViewer::~DiffViewer()
{
}

void DiffViewer::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(widget_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), palette().color(background_role()));
    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    // Why we need to translate here again? We've already translated the painter.
    // Anyways, it paints correctly so I'll leave it like this.
    painter.fill_rect_with_dither_pattern(
        separator_rect().translated(horizontal_scrollbar().value(), vertical_scrollbar().value()),
        Gfx::Color::LightGray,
        Gfx::Color::White);

    size_t y_offset = 10;
    size_t current_original_line_index = 0;
    for (const auto& hunk : m_hunks) {
        for (size_t i = current_original_line_index; i < hunk.original_start_line; ++i) {
            draw_line(painter, m_original_lines[i], y_offset, LinePosition::Both, LineType::Normal);
            y_offset += line_height();
        }
        current_original_line_index = hunk.original_start_line + hunk.removed_lines.size();

        size_t left_y_offset = y_offset;
        for (const auto& removed_line : hunk.removed_lines) {
            draw_line(painter, removed_line, left_y_offset, LinePosition::Left, LineType::Diff);
            left_y_offset += line_height();
        }
        for (int i = 0; i < (int)hunk.added_lines.size() - (int)hunk.removed_lines.size(); ++i) {
            draw_line(painter, "", left_y_offset, LinePosition::Left, LineType::Missing);
            left_y_offset += line_height();
        }

        size_t right_y_offset = y_offset;
        for (const auto& added_line : hunk.added_lines) {
            draw_line(painter, added_line, right_y_offset, LinePosition::Right, LineType::Diff);
            right_y_offset += line_height();
        }
        for (int i = 0; i < (int)hunk.removed_lines.size() - (int)hunk.added_lines.size(); ++i) {
            draw_line(painter, "", right_y_offset, LinePosition::Right, LineType::Missing);
            right_y_offset += line_height();
        }

        ASSERT(left_y_offset == right_y_offset);
        y_offset = left_y_offset;
    }
    for (size_t i = current_original_line_index; i < m_original_lines.size(); ++i) {
        draw_line(painter, m_original_lines[i], y_offset, LinePosition::Both, LineType::Normal);
        y_offset += line_height();
    }
}

void DiffViewer::draw_line(GUI::Painter& painter, const String& line, size_t y_offset, LinePosition line_position, LineType line_type)
{
    size_t line_width = font().width(line);

    constexpr size_t padding = 10;
    size_t left_side_x_offset = padding;
    size_t right_side_x_offset = separator_rect().x() + padding;

    // FIXME: Long lines will overflow out of their side of the diff view
    Gfx::IntRect left_line_rect { (int)left_side_x_offset, (int)y_offset, (int)line_width, (int)line_height() };
    Gfx::IntRect right_line_rect { (int)right_side_x_offset, (int)y_offset, (int)line_width, (int)line_height() };
    auto color = palette().color(foreground_role());

    if (line_position == LinePosition::Left || line_position == LinePosition::Both) {
        painter.draw_text(left_line_rect, line, Gfx::TextAlignment::TopLeft, color);
        if (line_type != LineType::Normal) {
            Gfx::IntRect outline = { (int)left_side_x_offset, ((int)y_offset) - 2, separator_rect().x() - (int)(padding * 2), (int)line_height() };
            if (line_type == LineType::Diff) {
                painter.fill_rect(
                    outline,
                    red_background());
            }
            if (line_type == LineType::Missing) {
                painter.fill_rect(
                    outline,
                    gray_background());
            }
        }
    }
    if (line_position == LinePosition::Right || line_position == LinePosition::Both) {
        painter.draw_text(right_line_rect, line, Gfx::TextAlignment::TopLeft, color);
        if (line_type != LineType::Normal) {
            Gfx::IntRect outline = { (int)right_side_x_offset, ((int)y_offset) - 2, frame_inner_rect().width() - separator_rect().x() - (int)(padding * 2) - 10, (int)line_height() };
            if (line_type == LineType::Diff) {
                painter.fill_rect(
                    outline,
                    green_background());
            }
            if (line_type == LineType::Missing) {
                painter.fill_rect(
                    outline,
                    gray_background());
            }
        }
    }
}

size_t DiffViewer::line_height() const
{
    return font().glyph_height() + 4;
}

Gfx::IntRect DiffViewer::separator_rect() const
{
    return Gfx::IntRect { frame_inner_rect().width() / 2 - 2,
        0,
        4,
        frame_inner_rect().height() };
}

void DiffViewer::set_content(const String& original, const String& diff)
{
    m_original_lines = split_to_lines(original);
    m_hunks = Diff::parse_hunks(diff);

#ifdef DEBUG_DIFF
    for (size_t i = 0; i < m_original_lines.size(); ++i)
        dbgln("{}:{}", i, m_original_lines[i]);
#endif
}

DiffViewer::DiffViewer()
{
    setup_properties();
}

DiffViewer::DiffViewer(const String& original, const String& diff)
    : m_original_lines(split_to_lines(original))
    , m_hunks(Diff::parse_hunks(diff))
{
    setup_properties();
}

void DiffViewer::setup_properties()
{
    set_font(Gfx::Font::default_fixed_width_font());
    set_background_role(ColorRole::Base);
    set_foreground_role(ColorRole::BaseText);
}

Vector<String> DiffViewer::split_to_lines(const String& text)
{
    // NOTE: This is slightly different than text.split('\n')
    Vector<String> lines;
    size_t next_line_start_index = 0;
    for (size_t i = 0; i < text.length(); ++i) {
        if (text[i] == '\n') {
            auto line_text = text.substring(next_line_start_index, i - next_line_start_index);
            lines.append(move(line_text));
            next_line_start_index = i + 1;
        }
    }
    lines.append(text.substring(next_line_start_index, text.length() - next_line_start_index));
    return lines;
}

Gfx::Color DiffViewer::red_background()
{
    static Gfx::Color color = Gfx::Color::from_rgba(0x88ff0000);
    return color;
}

Gfx::Color DiffViewer::green_background()
{
    static Gfx::Color color = Gfx::Color::from_rgba(0x8800ff00);
    return color;
}

Gfx::Color DiffViewer::gray_background()
{
    static Gfx::Color color = Gfx::Color::from_rgba(0x88888888);
    return color;
}

void DiffViewer::update_content_size()
{
    if (m_hunks.is_empty()) {
        set_content_size({ 0, 0 });
        return;
    }

    size_t num_lines = 0;
    size_t current_original_line_index = 0;
    for (const auto& hunk : m_hunks) {
        num_lines += ((int)hunk.original_start_line - (int)current_original_line_index);

        num_lines += hunk.removed_lines.size();
        if (hunk.added_lines.size() > hunk.removed_lines.size()) {
            num_lines += ((int)hunk.added_lines.size() - (int)hunk.removed_lines.size());
        }
        current_original_line_index = hunk.original_start_line + hunk.removed_lines.size();
    }
    num_lines += ((int)m_original_lines.size() - (int)current_original_line_index);

    // TODO: Support Horizontal scrolling
    set_content_size({ 0, (int)(num_lines * line_height()) });
}

void DiffViewer::resize_event(GUI::ResizeEvent& event)
{
    ScrollableWidget::resize_event(event);
    update_content_size();
}

}
