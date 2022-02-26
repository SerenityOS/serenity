/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/StringBuilder.h>
#include <LibGfx/Painter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/Label.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::Layout {

TextNode::TextNode(DOM::Document& document, DOM::Text& text)
    : Node(document, &text)
{
    set_inline(true);
}

TextNode::~TextNode()
{
}

static bool is_all_whitespace(StringView string)
{
    for (size_t i = 0; i < string.length(); ++i) {
        if (!is_ascii_space(string[i]))
            return false;
    }
    return true;
}

void TextNode::paint_text_decoration(Gfx::Painter& painter, LineBoxFragment const& fragment) const
{
    Gfx::IntPoint line_start_point {};
    Gfx::IntPoint line_end_point {};

    auto& font = fragment.layout_node().font();
    auto fragment_box = enclosing_int_rect(fragment.absolute_rect());
    auto glyph_height = font.glyph_height();
    auto baseline = fragment_box.height() / 2 - (glyph_height + 4) / 2 + glyph_height;

    switch (computed_values().text_decoration_line()) {
    case CSS::TextDecorationLine::None:
        return;
    case CSS::TextDecorationLine::Underline:
        line_start_point = fragment_box.top_left().translated(0, baseline + 2);
        line_end_point = fragment_box.top_right().translated(0, baseline + 2);
        break;
    case CSS::TextDecorationLine::Overline:
        line_start_point = fragment_box.top_left().translated(0, baseline - glyph_height);
        line_end_point = fragment_box.top_right().translated(0, baseline - glyph_height);
        break;
    case CSS::TextDecorationLine::LineThrough: {
        auto x_height = font.x_height();
        line_start_point = fragment_box.top_left().translated(0, baseline - x_height / 2);
        line_end_point = fragment_box.top_right().translated(0, baseline - x_height / 2);
        break;
    }
    case CSS::TextDecorationLine::Blink:
        // Conforming user agents may simply not blink the text
        return;
    }

    switch (computed_values().text_decoration_style()) {
        // FIXME: Implement the other styles
    case CSS::TextDecorationStyle::Solid:
    case CSS::TextDecorationStyle::Double:
    case CSS::TextDecorationStyle::Dashed:
    case CSS::TextDecorationStyle::Dotted:
        painter.draw_line(line_start_point, line_end_point, computed_values().color());
        break;
    case CSS::TextDecorationStyle::Wavy:
        // FIXME: There is a thing called text-decoration-thickness which also affects the amplitude here.
        painter.draw_triangle_wave(line_start_point, line_end_point, computed_values().color(), 2);
        break;
    }
}

void TextNode::paint_fragment(PaintContext& context, const LineBoxFragment& fragment, PaintPhase phase) const
{
    auto& painter = context.painter();

    if (phase == PaintPhase::Foreground) {
        auto fragment_absolute_rect = fragment.absolute_rect();

        painter.set_font(font());

        if (document().inspected_node() == &dom_node())
            context.painter().draw_rect(enclosing_int_rect(fragment_absolute_rect), Color::Magenta);

        paint_text_decoration(painter, fragment);

        // FIXME: text-transform should be done already in layout, since uppercase glyphs may be wider than lowercase, etc.
        auto text = m_text_for_rendering;
        auto text_transform = computed_values().text_transform();
        if (text_transform == CSS::TextTransform::Uppercase)
            text = m_text_for_rendering.to_uppercase();
        if (text_transform == CSS::TextTransform::Lowercase)
            text = m_text_for_rendering.to_lowercase();

        // FIXME: This is a hack to prevent text clipping when painting a bitmap font into a too-small box.
        auto draw_rect = enclosing_int_rect(fragment_absolute_rect);
        draw_rect.set_height(max(draw_rect.height(), font().glyph_height()));
        painter.draw_text(draw_rect, text.substring_view(fragment.start(), fragment.length()), Gfx::TextAlignment::CenterLeft, computed_values().color());

        auto selection_rect = fragment.selection_rect(font());
        if (!selection_rect.is_empty()) {
            painter.fill_rect(enclosing_int_rect(selection_rect), context.palette().selection());
            Gfx::PainterStateSaver saver(painter);
            painter.add_clip_rect(enclosing_int_rect(selection_rect));
            painter.draw_text(enclosing_int_rect(fragment_absolute_rect), text.substring_view(fragment.start(), fragment.length()), Gfx::TextAlignment::CenterLeft, context.palette().selection_text());
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

bool TextNode::wants_mouse_events() const
{
    return first_ancestor_of_type<Label>();
}

void TextNode::handle_mousedown(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned button, unsigned)
{
    auto* label = first_ancestor_of_type<Label>();
    if (!label)
        return;
    label->handle_mousedown_on_label({}, position, button);
    browsing_context().event_handler().set_mouse_event_tracking_layout_node(this);
}

void TextNode::handle_mouseup(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned button, unsigned)
{
    auto* label = first_ancestor_of_type<Label>();
    if (!label)
        return;

    // NOTE: Changing the state of the DOM node may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protect = *this;

    label->handle_mouseup_on_label({}, position, button);
    browsing_context().event_handler().set_mouse_event_tracking_layout_node(nullptr);
}

void TextNode::handle_mousemove(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned button, unsigned)
{
    auto* label = first_ancestor_of_type<Label>();
    if (!label)
        return;
    label->handle_mousemove_on_label({}, position, button);
}

TextNode::ChunkIterator::ChunkIterator(StringView text, LayoutMode layout_mode, bool wrap_lines, bool respect_linebreaks)
    : m_layout_mode(layout_mode)
    , m_wrap_lines(wrap_lines)
    , m_respect_linebreaks(respect_linebreaks)
    , m_utf8_view(text)
    , m_iterator(m_utf8_view.begin())
{
    m_last_was_space = !text.is_empty() && is_ascii_space(*m_utf8_view.begin());
}

Optional<TextNode::Chunk> TextNode::ChunkIterator::next()
{
    if (m_iterator == m_utf8_view.end())
        return {};

    auto start_of_chunk = m_iterator;
    while (m_iterator != m_utf8_view.end()) {
        ++m_iterator;

        if (m_last_was_newline) {
            // NOTE: This expression looks out for the case where we have
            //       multiple newlines in a row. Because every output next()
            //       that's a newline newline must be prepared for in advance by
            //       the previous next() call, we need to check whether the next
            //       character is a newline here as well. Otherwise, the newline
            //       becomes part of the next expression and causes rendering
            //       issues.
            m_last_was_newline = m_iterator != m_utf8_view.end() && *m_iterator == '\n';
            if (auto result = try_commit_chunk(start_of_chunk, m_iterator, true); result.has_value())
                return result.release_value();
        }

        if (m_layout_mode == LayoutMode::AllPossibleLineBreaks) {
            if (auto result = try_commit_chunk(start_of_chunk, m_iterator, false); result.has_value()) {
                return result.release_value();
            }
        }

        // NOTE: The checks after this need to look at the current iterator
        //       position, which depends on not being at the end.
        if (m_iterator == m_utf8_view.end())
            break;

        // NOTE: When we're supposed to stop on linebreaks, we're actually
        //       supposed to output two chunks: "content" and "\n". Since we
        //       can't output two chunks at once, we store this information as a
        //       flag to output the newline immediately at the earliest
        //       opportunity.
        if (m_respect_linebreaks && *m_iterator == '\n') {
            m_last_was_newline = true;
            if (auto result = try_commit_chunk(start_of_chunk, m_iterator, false); result.has_value()) {
                return result.release_value();
            }
        }

        if (m_wrap_lines) {
            bool is_space = is_ascii_space(*m_iterator);
            if (is_space != m_last_was_space) {
                m_last_was_space = is_space;
                if (auto result = try_commit_chunk(start_of_chunk, m_iterator, false); result.has_value()) {
                    return result.release_value();
                }
            }
        }
    }

    if (start_of_chunk != m_utf8_view.end()) {
        // Try to output whatever's left at the end of the text node.
        if (auto result = try_commit_chunk(start_of_chunk, m_utf8_view.end(), false, true); result.has_value())
            return result.release_value();
    }

    return {};
}

Optional<TextNode::Chunk> TextNode::ChunkIterator::try_commit_chunk(Utf8View::Iterator const& start, Utf8View::Iterator const& end, bool has_breaking_newline, bool must_commit)
{
    if (m_layout_mode == LayoutMode::OnlyRequiredLineBreaks && !must_commit)
        return {};

    auto byte_offset = m_utf8_view.byte_offset_of(start);
    auto byte_length = m_utf8_view.byte_offset_of(end) - byte_offset;

    if (byte_length > 0) {
        auto chunk_view = m_utf8_view.substring_view(byte_offset, byte_length);
        return Chunk {
            .view = chunk_view,
            .start = byte_offset,
            .length = byte_length,
            .has_breaking_newline = has_breaking_newline,
            .is_all_whitespace = is_all_whitespace(chunk_view.as_string()),
        };
    }

    return {};
}

void TextNode::paint(PaintContext&, PaintPhase)
{
}

}
