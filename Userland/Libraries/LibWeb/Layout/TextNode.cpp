/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <LibGfx/Painter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/BlockBox.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/Label.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Page/BrowsingContext.h>

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
        if (!is_ascii_space(string[i]))
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
    if (!browsing_context().is_focused_context())
        return;

    if (!browsing_context().cursor_blink_state())
        return;

    if (browsing_context().cursor_position().node() != &dom_node())
        return;

    // NOTE: This checks if the cursor is before the start or after the end of the fragment. If it is at the end, after all text, it should still be painted.
    if (browsing_context().cursor_position().offset() < (unsigned)fragment.start() || browsing_context().cursor_position().offset() > (unsigned)(fragment.start() + fragment.length()))
        return;

    if (!fragment.layout_node().dom_node() || !fragment.layout_node().dom_node()->is_editable())
        return;

    auto fragment_rect = fragment.absolute_rect();

    float cursor_x = fragment_rect.x() + font().width(fragment.text().substring_view(0, browsing_context().cursor_position().offset() - fragment.start()));
    float cursor_top = fragment_rect.top();
    float cursor_height = fragment_rect.height();
    Gfx::IntRect cursor_rect(cursor_x, cursor_top, 1, cursor_height);

    context.painter().draw_rect(cursor_rect, computed_values().color());
}

// NOTE: This collapes whitespace into a single ASCII space if collapse is true. If previous_is_empty_or_ends_in_whitespace, it also strips leading whitespace.
void TextNode::compute_text_for_rendering(bool collapse, bool previous_is_empty_or_ends_in_whitespace)
{
    auto& data = dom_node().data();
    if (!collapse || data.is_empty()) {
        m_text_for_rendering = data;
        return;
    }

    // NOTE: A couple fast returns to avoid unnecessarily allocating a StringBuilder.
    if (data.length() == 1) {
        if (is_ascii_space(data[0])) {
            if (previous_is_empty_or_ends_in_whitespace)
                m_text_for_rendering = String::empty();
            else {
                static String s_single_space_string = " ";
                m_text_for_rendering = s_single_space_string;
            }
        } else {
            m_text_for_rendering = data;
        }
        return;
    }

    bool contains_space = false;
    for (auto& c : data) {
        if (is_ascii_space(c)) {
            contains_space = true;
            break;
        }
    }
    if (!contains_space) {
        m_text_for_rendering = data;
        return;
    }

    StringBuilder builder(data.length());
    size_t index = 0;

    auto skip_over_whitespace = [&index, &data] {
        while (index < data.length() && is_ascii_space(data[index]))
            ++index;
    };

    if (previous_is_empty_or_ends_in_whitespace)
        skip_over_whitespace();
    while (index < data.length()) {
        if (is_ascii_space(data[index])) {
            builder.append(' ');
            ++index;
            skip_over_whitespace();
        } else {
            builder.append(data[index]);
            ++index;
        }
    }

    m_text_for_rendering = builder.to_string();
}

void TextNode::split_into_lines_by_rules(InlineFormattingContext& context, LayoutMode layout_mode, bool do_collapse, bool do_wrap_lines, bool do_wrap_breaks)
{
    auto& containing_block = context.containing_block();

    auto& font = this->font();

    auto& line_boxes = containing_block.line_boxes();
    containing_block.ensure_last_line_box();
    float available_width = context.available_width_at_line(line_boxes.size() - 1) - line_boxes.last().width();

    compute_text_for_rendering(do_collapse, line_boxes.last().is_empty_or_ends_in_whitespace());
    ChunkIterator iterator(m_text_for_rendering, layout_mode, do_wrap_lines, do_wrap_breaks);

    for (;;) {
        auto chunk_opt = iterator.next();
        if (!chunk_opt.has_value())
            break;
        auto& chunk = chunk_opt.value();

        // Collapse entire fragment into non-existence if previous fragment on line ended in whitespace.
        if (do_collapse && line_boxes.last().is_empty_or_ends_in_whitespace() && chunk.is_all_whitespace)
            continue;

        float chunk_width;
        if (do_wrap_lines) {
            if (do_collapse && is_ascii_space(*chunk.view.begin()) && line_boxes.last().is_empty_or_ends_in_whitespace()) {
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

        if (do_wrap_lines && available_width < 0) {
            containing_block.add_line_box();
            available_width = context.available_width_at_line(line_boxes.size() - 1);
        }

        if (do_wrap_breaks && chunk.has_breaking_newline) {
            containing_block.add_line_box();
            available_width = context.available_width_at_line(line_boxes.size() - 1);
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

bool TextNode::wants_mouse_events() const
{
    return parent() && is<Label>(parent());
}

void TextNode::handle_mousedown(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned button, unsigned)
{
    if (!parent() || !is<Label>(*parent()))
        return;
    downcast<Label>(*parent()).handle_mousedown_on_label({}, position, button);
    browsing_context().event_handler().set_mouse_event_tracking_layout_node(this);
}

void TextNode::handle_mouseup(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned button, unsigned)
{
    if (!parent() || !is<Label>(*parent()))
        return;

    // NOTE: Changing the state of the DOM node may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protect = *this;

    downcast<Label>(*parent()).handle_mouseup_on_label({}, position, button);
    browsing_context().event_handler().set_mouse_event_tracking_layout_node(nullptr);
}

void TextNode::handle_mousemove(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned button, unsigned)
{
    if (!parent() || !is<Label>(*parent()))
        return;
    downcast<Label>(*parent()).handle_mousemove_on_label({}, position, button);
}

TextNode::ChunkIterator::ChunkIterator(StringView const& text, LayoutMode layout_mode, bool wrap_lines, bool wrap_breaks)
    : m_layout_mode(layout_mode)
    , m_wrap_lines(wrap_lines)
    , m_wrap_breaks(wrap_breaks)
    , m_utf8_view(text)
    , m_start_of_chunk(m_utf8_view.begin())
    , m_iterator(m_utf8_view.begin())
{
    m_last_was_space = !text.is_empty() && is_ascii_space(*m_utf8_view.begin());
}

Optional<TextNode::Chunk> TextNode::ChunkIterator::next()
{
    while (m_iterator != m_utf8_view.end()) {
        auto guard = ScopeGuard([&] { ++m_iterator; });
        if (m_layout_mode == LayoutMode::AllPossibleLineBreaks) {
            if (auto result = try_commit_chunk(m_iterator, false); result.has_value())
                return result.release_value();
        }
        if (m_last_was_newline) {
            m_last_was_newline = false;
            if (auto result = try_commit_chunk(m_iterator, true); result.has_value())
                return result.release_value();
        }
        if (m_wrap_breaks && *m_iterator == '\n') {
            m_last_was_newline = true;
            if (auto result = try_commit_chunk(m_iterator, false); result.has_value())
                return result.release_value();
        }
        if (m_wrap_lines) {
            bool is_space = is_ascii_space(*m_iterator);
            if (is_space != m_last_was_space) {
                m_last_was_space = is_space;
                if (auto result = try_commit_chunk(m_iterator, false); result.has_value())
                    return result.release_value();
            }
        }
    }

    if (m_last_was_newline) {
        m_last_was_newline = false;
        if (auto result = try_commit_chunk(m_utf8_view.end(), true); result.has_value())
            return result.release_value();
    }
    if (m_start_of_chunk != m_utf8_view.end()) {
        if (auto result = try_commit_chunk(m_utf8_view.end(), false, true); result.has_value())
            return result.release_value();
    }

    return {};
}

Optional<TextNode::Chunk> TextNode::ChunkIterator::try_commit_chunk(Utf8View::Iterator const& it, bool has_breaking_newline, bool must_commit)
{
    if (m_layout_mode == LayoutMode::OnlyRequiredLineBreaks && !must_commit)
        return {};

    auto start = m_utf8_view.byte_offset_of(m_start_of_chunk);
    auto length = m_utf8_view.byte_offset_of(it) - m_utf8_view.byte_offset_of(m_start_of_chunk);

    if (has_breaking_newline || length > 0) {
        auto chunk_view = m_utf8_view.substring_view(start, length);
        m_start_of_chunk = it;
        return Chunk {
            .view = chunk_view,
            .start = start,
            .length = length,
            .has_breaking_newline = has_breaking_newline,
            .is_all_whitespace = is_all_whitespace(chunk_view.as_string()),
        };
    }

    m_start_of_chunk = it;
    return {};
}

}
