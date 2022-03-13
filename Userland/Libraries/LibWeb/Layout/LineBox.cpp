/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/TypeCasts.h>
#include <AK/Utf8View.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Layout/BreakNode.h>
#include <LibWeb/Layout/LineBox.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/TextNode.h>

namespace Web::Layout {

void LineBox::add_fragment(Node const& layout_node, int start, int length, float leading_size, float trailing_size, float leading_margin, float trailing_margin, float content_width, float content_height, float border_box_top, float border_box_bottom, LineBoxFragment::Type fragment_type)
{
    bool text_align_is_justify = layout_node.computed_values().text_align() == CSS::TextAlign::Justify;
    if (!text_align_is_justify && !m_fragments.is_empty() && &m_fragments.last().layout_node() == &layout_node) {
        // The fragment we're adding is from the last Layout::Node on the line.
        // Expand the last fragment instead of adding a new one with the same Layout::Node.
        m_fragments.last().m_length = (start - m_fragments.last().m_start) + length;
        m_fragments.last().set_width(m_fragments.last().width() + content_width);
    } else {
        float x_offset = leading_margin + leading_size + m_width;
        float y_offset = 0.0f;
        m_fragments.append(LineBoxFragment { layout_node, start, length, Gfx::FloatPoint(x_offset, y_offset), Gfx::FloatSize(content_width, content_height), border_box_top, border_box_bottom, fragment_type });
    }
    m_width += leading_margin + leading_size + content_width + trailing_size + trailing_margin;
}

void LineBox::trim_trailing_whitespace()
{
    while (!m_fragments.is_empty() && m_fragments.last().is_justifiable_whitespace()) {
        auto fragment = m_fragments.take_last();
        m_width -= fragment.width();
    }

    if (m_fragments.is_empty())
        return;

    auto& last_fragment = m_fragments.last();
    auto last_text = last_fragment.text();
    if (last_text.is_null())
        return;

    while (last_fragment.length()) {
        auto last_character = last_text[last_fragment.length() - 1];
        if (!is_ascii_space(last_character))
            break;

        int last_character_width = last_fragment.layout_node().font().glyph_width(last_character);
        last_fragment.m_length -= 1;
        last_fragment.set_width(last_fragment.width() - last_character_width);
        m_width -= last_character_width;
    }
}

bool LineBox::is_empty_or_ends_in_whitespace() const
{
    if (m_fragments.is_empty())
        return true;

    return m_fragments.last().ends_in_whitespace();
}

}
