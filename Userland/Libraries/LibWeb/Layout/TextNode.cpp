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

#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibGUI/Painter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/BlockBox.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Page/Frame.h>
#include <ctype.h>

namespace Web::Layout {

TextNode::TextNode(DOM::Document& document, DOM::Text& text)
    : Node(document, &text)
{
    set_inline(true);
}

TextNode::~TextNode()
{
}

static bool is_all_whitespace(const StringView& string)
{
    for (size_t i = 0; i < string.length(); ++i) {
        if (!isspace(string[i]))
            return false;
    }
    return true;
}

void TextNode::paint_fragment(PaintContext& context, const LineBoxFragment& fragment, PaintPhase phase) const
{
    auto& painter = context.painter();

    if (phase == PaintPhase::Background) {
        painter.fill_rect(enclosing_int_rect(fragment.absolute_rect()), computed_values().background_color());
    }

    if (phase == PaintPhase::Foreground) {
        painter.set_font(font());

        if (document().inspected_node() == &dom_node())
            context.painter().draw_rect(enclosing_int_rect(fragment.absolute_rect()), Color::Magenta);

        if (computed_values().text_decoration_line() == CSS::TextDecorationLine::Underline)
            painter.draw_line(enclosing_int_rect(fragment.absolute_rect()).bottom_left().translated(0, 1), enclosing_int_rect(fragment.absolute_rect()).bottom_right().translated(0, 1), computed_values().color());

        // FIXME: text-transform should be done already in layout, since uppercase glyphs may be wider than lowercase, etc.
        auto text = m_text_for_rendering;
        auto text_transform = computed_values().text_transform();
        if (text_transform == CSS::TextTransform::Uppercase)
            text = m_text_for_rendering.to_uppercase();
        if (text_transform == CSS::TextTransform::Lowercase)
            text = m_text_for_rendering.to_lowercase();

        painter.draw_text(enclosing_int_rect(fragment.absolute_rect()), text.substring_view(fragment.start(), fragment.length()), Gfx::TextAlignment::CenterLeft, computed_values().color());

        auto selection_rect = fragment.selection_rect(font());
        if (!selection_rect.is_empty()) {
            painter.fill_rect(enclosing_int_rect(selection_rect), context.palette().selection());
            Gfx::PainterStateSaver saver(painter);
            painter.add_clip_rect(enclosing_int_rect(selection_rect));
            painter.draw_text(enclosing_int_rect(fragment.absolute_rect()), text.substring_view(fragment.start(), fragment.length()), Gfx::TextAlignment::CenterLeft, context.palette().selection_text());
        }

        paint_cursor_if_needed(context, fragment);
    }
}

void TextNode::paint_cursor_if_needed(PaintContext& context, const LineBoxFragment& fragment) const
{
    if (!frame().is_focused_frame())
        return;

    if (!frame().cursor_blink_state())
        return;

    if (frame().cursor_position().node() != &dom_node())
        return;

    if (!(frame().cursor_position().offset() >= (unsigned)fragment.start() && frame().cursor_position().offset() < (unsigned)(fragment.start() + fragment.length())))
        return;

    if (!fragment.layout_node().dom_node() || !fragment.layout_node().dom_node()->is_editable())
        return;

    auto fragment_rect = fragment.absolute_rect();

    float cursor_x = fragment_rect.x() + font().width(fragment.text().substring_view(0, frame().cursor_position().offset() - fragment.start()));
    float cursor_top = fragment_rect.top();
    float cursor_height = fragment_rect.height();
    Gfx::IntRect cursor_rect(cursor_x, cursor_top, 1, cursor_height);

    context.painter().draw_rect(cursor_rect, computed_values().color());
}

template<typename Callback>
void TextNode::for_each_chunk(Callback callback, LayoutMode layout_mode, bool do_wrap_lines, bool do_wrap_breaks) const
{
    Utf8View view(m_text_for_rendering);
    if (view.is_empty())
        return;

    auto start_of_chunk = view.begin();

    auto commit_chunk = [&](auto it, bool has_breaking_newline, bool must_commit = false) {
        if (layout_mode == LayoutMode::OnlyRequiredLineBreaks && !must_commit)
            return;

        int start = view.byte_offset_of(start_of_chunk);
        int length = view.byte_offset_of(it) - view.byte_offset_of(start_of_chunk);

        if (has_breaking_newline || length > 0) {
            auto chunk_view = view.substring_view(start, length);
            callback(chunk_view, start, length, has_breaking_newline, is_all_whitespace(chunk_view.as_string()));
        }

        start_of_chunk = it;
    };

    bool last_was_space = isspace(*view.begin());
    bool last_was_newline = false;
    for (auto it = view.begin(); it != view.end();) {
        if (layout_mode == LayoutMode::AllPossibleLineBreaks) {
            commit_chunk(it, false);
        }
        if (last_was_newline) {
            last_was_newline = false;
            commit_chunk(it, true);
        }
        if (do_wrap_breaks && *it == '\n') {
            last_was_newline = true;
            commit_chunk(it, false);
        }
        if (do_wrap_lines) {
            bool is_space = isspace(*it);
            if (is_space != last_was_space) {
                last_was_space = is_space;
                commit_chunk(it, false);
            }
        }
        ++it;
    }
    if (last_was_newline)
        commit_chunk(view.end(), true);
    if (start_of_chunk != view.end())
        commit_chunk(view.end(), false, true);
}

void TextNode::split_into_lines_by_rules(InlineFormattingContext& context, LayoutMode layout_mode, bool do_collapse, bool do_wrap_lines, bool do_wrap_breaks)
{
    auto& containing_block = context.containing_block();

    auto& font = this->font();

    auto& line_boxes = containing_block.line_boxes();
    containing_block.ensure_last_line_box();
    float available_width = context.available_width_at_line(line_boxes.size() - 1) - line_boxes.last().width();

    // Collapse whitespace into single spaces
    if (do_collapse) {
        auto utf8_view = Utf8View(dom_node().data());
        StringBuilder builder(dom_node().data().length());
        auto it = utf8_view.begin();
        auto skip_over_whitespace = [&] {
            auto prev = it;
            while (it != utf8_view.end() && isspace(*it)) {
                prev = it;
                ++it;
            }
            it = prev;
        };
        if (line_boxes.last().is_empty_or_ends_in_whitespace())
            skip_over_whitespace();
        for (; it != utf8_view.end(); ++it) {
            if (!isspace(*it)) {
                builder.append(utf8_view.as_string().characters_without_null_termination() + utf8_view.byte_offset_of(it), it.code_point_length_in_bytes());
            } else {
                builder.append(' ');
                skip_over_whitespace();
            }
        }
        m_text_for_rendering = builder.to_string();
    } else {
        m_text_for_rendering = dom_node().data();
    }

    // do_wrap_lines  => chunks_are_words
    // !do_wrap_lines => chunks_are_lines
    struct Chunk {
        Utf8View view;
        int start { 0 };
        int length { 0 };
        bool is_break { false };
        bool is_all_whitespace { false };
    };
    Vector<Chunk, 128> chunks;

    for_each_chunk(
        [&](const Utf8View& view, int start, int length, bool is_break, bool is_all_whitespace) {
            chunks.append({ Utf8View(view), start, length, is_break, is_all_whitespace });
        },
        layout_mode, do_wrap_lines, do_wrap_breaks);

    for (size_t i = 0; i < chunks.size(); ++i) {
        auto& chunk = chunks[i];

        // Collapse entire fragment into non-existence if previous fragment on line ended in whitespace.
        if (do_collapse && line_boxes.last().is_empty_or_ends_in_whitespace() && chunk.is_all_whitespace)
            continue;

        float chunk_width;
        if (do_wrap_lines) {
            if (do_collapse && isspace(*chunk.view.begin()) && line_boxes.last().is_empty_or_ends_in_whitespace()) {
                // This is a non-empty chunk that starts with collapsible whitespace.
                // We are at either at the start of a new line, or after something that ended in whitespace,
                // so we don't need to contribute our own whitespace to the line. Skip over it instead!
                ++chunk.start;
                --chunk.length;
                chunk.view = chunk.view.substring_view(1, chunk.view.byte_length() - 1);
            }

            chunk_width = font.width(chunk.view) + font.glyph_spacing();

            if (line_boxes.last().width() > 0 && chunk_width > available_width) {
                containing_block.add_line_box();
                available_width = context.available_width_at_line(line_boxes.size() - 1);

                if (do_collapse && chunk.is_all_whitespace)
                    continue;
            }
        } else {
            chunk_width = font.width(chunk.view);
        }

        line_boxes.last().add_fragment(*this, chunk.start, chunk.length, chunk_width, font.glyph_height());
        available_width -= chunk_width;

        if (do_wrap_lines) {
            if (available_width < 0) {
                containing_block.add_line_box();
                available_width = context.available_width_at_line(line_boxes.size() - 1);
            }
        }

        if (do_wrap_breaks) {
            if (chunk.is_break) {
                containing_block.add_line_box();
                available_width = context.available_width_at_line(line_boxes.size() - 1);
            }
        }
    }
}

void TextNode::split_into_lines(InlineFormattingContext& context, LayoutMode layout_mode)
{
    bool do_collapse = true;
    bool do_wrap_lines = true;
    bool do_wrap_breaks = false;

    if (computed_values().white_space() == CSS::WhiteSpace::Nowrap) {
        do_collapse = true;
        do_wrap_lines = false;
        do_wrap_breaks = false;
    } else if (computed_values().white_space() == CSS::WhiteSpace::Pre) {
        do_collapse = false;
        do_wrap_lines = false;
        do_wrap_breaks = true;
    } else if (computed_values().white_space() == CSS::WhiteSpace::PreLine) {
        do_collapse = true;
        do_wrap_lines = true;
        do_wrap_breaks = true;
    } else if (computed_values().white_space() == CSS::WhiteSpace::PreWrap) {
        do_collapse = false;
        do_wrap_lines = true;
        do_wrap_breaks = true;
    }

    split_into_lines_by_rules(context, layout_mode, do_collapse, do_wrap_lines, do_wrap_breaks);
}

}
