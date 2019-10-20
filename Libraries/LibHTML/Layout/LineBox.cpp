#include <LibHTML/Layout/LayoutNode.h>
#include <LibHTML/Layout/LayoutText.h>
#include <LibHTML/Layout/LineBox.h>
#include <ctype.h>

void LineBox::add_fragment(const LayoutNode& layout_node, int start, int length, int width, int height)
{
    bool text_align_is_justify = layout_node.style().string_or_fallback(CSS::PropertyID::TextAlign, "left") == "justify";
    if (!text_align_is_justify && !m_fragments.is_empty() && &m_fragments.last().layout_node() == &layout_node) {
        // The fragment we're adding is from the last LayoutNode on the line.
        // Expand the last fragment instead of adding a new one with the same LayoutNode.
        m_fragments.last().m_length = (start - m_fragments.last().m_start) + length;
        m_fragments.last().m_rect.set_width(m_fragments.last().m_rect.width() + width);
    } else {
        m_fragments.empend(layout_node, start, length, FloatRect(m_width, 0, width, height));
    }
    m_width += width;
}

void LineBox::trim_trailing_whitespace()
{
    while (!m_fragments.is_empty() && m_fragments.last().is_justifiable_whitespace()) {
        auto fragment = m_fragments.take_last();
        m_width -= fragment.width();
    }

    if (m_fragments.is_empty())
        return;

    auto last_text = m_fragments.last().text();
    if (last_text.is_null())
        return;
    auto& last_fragment = m_fragments.last();

    int space_width = last_fragment.layout_node().style().font().glyph_width(' ');
    while (last_fragment.length() && isspace(last_text[last_fragment.length() - 1])) {
        last_fragment.m_length -= 1;
        last_fragment.m_rect.set_width(last_fragment.m_rect.width() - space_width);
        m_width -= space_width;
    }
}
