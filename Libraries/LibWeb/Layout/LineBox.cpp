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

#include <AK/Utf8View.h>
#include <LibWeb/Layout/LayoutBox.h>
#include <LibWeb/Layout/LayoutNode.h>
#include <LibWeb/Layout/LayoutText.h>
#include <LibWeb/Layout/LineBox.h>
#include <ctype.h>

namespace Web {

void LineBox::add_fragment(const LayoutNode& layout_node, int start, int length, int width, int height)
{
    bool text_align_is_justify = layout_node.style().text_align() == CSS::TextAlign::Justify;
    if (!text_align_is_justify && !m_fragments.is_empty() && &m_fragments.last().layout_node() == &layout_node) {
        // The fragment we're adding is from the last LayoutNode on the line.
        // Expand the last fragment instead of adding a new one with the same LayoutNode.
        m_fragments.last().m_length = (start - m_fragments.last().m_start) + length;
        m_fragments.last().set_width(m_fragments.last().width() + width);
    } else {
        m_fragments.append(make<LineBoxFragment>(layout_node, start, length, Gfx::FloatPoint(m_width, 0.0f), Gfx::FloatSize(width, height)));
    }
    m_width += width;

    if (is<LayoutBox>(layout_node))
        const_cast<LayoutBox&>(downcast<LayoutBox>(layout_node)).set_containing_line_box_fragment(m_fragments.last());
}

void LineBox::trim_trailing_whitespace()
{
    while (!m_fragments.is_empty() && m_fragments.last().is_justifiable_whitespace()) {
        auto fragment = m_fragments.take_last();
        m_width -= fragment->width();
    }

    if (m_fragments.is_empty())
        return;

    auto last_text = m_fragments.last().text();
    if (last_text.is_null())
        return;
    auto& last_fragment = m_fragments.last();

    int space_width = last_fragment.layout_node().specified_style().font().glyph_width(' ');
    while (last_fragment.length() && isspace(last_text[last_fragment.length() - 1])) {
        last_fragment.m_length -= 1;
        last_fragment.set_width(last_fragment.width() - space_width);
        m_width -= space_width;
    }
}

bool LineBox::ends_in_whitespace() const
{
    if (m_fragments.is_empty())
        return false;
    return m_fragments.last().ends_in_whitespace();
}

}
