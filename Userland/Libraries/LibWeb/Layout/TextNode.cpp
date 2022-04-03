/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/StringBuilder.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/InlineFormattingContext.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Painting/TextPaintable.h>

namespace Web::Layout {

TextNode::TextNode(DOM::Document& document, DOM::Text& text)
    : Node(document, &text)
{
    set_inline(true);
}

TextNode::~TextNode() = default;

static bool is_all_whitespace(StringView string)
{
    for (size_t i = 0; i < string.length(); ++i) {
        if (!is_ascii_space(string[i]))
            return false;
    }
    return true;
}

// NOTE: This collapses whitespace into a single ASCII space if collapse is true.
void TextNode::compute_text_for_rendering(bool collapse)
{
    auto& data = dom_node().data();
    if (!collapse || data.is_empty()) {
        m_text_for_rendering = data;
        return;
    }

    // NOTE: A couple fast returns to avoid unnecessarily allocating a StringBuilder.
    if (data.length() == 1) {
        if (is_ascii_space(data[0])) {
            static String s_single_space_string = " ";
            m_text_for_rendering = s_single_space_string;
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

TextNode::ChunkIterator::ChunkIterator(StringView text, LayoutMode layout_mode, bool wrap_lines, bool respect_linebreaks)
    : m_layout_mode(layout_mode)
    , m_wrap_lines(wrap_lines)
    , m_respect_linebreaks(respect_linebreaks)
    , m_utf8_view(text)
    , m_iterator(m_utf8_view.begin())
{
}

Optional<TextNode::Chunk> TextNode::ChunkIterator::next()
{
    if (m_iterator == m_utf8_view.end())
        return {};

    auto start_of_chunk = m_iterator;

    while (m_iterator != m_utf8_view.end()) {
        if (m_respect_linebreaks && *m_iterator == '\n') {
            // Newline encountered, and we're supposed to preserve them.
            // If we have accumulated some code points in the current chunk, commit them now and continue with the newline next time.
            if (auto result = try_commit_chunk(start_of_chunk, m_iterator, false); result.has_value())
                return result.release_value();

            // Otherwise, commit the newline!
            ++m_iterator;
            auto result = try_commit_chunk(start_of_chunk, m_iterator, true, true);
            VERIFY(result.has_value());
            return result.release_value();
        }

        if (m_wrap_lines || m_layout_mode == LayoutMode::MinContent) {
            if (is_ascii_space(*m_iterator)) {
                // Whitespace encountered, and we're allowed to break on whitespace.
                // If we have accumulated some code points in the current chunk, commit them now and continue with the whitespace next time.
                if (auto result = try_commit_chunk(start_of_chunk, m_iterator, false); result.has_value())
                    return result.release_value();

                // Otherwise, commit the whitespace!
                ++m_iterator;
                if (auto result = try_commit_chunk(start_of_chunk, m_iterator, false); result.has_value())
                    return result.release_value();
                continue;
            }
        }

        ++m_iterator;
    }

    if (start_of_chunk != m_utf8_view.end()) {
        // Try to output whatever's left at the end of the text node.
        if (auto result = try_commit_chunk(start_of_chunk, m_utf8_view.end(), false, true); result.has_value())
            return result.release_value();
    }

    return {};
}

Optional<TextNode::Chunk> TextNode::ChunkIterator::try_commit_chunk(Utf8View::Iterator const& start, Utf8View::Iterator const& end, bool has_breaking_newline, bool must_commit) const
{
    if (m_layout_mode == LayoutMode::MaxContent && !must_commit)
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

RefPtr<Painting::Paintable> TextNode::create_paintable() const
{
    return Painting::TextPaintable::create(*this);
}

}
